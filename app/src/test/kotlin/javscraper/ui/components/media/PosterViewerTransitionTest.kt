package javscraper.ui.components.media

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PosterViewerTransitionTest {

    @Test
    fun `poster viewer key is stable and separate from fanart carousel keys`() {
        val poster = File("F:/Videos/ABC-001/fanart.jpg")

        val key = posterViewerKey(poster)

        assertEquals("detail-poster-F:\\Videos\\ABC-001\\fanart.jpg", key)
        assertNotEquals(carouselImageKey(poster), key)
    }
}
