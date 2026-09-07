package javscraper.scrape

import javscraper.models.ScannedFile
import javscraper.models.Video
import javscraper.models.WebpageArchiver
import javscraper.models.WebpageImageResult
import javscraper.sidecar.SidecarManager
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.test.assertFalse
import kotlin.test.assertNull
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
private class RecordingWebpageArchiver(
    private val result: WebpageImageResult = WebpageImageResult(true)
) : WebpageArchiver {
    var mhtmlPath: Path? = null
    var outputDir: Path? = null
    var video: Video? = null

    override suspend fun extractImages(
        mhtmlPath: Path,
        outputDir: Path,
        video: Video
    ): WebpageImageResult {
        this.mhtmlPath = mhtmlPath
        this.outputDir = outputDir
        this.video = video
        return result
    }
}

@Test
fun `writeToDisk saves webpage and extracts images from mhtml`() {
    val output = createTempDirectory("javscraper-webpage").toFile()
    val source = output.resolve("FC2-PPV-1723984.mp4")
    source.writeText("video")
    val archiver = RecordingWebpageArchiver()
    val orchestrator = ScrapeOrchestrator(
        sidecar = SidecarManager("unused-worker.exe"),
        outputDir = output.absolutePath,
        createMovieFolders = false,
        downloadImages = true,
        downloadWebPages = true,
        webpageArchiver = archiver
    )
    val video = Video(
        number = "FC2-PPV-1723984",
        title = "Test",
        source = "fc2",
        webpage = java.util.Base64.getEncoder().encodeToString("mhtml-content".toByteArray())
    )

    runTest {
        val result = orchestrator.writeToDisk(listOf(ScannedFile(source.absolutePath, source.name, video.number)), video)

        assertTrue(result.success, result.error?.message ?: "writeToDisk failed")
        val mhtml = output.resolve("FC2-PPV-1723984-FC2.mhtml")
        assertEquals("mhtml-content", mhtml.readText())
        assertEquals(mhtml.toPath(), archiver.mhtmlPath)
        assertEquals(output.toPath(), archiver.outputDir)
        assertEquals(video.number, archiver.video?.number)
    }
}

@Test
fun `writeToDisk omits webpage when setting is disabled`() {
    val output = createTempDirectory("javscraper-webpage-off").toFile()
    val source = output.resolve("ABC-001.mp4")
    source.writeText("video")
    val archiver = RecordingWebpageArchiver()
    val orchestrator = ScrapeOrchestrator(
        sidecar = SidecarManager("unused-worker.exe"),
        outputDir = output.absolutePath,
        createMovieFolders = false,
        downloadImages = false,
        downloadWebPages = false,
        webpageArchiver = archiver
    )

    runTest {
        val result = orchestrator.writeToDisk(
            listOf(ScannedFile(source.absolutePath, source.name, "ABC-001")),
            Video(number = "ABC-001", title = "Test", source = "javbus", webpage = "YXJjaGl2ZQ==")
        )

        assertTrue(result.success, result.error?.message ?: "writeToDisk failed")
        assertFalse(output.resolve("ABC-001-JAVBUS.mhtml").exists())
        assertNull(archiver.mhtmlPath)
    }
}

@Test
fun `writeToDisk reports missing webpage and image extraction failures`() {
    val output = createTempDirectory("javscraper-webpage-error").toFile()
    val source = output.resolve("ABC-002.mp4")
    source.writeText("video")
    val archiver = RecordingWebpageArchiver(WebpageImageResult(false, "missing image"))
    val orchestrator = ScrapeOrchestrator(
        sidecar = SidecarManager("unused-worker.exe"),
        outputDir = output.absolutePath,
        createMovieFolders = false,
        downloadImages = true,
        downloadWebPages = true,
        webpageArchiver = archiver
    )

    runTest {
        val missing = orchestrator.writeToDisk(
            listOf(ScannedFile(source.absolutePath, source.name, "ABC-002")),
            Video(number = "ABC-002", title = "Test", source = "javbus")
        )
        assertFalse(missing.success)
        assertTrue(missing.error?.message?.contains("Webpage content missing") == true)

        val extractionFailed = orchestrator.writeToDisk(
            listOf(ScannedFile(source.absolutePath, source.name, "ABC-002")),
            Video(number = "ABC-002", title = "Test", source = "javbus", webpage = "YXJjaGl2ZQ==")
        )
        assertFalse(extractionFailed.success)
        assertTrue(extractionFailed.error?.message?.contains("missing image") == true)
    }
}}
