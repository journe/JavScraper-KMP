package javscraper.ui.screens

import javscraper.models.ScannedFile
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

    @Test
    fun `task lookup returns every pending file with the same number`() {
        val task = ScrapeTask(number = "FC2-4694056", fileName = "FC2-4694056.mp4", partCount = 4)
        val files = listOf(
            ScannedFile(path = "one.mp4", fileName = "FC2-4694056.mp4", number = task.number),
            ScannedFile(path = "two.mp4", fileName = "FC2-4694056-2.mp4", number = task.number),
            ScannedFile(path = "three.mp4", fileName = "FC2-4694056-3.mp4", number = task.number),
            ScannedFile(path = "four.mp4", fileName = "FC2-PPV 4694056-4.mp4", number = task.number)
        )

        assertEquals(files, filesForScrapeTask(task, files))
    }

    @Test
    fun `task lookup excludes already scraped files`() {
        val task = ScrapeTask(number = "SONE-205", fileName = "SONE-205.mp4")
        val pending = ScannedFile(path = "pending.mp4", fileName = "SONE-205-2.mp4", number = task.number)
        val scraped = ScannedFile(
            path = "scraped.mp4",
            fileName = "SONE-205.mp4",
            number = task.number,
            isScraped = true
        )

        assertEquals(listOf(pending), filesForScrapeTask(task, listOf(scraped, pending)))
    }

    @Test
    fun `same folder siblings groups all matching number files in the folder`() {
        val current = ScannedFile(
            path = "H:/fc2/[FC2-4620098]/FC2-4620098-1.mp4",
            fileName = "FC2-4620098-1.mp4",
            number = "FC2-4620098",
            isScraped = true
        )
        val scanned = listOf(
            current,
            ScannedFile("H:/fc2/[FC2-4620098]/FC2-4620098-2.mp4", "FC2-4620098-2.mp4", "FC2-4620098"),
            ScannedFile("H:/fc2/[FC2-4620098]/FC2-4620098-3.mp4", "FC2-4620098-3.mp4", "FC2-4620098"),
            ScannedFile("H:/fc2/[OTHER]/FC2-4620098.mp4", "FC2-4620098.mp4", "FC2-4620098"),
            ScannedFile("H:/fc2/[FC2-4620098]/FC2-1111111.mp4", "FC2-1111111.mp4", "FC2-1111111")
        )

        val grouped = sameFolderSiblings(current, scanned)

        assertEquals(3, grouped.size)
        assertTrue(grouped.all { it.path.startsWith("H:/fc2/[FC2-4620098]") })
    }

    @Test
    fun `same folder siblings matches number case-insensitively`() {
        val current = ScannedFile("C:/movies/ABC-001/abc-001-1.mp4", "abc-001-1.mp4", "ABC-001")
        val sibling = ScannedFile("C:/movies/ABC-001/ABC-001-2.mp4", "ABC-001-2.mp4", "abc-001")

        val grouped = sameFolderSiblings(current, listOf(current, sibling))

        assertEquals(2, grouped.size)
        assertTrue(grouped.contains(sibling))
    }

    @Test
    fun `same folder siblings falls back to current file without matching folder`() {
        val current = ScannedFile("C:/movies/ABC-001.mp4", "ABC-001.mp4", "ABC-001")
        val other = ScannedFile("C:/movies/ABC-002.mp4", "ABC-002.mp4", "ABC-002")

        assertEquals(listOf(current), sameFolderSiblings(current, listOf(current, other)))
    }

    @Test
    fun `same folder siblings keeps current file when path is blank`() {
        val current = ScannedFile("", "ABC-001.mp4", "ABC-001")

        assertEquals(listOf(current), sameFolderSiblings(current, emptyList()))
    }

    @Test
    fun `same folder siblings resolves output path back to source folder`() {
        val current = ScannedFile(
            path = "D:/library/FC2-4620098/FC2-4620098 - part1.mp4",
            fileName = "FC2-4620098 - part1.mp4",
            number = "FC2-4620098",
            isScraped = true
        )
        val source = ScannedFile(
            path = "H:/fc2/[FC2-4620098]/FC2-4620098-1.mp4",
            fileName = "FC2-4620098-1.mp4",
            number = "FC2-4620098",
            isScraped = true
        )
        val sibling = ScannedFile(
            path = "H:/fc2/[FC2-4620098]/FC2-4620098-2.mp4",
            fileName = "FC2-4620098-2.mp4",
            number = "FC2-4620098"
        )

        val grouped = sameFolderSiblings(current, listOf(source, sibling))

        assertEquals(2, grouped.size)
        assertTrue(grouped.contains(sibling))
    }

}
