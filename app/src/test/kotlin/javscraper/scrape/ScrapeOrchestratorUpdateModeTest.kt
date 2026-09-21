package javscraper.scrape

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger
import javscraper.models.ScannedFile
import javscraper.models.Video
import javscraper.models.VideoUpdateField
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
    fun `update mode reuses same-name fanart and poster variants without download`() = runTest {
        val scanDir = createTempDirectory("javscraper-update-variant").toFile()
        val oldFolder = scanDir.resolve("OLD-001")
        oldFolder.mkdirs()
        val source = oldFolder.resolve("FC2-3264420.mp4")
        source.writeText("video")
        oldFolder.resolve("FC2-3264420.nfo").writeText(
            "<movie><title>Old</title><num>FC2-3264420</num></movie>"
        )
        // 同名变体命名，而非通用 fanart.jpg/poster.jpg
        oldFolder.resolve("FC2-3264420-fanart.jpg").writeText("old-fanart")
        oldFolder.resolve("FC2-3264420-poster.jpg").writeText("old-poster")
        val requests = AtomicInteger()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/cover.jpg") { exchange ->
            requests.incrementAndGet()
            val content = "downloaded-cover".toByteArray()
            exchange.sendResponseHeaders(200, content.size.toLong())
            exchange.responseBody.use { it.write(content) }
        }
        server.start()
        val orchestrator = ScrapeOrchestrator(
            sidecar = SidecarManager("unused-worker.exe"),
            options = ScrapeOptions.from(
                AppSettings(
                    scanDir = scanDir.absolutePath,
                    createMovieFolders = true,
                    downloadImages = true,
                    updateMode = true,
                    folderLayers = listOf("{num} {title}")
                )
            )
        )

        try {
            val result = orchestrator.writeSingleScrapeToDisk(
                listOf(ScannedFile(source.absolutePath, source.name, "FC2-3264420")),
                Video(
                    number = "FC2-3264420",
                    title = "New Title",
                    coverUrl = "http://127.0.0.1:${server.address.port}/cover.jpg"
                )
            )

            assertTrue(result.success, result.error?.message ?: "writeToDisk failed")
            val newFolder = scanDir.resolve("FC2-3264420 New Title")
            assertEquals("old-fanart", newFolder.resolve("FC2-3264420-fanart.jpg").readText())
            assertEquals("old-poster", newFolder.resolve("FC2-3264420-poster.jpg").readText())
            assertEquals(0, requests.get())
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `update mode without nfo falls back to standard write in scan dir`() = runTest {
        val scanDir = createTempDirectory("javscraper-update-no-nfo").toFile()
        val oldFolder = scanDir.resolve("OLD-001")
        oldFolder.mkdirs()
        val source = oldFolder.resolve("OLD-001.mp4")
        source.writeText("video")
        val orchestrator = ScrapeOrchestrator(
            sidecar = SidecarManager("unused-worker.exe"),
            options = ScrapeOptions.from(
                AppSettings(
                    scanDir = scanDir.absolutePath,
                    createMovieFolders = true,
                    downloadImages = false,
                    updateMode = true,
                    folderLayers = listOf("{num} {title}"),
                    filenameFormat = "{num} {title}"
                )
            )
        )

        val result = orchestrator.writeSingleScrapeToDisk(
            listOf(ScannedFile(source.absolutePath, source.name, "NEW-001")),
            Video(number = "NEW-001", title = "New Title")
        )

        assertTrue(result.success, result.error?.message ?: "writeToDisk failed")
        // 标准写盘流程：目标目录基于扫描目录，视频重命名并移动，NFO 全新生成
        val newFolder = scanDir.resolve("NEW-001 New Title")
        assertTrue(newFolder.resolve("NEW-001 New Title.mp4").isFile)
        assertTrue(newFolder.resolve("NEW-001 New Title.nfo").isFile)
        assertFalse(oldFolder.resolve("OLD-001.mp4").exists())
        // 旧文件夹已空则被清掉；否则允许残留空目录之外无视频文件
        assertTrue(
            !oldFolder.exists() ||
                oldFolder.listFiles().orEmpty().none { it.isFile }
        )
        assertEquals(newFolder.resolve("NEW-001 New Title.mp4").absolutePath, result.data?.path)
    }

    @Test
    fun `update mode without nfo writes into scan dir root when source file is directly inside`() = runTest {
        // 复现用户场景：扫描目录 H:\fc2，视频直接放在扫描目录根部（无子文件夹、无 NFO）。
        // 回退基准必须取设置的扫描目录本身，而不是源文件夹的父目录（那会是盘符根）。
        val scanDir = createTempDirectory("javscraper-update-no-nfo-root").toFile()
        val source = scanDir.resolve("FC2-1234567.mp4")
        source.writeText("video")
        val orchestrator = ScrapeOrchestrator(
            sidecar = SidecarManager("unused-worker.exe"),
            options = ScrapeOptions.from(
                AppSettings(
                    scanDir = scanDir.absolutePath,
                    createMovieFolders = true,
                    downloadImages = false,
                    updateMode = true,
                    folderLayers = listOf("{num} {title}"),
                    filenameFormat = "{num} {title}"
                )
            )
        )

        val result = orchestrator.writeSingleScrapeToDisk(
            listOf(ScannedFile(source.absolutePath, source.name, "FC2-1234567")),
            Video(number = "FC2-1234567", title = "New Title")
        )

        assertTrue(result.success, result.error?.message ?: "writeToDisk failed")
        val newFolder = scanDir.resolve("FC2-1234567 New Title")
        assertTrue(newFolder.resolve("FC2-1234567 New Title.mp4").isFile)
        assertTrue(newFolder.resolve("FC2-1234567 New Title.nfo").isFile)
        assertFalse(source.exists())
        assertEquals(newFolder.resolve("FC2-1234567 New Title.mp4").absolutePath, result.data?.path)
    }

    @Test
    fun `update mode moves the whole folder and reuses existing artwork`() = runTest {
        val output = createTempDirectory("javscraper-update-output").toFile()
        val oldFolder = output.resolve("OLD-001")
        oldFolder.mkdirs()
        val source = oldFolder.resolve("OLD-001.mp4")
        source.writeText("video")
        val oldTags = listOf(
            "AVC1", "1080P", "IPZZ", "桃乃木香奈", "口交", "剧情", "窈窕",
            "巨乳", "美少女", "中文字幕", "有码", "片商: S级素人", "发行: ティッシュ"
        )
        val newTags = listOf(
            "高画质", "DMM独家", "口交", "美少女", "巨乳", "戏剧", "苗条", "单体作品", "有码"
        )
        val expectedTags = (oldTags + newTags).distinct()
        val oldTagLines = oldTags.joinToString("\n") { "  <tag>$it</tag>" }
        oldFolder.resolve("OLD-001.nfo").writeText(
            """
                <?xml version="1.0" encoding="UTF-8"?>
                <movie>
                  <title>Old Title</title>
                  <num>OLD-001</num>
                  <plot>Keep summary</plot>
                  <genre>Old Genre</genre>
                  OLD_TAG_LINES
                  <customfield>custom</customfield>
                </movie>
            """.trimIndent().replace("OLD_TAG_LINES", oldTagLines)
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
                tags = newTags,
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
        assertTrue(updatedNfo.contains("<genre>Old Genre</genre>"))
        expectedTags.forEach { tag -> assertTrue(updatedNfo.contains("<tag>$tag</tag>"), tag) }
        assertEquals(expectedTags.size, Regex("<tag>[^<]+</tag>").findAll(updatedNfo).count())
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
                    source = "javdb",
                    posterUrl = "https://example.invalid/poster.jpg",
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
            val updatedNfo = newFolder.resolve("OLD-001.nfo").readText()
            assertTrue(updatedNfo.contains("<source>javdb</source>"))
            assertTrue(updatedNfo.contains("<fanart>"))
            assertTrue(updatedNfo.contains("<thumb>$baseUrl/sample.jpg</thumb>"))
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
    fun `update mode preserves edited poster without fanart`() = runTest {
        val scanDir = createTempDirectory("javscraper-edited-poster").toFile()
        val outputDir = createTempDirectory("javscraper-edited-output").toFile()
        val oldFolder = scanDir.resolve("OLD-001")
        oldFolder.mkdirs()
        val source = oldFolder.resolve("OLD-001.mp4")
        source.writeText("video")
        oldFolder.resolve("OLD-001.nfo").writeText("<movie><title>Old</title><num>OLD-001</num></movie>")
        oldFolder.resolve("poster.jpg").writeText("edited-poster")
        val requests = AtomicInteger()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/cover.jpg") { exchange ->
            requests.incrementAndGet()
            val content = "downloaded-cover".toByteArray()
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
                    downloadWebPages = false,
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
                    coverUrl = "http://127.0.0.1:${server.address.port}/cover.jpg"
                )
            )

            assertTrue(result.success, result.error?.message ?: "writeToDisk failed")
            val newFolder = scanDir.resolve("NEW-001 New Title")
            // fanart 缺失 → 重新下载封面（fanart）；poster 保持用户编辑版不被覆盖
            assertEquals("edited-poster", newFolder.resolve("poster.jpg").readText())
            assertEquals("downloaded-cover", newFolder.resolve("fanart.jpg").readText())
            assertEquals(1, requests.get())
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `update mode fills poster copy from fanart without network`() = runTest {
        val scanDir = createTempDirectory("javscraper-update-poster-copy").toFile()
        val oldFolder = scanDir.resolve("OLD-001")
        oldFolder.mkdirs()
        val source = oldFolder.resolve("OLD-001.mp4")
        source.writeText("video")
        oldFolder.resolve("OLD-001.nfo").writeText("<movie><title>Old</title><num>OLD-001</num></movie>")
        // 只有 fanart（封面本体），poster 缺失
        oldFolder.resolve("fanart.jpg").writeText("old-fanart")
        val requests = AtomicInteger()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/cover.jpg") { exchange ->
            requests.incrementAndGet()
            val content = "downloaded-cover".toByteArray()
            exchange.sendResponseHeaders(200, content.size.toLong())
            exchange.responseBody.use { it.write(content) }
        }
        server.start()
        val orchestrator = ScrapeOrchestrator(
            sidecar = SidecarManager("unused-worker.exe"),
            options = ScrapeOptions.from(
                AppSettings(
                    scanDir = scanDir.absolutePath,
                    createMovieFolders = true,
                    downloadImages = true,
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
                    coverUrl = "http://127.0.0.1:${server.address.port}/cover.jpg"
                )
            )

            assertTrue(result.success, result.error?.message ?: "writeToDisk failed")
            val newFolder = scanDir.resolve("NEW-001 New Title")
            // fanart 复用（不联网），poster 由 fanart 本地补副本
            assertEquals("old-fanart", newFolder.resolve("fanart.jpg").readText())
            assertEquals("old-fanart", newFolder.resolve("poster.jpg").readText())
            assertEquals(0, requests.get())
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `update mode downloads only fanart when poster is user edited`() = runTest {
        val scanDir = createTempDirectory("javscraper-update-fanart-only").toFile()
        val oldFolder = scanDir.resolve("OLD-001")
        oldFolder.mkdirs()
        val source = oldFolder.resolve("OLD-001.mp4")
        source.writeText("video")
        oldFolder.resolve("OLD-001.nfo").writeText("<movie><title>Old</title><num>OLD-001</num></movie>")
        // 只有用户编辑过的 poster，fanart 缺失
        oldFolder.resolve("poster.jpg").writeText("edited-poster")
        val requests = AtomicInteger()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/cover.jpg") { exchange ->
            requests.incrementAndGet()
            val content = "downloaded-cover".toByteArray()
            exchange.sendResponseHeaders(200, content.size.toLong())
            exchange.responseBody.use { it.write(content) }
        }
        server.start()
        val orchestrator = ScrapeOrchestrator(
            sidecar = SidecarManager("unused-worker.exe"),
            options = ScrapeOptions.from(
                AppSettings(
                    scanDir = scanDir.absolutePath,
                    createMovieFolders = true,
                    downloadImages = true,
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
                    coverUrl = "http://127.0.0.1:${server.address.port}/cover.jpg"
                )
            )

            assertTrue(result.success, result.error?.message ?: "writeToDisk failed")
            val newFolder = scanDir.resolve("NEW-001 New Title")
            // 只下载 fanart；poster 保持用户编辑版不被覆盖
            assertEquals("downloaded-cover", newFolder.resolve("fanart.jpg").readText())
            assertEquals("edited-poster", newFolder.resolve("poster.jpg").readText())
            assertEquals(1, requests.get())
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `update mode field mask preserves unselected fields`() = runTest {
        val scanDir = createTempDirectory("javscraper-field-mask").toFile()
        val oldFolder = scanDir.resolve("OLD-001")
        oldFolder.mkdirs()
        val source = oldFolder.resolve("OLD-001.mp4")
        source.writeText("video")
        oldFolder.resolve("OLD-001.nfo").writeText(
            """
                <?xml version="1.0" encoding="UTF-8"?>
                <movie>
                  <title>Old Title</title>
                  <num>OLD-001</num>
                  <plot>Old plot</plot>
                  <genre>Old genre</genre>
                  <tag>Old tag</tag>
                  <actor><name>Old actor</name></actor>
                </movie>
            """.trimIndent()
        )
        oldFolder.resolve("poster.jpg").writeText("old-poster")
        val orchestrator = ScrapeOrchestrator(
            sidecar = SidecarManager("unused-worker.exe"),
            options = ScrapeOptions.from(
                AppSettings(
                    scanDir = scanDir.absolutePath,
                    createMovieFolders = true,
                    downloadImages = false,
                    updateMode = true,
                    folderLayers = listOf("{num} {title}"),
                    filenameFormat = "{num} {title}"
                )
            )
        )

        val result = orchestrator.writeSingleScrapeToDisk(
            files = listOf(ScannedFile(source.absolutePath, source.name, "NEW-001")),
            video = Video(
                number = "NEW-001",
                title = "Old Title",
                summary = "New plot",
                tags = listOf("New tag"),
                actresses = listOf("New actor")
            ),
            updateFields = setOf(VideoUpdateField.NUMBER, VideoUpdateField.SUMMARY)
        )

        assertTrue(result.success, result.error?.message ?: "writeToDisk failed")
        val nfo = scanDir.resolve("NEW-001 Old Title").resolve("OLD-001.nfo").readText()
        assertTrue(nfo.contains("<num>NEW-001</num>"))
        assertTrue(nfo.contains("<plot>New plot</plot>"))
        assertTrue(nfo.contains("<title>Old Title</title>"))
        assertTrue(nfo.contains("<genre>Old genre</genre>"))
        assertTrue(nfo.contains("<tag>Old tag</tag>"))
        assertTrue(nfo.contains("<name>Old actor</name>"))
    }

    @Test
    fun `update mode rejects invalid nfo before moving the folder`() = runTest {
        val scanDir = createTempDirectory("javscraper-update-invalid").toFile()
        val oldFolder = scanDir.resolve("OLD-001")
        oldFolder.mkdirs()
        val source = oldFolder.resolve("OLD-001.mp4")
        source.writeText("video")
        // 模拟用户遇到的场景：文本节点含裸 "<"（第 5 行）
        oldFolder.resolve("OLD-001.nfo").writeText(
            """
                <?xml version="1.0" encoding="UTF-8"?>
                <movie>
                  <title>Old Title</title>
                  <tag>年龄 <18 禁止观看</tag>
                </movie>
            """.trimIndent()
        )
        val orchestrator = ScrapeOrchestrator(
            sidecar = SidecarManager("unused-worker.exe"),
            options = ScrapeOptions.from(
                AppSettings(
                    scanDir = scanDir.absolutePath,
                    createMovieFolders = true,
                    downloadImages = false,
                    updateMode = true,
                    folderLayers = listOf("{num} {title}")
                )
            )
        )

        val result = orchestrator.writeSingleScrapeToDisk(
            listOf(ScannedFile(source.absolutePath, source.name, "NEW-001")),
            Video(number = "NEW-001", title = "New Title")
        )

        assertFalse(result.success)
        val message = result.error?.message.orEmpty()
        assertTrue(message.contains("OLD-001.nfo"), message)
        assertTrue(message.contains("line 4"), message)
        assertTrue(message.contains("年龄 <18 禁止观看"), message)
        // 失败发生在移动之前：源文件夹原封不动，目标目录不存在
        assertTrue(oldFolder.resolve(source.name).isFile)
        assertTrue(oldFolder.resolve("OLD-001.nfo").isFile)
        assertFalse(scanDir.resolve("NEW-001 New Title").exists())
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
