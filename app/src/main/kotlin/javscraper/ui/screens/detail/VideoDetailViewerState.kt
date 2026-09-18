package javscraper.ui.screens.detail

internal enum class PosterViewerState {
    HIDDEN,
    VISIBLE,
    EXITING
}

internal enum class DetailEscapeAction {
    CLOSE_EXTRA_FANART_VIEWER,
    CLOSE_POSTER_VIEWER,
    IGNORE,
    BACK
}

internal fun detailEscapeAction(
    imageViewerIndex: Int?,
    posterViewerState: PosterViewerState
): DetailEscapeAction = when {
    imageViewerIndex != null -> DetailEscapeAction.CLOSE_EXTRA_FANART_VIEWER
    posterViewerState == PosterViewerState.VISIBLE -> DetailEscapeAction.CLOSE_POSTER_VIEWER
    posterViewerState == PosterViewerState.EXITING -> DetailEscapeAction.IGNORE
    else -> DetailEscapeAction.BACK
}

internal fun shouldRestoreDetailFocus(
    imageViewerIndex: Int?,
    posterViewerState: PosterViewerState
): Boolean = imageViewerIndex == null && posterViewerState == PosterViewerState.HIDDEN
