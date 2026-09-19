package javscraper.ui

import javscraper.i18n.TranslationEn
import javscraper.models.Video
import kotlin.math.roundToInt

internal data class VideoFieldValue(
    val label: String,
    val value: String,
    val items: List<VideoFieldItem>? = null
)

internal data class VideoFieldItem(
    val value: String,
    val selected: Boolean = true
)

private fun listFieldValue(label: String, values: List<String>) = VideoFieldValue(
    label = label,
    value = values.joinToString(", "),
    items = values.map(::VideoFieldItem)
)

internal fun videoFieldValues(video: Video, t: TranslationEn): List<VideoFieldValue> = listOf(
    VideoFieldValue(t.commonNumber, video.number),
    VideoFieldValue(t.videoFieldDate, video.date),
    listFieldValue(t.videoFieldActresses, video.actresses),
    VideoFieldValue(t.videoFieldTitle, video.title),
    VideoFieldValue(t.videoFieldSummary, video.summary),
    VideoFieldValue(t.videoFieldMaker, video.maker),
    VideoFieldValue(t.videoFieldLabel, video.label),
    VideoFieldValue(t.videoFieldSeries, video.series),
    VideoFieldValue(t.videoFieldDirector, video.director),
    VideoFieldValue(t.videoFieldDuration, video.duration?.toString().orEmpty()),
    VideoFieldValue(t.videoFieldRating, video.rating?.toString().orEmpty()),
    listFieldValue(t.videoFieldTags, video.tags),
    VideoFieldValue(t.videoFieldSource, video.source),
    VideoFieldValue(t.videoFieldDetailUrl, video.detailUrl),
    VideoFieldValue(t.videoFieldCoverUrl, video.coverUrl),
    VideoFieldValue(t.videoFieldPosterUrl, video.posterUrl),
    VideoFieldValue(t.videoFieldSampleImages, video.sampleImages.joinToString("\n"))
)

internal fun editRatingSliderValue(value: String): Float =
    value.toFloatOrNull()
        ?.takeIf { it.isFinite() }
        ?.coerceIn(0.0f, 10.0f)
        ?: 0.0f

internal fun formatEditRating(value: Float): String {
    val tenths = (value.coerceIn(0.0f, 10.0f) * 10).roundToInt()
    return "${tenths / 10}.${tenths % 10}"
}

internal fun videoFieldEditValues(video: Video, t: TranslationEn): List<VideoFieldValue> = listOf(
    VideoFieldValue(t.commonNumber, video.number),
    VideoFieldValue(t.videoFieldTitle, video.title),
    VideoFieldValue(t.videoFieldActresses, video.actresses.joinToString(", ")),
    VideoFieldValue(t.videoFieldSummary, video.summary),
    VideoFieldValue(t.videoFieldRating, video.rating?.let { formatEditRating(it.toFloat()) }.orEmpty()),
    VideoFieldValue(t.videoFieldMaker, video.maker),
    VideoFieldValue(t.videoFieldLabel, video.label),
    VideoFieldValue(t.videoFieldSeries, video.series),
    VideoFieldValue(t.videoFieldDirector, video.director),
    listFieldValue(t.videoFieldTags, video.tags),
    VideoFieldValue(t.videoFieldSource, video.source),
    VideoFieldValue(t.videoFieldDetailUrl, video.detailUrl),
)
