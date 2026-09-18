package javscraper.scrape

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import javscraper.models.ScannedFile
import javscraper.models.Video
import javscraper.models.WebpageArchiver
import javscraper.models.WebpageImageResult
import javscraper.settings.AppSettings
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
            options = ScrapeOptions.from(AppSettings(
                outputDir = output.absolutePath,
                createMovieFolders = true,
                folderLayers = emptyList(),
                downloadImages = false,
                filenameFormat = "{num} {title}{suffix}",
                suffixKeywords = listOf("-cd1")
            )),
        )

        runTest {
            val result = orchestrator.writeToDisk(
                listOf(ScannedFile(source.absolutePath, source.name, "ABC-001")),
                Video(number = "ABC-001", title = "Test")
            )

            assertTrue(result.success, result.error?.message ?: "writeToDisk failed")
            assertEquals(output.resolve("ABC-001 Test-cd1.mp4").absolutePath, result.data?.path)
            assertTrue(output.resolve("ABC-001 Test-cd1.mp4").isFile)
            assertTrue(output.resolve("ABC-001 Test.nfo").isFile)
            assertFalse(output.resolve(".nfo").isFile)
        }
    }

    @Test
    fun `writeToDisk groups multiple files into one Jellyfin multi-part folder`() {
        val sourceDir = createTempDirectory("javscraper-multi-source").toFile()
        val output = createTempDirectory("javscraper-multi-output").toFile()
        val files = listOf(
            "FC2-4694056.mp4",
            "FC2-4694056-2.mp4",
            "FC2-4694056-3.mp4",
            "FC2-PPV 4694056-4.mp4"
        ).map { name ->
            sourceDir.resolve(name).apply { writeText(name) }
        }
        val orchestrator = ScrapeOrchestrator(
            sidecar = SidecarManager("unused-worker.exe"),
            options = ScrapeOptions.from(AppSettings(
                outputDir = output.absolutePath,
                createMovieFolders = true,
                moveInsteadOfCopy = true,
                downloadImages = false,
                folderLayers = listOf("{num} {title}"),
                filenameFormat = "{num} {title}"
            )),
        )

        runTest {
            val result = orchestrator.writeToDisk(
                files.map { ScannedFile(it.absolutePath, it.name, "FC2-4694056") },
                Video(number = "FC2-4694056", title = "Test")
            )

            assertTrue(result.success, result.error?.message ?: "writeToDisk failed")
            val folder = output.resolve("FC2-4694056")
            assertTrue(folder.resolve("FC2-4694056.nfo").isFile)
            listOf(
                "FC2-4694056 - part1.mp4",
                "FC2-4694056 - part2.mp4",
                "FC2-4694056 - part3.mp4",
                "FC2-4694056 - part4.mp4"
            ).forEach { name ->
                assertTrue(folder.resolve(name).isFile, "missing $name")
            }
            assertTrue(files.none { it.exists() })
        }
    }

    @Test
    fun `writeToDisk reuses one folder for resolution versions`() {
        val sourceDir = createTempDirectory("javscraper-versions-source").toFile()
        val output = createTempDirectory("javscraper-versions-output").toFile()
        val files = listOf("FC2-4694056-1080p.mp4", "FC2-4694056-4K.mp4").map { name ->
            sourceDir.resolve(name).apply { writeText(name) }
        }
        val orchestrator = ScrapeOrchestrator(
            sidecar = SidecarManager("unused-worker.exe"),
            options = ScrapeOptions.from(AppSettings(
                outputDir = output.absolutePath,
                createMovieFolders = true,
                moveInsteadOfCopy = true,
                downloadImages = false,
                folderLayers = listOf("{num} {title}"),
                filenameFormat = "{num} {title}"
            )),
        )

        runTest {
            val result = orchestrator.writeToDisk(
                files.map { ScannedFile(it.absolutePath, it.name, "FC2-4694056") },
                Video(number = "FC2-4694056", title = "Test")
            )

            assertTrue(result.success, result.error?.message ?: "writeToDisk failed")
            val folder = output.resolve("FC2-4694056")
            assertTrue(folder.resolve("FC2-4694056.nfo").isFile)
            assertTrue(folder.resolve("FC2-4694056 - 1080p.mp4").isFile)
            assertTrue(folder.resolve("FC2-4694056 - 4K.mp4").isFile)
            assertTrue(files.none { it.exists() })
        }
    }

    @Test
    fun `writeToDisk preserves special version suffix and fills video version`() {
        val sourceDir = createTempDirectory("javscraper-special-source").toFile()
        val output = createTempDirectory("javscraper-special-output").toFile()
        val files = listOf("ABC-123-2-C.mp4", "ABC-123-3-C.mp4").map { name ->
            sourceDir.resolve(name).apply { writeText(name) }
        }
        val orchestrator = ScrapeOrchestrator(
            sidecar = SidecarManager("unused-worker.exe"),
            options = ScrapeOptions.from(AppSettings(
                outputDir = output.absolutePath,
                createMovieFolders = true,
                moveInsteadOfCopy = true,
                downloadImages = false,
                folderLayers = listOf("{num} {title}"),
                filenameFormat = "{num} {title}"
            )),
        )

        runTest {
            val result = orchestrator.writeToDisk(
                files.map { ScannedFile(it.absolutePath, it.name, "ABC-123") },
                Video(number = "ABC-123", title = "Test")
            )

            assertTrue(result.success, result.error?.message ?: "writeToDisk failed")
            assertEquals("C", result.data?.version)
            val folder = output.resolve("ABC-123")
            assertTrue(folder.resolve("ABC-123 - part2-C.mp4").isFile)
            assertTrue(folder.resolve("ABC-123 - part3-C.mp4").isFile)
        }
    }

    @Test
    fun `writeToDisk moves source video to output`() {
        val sourceDir = createTempDirectory("javscraper-move-source").toFile()
        val outputDir = createTempDirectory("javscraper-move-output").toFile()
        val source = sourceDir.resolve("ABC-005.mp4")
        source.writeText("video")
        val orchestrator = ScrapeOrchestrator(
            sidecar = SidecarManager("unused-worker.exe"),
            options = ScrapeOptions.from(AppSettings(
                outputDir = outputDir.absolutePath,
                createMovieFolders = false,
                moveInsteadOfCopy = true,
                downloadImages = false,
                filenameFormat = "{num}"
            )),
        )

        runTest {
            val result = orchestrator.writeToDisk(
                listOf(ScannedFile(source.absolutePath, source.name, "ABC-005")),
                Video(number = "ABC-005", title = "Test")
            )

            assertTrue(result.success, result.error?.message ?: "writeToDisk failed")
            val target = outputDir.resolve("ABC-005.mp4")
            assertTrue(target.isFile)
            assertEquals("video", target.readText())
            assertFalse(source.exists())
        }
    }
    @Test
    fun `writeToDisk reports failure when source file is missing`() {
        val output = createTempDirectory("javscraper-io-failure").toFile()
        val source = output.resolve("missing-source.mp4")
        val orchestrator = ScrapeOrchestrator(
            sidecar = SidecarManager("unused-worker.exe"),
            options = ScrapeOptions.from(AppSettings(
                outputDir = output.absolutePath,
                downloadImages = false
            )),
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
