package javscraper.ui.components

import javscraper.i18n.TranslationEn
import javscraper.models.Video
import javscraper.ui.components.media.videoInfoRows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VideoInfoCardTest {

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