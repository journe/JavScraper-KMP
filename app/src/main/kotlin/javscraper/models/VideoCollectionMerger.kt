package javscraper.models

object VideoCollectionMerger {
    fun mergeRankings(existing: List<Ranking>, incoming: List<Ranking>): List<Ranking> {
        val names = existing.mapTo(mutableSetOf()) { it.listName.trim().lowercase() }
        return existing + incoming.filter { ranking ->
            ranking.listName.isNotBlank() && names.add(ranking.listName.trim().lowercase())
        }
    }

    fun mergeReviews(existing: List<Review>, incoming: List<Review>): List<Review> {
        val keys = existing.mapTo(mutableSetOf(), ::reviewKey)
        return existing + incoming.filter { review ->
            review.content.isNotBlank() && keys.add(reviewKey(review))
        }
    }

    private fun reviewKey(review: Review): String =
        review.id.ifBlank { listOf(review.author, review.date, review.content).joinToString("|") }
}