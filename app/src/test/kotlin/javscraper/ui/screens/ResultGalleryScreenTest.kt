package javscraper.ui.screens

import javscraper.models.ScannedFile
import javscraper.models.Video
import kotlin.test.Test
import kotlin.test.assertEquals

class ResultGalleryScreenTest {
    @Test
    fun `gallery videos use nfo metadata and source path`() {
        val metadata = Video(number = "ABP-123", title = "Metadata title")
        val state = GalleryState(
            scrapedFiles = listOf(
                ScannedFile(
                    path = "D:/videos/ABP-123.mp4",
                    fileName = "ABP-123.mp4",
                    number = "ABP-123",
                    isScraped = true,
                    metadata = metadata
                )
            ),
            outputDir = "D:/output"
        )

        assertEquals(
            listOf(metadata.copy(path = "D:/videos/ABP-123.mp4")),
            state.videos
        )
    }

    @Test
    fun `gallery videos fall back to file info without metadata`() {
        val state = GalleryState(
            scrapedFiles = listOf(
                ScannedFile(
                    path = "D:/videos/movie.mp4",
                    fileName = "movie.mp4",
                    number = "ABC-001",
                    isScraped = true
                )
            ),
            outputDir = "D:/output"
        )

        assertEquals(
            listOf(Video(number = "ABC-001", title = "movie.mp4", path = "D:/videos/movie.mp4")),
            state.videos
        )
    }
}
