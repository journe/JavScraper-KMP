package javscraper

import javscraper.models.ScannedFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import java.nio.file.Path

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelScanTest {
    @Test
    fun `startScan updates state progressively on background thread`() {
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val mainThread = Thread.currentThread()
        val scanThreads = mutableSetOf<Thread>()
        val sizesAfterFirstEmission = mutableListOf<Int>()
        val first = ScannedFile(path = "D:/videos/ABC-001.mp4", fileName = "ABC-001.mp4", number = "ABC-001")
        val second = ScannedFile(path = "D:/videos/ABC-002.mp4", fileName = "ABC-002.mp4", number = "ABC-002")
        lateinit var viewModel: AppViewModel

        viewModel = AppViewModel(
            scope = scope,
            scanDirectoryFlow = { _: Path, _: Boolean ->
                flow {
                    scanThreads += Thread.currentThread()
                    emit(listOf(first))
                    delay(20)
                    sizesAfterFirstEmission += viewModel.scannedFiles.size
                    emit(listOf(second))
                }
            }
        )

        viewModel.startScan()

        runBlocking {
            withTimeout(1_000) {
                while (viewModel.scanning || viewModel.scannedFiles.size < 2) delay(1)
            }
        }

        assertNotEquals(mainThread, scanThreads.single())
        assertEquals(listOf(1), sizesAfterFirstEmission)
        assertEquals(listOf(first, second), viewModel.scannedFiles)
        assertEquals(2, viewModel.tasks.size)
    }

    @Test
    fun `openSingleScrape groups same-folder same-number siblings`() {
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val current = ScannedFile(
            path = "D:/fc2/[FC2-4620098]/FC2-4620098-1.mp4",
            fileName = "FC2-4620098-1.mp4",
            number = "FC2-4620098",
            isScraped = true
        )
        val sibling = ScannedFile(
            path = "D:/fc2/[FC2-4620098]/FC2-4620098-2.mp4",
            fileName = "FC2-4620098-2.mp4",
            number = "FC2-4620098"
        )
        val viewModel = AppViewModel(
            scope = scope,
            scanDirectoryFlow = { _, _ -> flow { emit(listOf(current, sibling)) } }
        )

        viewModel.startScan()
        runBlocking {
            withTimeout(1_000) {
                while (viewModel.scanning || viewModel.scannedFiles.isEmpty()) delay(1)
            }
        }

        viewModel.openSingleScrape(current)

        assertEquals(2, viewModel.singleScrapeTask?.partCount)
    }

}
