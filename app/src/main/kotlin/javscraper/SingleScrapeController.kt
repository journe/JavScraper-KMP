package javscraper

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import javscraper.models.ScannedFile
import javscraper.models.SingleScrapeDialogState
import javscraper.models.Video
import javscraper.scrape.ScrapeOrchestrator
import javscraper.ui.screens.ScrapeTask
import javscraper.ui.screens.ScrapeTaskStatus
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Manages the single-scrape dialog state machine and its background job. */
class SingleScrapeController(
    private val scope: CoroutineScope,
    private val orch: () -> ScrapeOrchestrator?,
    private val outputDir: () -> String
) {

    var singleScrapeDialogState by mutableStateOf<SingleScrapeDialogState>(SingleScrapeDialogState.Closed)
    var singleScrapeFile by mutableStateOf<ScannedFile?>(null)
    var singleScrapeNumber by mutableStateOf("")
    var singleScrapeSite by mutableStateOf<String?>(null)
    var singleScrapeTask by mutableStateOf<ScrapeTask?>(null)
    var singleScrapeError by mutableStateOf<String?>(null)
    var showMissingOutputDir by mutableStateOf(false)

    /** Invoked with the finished task and its video when the user confirms a result. */
    var onConfirmResult: (ScrapeTask, Video?) -> Unit = { _, _ -> }

    private var singleScrapeJob: Job? = null
    private var previewConfirm: CompletableDeferred<Int?>? = null

    fun updateSingleScrapeNumber(value: String) {
        singleScrapeNumber = value
        singleScrapeTask = singleScrapeTask?.copy(number = value)
    }

    fun updateSingleScrapeSite(value: String?) {
        singleScrapeSite = value
    }

    fun openSingleScrape(file: ScannedFile) {
        singleScrapeFile = file
        singleScrapeNumber = file.number
        singleScrapeSite = null
        singleScrapeTask = ScrapeTask(file.number, file.fileName, status = ScrapeTaskStatus.PENDING)
        singleScrapeError = null
        singleScrapeDialogState = SingleScrapeDialogState.Input
    }

    fun openSingleScrapeFromTask(task: ScrapeTask, scannedFiles: List<ScannedFile>) {
        val sf = scannedFiles.find { it.fileName == task.fileName }
            ?: ScannedFile(path = task.path, fileName = task.fileName, number = task.number)
        openSingleScrape(sf)
    }

    fun closeSingleScrape() {
        singleScrapeDialogState = SingleScrapeDialogState.Closed
        singleScrapeFile = null
        singleScrapeTask = null
    }

    fun startSingleScrape() {
        val file = singleScrapeFile ?: return
        val number = singleScrapeNumber
        val site = singleScrapeSite
        if (number.isBlank()) return
        if (outputDir().isBlank()) {
            showMissingOutputDir = true
            return
        }
        singleScrapeError = null
        singleScrapeDialogState = SingleScrapeDialogState.Scraping
        singleScrapeTask = singleScrapeTask?.copy(status = ScrapeTaskStatus.SCRAPING)
        singleScrapeJob = scope.launch {
            val sf = file.copy(number = number)
            try {
                val candidates = orch()?.fetchCandidates(sf, site)
                if (candidates == null) {
                    failSingleScrape("Worker not running")
                    return@launch
                }
                if (candidates.isEmpty()) {
                    failSingleScrape("No data")
                    return@launch
                }

                val deferred = CompletableDeferred<Int?>()
                previewConfirm = deferred
                singleScrapeDialogState = SingleScrapeDialogState.Preview(candidates)
                val selectedIndex = deferred.await()
                previewConfirm = null
                if (selectedIndex == null) {
                    singleScrapeTask = singleScrapeTask?.copy(status = ScrapeTaskStatus.PENDING)
                    closeSingleScrape()
                    return@launch
                }
                val video = (singleScrapeDialogState as? SingleScrapeDialogState.Preview)?.video
                    ?: candidates[selectedIndex]
                // 确认写入 → 建目录/写 NFO/下载图片/复制文件
                singleScrapeDialogState = SingleScrapeDialogState.Scraping
                val writeResult = withContext(Dispatchers.IO) { orch()?.writeToDisk(listOf(sf), video) }
                if (writeResult == null || !writeResult.success) {
                    failSingleScrape(writeResult?.error?.message ?: "Write failed")
                    return@launch
                }
                singleScrapeDialogState = SingleScrapeDialogState.Result(video, null)
                singleScrapeTask = singleScrapeTask?.copy(status = ScrapeTaskStatus.SUCCESS, video = video)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failSingleScrape(e.message ?: "Unknown error")
            } finally {
                singleScrapeJob = null
                previewConfirm?.cancel()
                previewConfirm = null
            }
        }
    }

    /** Cancel the running single-scrape job and close the dialog. */
    fun cancelSingleScrape() {
        singleScrapeJob?.cancel()
        if (singleScrapeTask?.status == ScrapeTaskStatus.SCRAPING) {
            singleScrapeTask = singleScrapeTask?.copy(status = ScrapeTaskStatus.PENDING)
        }
        closeSingleScrape()
    }

    /** User confirmed the preview: proceed with file IO. */
    fun confirmPreviewWrite() {
        val selectedIndex = (singleScrapeDialogState as? SingleScrapeDialogState.Preview)?.selectedIndex ?: 0
        previewConfirm?.complete(selectedIndex)
    }

    /** Select one candidate while the preview dialog is waiting for confirmation. */
    fun selectPreviewCandidate(index: Int) {
        val preview = singleScrapeDialogState as? SingleScrapeDialogState.Preview ?: return
        singleScrapeDialogState = preview.select(index)
    }
    /** User cancelled the preview: abort without writing files. */
    fun cancelPreviewWrite() {
        previewConfirm?.complete(null)
    }

    fun dismissMissingOutputDir() {
        showMissingOutputDir = false
    }

    /** Adds the finished task to the task list and the video to results, then closes. */
    fun confirmSingleScrape() {
        val task = singleScrapeTask
        val video = (singleScrapeDialogState as? SingleScrapeDialogState.Result)?.video
        onConfirmResult(task ?: ScrapeTask("", ""), video)
        closeSingleScrape()
    }

    private fun failSingleScrape(message: String) {
        singleScrapeError = message
        singleScrapeTask = singleScrapeTask?.copy(status = ScrapeTaskStatus.FAILED, error = message)
        singleScrapeDialogState = SingleScrapeDialogState.Input
    }
}
