package javscraper.sidecar

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.runBlocking

class SidecarStageErrorsTest {
    private val pythonPath = Paths.get(
        System.getProperty("user.dir"),
        "..", "scraper-worker", "venv", "Scripts", "python.exe"
    ).toAbsolutePath().normalize() as Path

    @Test
    fun `json rpc error preserves scrape stage`() = runBlocking {
        val directory = Files.createTempDirectory("javscraper-stage-error-")
        val scriptPath = directory.resolve("worker.py")
        val batchPath = directory.resolve("worker.bat")
        scriptPath.toFile().writeText(
            """
            import json
            import sys

            for line in sys.stdin:
                request = json.loads(line)
                response = {
                    "jsonrpc": "2.0",
                    "id": request["id"],
                    "error": {
                        "code": -31,
                        "message": "Saving webpage timed out",
                        "data": {
                            "stage": "webpage_archive",
                            "site_id": "mmtv",
                            "detail_url": "https://example.test/video.html"
                        }
                    }
                }
                sys.stdout.write(json.dumps(response, separators=(",", ":")) + "\n")
                sys.stdout.flush()
            """.trimIndent()
        )
        batchPath.toFile().writeText("@echo off\r\n\"${pythonPath}\" \"$scriptPath\"\r\n")
        val manager = SidecarManager(batchPath.toString())

        try {
            assertTrue(manager.start(), "Fake worker should start")
            try {
                manager.searchCandidates("ABC-123", saveWebpage = true)
                fail("Expected SidecarRequestException")
            } catch (e: SidecarRequestException) {
                assertEquals(-31, e.code, e.message)
                assertEquals("webpage_archive", e.stage, e.message)
                assertEquals("mmtv", e.siteId, e.message)
                assertEquals("https://example.test/video.html", e.detailUrl, e.message)
            }
        } finally {
            manager.close()
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `response timeout carries latest scrape progress stage`() = runBlocking {
        val directory = Files.createTempDirectory("javscraper-stage-timeout-")
        val scriptPath = directory.resolve("worker.py")
        val batchPath = directory.resolve("worker.bat")
        scriptPath.toFile().writeText(
            """
            import json
            import sys
            import time

            for line in sys.stdin:
                request = json.loads(line)
                progress = {
                    "jsonrpc": "2.0",
                    "method": "scrape.progress",
                    "params": {
                        "request_id": request["id"],
                        "stage": "webpage_archive",
                        "status": "start",
                        "site_id": "mmtv",
                        "number": request["params"]["number"]
                    }
                }
                sys.stdout.write(json.dumps(progress, separators=(",", ":")) + "\n")
                sys.stdout.flush()
                time.sleep(2)
            """.trimIndent()
        )
        batchPath.toFile().writeText("@echo off\r\n\"${pythonPath}\" \"$scriptPath\"\r\n")
        val manager = SidecarManager(batchPath.toString(), requestTimeoutMs = 300)

        try {
            assertTrue(manager.start(), "Fake worker should start")
            try {
                manager.searchCandidates("ABC-123", saveWebpage = true)
                fail("Expected SidecarTimeoutException")
            } catch (e: SidecarTimeoutException) {
                assertEquals("webpage_archive", e.stage, e.message)
                assertTrue(e.message?.contains("latestStage=webpage_archive") == true, e.message)
            }
        } finally {
            manager.close()
            directory.toFile().deleteRecursively()
        }
    }
}