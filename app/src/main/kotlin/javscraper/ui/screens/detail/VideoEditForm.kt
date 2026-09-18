package javscraper.ui.screens.detail

import javscraper.i18n.TranslationEn
import javscraper.models.Video
import javscraper.ui.VideoFieldValue
import javscraper.ui.videoFieldValues

internal fun videoEditFields(video: Video, t: TranslationEn): List<VideoFieldValue> =
    videoFieldValues(video, t)

internal fun videoFromEditFields(
    original: Video,
    fields: List<VideoFieldValue>
): Video? {
    if (fields.size != 17) return null

    val values = fields.map { it.value.trim() }
    val number = values[0]
    val duration = if (values[9].isBlank()) null else parsePositiveInt(values[9]) ?: return null
    val rating = if (values[10].isBlank()) null else parseRating(values[10]) ?: return null
    if (number.isBlank()) return null

    return original.copy(
        number = number,
        title = values[1],
        actresses = parseList(values[2]),
        date = values[3],
        summary = values[4],
        maker = values[5],
        label = values[6],
        series = values[7],
        director = values[8],
        duration = duration,
        rating = rating,
        tags = parseList(values[11]).distinct(),
        source = values[12],
        detailUrl = values[13],
        coverUrl = values[14],
        posterUrl = values[15],
        sampleImages = parseList(values[16]).distinct(),
        path = original.path
    )
}

private fun parsePositiveInt(value: String): Int? {
    if (value.isBlank()) return null
    return value.toIntOrNull()?.takeIf { it > 0 }
}

private fun parseRating(value: String): Double? {
    if (value.isBlank()) return null
    return value.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }
}

private fun parseList(value: String): List<String> =
    value.split(',', '，', '\n')
        .map(String::trim)
        .filter(String::isNotEmpty)
