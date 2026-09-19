package javscraper.ui.components

import javscraper.i18n.TranslationEn
import javscraper.models.Ranking
import javscraper.models.Video
import javscraper.ui.VideoFieldItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VideoInfoCardTest {

    @Test
    fun `video info rows contain every video field`() {
        val video = Video(
            number = "SONE-001",
            title = "Full metadata title",
            actresses = listOf("Actor A", "Actor B"),
            date = "2026-08-26",
            maker = "Maker",
            label = "Label",
            series = "Series",
            director = "Director",
            duration = 120,
            rating = 9.2,
            wantCount = 7411,
            watchedCount = 1614,
            ratingCount = 1614,
            tags = listOf("Tag A", "Tag B"),
            rankings = listOf(Ranking(212, "JavDB 2022年度TOP250")),
            coverUrl = "https://example.com/cover.jpg",
            posterUrl = "https://example.com/poster.jpg",
            sampleImages = listOf("https://example.com/sample-1.jpg", "https://example.com/sample-2.jpg"),
            summary = "Full summary",
            source = "JavBus",
            detailUrl = "https://example.com/detail"
        )

        val rows = videoInfoRows(video, TranslationEn())

        assertEquals(21, rows.size)
        assertEquals("Number", rows[0].label)
        assertEquals("SONE-001", rows[0].value)
        assertEquals("Title", rows[1].label)
        assertEquals("Full metadata title", rows[1].value)
        assertEquals("Actresses", rows[2].label)
        assertEquals("Actor A, Actor B", rows[2].value)
        assertEquals(listOf(VideoFieldItem("Actor A"), VideoFieldItem("Actor B")), rows[2].items)
        assertEquals("Date", rows[3].label)
        assertEquals("2026-08-26", rows[3].value)
        assertEquals("Summary", rows[4].label)
        assertEquals("Full summary", rows[4].value)
        assertEquals("Maker", rows[5].label)
        assertEquals("Label", rows[6].label)
        assertEquals("Series", rows[7].label)
        assertEquals("Director", rows[8].label)
        assertTrue(rows[9].value.contains("120"))
        assertTrue(rows[10].value.contains("9.2"))
        assertEquals("7411", rows[11].value)
        assertEquals("1614", rows[12].value)
        assertEquals("1614", rows[13].value)
        assertEquals("No.212 JavDB 2022年度TOP250", rows[14].value)
        assertEquals("Tags", rows[15].label)
        assertEquals("Tag A, Tag B", rows[15].value)
        assertEquals(listOf(VideoFieldItem("Tag A"), VideoFieldItem("Tag B")), rows[15].items)
        assertEquals("Source", rows[16].label)
        assertEquals("JavBus", rows[16].value)
        assertEquals("Detail URL", rows[17].label)
        assertEquals("https://example.com/detail", rows[17].value)
        assertEquals("Cover URL", rows[18].label)
        assertEquals("https://example.com/cover.jpg", rows[18].value)
        assertEquals("Poster URL", rows[19].label)
        assertEquals("https://example.com/poster.jpg", rows[19].value)
        assertEquals("Sample Images", rows[20].label)
        assertEquals(
            "https://example.com/sample-1.jpg\nhttps://example.com/sample-2.jpg",
            rows[20].value
        )
        assertTrue(rows.all { !it.isEmpty })
    }

    @Test
    fun `video info rows keep empty placeholders for every field`() {
        val rows = videoInfoRows(Video(number = "SONE-002"), TranslationEn())

        assertEquals(21, rows.size)
        assertEquals("SONE-002", rows[0].value)
        assertEquals(rows.drop(1).size, rows.drop(1).count { it.isEmpty })
        assertTrue(rows.drop(1).all { it.value == "Not set" })
        assertTrue(rows.all { it.items.isNullOrEmpty() })
    }
}
