package javscraper.sidecar

import javscraper.io.logging.AppLogController
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest

class SidecarManagerLoggingTest {

    @Test
    fun `sidecar methods emit lifecycle and request logs`() = runTest {
        val logFile = createTempDirectory("javscraper-sidecar-logs").resolve("javscraper.log")
        val controller = AppLogController(logFile)
        val manager = SidecarManager("C:/definitely-missing/javscraper-worker.exe")

        try {
            manager.start()
            manager.close()

            listOf("list_sites", "check_sites", "scrape").forEach { operation ->
                try {
                    when (operation) {
                        "list_sites" -> manager.listSites()
                        "check_sites" -> manager.checkSites(listOf("javbus"))
                        else -> manager.scrape("SONE-001", enabledSites = listOf("javbus"))
                    }
                    fail("Expected worker-not-running failure")
                } catch (_: RuntimeException) {
                }
            }

            manager.close()
        } finally {
            manager.close()
            controller.dispose()
        }

        val loggerName = SidecarManager::class.qualifiedName
        val messages = controller.entries
            .filter { it.loggerName == loggerName }
            .joinToString("\n") { it.message }

        listOf(
            "Worker start:",
            "Worker close:",
            "Worker stop:",
            "list_sites request",
            "check_sites request",
            "scrape request",
            "JSON-RPC request",
            "Worker not running",
            "Response reader started",
            "Worker cleanup"
        ).forEach { marker ->
            assertTrue(marker in messages, "Missing log marker: $marker\n$messages")
        }
    }

    @Test
    fun `json rpc request and response bodies are logged`() = runBlocking {
        val workerScript = createTempFile("javscraper-fake-worker-", ".bat")
        workerScript.writeText(
            "@echo off\r\n" +
                "set /p request=\r\n" +
                "echo {\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"success\":true,\"data\":null}}\r\n"
        )
        val logFile = createTempDirectory("javscraper-json-logs").resolve("javscraper.log")
        val controller = AppLogController(logFile)
        val manager = SidecarManager(workerScript.toString())

        try {
            assertTrue(manager.start(), "Fake worker should start")
            val result = manager.scrape("SONE-001", enabledSites = listOf("javbus"))
            assertTrue(result.success, "Fake worker should return a successful scrape result")
        } finally {
            manager.close()
            controller.dispose()
        }

        val messages = controller.entries
            .filter { it.loggerName == SidecarManager::class.qualifiedName }
            .joinToString("\n") { it.message }

        val expectedRequestJson = """
            {
                "jsonrpc": "2.0",
                "id": "1",
                "method": "scrape",
                "params": {
                    "number": "SONE-001",
                    "sites": [
                        "javbus"
                    ]
                }
            }
        """.trimIndent()
        val expectedResponseJson = """
            {
                "jsonrpc": "2.0",
                "id": "1",
                "result": {
                    "success": true,
                    "data": null
                }
            }
        """.trimIndent()

        assertTrue(
            "JSON-RPC request body: $expectedRequestJson" in messages,
            "Missing formatted request JSON\n$messages"
        )
        assertTrue(
            "JSON-RPC response body: id=1, json=$expectedResponseJson" in messages,
            "Missing formatted response JSON\n$messages"
        )
    }
}
