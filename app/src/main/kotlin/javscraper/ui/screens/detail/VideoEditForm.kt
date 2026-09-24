package javscraper.ui.screens.detail

import javscraper.models.Video
import javscraper.ui.VideoFieldValue
import javscraper.ui.VideoFieldItem

internal const val VIDEO_EDIT_RATING_FIELD_INDEX = 4

internal fun videoFromEditFields(
    original: Video,
    fields: List<VideoFieldValue>
): Video? {
    if (fields.size != 12) return null

    val values = fields.map { it.value.trim() }
    val actresses = selectedFieldValues(fields[2]) ?: return null
    val tags = selectedFieldValues(fields[9]) ?: return null
    val number = values[0]
    val rating = if (values[4].isBlank()) null else parseRating(values[4]) ?: return null
    if (number.isBlank()) return null

    return original.copy(
        number = number,
        title = values[1],
        actresses = actresses,
        summary = values[3],
        rating = rating,
        maker = values[5],
        label = values[6],
        series = values[7],
        director = values[8],
        tags = tags.distinct(),
        source = values[10],
        detailUrl = values[11],
        path = original.path
    )
}

private fun parseRating(value: String): Double? {
    if (value.isBlank()) return null
    return value.toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..10.0 }
}

private fun selectedFieldValues(field: VideoFieldValue): List<String>? =
    field.items?.filter { it.selected }?.map { it.value }

private fun VideoFieldValue.withItems(updatedItems: List<VideoFieldItem>): VideoFieldValue = copy(
    value = updatedItems.filter { it.selected }.joinToString(", "),
    items = updatedItems
)

internal fun updateVideoEditItemSelection(
    fields: List<VideoFieldValue>,
    fieldIndex: Int,
    itemIndex: Int,
    selected: Boolean
): List<VideoFieldValue> {
    val field = fields.getOrNull(fieldIndex) ?: return fields
    val items = field.items ?: return fields
    if (itemIndex !in items.indices) return fields

    val updatedItems = items.mapIndexed { index, item ->
        if (index == itemIndex) item.copy(selected = selected) else item
    }
    val updatedField = field.withItems(updatedItems)
    return fields.mapIndexed { index, current ->
        if (index == fieldIndex) updatedField else current
    }
}

internal fun addVideoEditItem(
    fields: List<VideoFieldValue>,
    fieldIndex: Int,
    value: String
): List<VideoFieldValue> {
    val newValue = value.trim()
    if (newValue.isEmpty()) return fields
    val field = fields.getOrNull(fieldIndex) ?: return fields
    val items = field.items ?: return fields

    val updatedItems = items
        .map { item -> if (item.value == newValue) item.copy(selected = true) else item }
        .let { current ->
            if (current.any { it.value == newValue }) current else current + VideoFieldItem(newValue)
        }
    val updatedField = field.withItems(updatedItems)
    return fields.mapIndexed { index, current ->
        if (index == fieldIndex) updatedField else current
    }
}
