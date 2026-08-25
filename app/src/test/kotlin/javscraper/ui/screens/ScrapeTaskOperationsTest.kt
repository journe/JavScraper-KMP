package javscraper.ui.screens

import javscraper.models.Video
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScrapeTaskOperationsTest {

    @Test
    fun `upsert replaces task with same path`() {
        val original = ScrapeTask("OLD-001", "video.mp4", path = "videos/video.mp4")
        val updated = original.copy(number = "NEW-001", status = ScrapeTaskStatus.SUCCESS)

        val tasks = upsertScrapeTask(listOf(original), updated)

        assertEquals(listOf(updated), tasks)
    }

    @Test
    fun `upsert replaces task with same file name when path is absent`() {
        val original = ScrapeTask("OLD-001", "video.mp4")
        val updated = original.copy(number = "NEW-001", status = ScrapeTaskStatus.SUCCESS)

        val tasks = upsertScrapeTask(listOf(original), updated)

        assertEquals(listOf(updated), tasks)
    }

    @Test
    fun `upsert appends unknown task`() {
        val original = ScrapeTask("OLD-001", "old.mp4")
        val updated = ScrapeTask("NEW-001", "new.mp4")

        val tasks = upsertScrapeTask(listOf(original), updated)

        assertEquals(listOf(original, updated), tasks)
    }

    @Test
    fun `upsert video replaces result with same number`() {
        val original = Video("SONE-001", title = "Old")
        val updated = original.copy(title = "New")

        val results = upsertVideo(listOf(original), updated)

        assertEquals(listOf(updated), results)
        assertTrue(results.size == 1)
    }
}
