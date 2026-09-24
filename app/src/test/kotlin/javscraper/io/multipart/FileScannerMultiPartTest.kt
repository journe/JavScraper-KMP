package javscraper.io.multipart

import javscraper.io.FileScanner
import javscraper.settings.MultiPartSuffix
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileScannerMultiPartTest {
    @Test
    fun `movie nfo marks every part scraped and supplies master metadata`() {
        val root = Files.createTempDirectory("javscanner-movie-nfo")
        try {
            val folder = root.resolve("[FC2-4694056] Test")
            Files.createDirectories(folder)
            listOf("cd1", "cd2", "cd3", "cd4").forEach { part ->
                Files.createFile(folder.resolve("[FC2-4694056] Test - $part.mp4"))
            }
            Files.writeString(
                folder.resolve("movie.nfo"),
                "<movie><title>Master Title</title><num>FC2-4694056</num></movie>"
            )
            Files.writeString(
                folder.resolve("[FC2-4694056] Test - cd2.nfo"),
                "<movie><title>cd2</title><num>FC2-4694056</num></movie>"
            )

            val result = FileScanner.scanDirectory(root)

            assertEquals(4, result.size)
            assertTrue(result.all { it.number == "FC2-4694056" })
            assertTrue(result.all { it.isScraped })
            assertTrue(result.all { it.metadata?.title == "Master Title" })
            val cd2 = folder.resolve("[FC2-4694056] Test - cd2.mp4")
            assertEquals(
                folder.resolve("movie.nfo"),
                FileScanner.findMatchingNfo(cd2)
            )
        } finally {
            cleanup(root)
        }
    }

    @Test
    fun `recognizes Jellyfin part suffixes and single digit numeric parts`() {
        MultiPartSuffix.entries.forEach { suffix ->
            val label = "${suffix.value}1"
            assertTrue(FileScanner.isMultiPartLabel(label))
            assertEquals(1, FileScanner.multiPartLabelNumber(label))
        }
        listOf("1", "2", "3").forEach { label ->
            assertTrue(FileScanner.isMultiPartLabel(label))
            assertEquals(label.toInt(), FileScanner.multiPartLabelNumber(label))
        }
        listOf("0", "01", "12", "part a", "disc-d").forEach { label ->
            assertFalse(FileScanner.isMultiPartLabel(label))
        }
        assertEquals("011225_01", FileScanner.extractNumber("011225_01.mp4"))
        assertEquals("", FileScanner.parseFileName("011225_01.mp4").versionLabel)
    }

    @Test
    fun `recognizes first part and label-less master file as primary`() {
        val root = Files.createTempDirectory("javscraper-primary-part")
        try {
            val folder = root.resolve("[FC2-1] Title")
            Files.createDirectories(folder)
            Files.writeString(folder.resolve("movie.nfo"), "<movie><title>Master</title></movie>")

            assertTrue(FileScanner.isPrimaryPart("[FC2-1] Title - cd1.mp4", folder))
            assertTrue(FileScanner.isPrimaryPart("[FC2-1] Title.mp4", folder))
            assertFalse(FileScanner.isPrimaryPart("[FC2-1] Title - cd2.mp4", folder))
            assertFalse(FileScanner.isPrimaryPart("[FC2-1] Title - 4k.mp4", folder))
        } finally {
            cleanup(root)
        }
    }

    private fun cleanup(root: Path) {
        Files.walk(root).sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
    }
}
