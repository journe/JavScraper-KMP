package javscraper.models

enum class VideoUpdateField {
    NUMBER,
    TITLE,
    DATE,
    ACTRESSES,
    SUMMARY,
    MAKER,
    LABEL,
    SERIES,
    DIRECTOR,
    DURATION,
    RATING,
    WANT_COUNT,
    WATCHED_COUNT,
    RATING_COUNT,
    TAGS,
    RANKINGS,
    REVIEWS,
    SOURCE,
    DETAIL_URL
}

data class ExistingVideoMetadata(
    val video: Video,
    val presentFields: Set<VideoUpdateField>
)

enum class VideoFieldUpdateChoiceStatus {
    NEW_FIELD,
    OVERWRITE,
    SAME,
    EMPTY_INCOMING
}

data class VideoFieldUpdateChoice(
    val field: VideoUpdateField,
    val status: VideoFieldUpdateChoiceStatus,
    val selected: Boolean,
    val enabled: Boolean,
    val oldValue: String,
    val newValue: String
) {
    fun withSelection(value: Boolean): VideoFieldUpdateChoice =
        if (enabled) copy(selected = value) else this
}