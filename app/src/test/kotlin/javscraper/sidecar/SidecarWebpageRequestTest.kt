package javscraper.sidecar

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class SidecarWebpageRequestTest {
    private val pythonPath = Paths.get(
        System.getProperty("user.dir"),
        "..", "scraper-worker", "venv", "Scripts", "python.exe"
    ).toAbsolutePath().normalize() as Path

    @Test
    fun `scrape and search pass save webpage option`() = runBlocking {
        val directory = Files.createTempDirectory("javscraper-webpage-request-")
        val scriptPath = directory.resolve("worker.py")
        val requestPath = directory.resolve("requests.log")
        val batchPath = directory.resolve("worker.bat")
        scriptPath.toFile().writeText(
            """
            import json
            import sys

            for line in sys.stdin:
                request = json.loads(line)
                with open(r"${requestPath}", "a", encoding="utf-8") as output:
                    output.write(line.strip() + "\n")
                result = (
                    {"success": True, "data": {"number": "ABC-123"}}
                    if request["method"] == "scrape"
                    else [] if request["method"] == "check_sites"
                    else [{"number": "ABC-123"}]
                )
                response = {"jsonrpc": "2.0", "id": request["id"], "result": result}
                sys.stdout.write(json.dumps(response, separators=(",", ":")) + "\n")
                sys.stdout.flush()
            """.trimIndent()
        )
        batchPath.toFile().writeText("@echo off\r\n\"${pythonPath}\" \"$scriptPath\"\r\n")
        val manager = SidecarManager(batchPath.toString())

        try {
            assertTrue(manager.start(), "Fake worker should start")
            val mirrors = mapOf("javbus" to "https://www.dmmsee.casa")
            manager.scrape("ABC-123", saveWebpage = true, siteMirrors = mirrors)
            manager.searchCandidates("ABC-123", saveWebpage = true, siteMirrors = mirrors)
            manager.checkSites(listOf("javbus"), siteMirrors = mirrors)

            val requests = requestPath.toFile().readText()
            assertTrue(requests.contains("\"save_webpage\":true"), requests)
            assertTrue(requests.contains("\"site_mirrors\":{\"javbus\":\"https://www.dmmsee.casa\"}"), requests)
            assertTrue(requests.lines().count { it.contains("\"site_mirrors\"") } == 3, requests)
            assertTrue(requests.lines().count { it.contains("\"method\":\"scrape\"") } == 1, requests)
            assertTrue(requests.lines().count { it.contains("\"method\":\"search\"") } == 1, requests)
        } finally {
            manager.close()
            directory.toFile().deleteRecursively()
        }
    }
}
