package javscraper.ui.screens

import javscraper.models.ScannedFile
import javscraper.models.Video
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ResultGalleryScreenTest {
    @Test
    fun `gallery item key is stable across path changes and falls back to path`() {
        val sourceVideo = Video(number = "ABP-123", title = "Old", path = "D:/videos/ABP-123.mp4")
        val outputVideo = Video(number = "ABP-123", title = "New", path = "D:/output/ABP-123.mp4")
        val pathOnlyVideo = Video(number = "", title = "Path only", path = "D:/videos/path-only.mp4")

        assertEquals("ABP-123", galleryListItemKey(sourceVideo))
        assertEquals("ABP-123", galleryListItemKey(outputVideo))
        assertEquals("D:/videos/path-only.mp4", galleryListItemKey(pathOnlyVideo))
    }

    @Test
    fun `gallery source file is resolved by video path`() {
        val file = ScannedFile(
            path = "D:/videos/ABP-123.mp4",
            fileName = "ABP-123.mp4",
            number = "ABP-123",
            isScraped = true
        )
        val state = GalleryState(
            scrapedFiles = listOf(file),
            outputDir = "D:/output"
        )

        assertEquals(file, state.fileByPath("D:/videos/ABP-123.mp4"))
        assertNull(state.fileByPath("D:/videos/missing.mp4"))
    }

    @Test
    fun `gallery source file falls back to session result by path`() {
        val video = Video(
            number = "SONE-001",
            title = "Session result",
            path = "D:/videos/SONE-001.mp4"
        )
        val state = GalleryState(
            scrapedFiles = emptyList(),
            outputDir = "D:/output",
            sessionResults = listOf(video)
        )

        assertEquals(
            ScannedFile(
                path = "D:/videos/SONE-001.mp4",
                fileName = "SONE-001.mp4",
                number = "SONE-001",
                isScraped = true,
                metadata = video
            ),
            state.fileByPath("D:/videos/SONE-001.mp4")
        )
    }

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
