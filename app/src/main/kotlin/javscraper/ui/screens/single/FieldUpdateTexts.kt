package javscraper.ui.screens.single

import javscraper.i18n.TranslationEn
import javscraper.models.VideoFieldUpdateChoiceStatus
import javscraper.models.VideoUpdateField

internal fun fieldUpdateLabel(field: VideoUpdateField, t: TranslationEn): String = when (field) {
    VideoUpdateField.NUMBER -> t.commonNumber
    VideoUpdateField.TITLE -> t.videoFieldTitle
    VideoUpdateField.DATE -> t.videoFieldDate
    VideoUpdateField.ACTRESSES -> t.videoFieldActresses
    VideoUpdateField.SUMMARY -> t.videoFieldSummary
    VideoUpdateField.MAKER -> t.videoFieldMaker
    VideoUpdateField.LABEL -> t.videoFieldLabel
    VideoUpdateField.SERIES -> t.videoFieldSeries
    VideoUpdateField.DIRECTOR -> t.videoFieldDirector
    VideoUpdateField.DURATION -> t.videoFieldDuration
    VideoUpdateField.RATING -> t.videoFieldRating
    VideoUpdateField.WANT_COUNT -> t.videoFieldWantCount
    VideoUpdateField.WATCHED_COUNT -> t.videoFieldWatchedCount
    VideoUpdateField.RATING_COUNT -> t.videoFieldRatingCount
    VideoUpdateField.TAGS -> t.videoFieldTags
    VideoUpdateField.RANKINGS -> t.videoFieldRankings
    VideoUpdateField.REVIEWS -> t.videoFieldReviews
    VideoUpdateField.SOURCE -> t.videoFieldSource
    VideoUpdateField.DETAIL_URL -> t.videoFieldDetailUrl
}

internal fun fieldUpdateStatusText(
    status: VideoFieldUpdateChoiceStatus,
    t: TranslationEn
): String = when (status) {
    VideoFieldUpdateChoiceStatus.NEW_FIELD -> t.singleScrapeFieldUpdateStatusNew
    VideoFieldUpdateChoiceStatus.OVERWRITE -> t.singleScrapeFieldUpdateStatusOverwrite
    VideoFieldUpdateChoiceStatus.SAME -> t.singleScrapeFieldUpdateStatusSame
    VideoFieldUpdateChoiceStatus.EMPTY_INCOMING -> t.singleScrapeFieldUpdateStatusEmptyIncoming
}