package javscraper

import javscraper.models.ScannedFile
import javscraper.models.SingleScrapeDialogState
import javscraper.models.Video
import kotlin.test.Test
import kotlinx.coroutines.test.TestScope
import kotlin.test.assertEquals

class SingleScrapeControllerTest {

    @Test
    fun `showing preview publishes network candidates`() {
        val controller = SingleScrapeController(
            scope = TestScope(),
            orch = { null },
            outputDir = { "output" }
        )
        val candidate = Video(number = "ABC-001", detailUrl = "https://example.com/1")
        var published = emptyList<Video>()

        controller.onPreviewCandidates = { published = it }
        controller.showPreview(listOf(candidate))

        assertEquals(listOf(candidate), published)
        assertEquals(
            SingleScrapeDialogState.Preview(listOf(candidate)),
            controller.singleScrapeDialogState
        )
    }

    @Test
    fun `updating number keeps task display in sync`() {
        val controller = SingleScrapeController(
            scope = kotlinx.coroutines.test.TestScope(),
            orch = { null },
            outputDir = { "output" }
        )
        controller.openSingleScrape(
            ScannedFile(path = "video.mp4", fileName = "video.mp4", number = "OLD-001")
        )

        controller.updateSingleScrapeNumber("NEW-001")

        assertEquals("NEW-001", controller.singleScrapeTask?.number)
    }
}
