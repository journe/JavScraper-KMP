package javscraper.scrape.multipart

import javscraper.models.ScannedFile
import javscraper.models.Video
import javscraper.models.ScrapeResult
import javscraper.scrape.ScrapeOptions
import javscraper.scrape.ScrapeOrchestrator
import javscraper.settings.AppSettings
import javscraper.settings.MultiPartSuffix
import javscraper.sidecar.SidecarManager
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class MultiPartScrapeOrchestratorTest {
    @Test
    fun `writeToDisk creates movie nfo part nfos and extrafanart based part images`() {
        val source = createTempDirectory("javscraper-parts-source")
        val output = createTempDirectory("javscraper-parts-output")
        val assets = createTempDirectory("javscraper-parts-assets")
        try {
            val files = listOf("cd1", "cd2", "cd3", "cd4").map { part ->
                val file = source.resolve("FC2-4694056-$part.mp4")
                Files.writeString(file, part)
                ScannedFile(file.toString(), file.fileName.toString(), "FC2-4694056")
            }
            Files.writeString(assets.resolve("cover.jpg"), "cover")
            Files.writeString(assets.resolve("fanart1.jpg"), "ignored")
            Files.writeString(assets.resolve("fanart2.jpg"), "extra-2")
            Files.writeString(assets.resolve("fanart3.jpg"), "extra-3")
            Files.writeString(assets.resolve("fanart4.jpg"), "extra-4")
            val orchestrator = ScrapeOrchestrator(
                sidecar = SidecarManager("unused-worker.exe"),
                options = AppSettings(
                    outputDir = output.toString(),
                    createMovieFolders = true,
                    moveInsteadOfCopy = true,
                    downloadImages = true,
                    downloadPreviewImages = true,
                    folderLayers = listOf("[{num}] {title}"),
                    filenameFormat = "{num} {title}",
                    multiPartSuffix = MultiPartSuffix.DISC
                ).let(ScrapeOptions::from)
            )

            lateinit var result: ScrapeResult
            runTest {
                result = orchestrator.writeToDisk(
                    files,
                    Video(
                        number = "FC2-4694056",
                        title = "Test",
                        coverUrl = assets.resolve("cover.jpg").toUri().toString(),
                        sampleImages = listOf(
                            assets.resolve("fanart1.jpg").toUri().toString(),
                            assets.resolve("fanart2.jpg").toUri().toString(),
                            assets.resolve("fanart3.jpg").toUri().toString(),
                            assets.resolve("fanart4.jpg").toUri().toString()
                        )
                    )
                )
            }

            assertTrue(result.success, result.error?.message ?: "writeToDisk failed")
            val folder = output.resolve("[FC2-4694056] Test")
            listOf("disc1", "disc2", "disc3", "disc4").forEach { part ->
                assertTrue(Files.isRegularFile(folder.resolve("[FC2-4694056] Test - $part.mp4")))
            }
            assertTrue(Files.isRegularFile(folder.resolve("movie.nfo")))
            assertTrue(Files.readString(folder.resolve("movie.nfo")).contains("<title>Test</title>"))
            listOf("disc2", "disc3", "disc4").forEach { part ->
                val nfo = folder.resolve("[FC2-4694056] Test - $part.nfo")
                assertTrue(Files.isRegularFile(nfo))
                assertTrue(Files.readString(nfo).contains("<title>$part</title>"))
            }
            assertFalse(Files.exists(folder.resolve("[FC2-4694056] Test - disc1.nfo")))

            assertFalse(Files.exists(folder.resolve("[FC2-4694056] Test - disc1-poster.jpg")))
            assertFalse(Files.exists(folder.resolve("[FC2-4694056] Test - disc1-fanart.jpg")))
            listOf("disc2", "disc3", "disc4").forEachIndexed { index, part ->
                val expected = "extra-${index + 2}"
                assertEquals(expected, Files.readString(folder.resolve("[FC2-4694056] Test - $part-poster.jpg")))
                assertEquals(expected, Files.readString(folder.resolve("[FC2-4694056] Test - $part-fanart.jpg")))
            }
            assertTrue(Files.isRegularFile(folder.resolve("poster.jpg")))
            assertTrue(Files.isRegularFile(folder.resolve("fanart.jpg")))
        } finally {
            listOf(source, output, assets).forEach(::cleanup)
        }
    }

    @Test
    fun `update mode renames multi-part folder and keeps part nfo titles`() {
        val root = Files.createTempDirectory("javscraper-parts-update")
        try {
            val oldFolder = root.resolve("[FC2-4694056] Old title")
            Files.createDirectories(oldFolder)
            val files = listOf("cd4", "cd3", "cd2", "cd1").map { part ->
                val file = oldFolder.resolve("FC2-4694056-$part.mp4")
                Files.writeString(file, part)
                ScannedFile(file.toString(), file.fileName.toString(), "FC2-4694056")
            }
            Files.writeString(
                oldFolder.resolve("movie.nfo"),
                "<movie><title>Old title</title><num>FC2-4694056</num></movie>"
            )
            Files.writeString(
                oldFolder.resolve("FC2-4694056-cd2.nfo"),
                "<movie><title>cd2</title><num>FC2-4694056</num></movie>"
            )
            val orchestrator = ScrapeOrchestrator(
                sidecar = SidecarManager("unused-worker.exe"),
                options = AppSettings(
                    scanDir = root.toString(),
                    createMovieFolders = true,
                    updateMode = true,
                    downloadImages = false,
                    folderLayers = listOf("[{num}] {title}"),
                    multiPartSuffix = MultiPartSuffix.DISC
                ).let(ScrapeOptions::from)
            )

            lateinit var result: ScrapeResult
            runTest {
                result = orchestrator.writeSingleScrapeToDisk(
                    files,
                    Video(number = "FC2-4694056", title = "New title")
                )
            }

            assertTrue(result.success, result.error?.message ?: "update failed")
            val folder = root.resolve("[FC2-4694056] New title")
            val base = "[FC2-4694056] New title"
            assertEquals(folder.resolve("$base - disc1.mp4").toString(), result.data?.path)
            listOf("disc1", "disc2", "disc3", "disc4").forEach { part ->
                assertTrue(Files.isRegularFile(folder.resolve("$base - $part.mp4")))
            }
            assertTrue(Files.readString(folder.resolve("movie.nfo")).contains("<title>New title</title>"))
            assertTrue(Files.readString(folder.resolve("$base - disc2.nfo")).contains("<title>cd2</title>"))
        } finally {
            cleanup(root)
        }
    }

    @Test
    fun `update mode keeps multi-version filenames when movie nfo exists`() {
        val root = Files.createTempDirectory("javscraper-versions-update")
        try {
            val oldFolder = root.resolve("[FC2-4694056] Old title")
            Files.createDirectories(oldFolder)
            val files = listOf("4K", "1080p").map { version ->
                val file = oldFolder.resolve("FC2-4694056-$version.mp4")
                Files.writeString(file, version)
                ScannedFile(file.toString(), file.fileName.toString(), "FC2-4694056")
            }
            Files.writeString(
                oldFolder.resolve("movie.nfo"),
                "<movie><title>Old title</title><num>FC2-4694056</num></movie>"
            )
            Files.writeString(oldFolder.resolve("poster.jpg"), "generic poster")
            Files.writeString(oldFolder.resolve("fanart.jpg"), "generic fanart")
            val orchestrator = ScrapeOrchestrator(
                sidecar = SidecarManager("unused-worker.exe"),
                options = AppSettings(
                    scanDir = root.toString(),
                    createMovieFolders = true,
                    updateMode = true,
                    downloadImages = false,
                    folderLayers = listOf("[{num}] {title}"),
                    multiPartSuffix = MultiPartSuffix.DISC
                ).let(ScrapeOptions::from)
            )

            lateinit var result: ScrapeResult
            runTest {
                result = orchestrator.writeSingleScrapeToDisk(
                    files,
                    Video(number = "FC2-4694056", title = "New title")
                )
            }

            assertTrue(result.success, result.error?.message ?: "update failed")
            val folder = root.resolve("FC2-4694056")
            assertTrue(Files.isRegularFile(folder.resolve("FC2-4694056-1080p.mp4")))
            assertTrue(Files.isRegularFile(folder.resolve("FC2-4694056-4K.mp4")))
            assertEquals("generic poster", Files.readString(folder.resolve("poster.jpg")))
            assertEquals("generic fanart", Files.readString(folder.resolve("fanart.jpg")))
            listOf("1080p", "4K").forEach { version ->
                assertFalse(Files.exists(folder.resolve("FC2-4694056-$version-poster.jpg")))
                assertFalse(Files.exists(folder.resolve("FC2-4694056-$version-fanart.jpg")))
            }
            assertFalse(Files.exists(folder.resolve("FC2-4694056 - part1.mp4")))
            assertFalse(Files.exists(folder.resolve("FC2-4694056 - part2.mp4")))
        } finally {
            cleanup(root)
        }
    }

    private fun cleanup(root: Path) {
        Files.walk(root).sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
    }
}
