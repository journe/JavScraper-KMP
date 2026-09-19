package javscraper.scrape.update

import javscraper.models.ExistingVideoMetadata
import javscraper.models.Video
import javscraper.models.VideoCollectionMerger
import javscraper.models.VideoFieldUpdateChoice
import javscraper.models.VideoFieldUpdateChoiceStatus
import javscraper.models.VideoUpdateField

object VideoFieldUpdatePlanner {
    fun choices(
        existing: ExistingVideoMetadata,
        incoming: Video
    ): List<VideoFieldUpdateChoice> = VideoUpdateField.entries.map { field ->
        val oldValue = field.updateFieldText(existing.video)
        val newValue = field.updateFieldText(incoming)
        val present = field in existing.presentFields
        val incomingBlank = newValue.isBlank()
        val status = when {
            !present && !incomingBlank -> VideoFieldUpdateChoiceStatus.NEW_FIELD
            !present -> VideoFieldUpdateChoiceStatus.EMPTY_INCOMING
            incomingBlank -> VideoFieldUpdateChoiceStatus.EMPTY_INCOMING
            oldValue == newValue -> VideoFieldUpdateChoiceStatus.SAME
            else -> VideoFieldUpdateChoiceStatus.OVERWRITE
        }
        VideoFieldUpdateChoice(
            field = field,
            status = status,
            selected = status == VideoFieldUpdateChoiceStatus.NEW_FIELD,
            enabled = status == VideoFieldUpdateChoiceStatus.NEW_FIELD ||
                status == VideoFieldUpdateChoiceStatus.OVERWRITE,
            oldValue = oldValue,
            newValue = newValue
        )
    }
}

object VideoFieldMerger {
    fun merge(
        existing: ExistingVideoMetadata,
        incoming: Video,
        selectedFields: Set<VideoUpdateField>
    ): Video {
        var merged = existing.video.copy(webpage = incoming.webpage, version = incoming.version)
        VideoUpdateField.entries.forEach { field ->
            if (field !in selectedFields) return@forEach
            merged = field.applySelected(merged, existing.video, incoming)
        }
        return merged
    }

    private fun VideoUpdateField.applySelected(
        merged: Video,
        existing: Video,
        incoming: Video
    ): Video = when (this) {
        VideoUpdateField.NUMBER -> merged.copy(number = incoming.number.takeIf { it.isNotBlank() } ?: merged.number)
        VideoUpdateField.TITLE -> merged.copy(title = incoming.title.takeIf { it.isNotBlank() } ?: merged.title)
        VideoUpdateField.DATE -> merged.copy(date = incoming.date.takeIf { it.isNotBlank() } ?: merged.date)
        VideoUpdateField.SUMMARY -> merged.copy(summary = incoming.summary.takeIf { it.isNotBlank() } ?: merged.summary)
        VideoUpdateField.MAKER -> merged.copy(maker = incoming.maker.takeIf { it.isNotBlank() } ?: merged.maker)
        VideoUpdateField.LABEL -> merged.copy(label = incoming.label.takeIf { it.isNotBlank() } ?: merged.label)
        VideoUpdateField.SERIES -> merged.copy(series = incoming.series.takeIf { it.isNotBlank() } ?: merged.series)
        VideoUpdateField.DIRECTOR -> merged.copy(director = incoming.director.takeIf { it.isNotBlank() } ?: merged.director)
        VideoUpdateField.DURATION -> incoming.duration?.takeIf { it > 0 }?.let { merged.copy(duration = it) } ?: merged
        VideoUpdateField.RATING -> incoming.rating?.takeIf { it > 0 }?.let { merged.copy(rating = it) } ?: merged
        VideoUpdateField.WANT_COUNT -> incoming.wantCount?.let { merged.copy(wantCount = it) } ?: merged
        VideoUpdateField.WATCHED_COUNT -> incoming.watchedCount?.let { merged.copy(watchedCount = it) } ?: merged
        VideoUpdateField.RATING_COUNT -> incoming.ratingCount?.let { merged.copy(ratingCount = it) } ?: merged
        VideoUpdateField.SOURCE -> merged.copy(source = incoming.source.takeIf { it.isNotBlank() } ?: merged.source)
        VideoUpdateField.DETAIL_URL -> merged.copy(
            detailUrl = incoming.detailUrl.takeIf { it.isNotBlank() } ?: merged.detailUrl
        )
        VideoUpdateField.ACTRESSES -> merged.copy(actresses = mergeText(existing.actresses, incoming.actresses))
        VideoUpdateField.TAGS -> merged.copy(tags = mergeText(existing.tags, incoming.tags))
        VideoUpdateField.RANKINGS -> merged.copy(rankings = VideoCollectionMerger.mergeRankings(existing.rankings, incoming.rankings))
        VideoUpdateField.REVIEWS -> merged.copy(reviews = VideoCollectionMerger.mergeReviews(existing.reviews, incoming.reviews))
    }

    private fun mergeText(existing: List<String>, incoming: List<String>): List<String> =
        (existing + incoming).map { it.trim() }.filter { it.isNotBlank() }.distinct()

}

private fun VideoUpdateField.updateFieldText(video: Video): String = when (this) {
        VideoUpdateField.NUMBER -> video.number
        VideoUpdateField.TITLE -> video.title
        VideoUpdateField.DATE -> video.date
        VideoUpdateField.ACTRESSES -> video.actresses.joinToString(", ")
        VideoUpdateField.SUMMARY -> video.summary
        VideoUpdateField.MAKER -> video.maker
        VideoUpdateField.LABEL -> video.label
        VideoUpdateField.SERIES -> video.series
        VideoUpdateField.DIRECTOR -> video.director
        VideoUpdateField.DURATION -> video.duration?.takeIf { it > 0 }?.toString().orEmpty()
        VideoUpdateField.RATING -> video.rating?.takeIf { it > 0 }?.toString().orEmpty()
        VideoUpdateField.WANT_COUNT -> video.wantCount?.toString().orEmpty()
        VideoUpdateField.WATCHED_COUNT -> video.watchedCount?.toString().orEmpty()
        VideoUpdateField.RATING_COUNT -> video.ratingCount?.toString().orEmpty()
        VideoUpdateField.TAGS -> video.tags.joinToString(", ")
        VideoUpdateField.RANKINGS -> video.rankings.joinToString(", ") { "No.${it.rank} ${it.listName}" }
        VideoUpdateField.REVIEWS -> video.reviews.joinToString("\n") {
            listOf(it.author, it.date, it.content).filter { value -> value.isNotBlank() }.joinToString(" - ")
        }
        VideoUpdateField.SOURCE -> video.source
        VideoUpdateField.DETAIL_URL -> video.detailUrl
}
