package javscraper.controllers

import javscraper.models.Video
import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkPreviewControllerTest {

    @Test
    fun `controller accumulates candidates across searches and can clear`() {
        val controller = NetworkPreviewController()

        controller.add(listOf(Video(number = "ABC-001", detailUrl = "https://example.com/1")))
        controller.add(listOf(
            Video(number = "ABC-002", detailUrl = "https://example.com/2"),
            Video(number = "ABC-001", detailUrl = "https://example.com/1")
        ))

        assertEquals(listOf("ABC-001", "ABC-002"), controller.candidates.map { it.number })

        controller.clear()

        assertEquals(emptyList(), controller.candidates)
    }
}