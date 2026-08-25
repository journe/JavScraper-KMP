package javscraper

import javscraper.models.ScannedFile
import kotlin.test.Test
import kotlinx.coroutines.test.TestScope
import kotlin.test.assertEquals

class SingleScrapeControllerTest {

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
