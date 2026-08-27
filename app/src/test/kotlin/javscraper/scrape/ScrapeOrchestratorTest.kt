package javscraper.scrape

import javscraper.models.ScannedFile
import javscraper.models.Video
import javscraper.sidecar.SidecarManager
import kotlin.io.path.createTempDirectory
import kotlin.test.assertFalse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * Tests ScrapeOrchestrator's path computation logic.
 * Full integration tests with Python worker are in SidecarE2ETest.
 */
class ScrapeOrchestratorTest {

    @Test
    fun `ScannedFile creation`() {
        val sf = ScannedFile(path = "C:\\videos\\SONE-205.mp4", fileName = "SONE-205.mp4", number = "SONE-205")
        assertEquals("SONE-205", sf.number)
        assertEquals("SONE-205.mp4", sf.fileName)
        assertEquals("C:\\videos\\SONE-205.mp4", sf.path)
    }

    @Test
    fun `ScannedFile with empty number`() {
        val sf = ScannedFile(path = "C:\\videos\\unknown.mp4", fileName = "unknown.mp4")
        assertEquals("", sf.number)
    }

    @Test
    fun `ScannedFile creation with path only`() {
        val sf = ScannedFile(path = "/videos/test.mp4", fileName = "test.mp4")
        assertEquals("test.mp4", sf.fileName)
        assertEquals("", sf.number)
    }
    @Test
    fun `writeToDisk names shared NFO after formatted filename without part suffix`() {
        val output = createTempDirectory("javscraper-nfo-name").toFile()
        val source = output.resolve("ABC-001-cd1.mp4")
        source.writeText("video")
        val orchestrator = ScrapeOrchestrator(
            sidecar = SidecarManager("unused-worker.exe"),
            outputDir = output.absolutePath,
            createMovieFolders = true,
            folderLayers = emptyList(),
            downloadImages = false,
            filenameFormat = "{num} {title}{suffix}",
            suffixKeywords = listOf("-cd1")
        )

        runTest {
            val result = orchestrator.writeToDisk(
                listOf(ScannedFile(source.absolutePath, source.name, "ABC-001")),
                Video(number = "ABC-001", title = "Test")
            )

            assertTrue(result.success, result.error?.message ?: "writeToDisk failed")
            assertTrue(output.resolve("ABC-001 Test-cd1.mp4").isFile)
            assertTrue(output.resolve("ABC-001 Test.nfo").isFile)
            assertFalse(output.resolve(".nfo").isFile)
        }
    }
    @Test
    fun `writeToDisk reports failure when source file is missing`() {
        val output = createTempDirectory("javscraper-io-failure").toFile()
        val source = output.resolve("missing-source.mp4")
        val orchestrator = ScrapeOrchestrator(
            sidecar = SidecarManager("unused-worker.exe"),
            outputDir = output.absolutePath,
            downloadImages = false
        )

        runTest {
            val result = orchestrator.writeToDisk(
                listOf(ScannedFile(source.absolutePath, source.name, "SONE-001")),
                Video(number = "SONE-001", title = "Test")
            )
            assertFalse(result.success)
        }
    }
}
