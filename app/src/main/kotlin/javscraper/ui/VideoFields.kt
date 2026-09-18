package javscraper.ui

import javscraper.i18n.TranslationEn
import javscraper.models.Video

internal data class VideoFieldValue(
    val label: String,
    val value: String
)

internal fun videoFieldValues(video: Video, t: TranslationEn): List<VideoFieldValue> = listOf(
    VideoFieldValue(t.commonNumber, video.number),
    VideoFieldValue(t.videoFieldTitle, video.title),
    VideoFieldValue(t.videoFieldActresses, video.actresses.joinToString(", ")),
    VideoFieldValue(t.videoFieldDate, video.date),
    VideoFieldValue(t.videoFieldSummary, video.summary),
    VideoFieldValue(t.videoFieldMaker, video.maker),
    VideoFieldValue(t.videoFieldLabel, video.label),
    VideoFieldValue(t.videoFieldSeries, video.series),
    VideoFieldValue(t.videoFieldDirector, video.director),
    VideoFieldValue(t.videoFieldDuration, video.duration?.toString().orEmpty()),
    VideoFieldValue(t.videoFieldRating, video.rating?.toString().orEmpty()),
    VideoFieldValue(t.videoFieldTags, video.tags.joinToString(", ")),
    VideoFieldValue(t.videoFieldSource, video.source),
    VideoFieldValue(t.videoFieldDetailUrl, video.detailUrl),
    VideoFieldValue(t.videoFieldCoverUrl, video.coverUrl),
    VideoFieldValue(t.videoFieldPosterUrl, video.posterUrl),
    VideoFieldValue(t.videoFieldSampleImages, video.sampleImages.joinToString("\n"))
)
