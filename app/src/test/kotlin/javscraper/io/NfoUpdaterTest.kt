package javscraper.io

import javscraper.io.metadata.SecureDocumentBuilderFactory
import javscraper.models.Ranking
import javscraper.models.Review
import javscraper.models.Video
import org.xml.sax.InputSource
import java.io.StringReader
import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
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
    fun `setFanart never touches the root thumb element and keeps XML valid`() {
        val directory = createTempDirectory("javscraper-nfo-fanart-thumb")
        try {
            val nfo = directory.resolve("movie.nfo")
            val poster = "https://example.invalid/poster.jpg" // 34 chars
            val oldRootThumb = "https://example.invalid/old-poster.jpgXX" // 36 chars: 2 longer than poster
            val original = """
                <?xml version="1.0" encoding="UTF-8"?>
                <movie>
                  <title>Old Title</title>
                  <thumb>$oldRootThumb</thumb>
                  <fanart>
                    <thumb>https://example.invalid/s1.jpg</thumb>
                  </fanart>
                </movie>
            """.trimIndent()
            Files.writeString(nfo, original)
            val video = Video(
                number = "ABP-583",
                coverUrl = poster,
                posterUrl = poster,
                sampleImages = listOf("https://example.invalid/s1.jpg", "https://example.invalid/s2.jpg")
            )

            val changed = NfoUpdater.update(nfo, video, lockData = false, insertMissingFields = true)

            assertTrue(changed)
            val updated = Files.readString(nfo)
            // The NFO must stay parseable after the update.
            SecureDocumentBuilderFactory.create()
                .parse(InputSource(StringReader(updated)))
            // The root <thumb> keeps the poster value (never a sample image, never a broken tag).
            assertContains(updated, "  <thumb>$poster</thumb>")
            assertFalse(updated.contains("s1.jpgthumb>"))
            // The fanart block carries exactly the sample images.
            val fanartStart = updated.indexOf("<fanart>")
            val fanartEnd = updated.indexOf("</fanart>")
            assertTrue(fanartStart in 0 until fanartEnd)
            val fanart = updated.substring(fanartStart, fanartEnd)
            assertContains(fanart, "https://example.invalid/s1.jpg</thumb>")
            assertContains(fanart, "https://example.invalid/s2.jpg</thumb>")
            assertEquals(2, Regex("<thumb>[^<]*</thumb>").findAll(fanart).count())
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
    fun `update throws InvalidNfoException with location context for malformed text content`() {
        val directory = createTempDirectory("javscraper-nfo-invalid")
        try {
            val nfo = directory.resolve("bad.nfo")
            val badLine = "  <tag>年龄 <18 禁止观看</tag>"
            Files.writeString(
                nfo,
                listOf(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                    "<movie>",
                    "  <title>Old Title</title>",
                    "  <num>OLD-001</num>",
                    "  <plot>Keep summary</plot>",
                    badLine,
                    "</movie>"
                ).joinToString("\n")
            )
            val video = Video(number = "NEW-001", title = "New Title")

            val error = assertFailsWith<InvalidNfoException> {
                NfoUpdater.update(nfo, video, lockData = false)
            }

            assertEquals(nfo.toString(), error.nfoPath)
            assertTrue(error.message.orEmpty().contains("line 6"), error.message)
            assertContains(error.message.orEmpty(), badLine)
        } finally {
            Files.walk(directory).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    @Test
    fun `update strips illegal 7mmtvid element and succeeds`() {
        val directory = createTempDirectory("javscraper-nfo-7mmtvid")
        try {
            val nfo = directory.resolve("mmtv.nfo")
            Files.writeString(
                nfo,
                listOf(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                    "<movie>",
                    "  <title>Old Title</title>",
                    "  <num>FC2-1967256</num>",
                    "  <website>https://7mmtv.sx/zh/uncensored_content/32216/fc2-ppv-1967256.html</website>",
                    "  <7mmtvid>https://7mmtv.sx/zh/uncensored_content/32216/fc2-ppv-1967256.html</7mmtvid>",
                    "</movie>"
                ).joinToString("\n")
            )
            val detailUrl = "https://7mmtv.sx/zh/uncensored_content/32216/fc2-ppv-1967256.html"
            val video = Video(number = "FC2-1967256", title = "New Title", detailUrl = detailUrl)

            val changed = NfoUpdater.update(nfo, video, lockData = false)

            assertTrue(changed)
            val updated = Files.readString(nfo)
            assertFalse(updated.contains("7mmtvid"))
            // 清理后文件必须是可解析的合法 XML
            SecureDocumentBuilderFactory.create()
                .parse(InputSource(StringReader(updated)))
            assertContains(updated, "<website>$detailUrl</website>")
        } finally {
            Files.walk(directory).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    @Test
    fun `update throws InvalidNfoException for bare ampersand`() {
        val directory = createTempDirectory("javscraper-nfo-bare-amp")
        try {
            val nfo = directory.resolve("amp.nfo")
            Files.writeString(nfo, "<movie><title>A & B</title></movie>")

            val error = assertFailsWith<InvalidNfoException> {
                NfoUpdater.update(nfo, Video(number = "NEW-001"), lockData = false)
            }

            assertEquals(nfo.toString(), error.nfoPath)
            assertContains(error.message.orEmpty(), "<title>A & B</title>")
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

    @Test
    fun `update synchronizes javdb extra fields`() {
        val directory = createTempDirectory("javscraper-nfo-javdb")
        try {
            val nfo = directory.resolve("extra.nfo")
            Files.writeString(
                nfo,
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <movie>
                  <num>SONE-001</num>
                  <javdb_extra>
                    <want_count>1</want_count>
                  </javdb_extra>
                </movie>
                """.trimIndent()
            )
            val video = Video(
                number = "SONE-001",
                wantCount = 7411,
                watchedCount = 1614,
                ratingCount = 1614,
                rankings = listOf(Ranking(212, "JavDB 2022年度TOP250")),
                reviews = listOf(Review("1", "author", "2023-12-11", 5.0, 10, "短评"))
            )

            val changed = NfoUpdater.update(nfo, video, lockData = false)

            assertTrue(changed)
            val updated = Files.readString(nfo)
            assertContains(updated, "<want_count>7411</want_count>")
            assertContains(updated, "<watched_count>1614</watched_count>")
            assertContains(updated, "<rating_count>1614</rating_count>")
            assertContains(updated, """<ranking rank="212">JavDB 2022年度TOP250</ranking>""")
            assertContains(updated, "<content>短评</content>")

            val changedAgain = NfoUpdater.update(nfo, video, lockData = false)
            assertFalse(changedAgain)
            assertEquals(updated, Files.readString(nfo))
        } finally {
            Files.walk(directory).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }
}
