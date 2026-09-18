package javscraper.ui.screens.detail

import javscraper.i18n.TranslationEn
import javscraper.models.Video
import javscraper.ui.editRatingSliderValue
import javscraper.ui.formatEditRating
import javscraper.ui.videoFieldEditValues
import javscraper.ui.VideoFieldItem
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
            summary = "Old summary",
            rating = 9.5,
            tags = listOf("Tag A", "Tag B"),
            source = "Example",
            detailUrl = "https://example.com/movie"
        )

        val fields = videoFieldEditValues(video, TranslationEn())

        assertEquals(12, fields.size)
        assertEquals("Number", fields[0].label)
        assertEquals("SONE-001", fields[0].value)
        assertEquals("Old title", fields[1].value)
        assertEquals("Actor A, Actor B", fields[2].value)
        assertNull(fields[2].items)
        assertEquals("Old summary", fields[3].value)
        assertEquals("9.5", fields[4].value)
        assertEquals("Tag A, Tag B", fields[9].value)
        assertEquals(listOf(VideoFieldItem("Tag A"), VideoFieldItem("Tag B")), fields[9].items)
        assertEquals("Example", fields[10].value)
        assertEquals("https://example.com/movie", fields[11].value)
        assertTrue(fields.all { it.value.isEmpty() || it.value.isNotBlank() })
    }

    @Test
    fun `actress edit field parses text input`() {
        val original = Video(
            number = "SONE-001",
            actresses = listOf("Actor A", "Actor B", "Actor C"),
            tags = listOf("Tag A", "Tag B")
        )
        val fields = videoFieldEditValues(original, TranslationEn())

        val edited = videoFromEditFields(
            original,
            fields.mapIndexed { index, field ->
                if (index == 2) field.copy(value = "Actor A, Actor C\nNew Actor") else field
            }
        )

        assertEquals(listOf("Actor A", "Actor C", "New Actor"), edited?.actresses)
    }

    @Test
    fun `tag edit field toggles selection and saves selected values only`() {
        val original = Video(
            number = "SONE-001",
            actresses = listOf("Actor A", "Actor B", "Actor C"),
            tags = listOf("Tag A", "Tag B")
        )
        val fields = videoFieldEditValues(original, TranslationEn())
        val editedFields = updateVideoEditItemSelection(
            fields,
            fieldIndex = 9,
            itemIndex = 0,
            selected = false
        )

        assertNull(editedFields[2].items)
        assertEquals(
            listOf(VideoFieldItem("Tag A", selected = false), VideoFieldItem("Tag B")),
            editedFields[9].items
        )

        val edited = videoFromEditFields(original, editedFields)
        assertEquals(listOf("Actor A", "Actor B", "Actor C"), edited?.actresses)
        assertEquals(listOf("Tag B"), edited?.tags)
    }

    @Test
    fun `adding a tag selects it by default and blank input is ignored`() {
        val original = Video(number = "SONE-001", tags = listOf("Tag A"))
        val fields = videoFieldEditValues(original, TranslationEn())

        val withTag = addVideoEditItem(fields, fieldIndex = 9, value = " New Tag ")
        val withBlank = addVideoEditItem(withTag, fieldIndex = 9, value = "   ")

        assertEquals(
            listOf(VideoFieldItem("Tag A"), VideoFieldItem("New Tag")),
            withBlank[9].items
        )
        assertEquals(listOf("Tag A", "New Tag"), videoFromEditFields(original, withBlank)?.tags)
    }

    @Test
    fun `deselecting every tag saves an empty tag list`() {
        val original = Video(number = "SONE-001", tags = listOf("Tag A", "Tag B"))
        val fields = videoFieldEditValues(original, TranslationEn())
        val noneSelected = fields.mapIndexed { index, field ->
            if (index != 9) field else field.copy(
                items = field.items?.map { it.copy(selected = false) }
            )
        }

        assertEquals(emptyList(), videoFromEditFields(original, noneSelected)?.tags)
    }

    @Test
    fun `edited fields convert back to video with list and numeric fields`() {
        val original = Video(
            number = "SONE-001",
            actresses = listOf("Actor A", "Actor B", "Actor C"),
            tags = listOf("Tag A", "Tag B"),
            date = "2026-01-01",
            duration = 125,
            coverUrl = "https://example.com/cover.jpg",
            posterUrl = "https://example.com/poster.jpg",
            sampleImages = listOf("https://example.com/a.jpg", "https://example.com/b.jpg"),
            path = "F:/Videos/SONE-001.mp4"
        )
        val fields = videoFieldEditValues(original, TranslationEn())
            .mapIndexed { index, field ->
                when (index) {
                    0 -> field.copy(value = "ABC-001")
                    1 -> field.copy(value = "New title")
                    3 -> field.copy(value = "Updated summary")
                    4 -> field.copy(value = "9.5")
                    10 -> field.copy(value = "Updated source")
                    11 -> field.copy(value = "https://example.com/updated")
                    else -> field
                }
            }

        val edited = videoFromEditFields(original, fields)

        assertEquals("ABC-001", edited?.number)
        assertEquals("New title", edited?.title)
        assertEquals(listOf("Actor A", "Actor B", "Actor C"), edited?.actresses)
        assertEquals("Updated summary", edited?.summary)
        assertEquals(9.5, edited?.rating)
        assertEquals(listOf("Tag A", "Tag B"), edited?.tags)
        assertEquals("Updated source", edited?.source)
        assertEquals("https://example.com/updated", edited?.detailUrl)
        assertEquals("2026-01-01", edited?.date)
        assertEquals(125, edited?.duration)
        assertEquals("https://example.com/cover.jpg", edited?.coverUrl)
        assertEquals("https://example.com/poster.jpg", edited?.posterUrl)
        assertEquals(2, edited?.sampleImages?.size)
        assertEquals("F:/Videos/SONE-001.mp4", edited?.path)
    }

    @Test
    fun `invalid numeric fields reject conversion`() {
        val original = Video(number = "SONE-001")
        val fields = videoFieldEditValues(original, TranslationEn())

        val invalidRating = videoFromEditFields(
            original,
            fields.mapIndexed { index, field -> if (index == 4) field.copy(value = "-1") else field }
        )

        assertNull(invalidRating)
    }

    @Test
    fun `blank rating remains nullable and non editable fields are preserved`() {
        val original = Video(number = "SONE-001", duration = 125)
        val fields = videoFieldEditValues(original, TranslationEn())

        val edited = videoFromEditFields(original, fields)

        assertEquals(125, edited?.duration)
        assertEquals(null, edited?.rating)
    }

    @Test
    fun `rating slider value is bounded and formatted to one decimal`() {
        assertEquals(0.0f, editRatingSliderValue(""))
        assertEquals(0.0f, editRatingSliderValue("-1"))
        assertEquals(9.5f, editRatingSliderValue("9.5"))
        assertEquals(10.0f, editRatingSliderValue("12.3"))

        assertEquals("0.0", formatEditRating(0.04f))
        assertEquals("9.6", formatEditRating(9.56f))
        assertEquals("10.0", formatEditRating(10.0f))
    }

    @Test
    fun `edit rating field normalizes existing values to slider precision`() {
        val fields = videoFieldEditValues(
            Video(number = "SONE-001", rating = 9.56),
            TranslationEn()
        )
        val aboveRange = videoFieldEditValues(
            Video(number = "SONE-002", rating = 12.3),
            TranslationEn()
        )

        assertEquals("9.6", fields[4].value)
        assertEquals("10.0", aboveRange[4].value)
    }

    @Test
    fun `rating edit rejects values above slider range`() {
        val original = Video(number = "SONE-001")
        val fields = videoFieldEditValues(original, TranslationEn())
            .mapIndexed { index, field ->
                if (index == 4) field.copy(value = "10.1") else field
            }

        assertNull(videoFromEditFields(original, fields))
    }
}
