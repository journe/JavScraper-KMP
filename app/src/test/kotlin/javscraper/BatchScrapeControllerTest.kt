package javscraper

import javscraper.i18n.TranslationEn
import javscraper.models.ScannedFile
import javscraper.models.Video
import javscraper.ui.screens.ScrapeTask
import javscraper.ui.screens.ScrapeTaskStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class BatchScrapeControllerTest {
    @Test
    fun `missing task files mark task failed without requiring orchestrator`() = runTest {
        val controller = BatchScrapeController(
            scope = this,
            strings = { TranslationEn() },
            orchestrator = { null }
        )
        controller.tasks = listOf(ScrapeTask(number = "ABC-001", fileName = "ABC-001.mp4"))

        controller.startAllScraping(emptyList())
        advanceUntilIdle()

        val task = controller.tasks.single()
        assertEquals(ScrapeTaskStatus.FAILED, task.status)
        assertEquals(TranslationEn().statusFileNotFound, task.error)
        assertFalse(controller.scraping)
    }

    @Test
    fun `missing worker marks task failed and stops running`() = runTest {
        val controller = BatchScrapeController(
            scope = this,
            strings = { TranslationEn() },
            orchestrator = { null }
        )
        controller.tasks = listOf(ScrapeTask(number = "ABC-001", fileName = "ABC-001.mp4"))

        controller.startAllScraping(
            listOf(ScannedFile(path = "ABC-001.mp4", fileName = "ABC-001.mp4", number = "ABC-001"))
        )
        advanceUntilIdle()

        val task = controller.tasks.single()
        assertEquals(ScrapeTaskStatus.FAILED, task.status)
        assertEquals("Worker not running", task.error)
        assertFalse(controller.scraping)
    }

    @Test
    fun `clear results removes batch tasks and videos`() = runTest {
        val controller = BatchScrapeController(
            scope = this,
            strings = { TranslationEn() },
            orchestrator = { null }
        )
        controller.tasks = listOf(ScrapeTask(number = "ABC-001", fileName = "ABC-001.mp4"))
        controller.results = listOf(Video(number = "ABC-001"))

        controller.clearResults()

        assertEquals(emptyList(), controller.tasks)
        assertEquals(emptyList(), controller.results)
    }
}
