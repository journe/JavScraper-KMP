package javscraper.io

import javscraper.models.Ranking
import javscraper.models.Review
import javscraper.models.Video
import javscraper.models.VideoUpdateField
import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NfoUpdaterFieldMaskTest {
    @Test
    fun `partial update changes only number and summary field group`() {
        val directory = createTempDirectory("javscraper-nfo-mask")
        val nfo = directory.resolve("movie.nfo")
        Files.writeString(
            nfo,
            """
                <?xml version="1.0" encoding="UTF-8"?>
                <movie>
                  <title>Old Title</title>
                  <originaltitle>OLD-001</originaltitle>
                  <sorttitle>OLD-001</sorttitle>
                  <num>OLD-001</num>
                  <plot>Old plot</plot>
                  <outline>Old outline</outline>
                  <studio>Old studio</studio>
                  <genre>Old genre</genre>
                  <tag>Old tag</tag>
                  <actor><name>Old actor</name></actor>
                  <cover>https://old.invalid/cover.jpg</cover>
                  <poster>https://old.invalid/poster.jpg</poster>
                  <fanart><thumb>https://old.invalid/sample.jpg</thumb></fanart>
                  <country>Japan</country>
                </movie>
            """.trimIndent()
        )
        val incoming = Video(
            number = "NEW-001",
            title = "New Title",
            summary = "New plot",
            maker = "New studio",
            tags = listOf("New tag"),
            actresses = listOf("New actor"),
            coverUrl = "https://new.invalid/cover.jpg",
            posterUrl = "https://new.invalid/poster.jpg",
            sampleImages = listOf("https://new.invalid/sample.jpg")
        )

        val changed = NfoUpdater.update(
            path = nfo,
            video = incoming,
            lockData = false,
            insertMissingFields = true,
            mergeTags = true,
            enabledFields = setOf(VideoUpdateField.NUMBER, VideoUpdateField.SUMMARY)
        )

        assertTrue(changed)
        val updated = Files.readString(nfo)
        assertContains(updated, "<num>NEW-001</num>")
        assertContains(updated, "<originaltitle>NEW-001</originaltitle>")
        assertContains(updated, "<sorttitle>NEW-001</sorttitle>")
        assertContains(updated, "<plot>New plot</plot>")
        assertContains(updated, "<outline>New plot</outline>")
        assertContains(updated, "<title>Old Title</title>")
        assertContains(updated, "<studio>Old studio</studio>")
        assertContains(updated, "<genre>Old genre</genre>")
        assertContains(updated, "<tag>Old tag</tag>")
        assertContains(updated, "<name>Old actor</name>")
        assertContains(updated, "<cover>https://old.invalid/cover.jpg</cover>")
        assertContains(updated, "<poster>https://old.invalid/poster.jpg</poster>")
        assertContains(updated, "https://old.invalid/sample.jpg")
        assertFalse(updated.contains("<mpaa>"))
        assertFalse(updated.contains("<language>"))
    }

    @Test
    fun `partial javdb update preserves unselected extra fields`() {
        val directory = createTempDirectory("javscraper-nfo-mask-extra")
        val nfo = directory.resolve("movie.nfo")
        Files.writeString(
            nfo,
            """
                <?xml version="1.0" encoding="UTF-8"?>
                <movie>
                  <num>ABC-001</num>
                  <javdb_extra>
                    <want_count>10</want_count>
                    <watched_count>20</watched_count>
                    <rating_count>30</rating_count>
                    <ranking rank="1">Old ranking</ranking>
                    <review id="old-review"><content>Old review</content></review>
                  </javdb_extra>
                </movie>
            """.trimIndent()
        )
        val incoming = Video(
            number = "ABC-001",
            wantCount = 100,
            watchedCount = 200,
            ratingCount = 300,
            rankings = listOf(Ranking(2, "New ranking")),
            reviews = listOf(Review("new-review", "Author", "", 5.0, null, "New review"))
        )

        val changed = NfoUpdater.update(
            path = nfo,
            video = incoming,
            lockData = false,
            enabledFields = setOf(VideoUpdateField.WANT_COUNT)
        )

        assertTrue(changed)
        val updated = Files.readString(nfo)
        assertContains(updated, "<want_count>100</want_count>")
        assertContains(updated, "<watched_count>20</watched_count>")
        assertContains(updated, "<rating_count>30</rating_count>")
        assertContains(updated, """<ranking rank="1">Old ranking</ranking>""")
        assertContains(updated, "<content>Old review</content>")
        assertFalse(updated.contains("200"))
        assertFalse(updated.contains("New ranking"))
    }
}