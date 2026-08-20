package javscraper

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import javscraper.i18n.TranslationEn
import javscraper.io.FileScanner
import javscraper.models.ScannedFile
import javscraper.models.SiteCheckResult
import javscraper.models.SingleScrapeDialogState
import javscraper.models.Video
import javscraper.ui.screens.ScrapeTask
import javscraper.ui.screens.ScrapeTaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Paths

/**
 * MVVM ViewModel for the main application.
 * Coordinates settings ([SettingsController]), worker lifecycle ([WorkerController])
 * and scan/scrape orchestration, keeping composables stateless.
 */
class AppViewModel(private val scope: CoroutineScope) {

    private val settings = SettingsController(scope)
    private val worker = WorkerController(scope, settings)

    init {
        settings.onScrapeSettingsChanged = { worker.rebuildOrchestrator() }
        settings.onStatusChange = { status = it }
        worker.onStatusChange = { status = it }
    }

    // --- Screen navigation ---
    var currentScreen by mutableStateOf(Screen.SCAN)
        private set

    // --- Status bar ---
    var status by mutableStateOf("")
        private set

    // --- Scan state ---
    var scannedFiles by mutableStateOf<List<ScannedFile>>(emptyList())
        private set
    var scanning by mutableStateOf(false)
        private set

    // --- Scrape state ---
    var tasks by mutableStateOf<List<ScrapeTask>>(emptyList())
        private set
    var scraping by mutableStateOf(false)
        private set
    var results by mutableStateOf<List<Video>>(emptyList())
        private set

    // --- Single scrape dialog state ---
    var singleScrapeDialogState by mutableStateOf<SingleScrapeDialogState>(SingleScrapeDialogState.Closed)
        private set
    var singleScrapeFile by mutableStateOf<ScannedFile?>(null)
        private set
    var singleScrapeNumber by mutableStateOf("")
        private set
    var singleScrapeSite by mutableStateOf<String?>(null)
        private set
    var singleScrapeTask by mutableStateOf<ScrapeTask?>(null)
        private set

    // --- Settings state (delegated to SettingsController) ---
    var scanDir by settings::scanDir
    var outputDir by settings::outputDir
    var workerPath by settings::workerPath
    var currentLanguage by settings::currentLanguage
    var showRestartHint by settings::showRestartHint
    var enabledSites by settings::enabledSites
    var scanRecursive by settings::scanRecursive
    var createMovieFolders by settings::createMovieFolders
    var hardlinkInsteadOfCopy by settings::hardlinkInsteadOfCopy
    var downloadImages by settings::downloadImages
    var autoScrape by settings::autoScrape
    var folderLayers by settings::folderLayers
    var filenameFormat by settings::filenameFormat
    var maxTitleLength by settings::maxTitleLength
    var maxFilenameLength by settings::maxFilenameLength
    var suffixKeywords by settings::suffixKeywords

    // --- Worker state (delegated to WorkerController) ---
    var sites by worker::sites
    var workerSetupVisible by worker::workerSetupVisible
    var workerSetupError by worker::workerSetupError
    var siteCheckRunning by worker::siteCheckRunning
    var siteCheckResults by worker::siteCheckResults

    // --- Locale-aware strings ---
    private val strings: TranslationEn
        get() = settings.strings

    // --- Lifecycle ---

    /** Initialize the sidecar worker and load sites. */
    fun init() {
        status = strings.statusInitializing
        scope.launch {
            worker.ensureWorkerRunning()
        }
    }

    /** Clean up the sidecar process. */
    fun dispose() {
        worker.dispose()
    }

    // --- Navigation ---

    fun navigate(screen: Screen) {
        currentScreen = screen
    }

    // --- Settings (forwarded to SettingsController) ---

    fun updateLanguage(lang: String) = settings.updateLanguage(lang)
    fun selectScanDir() = settings.selectScanDir()
    fun selectOutputDir() = settings.selectOutputDir()
    fun updateWorkerPath(v: String) = settings.updateWorkerPath(v)
    fun updateScanRecursive(v: Boolean) = settings.updateScanRecursive(v)
    fun updateCreateMovieFolders(v: Boolean) = settings.updateCreateMovieFolders(v)
    fun updateHardlink(v: Boolean) = settings.updateHardlink(v)
    fun updateDownloadImages(v: Boolean) = settings.updateDownloadImages(v)
    fun updateAutoScrape(v: Boolean) = settings.updateAutoScrape(v)
    fun toggleSite(id: String, enabled: Boolean) = settings.toggleSite(id, enabled)
    fun resetSettings() = settings.resetSettings()
    fun updateFolderLayer(index: Int, value: String) = settings.updateFolderLayer(index, value)
    fun addLayer() = settings.addLayer()
    fun removeLayer(index: Int) = settings.removeLayer(index)
    fun updateFilenameFormat(v: String) = settings.updateFilenameFormat(v)
    fun updateMaxTitleLength(v: Int) = settings.updateMaxTitleLength(v)
    fun updateMaxFilenameLength(v: Int) = settings.updateMaxFilenameLength(v)
    fun updateSuffixKeywords(v: List<String>) = settings.updateSuffixKeywords(v)

