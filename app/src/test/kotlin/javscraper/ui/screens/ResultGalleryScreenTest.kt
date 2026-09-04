package javscraper.ui.screens

import javscraper.models.ScannedFile
import javscraper.models.Video
import kotlin.test.Test
import kotlin.test.assertEquals

class ResultGalleryScreenTest {
    @Test
    fun `gallery videos use nfo metadata when available`() {
        val metadata = Video(number = "ABP-123", title = "Metadata title", coverUrl = "cover.jpg")
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

        assertEquals(listOf(metadata), state.entries.map { it.video })
        assertEquals("D:/videos/poster.jpg", state.entries.single().posterPath.replace('\\', '/'))
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
            listOf(Video(number = "ABC-001", title = "movie.mp4")),
            state.entries.map { it.video }
        )
    }
}
