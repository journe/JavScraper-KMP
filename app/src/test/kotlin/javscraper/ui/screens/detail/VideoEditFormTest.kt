package javscraper.ui.screens.detail

import javscraper.i18n.TranslationEn
import javscraper.models.Video
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VideoEditFormTest {

    @Test
    fun `edit fields expose raw values and preserve field labels`() {
        val video = Video(
            number = "SONE-001",
            title = "Old title",
            actresses = listOf("Actor A", "Actor B"),
            sampleImages = listOf("https://example.com/a.jpg", "https://example.com/b.jpg")
        )

        val fields = videoEditFields(video, TranslationEn())

        assertEquals(17, fields.size)
        assertEquals("Number", fields[0].label)
        assertEquals("SONE-001", fields[0].value)
        assertEquals("Actor A, Actor B", fields[2].value)
        assertEquals(
            "https://example.com/a.jpg\nhttps://example.com/b.jpg",
            fields[16].value
        )
        assertTrue(fields.drop(3).all { it.value.isEmpty() || it.value.isNotBlank() })
    }

    @Test
    fun `edited fields convert back to video and parse list and numeric fields`() {
        val original = Video(number = "SONE-001", path = "F:/Videos/SONE-001.mp4")
        val fields = videoEditFields(original, TranslationEn())
            .mapIndexed { index, field ->
                when (index) {
                    0 -> field.copy(value = "ABC-001")
                    1 -> field.copy(value = "New title")
                    2 -> field.copy(value = "Actor A, Actor B\nActor C")
                    3 -> field.copy(value = "2026-09-18")
                    4 -> field.copy(value = "Updated summary")
                    9 -> field.copy(value = "125")
                    10 -> field.copy(value = "9.5")
                    11 -> field.copy(value = "Tag A, Tag B")
                    16 -> field.copy(value = "https://example.com/a.jpg\nhttps://example.com/b.jpg")
                    else -> field
                }
            }

        val edited = videoFromEditFields(original, fields)

        assertEquals("ABC-001", edited?.number)
        assertEquals("New title", edited?.title)
        assertEquals(listOf("Actor A", "Actor B", "Actor C"), edited?.actresses)
        assertEquals("2026-09-18", edited?.date)
        assertEquals("Updated summary", edited?.summary)
        assertEquals(125, edited?.duration)
        assertEquals(9.5, edited?.rating)
        assertEquals(listOf("Tag A", "Tag B"), edited?.tags)
        assertEquals(2, edited?.sampleImages?.size)
        assertEquals("F:/Videos/SONE-001.mp4", edited?.path)
    }

    @Test
    fun `invalid numeric fields reject conversion`() {
        val original = Video(number = "SONE-001")
        val fields = videoEditFields(original, TranslationEn())

        val invalidDuration = videoFromEditFields(
            original,
            fields.mapIndexed { index, field -> if (index == 9) field.copy(value = "abc") else field }
        )
        val invalidRating = videoFromEditFields(
            original,
            fields.mapIndexed { index, field -> if (index == 10) field.copy(value = "-1") else field }
        )

        assertNull(invalidDuration)
        assertNull(invalidRating)
    }

    @Test
    fun `blank duration and rating remain nullable instead of blocking save`() {
        val original = Video(number = "SONE-001")
        val fields = videoEditFields(original, TranslationEn())

        val edited = videoFromEditFields(original, fields)

        assertEquals(null, edited?.duration)
        assertEquals(null, edited?.rating)
    }

    @Test
    fun `rating edit accepts values above ten because nfo has no upper limit`() {
        val original = Video(number = "SONE-001")
        val fields = videoEditFields(original, TranslationEn())
            .mapIndexed { index, field ->
                if (index == 10) field.copy(value = "12.3") else field
            }

        val edited = videoFromEditFields(original, fields)

        assertEquals(12.3, edited?.rating)
    }
}
