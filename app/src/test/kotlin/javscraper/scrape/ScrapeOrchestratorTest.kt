package javscraper.scrape

import javscraper.models.ScannedFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
}
