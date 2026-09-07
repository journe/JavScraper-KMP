package javscraper.sidecar

import javscraper.io.logging.AppLogController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files

class SidecarManagerSearchCandidatesTest {

    @Test
    fun `search candidates decodes all videos`() = runBlocking {
        val workerScript = File.createTempFile("javscraper-search-worker-", ".bat")
        workerScript.writeText(
            "@echo off\r\n" +
                "set /p request=\r\n" +
                "echo {\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":[{\"number\":\"ABC-123\",\"title\":\"First\",\"cover_url\":\"https://example.com/1.jpg\"},{\"number\":\"ABC-123\",\"title\":\"Second\",\"cover_url\":\"https://example.com/2.jpg\"}]}\r\n"
        )
        val logDirectory = Files.createTempDirectory("javscraper-search-log-")
        val controller = AppLogController(logDirectory.resolve("javscraper.log"))
        val manager = SidecarManager(workerScript.absolutePath)

        try {
            assertTrue(manager.start(), "Fake worker should start")
            val candidates = manager.searchCandidates("ABC-123", enabledSites = listOf("mmtv"))

            assertEquals(listOf("First", "Second"), candidates.map { it.title })
            assertEquals(listOf("https://example.com/1.jpg", "https://example.com/2.jpg"), candidates.map { it.coverUrl })
        } finally {
            manager.close()
            controller.dispose()
            workerScript.delete()
        }
    }
}