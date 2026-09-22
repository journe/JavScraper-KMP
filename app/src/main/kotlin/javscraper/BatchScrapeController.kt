package javscraper

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import javscraper.i18n.TranslationEn
import javscraper.models.ScannedFile
import javscraper.models.Video
import javscraper.scrape.ScrapeOrchestrator
import javscraper.ui.screens.ScrapeTask
import javscraper.ui.screens.ScrapeTaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BatchScrapeController(
    private val scope: CoroutineScope,
    private val strings: () -> TranslationEn,
    private val orchestrator: () -> ScrapeOrchestrator?,
    private val onError: (String) -> Unit = {}
) {
    var tasks by mutableStateOf<List<ScrapeTask>>(emptyList())
    var scraping by mutableStateOf(false)
    var results by mutableStateOf<List<Video>>(emptyList())

    fun startAllScraping(sourceFiles: List<ScannedFile>) {
        if (scraping) return
        scraping = true
        scope.launch {
            var worker: ScrapeOrchestrator? = null
            try {
                for (index in tasks.indices) {
                    if (!scraping) break
                    val task = tasks[index]
                    if (task.status == ScrapeTaskStatus.SUCCESS) continue
                    tasks = tasks.toMutableList().also {
                        it[index] = task.copy(status = ScrapeTaskStatus.SCRAPING)
                    }

                    val taskFiles = sourceFiles.filter { it.number == task.number && !it.isScraped }
                    if (taskFiles.isEmpty()) {
                        markTaskFailed(index, task, strings().statusFileNotFound)
                        continue
                    }

                    val currentWorker = worker ?: orchestrator()
                    if (currentWorker == null) {
                        markTaskFailed(index, task, "Worker not running")
                        break
                    }
                    worker = currentWorker
                    processTask(index, task, taskFiles, currentWorker)
                }
            } catch (e: Exception) {
                onError(strings().statusScrapeError(e.message ?: ""))
            } finally {
                scraping = false
            }
        }
    }

    private fun markTaskFailed(index: Int, task: ScrapeTask, error: String) {
        tasks = tasks.toMutableList().also {
            it[index] = task.copy(status = ScrapeTaskStatus.FAILED, error = error)
        }
    }

    private suspend fun processTask(
        index: Int,
        task: ScrapeTask,
        taskFiles: List<ScannedFile>,
        worker: ScrapeOrchestrator
    ) {
        val updatedTask = try {
            val result = withContext(Dispatchers.IO) { worker.processParts(taskFiles) }
            if (result.success && result.data != null) {
                results = results + result.data
                task.copy(status = ScrapeTaskStatus.SUCCESS, video = result.data)
            } else {
                task.copy(
                    status = ScrapeTaskStatus.FAILED,
                    error = result.error?.message ?: "Failed"
                )
            }
        } catch (e: Exception) {
            task.copy(status = ScrapeTaskStatus.FAILED, error = e.message ?: "Error")
        }
        tasks = tasks.toMutableList().also { it[index] = updatedTask }
    }

    fun cancelScraping() {
        scraping = false
    }

    fun clearResults() {
        results = emptyList()
        tasks = emptyList()
    }
}
