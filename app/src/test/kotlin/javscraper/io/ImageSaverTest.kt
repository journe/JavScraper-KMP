package javscraper.io

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class ImageSaverTest {

    @Test
    fun `download copies cover to poster when no independent poster`() = runTest {
        val output = createTempDirectory("imagesaver-copy")
        val (server, baseUrl) = startServer(
            mapOf("/cover.jpg" to "cover-bytes".toByteArray())
        )
        try {
            ImageSaver.download(
                output,
                coverUrl = "$baseUrl/cover.jpg"
            )

            // 封面(cover) → fanart.jpg，poster 缺省时复制自 fanart
            assertEquals("cover-bytes", output.resolve("fanart.jpg").toFile().readText())
            assertEquals("cover-bytes", output.resolve("poster.jpg").toFile().readText())
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `download uses independent poster url for poster`() = runTest {
        val output = createTempDirectory("imagesaver-poster")
        val (server, baseUrl) = startServer(
            mapOf(
                "/cover.jpg" to "cover-bytes".toByteArray(),
                "/poster.jpg" to "poster-bytes".toByteArray()
            )
        )
        try {
            ImageSaver.download(
                output,
                coverUrl = "$baseUrl/cover.jpg",
                posterUrl = "$baseUrl/poster.jpg"
            )

            // 封面(cover) → fanart.jpg；海报(poster)独立地址 → poster.jpg
            assertEquals("cover-bytes", output.resolve("fanart.jpg").toFile().readText())
            assertEquals("poster-bytes", output.resolve("poster.jpg").toFile().readText())
        } finally {
            server.stop(0)
        }
    }

    private fun startServer(responses: Map<String, ByteArray>): Pair<HttpServer, String> {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val content = responses[exchange.requestURI.path] ?: byteArrayOf()
            exchange.sendResponseHeaders(200, content.size.toLong())
            exchange.responseBody.use { it.write(content) }
        }
        server.start()
        return server to "http://127.0.0.1:${server.address.port}"
    }
}