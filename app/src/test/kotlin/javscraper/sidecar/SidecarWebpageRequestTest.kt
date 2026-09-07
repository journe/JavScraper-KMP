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
            manager.scrape("ABC-123", saveWebpage = true)
            manager.searchCandidates("ABC-123", saveWebpage = true)

            val requests = requestPath.toFile().readText()
            assertTrue(requests.contains("\"save_webpage\":true"), requests)
            assertTrue(requests.lines().count { it.contains("\"method\":\"scrape\"") } == 1, requests)
            assertTrue(requests.lines().count { it.contains("\"method\":\"search\"") } == 1, requests)
        } finally {
            manager.close()
            directory.toFile().deleteRecursively()
        }
    }
}
