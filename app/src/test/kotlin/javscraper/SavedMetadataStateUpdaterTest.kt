package javscraper

import javscraper.models.ScannedFile
import javscraper.models.Video
import kotlin.test.Test
import kotlin.test.assertEquals

class SavedMetadataStateUpdaterTest {
    @Test
    fun `apply updates every file in the renamed movie folder`() {
        val oldFolder = "D:/Movies/[ABC-001] Old"
        val newFolder = "D:/Movies/[ABC-002] New"
        val files = listOf(
            ScannedFile("$oldFolder/ABC-001-1.mp4", "ABC-001-1.mp4", "ABC-001"),
            ScannedFile("$oldFolder/ABC-001-2.mp4", "ABC-001-2.mp4", "ABC-001")
        )
        val results = listOf(
            Video(number = "ABC-001", title = "Old", path = "$oldFolder/ABC-001-1.mp4"),
            Video(number = "ABC-001", title = "Old", path = "$oldFolder/ABC-001-2.mp4")
        )
        val saved = Video(number = "ABC-002", title = "New", path = "$newFolder/ABC-001-1.mp4")

        val state = SavedMetadataStateUpdater.apply(
            scannedFiles = files,
            results = results,
            savedVideo = saved,
            previousPath = "$oldFolder/ABC-001-1.mp4"
        )

        assertEquals("$newFolder/ABC-001-1.mp4", state.scannedFiles[0].path)
        assertEquals("$newFolder/ABC-001-2.mp4", state.scannedFiles[1].path)
        assertEquals(saved, state.results[0])
        assertEquals("$newFolder/ABC-001-2.mp4", state.results[1].path)
    }
}
