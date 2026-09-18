package javscraper.ui.screens

import javscraper.ui.screens.detail.DetailEscapeAction
import javscraper.ui.screens.detail.PosterViewerState
import javscraper.ui.screens.detail.detailEscapeAction
import javscraper.ui.screens.detail.shouldRestoreDetailFocus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoDetailScreenTest {

    @Test
    fun `detail focus is restored only after every image viewer closes`() {
        assertTrue(shouldRestoreDetailFocus(imageViewerIndex = null, posterViewerState = PosterViewerState.HIDDEN))
        assertFalse(shouldRestoreDetailFocus(imageViewerIndex = 1, posterViewerState = PosterViewerState.HIDDEN))
        assertFalse(shouldRestoreDetailFocus(imageViewerIndex = null, posterViewerState = PosterViewerState.VISIBLE))
        assertFalse(shouldRestoreDetailFocus(imageViewerIndex = null, posterViewerState = PosterViewerState.EXITING))
    }

    @Test
    fun `escape closes poster viewer but is ignored while it is exiting`() {
        assertEquals(
            DetailEscapeAction.CLOSE_POSTER_VIEWER,
            detailEscapeAction(imageViewerIndex = null, posterViewerState = PosterViewerState.VISIBLE)
        )
        assertEquals(
            DetailEscapeAction.IGNORE,
            detailEscapeAction(imageViewerIndex = null, posterViewerState = PosterViewerState.EXITING)
        )
        assertEquals(
            DetailEscapeAction.BACK,
            detailEscapeAction(imageViewerIndex = null, posterViewerState = PosterViewerState.HIDDEN)
        )
    }

    @Test
    fun `escape still prioritizes extra fanart viewer`() {
        assertEquals(
            DetailEscapeAction.CLOSE_EXTRA_FANART_VIEWER,
            detailEscapeAction(imageViewerIndex = 1, posterViewerState = PosterViewerState.VISIBLE)
        )
    }
}
