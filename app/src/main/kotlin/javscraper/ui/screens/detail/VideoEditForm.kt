package javscraper.ui.screens.detail

import javscraper.models.Video
import javscraper.ui.VideoFieldValue

internal const val VIDEO_EDIT_RATING_FIELD_INDEX = 4

internal fun videoFromEditFields(
    original: Video,
    fields: List<VideoFieldValue>
): Video? {
    if (fields.size != 12) return null

    val values = fields.map { it.value.trim() }
    val number = values[0]
    val rating = if (values[4].isBlank()) null else parseRating(values[4]) ?: return null
    if (number.isBlank()) return null

    return original.copy(
        number = number,
        title = values[1],
        actresses = parseList(values[2]),
        summary = values[3],
        rating = rating,
        maker = values[5],
        label = values[6],
        series = values[7],
        director = values[8],
        tags = parseList(values[9]).distinct(),
        source = values[10],
        detailUrl = values[11],
        path = original.path
    )
}

private fun parseRating(value: String): Double? {
    if (value.isBlank()) return null
    return value.toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..10.0 }
}

private fun parseList(value: String): List<String> =
    value.split(',', '，', '\n')
        .map(String::trim)
        .filter(String::isNotEmpty)
