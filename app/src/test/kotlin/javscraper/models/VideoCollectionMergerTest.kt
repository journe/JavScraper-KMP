package javscraper.models

import kotlin.test.Test
import kotlin.test.assertEquals

class VideoCollectionMergerTest {
    @Test
    fun `rankings keep existing entries and add named incoming entries once`() {
        val existing = listOf(Ranking(1, "Weekly"), Ranking(2, ""))
        val incoming = listOf(
            Ranking(3, " weekly "),
            Ranking(1, "Monthly"),
            Ranking(4, "")
        )

        val merged = VideoCollectionMerger.mergeRankings(existing, incoming)

        assertEquals(listOf(Ranking(1, "Weekly"), Ranking(2, ""), Ranking(1, "Monthly")), merged)
    }

    @Test
    fun `reviews deduplicate by id and fallback key while keeping existing entries`() {
        val existing = listOf(
            Review(id = "r1", author = "Alice", content = "Good"),
            Review(id = "", author = "Bob", date = "2026-01-01", content = "Old")
        )
        val incoming = listOf(
            Review(id = "r1", author = "Different", content = "Changed"),
            Review(id = "", author = "Bob", date = "2026-01-01", content = "Old"),
            Review(id = "r2", author = "Cara", content = "Nice"),
            Review(id = "", author = "", date = "", content = "")
        )

        val merged = VideoCollectionMerger.mergeReviews(existing, incoming)

        assertEquals(
            listOf(
                Review(id = "r1", author = "Alice", content = "Good"),
                Review(id = "", author = "Bob", date = "2026-01-01", content = "Old"),
                Review(id = "r2", author = "Cara", content = "Nice")
            ),
            merged
        )
    }
}