package javscraper.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class ScanDirectoryHistoryTest {
    @Test
    fun `remember puts new directory first`() {
        val history = listOf("C:/videos", "D:/media")

        val updated = ScanDirectoryHistory.remember(history, "E:/new")

        assertEquals(listOf("E:/new", "C:/videos", "D:/media"), updated)
    }

    @Test
    fun `remember removes duplicate directory`() {
        val history = listOf("C:/videos", "D:/media")

        val updated = ScanDirectoryHistory.remember(history, "C:/videos")

        assertEquals(listOf("C:/videos", "D:/media"), updated)
    }

    @Test
    fun `remember keeps ten recent directories`() {
        val history = (10 downTo 1).map { "C:/dir-$it" }

        val updated = ScanDirectoryHistory.remember(history, "C:/new")

        assertEquals(
            (listOf("C:/new") + (10 downTo 2).map { "C:/dir-$it" }),
            updated
        )
        assertEquals(ScanDirectoryHistory.MAX_ENTRIES, updated.size)
    }

    @Test
    fun `remember ignores blank directory`() {
        val history = listOf("C:/videos", "D:/media")

        val updated = ScanDirectoryHistory.remember(history, " ")

        assertEquals(history, updated)
    }
}