    // --- Worker (forwarded to WorkerController) ---

    fun selectWorkerPath() = worker.selectWorkerPath()
    fun dismissWorkerSetup() = worker.dismissWorkerSetup()
    fun checkSites() = worker.checkSites()

    // --- Scan ---

    fun startScan() {
        scope.launch {
            scanning = true
            scannedFiles = FileScanner.scanDirectory(
                Paths.get(scanDir), scanRecursive
            )
            scanning = false
            val groups = scannedFiles
                .filter { it.number.isNotBlank() }
                .groupBy { it.number }
            tasks = groups.map { (number, groupedFiles) ->
                ScrapeTask(
                    number = number,
                    fileName = groupedFiles.first().fileName,
                    partCount = groupedFiles.size
                )
            }
        }
    }

    // --- Scrape ---

    fun startAllScraping() {
        scope.launch {
            scraping = true
            val o = worker.orch ?: run { scraping = false; return@launch }
            try {
                for (i in tasks.indices) {
                    if (!scraping) break
                    val t = tasks[i]
                    val updated = tasks.toMutableList()
                    updated[i] = t.copy(status = ScrapeTaskStatus.SCRAPING)
                    tasks = updated

                    val taskFiles = scannedFiles.filter { it.number == t.number }
                    if (taskFiles.isNotEmpty()) {
                        try {
                            val r = withContext(Dispatchers.IO) { o.processParts(taskFiles) }
                            val newTasks = tasks.toMutableList()
                            if (r.success && r.data != null) {
                                results = results + r.data
                                newTasks[i] = t.copy(
                                    status = ScrapeTaskStatus.SUCCESS,
                                    video = r.data
                                )
                            } else {
                                newTasks[i] = t.copy(
                                    status = ScrapeTaskStatus.FAILED,
                                    error = r.error?.message ?: "Failed"
                                )
                            }
                            tasks = newTasks
                        } catch (e: Exception) {
                            val newTasks = tasks.toMutableList()
                            newTasks[i] = t.copy(
                                status = ScrapeTaskStatus.FAILED,
                                error = e.message ?: "Error"
                            )
                            tasks = newTasks
                        }
                    } else {
                        val newTasks = tasks.toMutableList()
                        newTasks[i] = t.copy(
                            status = ScrapeTaskStatus.FAILED,
                            error = strings.statusFileNotFound
                        )
                        tasks = newTasks
                    }
                }
            } catch (e: Exception) {
                status = strings.statusScrapeError(e.message ?: "")
            }
            scraping = false
        }
    }

    fun cancelScraping() {
        scraping = false
    }

    fun clearResults() {
        results = emptyList()
        tasks = emptyList()
    }

    // --- Single scrape dialog ---

    fun updateSingleScrapeNumber(value: String) {
        singleScrapeNumber = value
    }

    fun updateSingleScrapeSite(value: String?) {
        singleScrapeSite = value
    }

    fun openSingleScrape(file: ScannedFile) {
        singleScrapeFile = file
        singleScrapeNumber = file.number
        singleScrapeSite = null
        singleScrapeTask = ScrapeTask(file.number, file.fileName, status = ScrapeTaskStatus.PENDING)
        singleScrapeDialogState = SingleScrapeDialogState.Input
    }

    fun openSingleScrapeFromTask(task: ScrapeTask) {
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
        singleScrapeDialogState = SingleScrapeDialogState.Scraping
        scope.launch {
            val sf = file.copy(number = number)
            singleScrapeTask = singleScrapeTask?.copy(status = ScrapeTaskStatus.SCRAPING)
            try {
                val result = worker.orch?.process(sf, site) ?: return@launch
                if (result.success && result.data != null) {
                    singleScrapeDialogState = SingleScrapeDialogState.Result(result.data, null)
                    singleScrapeTask = singleScrapeTask?.copy(status = ScrapeTaskStatus.SUCCESS, video = result.data)
                } else {
                    val errMsg = result.error?.message ?: "Unknown error"
                    singleScrapeDialogState = SingleScrapeDialogState.Result(null, errMsg)
                    singleScrapeTask = singleScrapeTask?.copy(status = ScrapeTaskStatus.FAILED, error = errMsg)
                }
            } catch (e: Exception) {
                singleScrapeDialogState = SingleScrapeDialogState.Result(null, e.message ?: "Unknown error")
                singleScrapeTask = singleScrapeTask?.copy(status = ScrapeTaskStatus.FAILED, error = e.message ?: "")
            }
        }
    }

    fun confirmSingleScrape() {
        val task = singleScrapeTask
        val state = singleScrapeDialogState
        if (task != null) {
            tasks = tasks + task
        }
        if (state is SingleScrapeDialogState.Result && state.video != null) {
            results = results + state.video
        }
        closeSingleScrape()
    }
}