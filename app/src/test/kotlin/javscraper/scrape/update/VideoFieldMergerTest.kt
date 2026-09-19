package javscraper.scrape.update

import javscraper.models.ExistingVideoMetadata
import javscraper.models.Ranking
import javscraper.models.Review
import javscraper.models.Video
import javscraper.models.VideoFieldUpdateChoiceStatus
import javscraper.models.VideoUpdateField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoFieldMergerTest {
    @Test
    fun `missing old fields with incoming values are forced additions`() {
        val existing = ExistingVideoMetadata(Video(number = "OLD-001", title = "Old title"), emptySet())
        val incoming = Video(number = "OLD-001", summary = "New plot", maker = "New maker")

        val choices = VideoFieldUpdatePlanner.choices(existing, incoming)

        val summary = choices.single { it.field == VideoUpdateField.SUMMARY }
        assertEquals(VideoFieldUpdateChoiceStatus.NEW_FIELD, summary.status)
        assertTrue(summary.selected)
        assertTrue(summary.enabled)
    }

    @Test
    fun `selected overlapping scalar field replaces old value`() {
        val existing = ExistingVideoMetadata(
            Video(number = "OLD-001", title = "Old title", summary = "Old plot"),
            setOf(VideoUpdateField.NUMBER, VideoUpdateField.TITLE, VideoUpdateField.SUMMARY)
        )
        val incoming = Video(number = "NEW-001", title = "New title", summary = "New plot")

        val merged = VideoFieldMerger.merge(
            existing = existing,
            incoming = incoming,
            selectedFields = setOf(VideoUpdateField.SUMMARY)
        )

        assertEquals("OLD-001", merged.number)
        assertEquals("Old title", merged.title)
        assertEquals("New plot", merged.summary)
    }

    @Test
    fun `unselected overlapping field keeps old value`() {
        val existing = ExistingVideoMetadata(
            Video(number = "OLD-001", title = "Old title"),
            setOf(VideoUpdateField.NUMBER, VideoUpdateField.TITLE)
        )
        val incoming = Video(number = "NEW-001", title = "New title")

        val merged = VideoFieldMerger.merge(existing, incoming, emptySet())

        assertEquals("OLD-001", merged.number)
        assertEquals("Old title", merged.title)
    }

    @Test
    fun `blank incoming scalar never overwrites existing value`() {
        val existing = ExistingVideoMetadata(
            Video(number = "OLD-001", summary = "Old plot"),
            setOf(VideoUpdateField.NUMBER, VideoUpdateField.SUMMARY)
        )
        val incoming = Video(number = "NEW-001")

        val merged = VideoFieldMerger.merge(
            existing,
            incoming,
            setOf(VideoUpdateField.SUMMARY)
        )

        assertEquals("Old plot", merged.summary)
    }

    @Test
    fun `selected list fields merge old and incoming values without duplicates`() {
        val existing = ExistingVideoMetadata(
            Video(
                number = "ABC-001",
                actresses = listOf("Alice", "Beth"),
                tags = listOf("Drama", "Old tag"),
                rankings = listOf(Ranking(1, "Weekly")),
                reviews = listOf(Review(id = "r1", author = "Alice", content = "Good"))
            ),
            setOf(
                VideoUpdateField.ACTRESSES,
                VideoUpdateField.TAGS,
                VideoUpdateField.RANKINGS,
                VideoUpdateField.REVIEWS
            )
        )
        val incoming = Video(
            number = "ABC-001",
            actresses = listOf("Beth", "Cara"),
            tags = listOf("Old tag", "New tag"),
            rankings = listOf(Ranking(2, "Weekly"), Ranking(1, "Monthly")),
            reviews = listOf(
                Review(id = "r1", author = "Different", content = "Changed"),
                Review(id = "r2", author = "Bob", content = "Nice")
            )
        )

        val merged = VideoFieldMerger.merge(
            existing,
            incoming,
            setOf(
                VideoUpdateField.ACTRESSES,
                VideoUpdateField.TAGS,
                VideoUpdateField.RANKINGS,
                VideoUpdateField.REVIEWS
            )
        )

        assertEquals(listOf("Alice", "Beth", "Cara"), merged.actresses)
        assertEquals(listOf("Drama", "Old tag", "New tag"), merged.tags)
        assertEquals(listOf(Ranking(1, "Weekly"), Ranking(1, "Monthly")), merged.rankings)
        assertEquals(
            listOf(
                Review(id = "r1", author = "Alice", content = "Good"),
                Review(id = "r2", author = "Bob", content = "Nice")
            ),
            merged.reviews
        )
    }

    @Test
    fun `field update keeps old image fields but carries incoming webpage payload`() {
        val existing = ExistingVideoMetadata(
            Video(
                number = "ABC-001",
                coverUrl = "https://old.invalid/cover.jpg",
                posterUrl = "https://old.invalid/poster.jpg",
                sampleImages = listOf("https://old.invalid/sample.jpg")
            ),
            setOf(VideoUpdateField.NUMBER)
        )
        val incoming = Video(
            number = "ABC-001",
            coverUrl = "https://new.invalid/cover.jpg",
            posterUrl = "https://new.invalid/poster.jpg",
            sampleImages = listOf("https://new.invalid/sample.jpg"),
            webpage = "webpage-base64"
        )

        val merged = VideoFieldMerger.merge(existing, incoming, emptySet())

        assertEquals("https://old.invalid/cover.jpg", merged.coverUrl)
        assertEquals("https://old.invalid/poster.jpg", merged.posterUrl)
        assertEquals(listOf("https://old.invalid/sample.jpg"), merged.sampleImages)
        assertEquals("webpage-base64", merged.webpage)
        assertFalse(merged === existing.video)
    }
}