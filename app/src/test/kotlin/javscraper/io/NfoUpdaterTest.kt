package javscraper.io

import javscraper.models.Video
import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertContains
import kotlin.test.assertTrue

class NfoUpdaterTest {
    @Test
    fun `targeted updates preserve original formatting and do not add blank lines`() {
        val directory = createTempDirectory("javscraper-nfo-format")
        try {
            val nfo = directory.resolve("old.nfo")
            val originalLines = listOf(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<movie>",
                "  <title>Old Title</title>",
                "  <num>OLD-001</num>",
                "  <plot>Keep summary</plot>",
                "  <customfield>Keep custom value</customfield>",
                "</movie>"
            )
            Files.write(nfo, originalLines)
            val video = Video(number = "NEW-001", title = "New Title", summary = "Keep summary")

            val changed = NfoUpdater.update(nfo, video, lockData = false)

            assertTrue(changed)
            val updatedLines = Files.readAllLines(nfo)
            assertEquals(originalLines.size, updatedLines.size)
            updatedLines.forEach { line -> assertFalse(line.isBlank(), "NFO should not contain blank lines") }
            assertEquals("  <title>New Title</title>", updatedLines[2])
            assertEquals("  <num>NEW-001</num>", updatedLines[3])
            assertEquals("  <plot>Keep summary</plot>", updatedLines[4])
            assertEquals("  <customfield>Keep custom value</customfield>", updatedLines[5])
        } finally {
            Files.walk(directory).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    @Test
    fun `update changes only changed fields and keeps unknown content`() {
        val directory = createTempDirectory("javscraper-nfo-update")
        try {
            val nfo = directory.resolve("old.nfo")
            val original = """
                <?xml version="1.0" encoding="UTF-8"?>
                <movie>
                  <title>Old Title</title>
                  <originaltitle>OLD-001</originaltitle>
                  <sorttitle>OLD-001</sorttitle>
                  <num>OLD-001</num>
                  <plot>Keep this summary</plot>
                  <customfield>Keep custom value</customfield>
                </movie>
            """.trimIndent()
            Files.writeString(nfo, original)
            val video = Video(number = "NEW-001", title = "New Title", summary = "Keep this summary")

            val changed = NfoUpdater.update(nfo, video, lockData = false)

            assertTrue(changed)
            val updated = Files.readString(nfo)
            assertContains(updated, "<title>New Title</title>")
            assertContains(updated, "<num>NEW-001</num>")
            assertContains(updated, "<originaltitle>NEW-001</originaltitle>")
            assertContains(updated, "<sorttitle>NEW-001</sorttitle>")
            assertContains(updated, "<plot>Keep this summary</plot>")
            assertContains(updated, "<customfield>Keep custom value</customfield>")
            assertFalse(updated.contains("<mpaa>"))

            val beforeSecondUpdate = Files.readString(nfo)
            val changedAgain = NfoUpdater.update(nfo, video, lockData = false)
            assertFalse(changedAgain)
            assertEquals(beforeSecondUpdate, Files.readString(nfo))
        } finally {
            Files.walk(directory).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    @Test
    fun `update merges old and new tags without duplicates`() {
        val directory = createTempDirectory("javscraper-nfo-tags")
        try {
            val nfo = directory.resolve("old.nfo")
            val original = """
                <?xml version="1.0" encoding="UTF-8"?>
                <movie>
                  <title>Old Title</title>
                  <num>OLD-001</num>
                  <genre>Old Genre</genre>
                  <genre>Shared</genre>
                  <tag>Old Tag</tag>
                  <tag>Shared</tag>
                </movie>
            """.trimIndent()
            Files.writeString(nfo, original)
            val video = Video(
                number = "NEW-001",
                title = "New Title",
                tags = listOf("New Tag", "Shared")
            )

            val changed = NfoUpdater.update(nfo, video, lockData = false, mergeTags = true)

            assertTrue(changed)
            val updated = Files.readString(nfo)
            assertContains(updated, "<genre>Old Genre</genre>")
            assertContains(updated, "<genre>Shared</genre>")
            assertContains(updated, "<genre>New Tag</genre>")
            assertContains(updated, "<tag>Old Tag</tag>")
            assertContains(updated, "<tag>Shared</tag>")
            assertContains(updated, "<tag>New Tag</tag>")
            assertEquals(3, Regex("<genre>[^<]+</genre>").findAll(updated).count())
            assertEquals(3, Regex("<tag>[^<]+</tag>").findAll(updated).count())

            val changedAgain = NfoUpdater.update(nfo, video, lockData = false, mergeTags = true)
            assertFalse(changedAgain)
            assertEquals(updated, Files.readString(nfo))
        } finally {
            Files.walk(directory).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    @Test
    fun `targeted updates patch nested fields without changing formatting`() {
        val directory = createTempDirectory("javscraper-nfo-nested")
        try {
            val nfo = directory.resolve("old.nfo")
            val originalLines = listOf(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<movie>",
                "  <title>Old Title</title>",
                "  <fanart>",
                "    <thumb>https://example.invalid/old.jpg</thumb>",
                "  </fanart>",
                "  <actor>",
                "    <name>Old Actor</name>",
                "    <role>Old Actor</role>",
                "    <order>1</order>",
                "  </actor>",
                "</movie>"
            )
            Files.write(nfo, originalLines)
            val video = Video(
                number = "NEW-001",
                title = "New Title",
                sampleImages = listOf("https://example.invalid/new.jpg"),
                actresses = listOf("New Actor")
            )

            val changed = NfoUpdater.update(nfo, video, lockData = false)

            assertTrue(changed)
            val updatedLines = Files.readAllLines(nfo)
            assertEquals(originalLines.size, updatedLines.size)
            updatedLines.forEach { line -> assertFalse(line.isBlank(), "NFO should not contain blank lines") }
            assertEquals("    <thumb>https://example.invalid/new.jpg</thumb>", updatedLines[4])
            assertEquals("    <name>New Actor</name>", updatedLines[7])
            assertEquals("    <role>New Actor</role>", updatedLines[8])
            assertEquals("    <order>1</order>", updatedLines[9])
        } finally {
            Files.walk(directory).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }
}
