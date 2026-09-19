package javscraper

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import javscraper.models.ScannedFile
import javscraper.models.SingleScrapeDialogState
import javscraper.models.ExistingVideoMetadata
import javscraper.models.Video
import javscraper.models.VideoUpdateField
import javscraper.scrape.ScrapeOrchestrator
import javscraper.scrape.update.VideoFieldMerger
import javscraper.scrape.update.ExistingMetadataResult
import javscraper.scrape.update.VideoFieldUpdatePlanner
import javscraper.sidecar.SidecarRequestException
import javscraper.sidecar.SidecarTimeoutException
import javscraper.ui.screens.ScrapeTask
import javscraper.ui.screens.ScrapeTaskStatus
import javscraper.ui.screens.filesForScrapeTask
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/** Manages the single-scrape dialog state machine and its background job. */
class SingleScrapeController(
    private val scope: CoroutineScope,
    private val orch: () -> ScrapeOrchestrator?,
    private val outputDir: () -> String,
    private val updateMode: () -> Boolean = { false }
) {

    var singleScrapeDialogState by mutableStateOf<SingleScrapeDialogState>(SingleScrapeDialogState.Closed)
    var singleScrapeFile by mutableStateOf<ScannedFile?>(null)
    private var singleScrapeFiles = emptyList<ScannedFile>()
    var singleScrapeNumber by mutableStateOf("")
    var singleScrapeSite by mutableStateOf<String?>(null)
    private var lastSelectedSingleScrapeSite: String? = null
    var singleScrapeTask by mutableStateOf<ScrapeTask?>(null)
    var singleScrapeError by mutableStateOf<String?>(null)
    var singleScrapeErrorStage by mutableStateOf<String?>(null)
    var showMissingOutputDir by mutableStateOf(false)

    /** Invoked with the finished task and its video when the user confirms a result. */
    var onConfirmResult: (ScrapeTask, Video?) -> Unit = { _, _ -> }

    /** Invoked when a network search returns candidates, before user confirmation. */
    var onPreviewCandidates: (List<Video>) -> Unit = { }

    private var singleScrapeJob: Job? = null
    private var previewConfirm: CompletableDeferred<Int?>? = null

    fun updateSingleScrapeNumber(value: String) {
        singleScrapeNumber = value
        singleScrapeTask = singleScrapeTask?.copy(number = value)
    }

    fun updateSingleScrapeSite(value: String?) {
        lastSelectedSingleScrapeSite = value
        singleScrapeSite = value
    }

    fun openSingleScrape(file: ScannedFile) {
        openSingleScrapeGroup(listOf(file), null)
    }

    private fun openSingleScrapeGroup(files: List<ScannedFile>, task: ScrapeTask?) {
        val file = files.firstOrNull() ?: return
        singleScrapeFile = file
        singleScrapeFiles = files
        singleScrapeNumber = file.number
        singleScrapeSite = lastSelectedSingleScrapeSite
        singleScrapeTask = task?.copy(status = ScrapeTaskStatus.PENDING)
            ?: ScrapeTask(
                number = file.number,
                fileName = file.fileName,
                partCount = files.size,
                status = ScrapeTaskStatus.PENDING
            )
        singleScrapeError = null
        singleScrapeErrorStage = null
        singleScrapeDialogState = SingleScrapeDialogState.Input
    }

    fun openSingleScrapeFromTask(task: ScrapeTask, scannedFiles: List<ScannedFile>) {
        openSingleScrapeGroup(filesForScrapeTask(task, scannedFiles), task)
    }

    fun closeSingleScrape() {
        singleScrapeDialogState = SingleScrapeDialogState.Closed
        singleScrapeFile = null
        singleScrapeFiles = emptyList()
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
        singleScrapeErrorStage = null
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
                showPreview(candidates)
                val selectedIndex = deferred.await()
                previewConfirm = null
                if (selectedIndex == null) {
                    singleScrapeTask = singleScrapeTask?.copy(status = ScrapeTaskStatus.PENDING)
                    closeSingleScrape()
                    return@launch
                }
                val video = (singleScrapeDialogState as? SingleScrapeDialogState.Preview)?.video
                    ?: candidates[selectedIndex]
                // 确认写入 → 建目录/写 NFO/下载图片/移动或复制文件
                singleScrapeDialogState = SingleScrapeDialogState.Scraping
                val writeFiles = singleScrapeFiles.ifEmpty { listOf(sf) }
                    .map { it.copy(number = number) }
                val writeResult = withContext(Dispatchers.IO) {
                    orch()?.writeSingleScrapeToDisk(
                        writeFiles,
                        video
                    )
                }
                if (writeResult == null || !writeResult.success) {
                    failSingleScrape(writeResult?.error?.message ?: "Write failed")
                    return@launch
                }
                val written = writeResult.data ?: video
                singleScrapeDialogState = SingleScrapeDialogState.Result(written, null)
                singleScrapeTask =
                    singleScrapeTask?.copy(status = ScrapeTaskStatus.SUCCESS, video = written)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val stage = when (e) {
                    is SidecarRequestException -> e.stage
                    is SidecarTimeoutException -> e.stage
                    else -> null
                }
                failSingleScrape(e.message ?: "Unknown error", stage)
            } finally {
                singleScrapeJob = null
                previewConfirm?.cancel()
                previewConfirm = null
            }
        }
    }

    internal fun showPreview(candidates: List<Video>) {
        onPreviewCandidates(candidates)
        singleScrapeDialogState = SingleScrapeDialogState.Preview(candidates)
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
        val selectedIndex =
            (singleScrapeDialogState as? SingleScrapeDialogState.Preview)?.selectedIndex ?: 0
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
    fun configureFieldUpdate() {
        val preview = singleScrapeDialogState as? SingleScrapeDialogState.Preview ?: return
        if (!updateMode()) return
        val file = singleScrapeFile ?: return
        val number = singleScrapeNumber.ifBlank { preview.video.number }
        val files = singleScrapeFiles.ifEmpty { listOf(file) }
            .map { it.copy(number = number) }
        singleScrapeError = null
        singleScrapeErrorStage = null
        scope.launch {
            try {
                val result = orch()?.readExistingMetadata(files)
                when (result) {
                    is ExistingMetadataResult.Ready ->
                        showFieldUpdateSelection(preview.candidates, preview.selectedIndex, result.metadata)
                    is ExistingMetadataResult.Failed ->
                        failFieldUpdatePreparation(preview, result.errors.joinToString("; "))
                    null -> failFieldUpdatePreparation(preview, "Worker not running")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failFieldUpdatePreparation(preview, e.message ?: "Unknown error")
            }
        }
    }

    private fun failFieldUpdatePreparation(preview: SingleScrapeDialogState.Preview, message: String) {
        singleScrapeError = message
        singleScrapeDialogState = preview.copy(error = message)
    }

    internal fun showFieldUpdateSelection(
        candidates: List<Video>,
        selectedIndex: Int,
        existing: ExistingVideoMetadata
    ) {
        if (candidates.isEmpty()) return
        val index = selectedIndex.coerceIn(0, candidates.lastIndex)
        singleScrapeDialogState = SingleScrapeDialogState.FieldUpdateSelection(
            candidates = candidates,
            selectedIndex = index,
            existing = existing,
            choices = VideoFieldUpdatePlanner.choices(existing, candidates[index])
        )
    }

    fun toggleFieldUpdateField(field: VideoUpdateField, selected: Boolean) {
        val selection = singleScrapeDialogState as? SingleScrapeDialogState.FieldUpdateSelection ?: return
        singleScrapeDialogState = selection.copy(
            choices = selection.choices.map { current ->
                if (current.field == field) current.withSelection(selected) else current
            }
        )
    }

    fun confirmFieldUpdateSelection() {
        val selection = singleScrapeDialogState as? SingleScrapeDialogState.FieldUpdateSelection ?: return
        val selectedFields = selection.choices.filter { it.selected }.map { it.field }.toSet()
        if (selectedFields.isEmpty()) return
        val merged = VideoFieldMerger.merge(selection.existing, selection.incoming, selectedFields)
        singleScrapeDialogState = SingleScrapeDialogState.FieldUpdateConfirm(selection, merged, selectedFields)
    }

    fun backToFieldUpdateSelection() {
        val confirm = singleScrapeDialogState as? SingleScrapeDialogState.FieldUpdateConfirm ?: return
        singleScrapeDialogState = confirm.selection
    }

    fun backToPreviewFromFieldUpdate() {
        val current = singleScrapeDialogState
        val selection = when (current) {
            is SingleScrapeDialogState.FieldUpdateSelection -> current
            is SingleScrapeDialogState.FieldUpdateConfirm -> current.selection
            else -> return
        }
        singleScrapeError = null
        singleScrapeDialogState = SingleScrapeDialogState.Preview(
            candidates = selection.candidates,
            selectedIndex = selection.selectedIndex
        )
    }

    fun confirmFieldUpdateWrite() {
        val confirm = singleScrapeDialogState as? SingleScrapeDialogState.FieldUpdateConfirm ?: return
        val file = singleScrapeFile ?: return
        val number = confirm.merged.number.ifBlank { singleScrapeNumber }
        val files = singleScrapeFiles.ifEmpty { listOf(file) }
            .map { it.copy(number = number) }
        singleScrapeError = null
        singleScrapeErrorStage = null
        singleScrapeDialogState = SingleScrapeDialogState.Scraping
        singleScrapeTask = singleScrapeTask?.copy(status = ScrapeTaskStatus.SCRAPING)
        singleScrapeJob = scope.launch {
            try {
                val writeResult = withContext(Dispatchers.IO) {
                    orch()?.writeSingleScrapeToDisk(files, confirm.merged, confirm.selectedFields)
                }
                if (writeResult == null || !writeResult.success) {
                    failSingleScrape(writeResult?.error?.message ?: "Write failed")
                    return@launch
                }
                val written = writeResult.data ?: confirm.merged
                singleScrapeDialogState = SingleScrapeDialogState.Result(written, null)
                singleScrapeTask =
                    singleScrapeTask?.copy(status = ScrapeTaskStatus.SUCCESS, video = written)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failSingleScrape(e.message ?: "Unknown error")
            } finally {
                singleScrapeJob = null
            }
        }
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

    internal fun failSingleScrape(message: String, stage: String? = null) {
        singleScrapeErrorStage = stage
        singleScrapeError = message
        singleScrapeTask = singleScrapeTask?.copy(status = ScrapeTaskStatus.FAILED, error = message)
        singleScrapeDialogState = SingleScrapeDialogState.Input
    }
}
