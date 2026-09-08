package javscraper.ui.screens

import javscraper.models.ScannedFile
import javscraper.models.Video
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ResultGalleryScreenTest {
    @Test
    fun `gallery merges session results and prefers rescraped video`() {
        val staleMetadata = Video(number = "ABP-123", title = "Old title")
        val rescraped = Video(number = "ABP-123", title = "New title", path = "D:/output/ABP-123.mp4")
        val fresh = Video(number = "STARS-804", title = "Fresh", path = "D:/output/STARS-804.mp4")
        val state = GalleryState(
            scrapedFiles = listOf(
                ScannedFile(
                    path = "D:/videos/ABP-123.mp4",
                    fileName = "ABP-123.mp4",
                    number = "ABP-123",
                    isScraped = true,
                    metadata = staleMetadata
                )
            ),
            outputDir = "D:/output",
            sessionResults = listOf(rescraped, fresh)
        )

        assertEquals(listOf(rescraped, fresh), state.videos)
        assertEquals(fresh, state.videoByPath("D:/output/STARS-804.mp4"))
    }

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
    fun `gallery detail resolves video by source path`() {
        val video = Video(number = "ABP-123", title = "Metadata title")
        val state = GalleryState(
            scrapedFiles = listOf(
                ScannedFile(
                    path = "D:/videos/ABP-123.mp4",
                    fileName = "ABP-123.mp4",
                    number = "ABP-123",
                    isScraped = true,
                    metadata = video
                )
            ),
            outputDir = "D:/output"
        )

        assertEquals(
            video.copy(path = "D:/videos/ABP-123.mp4"),
            state.videoByPath("D:/videos/ABP-123.mp4")
        )
        assertNull(state.videoByPath("D:/videos/missing.mp4"))
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
