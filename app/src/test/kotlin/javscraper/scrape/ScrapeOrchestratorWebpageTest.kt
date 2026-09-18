package javscraper.scrape

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.file.Path
import javscraper.models.ScannedFile
import javscraper.models.Video
import javscraper.models.WebpageArchiver
import javscraper.models.WebpageImageResult
import javscraper.settings.AppSettings
import javscraper.sidecar.SidecarManager
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ScrapeOrchestratorWebpageTest {
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
        options = ScrapeOptions.from(AppSettings(
            outputDir = output.absolutePath,
            createMovieFolders = false,
            downloadImages = true,
            downloadWebPages = true,
        )),
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
        options = ScrapeOptions.from(AppSettings(
            outputDir = output.absolutePath,
            createMovieFolders = false,
            downloadImages = false,
            downloadWebPages = false,
        )),
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
        options = ScrapeOptions.from(AppSettings(
            outputDir = output.absolutePath,
            createMovieFolders = false,
            downloadImages = true,
            downloadWebPages = true,
        )),
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
}}

@Test
fun `writeToDisk downloads preview images from network when enabled`() = runTest {
    val output = createTempDirectory("javscraper-preview-on").toFile()
    val source = output.resolve("ABC-003.mp4")
    source.writeText("video")
    val archiver = RecordingWebpageArchiver()
    val (server, baseUrl) = startImageServer()
    try {
        val orchestrator = ScrapeOrchestrator(
            sidecar = SidecarManager("unused-worker.exe"),
            options = ScrapeOptions.from(AppSettings(
                outputDir = output.absolutePath,
                createMovieFolders = false,
                downloadImages = true,
                downloadPreviewImages = true,
                downloadWebPages = true,
            )),
            webpageArchiver = archiver
        )
        val video = Video(
            number = "ABC-003",
            source = "fc2",
            webpage = java.util.Base64.getEncoder().encodeToString("mhtml-content".toByteArray()),
            title = "Test",
            coverUrl = "$baseUrl/cover.jpg",
            sampleImages = listOf("$baseUrl/sample.jpg")
        )

        val result = orchestrator.writeToDisk(
            listOf(ScannedFile(source.absolutePath, source.name, video.number)),
            video
        )

        assertTrue(result.success, result.error?.message ?: "writeToDisk failed")
        assertEquals("sample-bytes", output.resolve("extrafanart").resolve("fanart1.jpg").readText())
        assertEquals(emptyList(), archiver.video?.sampleImages)
    } finally {
        server.stop(0)
    }
}

@Test
fun `writeToDisk omits preview images when disabled`() = runTest {
    val output = createTempDirectory("javscraper-preview-off").toFile()
    val source = output.resolve("ABC-004.mp4")
    source.writeText("video")
    val archiver = RecordingWebpageArchiver()
    val orchestrator = ScrapeOrchestrator(
        sidecar = SidecarManager("unused-worker.exe"),
        options = ScrapeOptions.from(AppSettings(
            outputDir = output.absolutePath,
            createMovieFolders = false,
            downloadImages = true,
            downloadPreviewImages = false,
            downloadWebPages = true,
        )),
        webpageArchiver = archiver
    )
    val video = Video(
        number = "ABC-004",
        title = "Test",
        source = "javbus",
        sampleImages = listOf("https://example.invalid/sample.jpg"),
        webpage = java.util.Base64.getEncoder().encodeToString("mhtml-content".toByteArray())
    )

    val result = orchestrator.writeToDisk(
        listOf(ScannedFile(source.absolutePath, source.name, video.number)),
        video
    )

    assertTrue(result.success, result.error?.message ?: "writeToDisk failed")
    assertEquals(emptyList(), archiver.video?.sampleImages)
    assertFalse(output.resolve("extrafanart").exists())
}

private fun startImageServer(): Pair<HttpServer, String> {
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/") { exchange ->
        val content = when (exchange.requestURI.path) {
            "/cover.jpg" -> "cover-bytes".toByteArray()
            "/sample.jpg" -> "sample-bytes".toByteArray()
            else -> byteArrayOf()
        }
        exchange.sendResponseHeaders(200, content.size.toLong())
        exchange.responseBody.use { output -> output.write(content) }
    }
    server.start()
    return server to "http://127.0.0.1:${server.address.port}"
}
}
