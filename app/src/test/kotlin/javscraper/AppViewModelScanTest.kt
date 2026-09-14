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
}
