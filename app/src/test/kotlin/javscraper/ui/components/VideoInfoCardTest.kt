package javscraper.ui.components

import javscraper.i18n.TranslationEn
import javscraper.models.Video
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
            tags = listOf("Tag A", "Tag B"),
            coverUrl = "https://example.com/cover.jpg",
            posterUrl = "https://example.com/poster.jpg",
            sampleImages = listOf("https://example.com/sample-1.jpg", "https://example.com/sample-2.jpg"),
            summary = "Full summary",
            source = "JavBus",
            detailUrl = "https://example.com/detail"
        )

        val rows = videoInfoRows(video, TranslationEn())

        assertEquals(17, rows.size)
        assertEquals("Number", rows[0].label)
        assertEquals("SONE-001", rows[0].value)
        assertEquals("Title", rows[1].label)
        assertEquals("Full metadata title", rows[1].value)
        assertEquals("Actresses", rows[2].label)
        assertEquals("Actor A, Actor B", rows[2].value)
        assertEquals("Date", rows[3].label)
        assertEquals("2026-08-26", rows[3].value)
        assertEquals("Maker", rows[4].label)
        assertEquals("Label", rows[5].label)
        assertEquals("Series", rows[6].label)
        assertEquals("Director", rows[7].label)
        assertTrue(rows[8].value.contains("120"))
        assertTrue(rows[9].value.contains("9.2"))
        assertEquals("Tags", rows[10].label)
        assertEquals("Tag A, Tag B", rows[10].value)
        assertEquals("Cover URL", rows[11].label)
        assertEquals("https://example.com/cover.jpg", rows[11].value)
        assertEquals("Poster URL", rows[12].label)
        assertEquals("https://example.com/poster.jpg", rows[12].value)
        assertEquals("Sample Images", rows[13].label)
        assertEquals(
            "https://example.com/sample-1.jpg\nhttps://example.com/sample-2.jpg",
            rows[13].value
        )
        assertEquals("Summary", rows[14].label)
        assertEquals("Full summary", rows[14].value)
        assertEquals("Source", rows[15].label)
        assertEquals("JavBus", rows[15].value)
        assertEquals("Detail URL", rows[16].label)
        assertEquals("https://example.com/detail", rows[16].value)
        assertTrue(rows.all { !it.isEmpty })
    }

    @Test
    fun `video info rows keep empty placeholders for every field`() {
        val rows = videoInfoRows(Video(number = "SONE-002"), TranslationEn())

        assertEquals(17, rows.size)
        assertEquals("SONE-002", rows[0].value)
        assertEquals(rows.drop(1).size, rows.drop(1).count { it.isEmpty })
        assertTrue(rows.drop(1).all { it.value == "Not set" })
    }
}