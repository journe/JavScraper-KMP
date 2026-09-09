package javscraper.io

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImageSaverTest {

    @Test
    fun `download with empty URLs returns empty map`() = runTest {
        val tmpDir = Files.createTempDirectory("javscraper-img-")
        try {
            val result = ImageSaver.download(tmpDir)
            assertEquals(0, result.size)
        } finally {
            Files.walk(tmpDir).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    @Test
    fun `download creates output directory`() = runTest {
        val tmpDir = Files.createTempDirectory("javscraper-img-")
        val subDir = tmpDir.resolve("nested").resolve("output")
        try {
            ImageSaver.download(subDir, coverUrl = "", fanartUrl = "")
            assertTrue(Files.exists(subDir), "Output directory should be created")
        } finally {
            Files.walk(tmpDir).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    @Test
    fun `download with data URI cover does not fail`() = runTest {
        val tmpDir = Files.createTempDirectory("javscraper-img-")
        try {
            // data URIs are skipped by dl(), but the result still records the target path
            val result = ImageSaver.download(
                tmpDir,
                coverUrl = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
            )
            // dl() returns normally (no exception) so poster path is recorded
            assertEquals(1, result.size, "data URI returns normally; poster path is recorded")
            assertTrue(result.containsKey("poster"))
        } finally {
            Files.walk(tmpDir).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    @Test
    fun `download with double-slash URL gets normalized`() = runTest {
        val tmpDir = Files.createTempDirectory("javscraper-img-")
        try {
            // URL starting with // should be normalized to https://
            // Download will fail silently but the attempt is made
            val result = ImageSaver.download(
                tmpDir,
                coverUrl = "//example.com/nonexistent-cover.jpg"
            )
            // The download fails because URL doesn't exist, so result is empty
            assertEquals(0, result.size)
        } finally {
            Files.walk(tmpDir).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    @Test
    fun `download with sample images creates extrafanart dir`() = runTest {
        val tmpDir = Files.createTempDirectory("javscraper-img-")
        try {
            val result = ImageSaver.download(
                tmpDir,
                sampleImages = listOf("//example.com/img1.jpg", "//example.com/img2.jpg")
            )
            val extraDir = tmpDir.resolve("extrafanart")
            assertTrue(Files.exists(extraDir), "extrafanart directory should be created")
            assertEquals(0, result.size, "Failed downloads return empty map")
        } finally {
            Files.walk(tmpDir).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    @Test
    fun `download copies cover as fanart after saving poster`() = runTest {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/cover.jpg") { exchange ->
            val content = "cover-bytes".toByteArray()
            exchange.sendResponseHeaders(200, content.size.toLong())
            exchange.responseBody.use { it.write(content) }
        }
        server.start()
        val tmpDir = Files.createTempDirectory("javscraper-img-fanart-")
        try {
            val result = ImageSaver.download(
                tmpDir,
                coverUrl = "http://127.0.0.1:${server.address.port}/cover.jpg"
            )

            assertEquals(2, result.size, "poster and fanart should be recorded")
            assertEquals(tmpDir.resolve("poster.jpg").toString(), result["poster"])
            assertEquals(tmpDir.resolve("fanart.jpg").toString(), result["fanart"])
            assertTrue(Files.exists(tmpDir.resolve("poster.jpg")), "poster.jpg should be downloaded")
            assertTrue(Files.exists(tmpDir.resolve("fanart.jpg")), "fanart.jpg should be copied from poster")
            assertTrue(
                Files.readAllBytes(tmpDir.resolve("poster.jpg")).contentEquals(
                    Files.readAllBytes(tmpDir.resolve("fanart.jpg"))
                ),
                "fanart.jpg should be identical to poster.jpg"
            )
        } finally {
            Files.walk(tmpDir).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
            server.stop(0)
        }
    }
}
