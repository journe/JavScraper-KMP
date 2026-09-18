package javscraper.scrape

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger
import javscraper.models.ScannedFile
import javscraper.models.Video
import javscraper.settings.AppSettings
import javscraper.sidecar.SidecarManager
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ScrapeOrchestratorUpdateModeTest {
    @Test
    fun `update mode moves the whole folder and reuses existing artwork`() = runTest {
        val output = createTempDirectory("javscraper-update-output").toFile()
        val oldFolder = output.resolve("OLD-001")
        oldFolder.mkdirs()
        val source = oldFolder.resolve("OLD-001.mp4")
        source.writeText("video")
        oldFolder.resolve("OLD-001.nfo").writeText(
            """
                <?xml version="1.0" encoding="UTF-8"?>
                <movie>
                  <title>Old Title</title>
                  <num>OLD-001</num>
                  <plot>Keep summary</plot>
                  <customfield>custom</customfield>
                </movie>
            """.trimIndent()
        )
        oldFolder.resolve("fanart.jpg").writeText("old-fanart")
        oldFolder.resolve("poster.jpg").writeText("old-poster")
        oldFolder.resolve("extra.srt").writeText("subtitle")
        val orchestrator = ScrapeOrchestrator(
            sidecar = SidecarManager("unused-worker.exe"),
            options = ScrapeOptions.from(
                AppSettings(
                    outputDir = output.absolutePath,
                    createMovieFolders = true,
                    moveInsteadOfCopy = false,
                    downloadImages = true,
                    downloadWebPages = false,
                    updateMode = true,
                    folderLayers = listOf("{num} {title}"),
                    filenameFormat = "{num} {title}"
                )
            )
        )

        val result = orchestrator.writeSingleScrapeToDisk(
            listOf(ScannedFile(source.absolutePath, source.name, "NEW-001")),
            Video(
                number = "NEW-001",
                title = "New Title",
                summary = "Keep summary",
                coverUrl = "https://example.invalid/cover.jpg"
            )
        )

        assertTrue(result.success, result.error?.message ?: "writeToDisk failed")
        val newFolder = output.resolve("NEW-001 New Title")
        assertFalse(oldFolder.exists())
        assertEquals(newFolder.resolve("OLD-001.mp4").absolutePath, result.data?.path)
        assertEquals("video", newFolder.resolve("OLD-001.mp4").readText())
        assertEquals("old-fanart", newFolder.resolve("fanart.jpg").readText())
        assertEquals("old-poster", newFolder.resolve("poster.jpg").readText())
        assertEquals("subtitle", newFolder.resolve("extra.srt").readText())
        val updatedNfo = newFolder.resolve("OLD-001.nfo").readText()
        assertTrue(updatedNfo.contains("<title>New Title</title>"))
        assertTrue(updatedNfo.contains("<num>NEW-001</num>"))
        assertTrue(updatedNfo.contains("<plot>Keep summary</plot>"))
        assertTrue(updatedNfo.contains("<customfield>custom</customfield>"))
    }

    @Test
    fun `update mode stays in scan directory and downloads missing previews`() = runTest {
        val scanDir = createTempDirectory("javscraper-update-scan").toFile()
        val outputDir = createTempDirectory("javscraper-update-output").toFile()
        val oldFolder = scanDir.resolve("Studio").resolve("OLD-001")
        oldFolder.mkdirs()
        val source = oldFolder.resolve("OLD-001.mp4")
        source.writeText("video")
        oldFolder.resolve("OLD-001.nfo").writeText("<movie><title>Old</title><num>OLD-001</num></movie>")
        oldFolder.resolve("fanart.jpg").writeText("old-fanart")
        oldFolder.resolve("poster.jpg").writeText("old-poster")
        val (server, baseUrl) = startImageServer()
        val orchestrator = ScrapeOrchestrator(
            sidecar = SidecarManager("unused-worker.exe"),
            options = ScrapeOptions.from(
                AppSettings(
                    outputDir = outputDir.absolutePath,
                    scanDir = scanDir.absolutePath,
                    createMovieFolders = true,
                    downloadImages = true,
                    downloadPreviewImages = true,
                    downloadWebPages = false,
                    updateMode = true,
                    folderLayers = listOf("{num} {title}"),
                    filenameFormat = "{num} {title}"
                )
            )
        )

        try {
            val result = orchestrator.writeSingleScrapeToDisk(
                listOf(ScannedFile(source.absolutePath, source.name, "NEW-001")),
                Video(
                    number = "NEW-001",
                    title = "New Title",
                    sampleImages = listOf("$baseUrl/sample.jpg")
                )
            )

            assertTrue(result.success, result.error?.message ?: "writeToDisk failed")
            val newFolder = scanDir.resolve("NEW-001 New Title")
            assertFalse(oldFolder.exists())
            assertEquals(newFolder.resolve("OLD-001.mp4").absolutePath, result.data?.path)
            assertEquals("old-fanart", newFolder.resolve("fanart.jpg").readText())
            assertEquals("old-poster", newFolder.resolve("poster.jpg").readText())
            assertEquals(
                "sample-bytes",
                newFolder.resolve("extrafanart").resolve("fanart1.jpg").readText()
            )
            assertEquals(0, outputDir.listFiles()?.size)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `update mode keeps existing previews without network requests`() = runTest {
        val scanDir = createTempDirectory("javscraper-preview-scan").toFile()
        val outputDir = createTempDirectory("javscraper-preview-output").toFile()
        val oldFolder = scanDir.resolve("OLD-001")
        oldFolder.mkdirs()
        val source = oldFolder.resolve("OLD-001.mp4")
        source.writeText("video")
        oldFolder.resolve("OLD-001.nfo").writeText("<movie><title>Old</title><num>OLD-001</num></movie>")
        oldFolder.resolve("fanart.jpg").writeText("old-fanart")
        oldFolder.resolve("poster.jpg").writeText("old-poster")
        val previews = oldFolder.resolve("extrafanart")
        previews.mkdirs()
        previews.resolve("fanart1.jpg").writeText("old-preview")
        val requests = AtomicInteger()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/sample.jpg") { exchange ->
            requests.incrementAndGet()
            val content = "sample-bytes".toByteArray()
            exchange.sendResponseHeaders(200, content.size.toLong())
            exchange.responseBody.use { it.write(content) }
        }
        server.start()
        val orchestrator = ScrapeOrchestrator(
            sidecar = SidecarManager("unused-worker.exe"),
            options = ScrapeOptions.from(
                AppSettings(
                    scanDir = scanDir.absolutePath,
                    outputDir = outputDir.absolutePath,
                    createMovieFolders = true,
                    downloadImages = true,
                    downloadPreviewImages = true,
                    updateMode = true,
                    folderLayers = listOf("{num} {title}")
                )
            )
        )

        try {
            val result = orchestrator.writeSingleScrapeToDisk(
                listOf(ScannedFile(source.absolutePath, source.name, "NEW-001")),
                Video(
                    number = "NEW-001",
                    title = "New Title",
                    sampleImages = listOf("http://127.0.0.1:${server.address.port}/sample.jpg")
                )
            )

            assertTrue(result.success, result.error?.message ?: "writeToDisk failed")
            val newFolder = scanDir.resolve("NEW-001 New Title")
            assertEquals("old-preview", newFolder.resolve("extrafanart").resolve("fanart1.jpg").readText())
            assertEquals(0, requests.get())
            assertEquals(0, outputDir.listFiles()?.size)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `update mode option keeps batch write behavior`() = runTest {
        val output = createTempDirectory("javscraper-batch-update").toFile()
        val oldFolder = output.resolve("OLD-001")
        oldFolder.mkdirs()
        val source = oldFolder.resolve("OLD-001.mp4")
        source.writeText("video")
        oldFolder.resolve("OLD-001.nfo").writeText("<movie><title>Old</title></movie>")
        val orchestrator = ScrapeOrchestrator(
            sidecar = SidecarManager("unused-worker.exe"),
            options = ScrapeOptions.from(
                AppSettings(
                    outputDir = output.absolutePath,
                    createMovieFolders = true,
                    downloadImages = false,
                    updateMode = true,
                    folderLayers = listOf("{num} {title}"),
                    filenameFormat = "{num} {title}"
                )
            )
        )

        val result = orchestrator.writeToDisk(
            listOf(ScannedFile(source.absolutePath, source.name, "NEW-001")),
            Video(number = "NEW-001", title = "New Title")
        )

        assertTrue(result.success, result.error?.message ?: "writeToDisk failed")
        assertTrue(oldFolder.resolve("OLD-001.nfo").isFile)
        val newFolder = output.resolve("NEW-001 New Title")
        assertTrue(newFolder.resolve("NEW-001 New Title.mp4").isFile)
        assertTrue(newFolder.resolve("NEW-001 New Title.nfo").isFile)
    }
}

private fun startImageServer(): Pair<HttpServer, String> {
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/sample.jpg") { exchange ->
        val content = "sample-bytes".toByteArray()
        exchange.sendResponseHeaders(200, content.size.toLong())
        exchange.responseBody.use { it.write(content) }
    }
    server.start()
    return server to "http://127.0.0.1:${server.address.port}"
}
