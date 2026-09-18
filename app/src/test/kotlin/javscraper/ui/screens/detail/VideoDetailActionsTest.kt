package javscraper.ui.screens.detail

import javscraper.io.metadata.VideoMetadataEditResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class VideoDetailActionsTest {

    @Test
    fun `crop poster action is hidden by default`() {
        val actions = VideoDetailActions(
            onBack = {},
            onRefresh = {},
            onSaveMetadata = { VideoMetadataEditResult.NfoMissing }
        )

        assertEquals(null, actions.onCropPoster)
    }

    @Test
    fun `crop poster action invokes its callback`() {
        var cropRequested = 0
        val actions = VideoDetailActions(
            onBack = {},
            onRefresh = {},
            onSaveMetadata = { VideoMetadataEditResult.NfoMissing },
            onCropPoster = { cropRequested++ }
        )

        val onCropPoster = assertNotNull(actions.onCropPoster)
        onCropPoster()
        onCropPoster()

        assertEquals(2, cropRequested)
    }
}