package javscraper.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class PosterCardTest {
    @Test
    fun `local poster path is resolved beside the video`() {
        val path = localPosterPath("D:/videos/ABP-123.mp4")

        assertEquals("D:/videos/poster.jpg", path.replace('\\', '/'))
    }

    @Test
    fun `local poster path is blank without a parent directory`() {
        assertEquals("", localPosterPath("ABP-123.mp4"))
    }
}