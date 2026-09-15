package javscraper

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import javscraper.controllers.NetworkPreviewController
import javscraper.i18n.TranslationEn
import javscraper.io.logging.AppLogController
import javscraper.io.logging.LogEntry
import javscraper.io.FileScanner
import javscraper.models.ScannedFile
import javscraper.models.SiteInfo
import javscraper.models.Video
import javscraper.ui.screens.FileScanActions
import javscraper.ui.screens.FileScanState
import javscraper.ui.screens.GalleryActions
import javscraper.ui.screens.NetworkPreviewActions
import javscraper.ui.screens.NetworkPreviewState
import javscraper.ui.screens.GalleryState
import javscraper.ui.screens.ScrapeProgressActions
import javscraper.ui.screens.ScrapeProgressState
import javscraper.ui.screens.ScrapeTask
import javscraper.ui.screens.ScrapeTaskStatus
import javscraper.ui.screens.settings.SettingsActions
import javscraper.ui.screens.settings.SettingsState
import javscraper.ui.screens.upsertScrapeTask
import javscraper.ui.screens.upsertVideo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.nio.file.Paths

/**
 * MVVM ViewModel for the main application.
 * Coordinates settings ([SettingsController]), worker lifecycle ([WorkerController])
 * and scan/scrape orchestration, keeping composables stateless.
 */
class AppViewModel(
    private val scope: CoroutineScope,
    private val scanDirectoryFlow: (Path, Boolean) -> Flow<List<ScannedFile>> = { dir, recursive ->
        FileScanner.scanDirectoryFlow(dir, recursive)
    }
) {

    private val settings = SettingsController(scope)
    private val appLog = AppLogController()
    private val worker = WorkerController(scope, settings)
    private val singleScrape = SingleScrapeController(scope, { worker.orch }, { outputDir })
    private val networkPreview = NetworkPreviewController()

    init {
        appLog.setFileLoggingEnabled(settings.fileLoggingEnabled)
        settings.onFileLoggingChanged = { enabled -> appLog.setFileLoggingEnabled(enabled) }
        settings.onScrapeSettingsChanged = { worker.rebuildOrchestrator() }
        settings.onStatusChange = { status = it }
        worker.onStatusChange = { status = it }
        singleScrape.onPreviewCandidates = { candidates -> networkPreview.add(candidates) }
        singleScrape.onConfirmResult = { task, video ->
            if (task.number.isNotBlank()) tasks = upsertScrapeTask(tasks, task)
            if (video != null) results = upsertVideo(results, video)
        }
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
    var scrapedFiles by mutableStateOf<List<ScannedFile>>(emptyList())
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

    // --- Single scrape dialog state (delegated to SingleScrapeController) ---
    var singleScrapeDialogState by singleScrape::singleScrapeDialogState
    var singleScrapeFile by singleScrape::singleScrapeFile
    var singleScrapeNumber by singleScrape::singleScrapeNumber
    var singleScrapeSite by singleScrape::singleScrapeSite
    var singleScrapeTask by singleScrape::singleScrapeTask
    var singleScrapeError by singleScrape::singleScrapeError
    var singleScrapeErrorStage by singleScrape::singleScrapeErrorStage
    var showMissingOutputDir by singleScrape::showMissingOutputDir

    // --- Settings state (delegated to SettingsController) ---
    var scanDir by settings::scanDir
    var scanDirHistory by settings::scanDirHistory
    var outputDir by settings::outputDir
    var workerPath by settings::workerPath
    var currentLanguage by settings::currentLanguage
    var showRestartHint by settings::showRestartHint
    var enabledSites by settings::enabledSites
    var fileLoggingEnabled by settings::fileLoggingEnabled
    var scanRecursive by settings::scanRecursive
    var createMovieFolders by settings::createMovieFolders
    var moveInsteadOfCopy by settings::moveInsteadOfCopy
    var downloadImages by settings::downloadImages
    var downloadPreviewImages by settings::downloadPreviewImages
    var downloadWebPages by settings::downloadWebPages
    var lockData by settings::lockData
    var autoScrape by settings::autoScrape
    var folderLayers by settings::folderLayers
    var filenameFormat by settings::filenameFormat
    var maxTitleLength by settings::maxTitleLength
    var maxFilenameLength by settings::maxFilenameLength
    var suffixKeywords by settings::suffixKeywords
    var requestTimeoutMs by settings::requestTimeoutMs

    // --- Worker state (delegated to WorkerController) ---
    var sites by worker::sites

    /** Sites that the user enabled in settings, for single-scrape site selection. */
    val enabledSiteInfos: List<SiteInfo>
        get() = sites.filter { it.id in enabledSites }
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
    val lifecycleLogEntries: List<LogEntry>
        get() = appLog.entries

    val lifecycleLogFilePath: String
        get() = appLog.logFilePath.toString()

    fun dispose() {
        worker.dispose()
        appLog.dispose()
    }

    // --- Navigation ---

    fun navigate(screen: Screen) {
        currentScreen = screen
    }

    // --- Settings (forwarded to SettingsController) ---

    fun updateLanguage(lang: String) = settings.updateLanguage(lang)
    fun selectScanDir() = settings.selectScanDir()
    fun selectScanDirFromHistory(directory: String) = settings.selectScanDirFromHistory(directory)
    fun selectOutputDir() = settings.selectOutputDir()
    fun updateWorkerPath(v: String) = settings.updateWorkerPath(v)
    fun updateScanRecursive(v: Boolean) = settings.updateScanRecursive(v)
    fun updateCreateMovieFolders(v: Boolean) = settings.updateCreateMovieFolders(v)
    fun updateMoveInsteadOfCopy(v: Boolean) = settings.updateMoveInsteadOfCopy(v)
    fun updateDownloadImages(v: Boolean) = settings.updateDownloadImages(v)
    fun updateDownloadPreviewImages(v: Boolean) = settings.updateDownloadPreviewImages(v)
    fun updateDownloadWebPages(v: Boolean) = settings.updateDownloadWebPages(v)
    fun updateLockData(v: Boolean) = settings.updateLockData(v)
    fun updateAutoScrape(v: Boolean) = settings.updateAutoScrape(v)
    fun updateFileLogging(v: Boolean) = settings.updateFileLogging(v)
    fun toggleSite(id: String, enabled: Boolean) = settings.toggleSite(id, enabled)
    fun resetSettings() = settings.resetSettings()
    fun updateFolderLayer(index: Int, value: String) = settings.updateFolderLayer(index, value)
    fun addLayer() = settings.addLayer()
    fun removeLayer(index: Int) = settings.removeLayer(index)
    fun updateFilenameFormat(v: String) = settings.updateFilenameFormat(v)
    fun updateMaxTitleLength(v: Int) = settings.updateMaxTitleLength(v)
    fun updateMaxFilenameLength(v: Int) = settings.updateMaxFilenameLength(v)
    fun updateSuffixKeywords(v: List<String>) = settings.updateSuffixKeywords(v)
    fun updateRequestTimeoutMs(v: Int) = settings.updateRequestTimeoutMs(v)

    // --- Worker (forwarded to WorkerController) ---

    fun selectWorkerPath() = worker.selectWorkerPath()
    fun dismissWorkerSetup() = worker.dismissWorkerSetup()
    fun checkSites() = worker.checkSites()

    // --- Scan ---

    fun startScan() {
        if (scanning) return
        scope.launch {
            scanning = true
            scannedFiles = emptyList()
            scrapedFiles = emptyList()
            try {
                scanDirectoryFlow(Paths.get(scanDir), scanRecursive)
                    .flowOn(Dispatchers.IO)
                    .collect { batch ->
                        scannedFiles = scannedFiles + batch
                        scrapedFiles = scannedFiles.filter { it.isScraped }
                    }
                val groups = scannedFiles
                    .filter { !it.isScraped && it.number.isNotBlank() }
                    .groupBy { it.number }
                tasks = groups.map { (number, groupedFiles) ->
                    ScrapeTask(
                        number = number,
                        fileName = groupedFiles.first().fileName,
                        partCount = groupedFiles.size
                    )
                }
            } finally {
                scanning = false
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

                        val taskFiles = scannedFiles.filter { it.number == t.number && !it.isScraped }
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
        scrapedFiles = emptyList()
        tasks = emptyList()
    }

    // --- Single scrape dialog (forwarded to SingleScrapeController) ---

    fun updateSingleScrapeNumber(value: String) = singleScrape.updateSingleScrapeNumber(value)
    fun updateSingleScrapeSite(value: String?) = singleScrape.updateSingleScrapeSite(value)
    fun openSingleScrape(file: ScannedFile) = singleScrape.openSingleScrape(file)
    fun openSingleScrapeFromTask(task: ScrapeTask) =
        singleScrape.openSingleScrapeFromTask(task, scannedFiles)

    fun closeSingleScrape() = singleScrape.closeSingleScrape()
    fun startSingleScrape() = singleScrape.startSingleScrape()
    fun cancelSingleScrape() = singleScrape.cancelSingleScrape()
    fun confirmPreviewWrite() = singleScrape.confirmPreviewWrite()
    fun selectPreviewCandidate(index: Int) = singleScrape.selectPreviewCandidate(index)
    fun cancelPreviewWrite() = singleScrape.cancelPreviewWrite()
    fun dismissMissingOutputDir() = singleScrape.dismissMissingOutputDir()
    fun confirmSingleScrape() = singleScrape.confirmSingleScrape()
    // --- UI state mappings (consumed by App.kt; screens stay stateless) ---

    val scanState: FileScanState
        get() = FileScanState(scannedFiles, scrapedFiles, scanDir, scanDirHistory, scanning)
    val scanActions: FileScanActions = FileScanActions(
        ::selectScanDir, ::selectScanDirFromHistory, ::startScan, { navigate(Screen.PROGRESS) }
    )

    val scrapeProgressState: ScrapeProgressState
        get() = ScrapeProgressState(
            tasks, scraping, singleScrapeDialogState, singleScrapeNumber,
            singleScrapeSite, singleScrapeTask, enabledSiteInfos,
            outputDir, singleScrapeError, singleScrapeErrorStage, showMissingOutputDir
        )
    val scrapeProgressActions: ScrapeProgressActions = ScrapeProgressActions(
        ::startAllScraping, ::cancelScraping, ::openSingleScrapeFromTask,
        ::updateSingleScrapeNumber, ::updateSingleScrapeSite,
        ::startSingleScrape, ::closeSingleScrape, ::cancelSingleScrape,
        ::confirmPreviewWrite, ::selectPreviewCandidate, ::cancelPreviewWrite, ::dismissMissingOutputDir,
        ::confirmSingleScrape
    )

    val galleryState: GalleryState
        get() = GalleryState(scrapedFiles, outputDir, results)
    val galleryActions: GalleryActions = GalleryActions(::clearResults, {}, {})

    val networkPreviewState: NetworkPreviewState
        get() = NetworkPreviewState(networkPreview.candidates)
    val networkPreviewActions: NetworkPreviewActions = NetworkPreviewActions(
        networkPreview::clear
    )
    val settingsState: SettingsState
        get() = SettingsState(
            workerPath, outputDir, scanDir, scanDirHistory, scanRecursive, createMovieFolders,
            moveInsteadOfCopy, downloadImages, downloadPreviewImages, downloadWebPages, lockData, autoScrape,
            fileLoggingEnabled, sites, enabledSites,
            currentLanguage, showRestartHint, folderLayers, filenameFormat,
            maxTitleLength, maxFilenameLength, suffixKeywords, requestTimeoutMs,
            siteCheckRunning, siteCheckResults
        )
    val settingsActions: SettingsActions = SettingsActions(
        ::updateLanguage, ::selectOutputDir, ::selectScanDir, ::selectScanDirFromHistory, ::selectWorkerPath,
        ::updateWorkerPath, ::updateScanRecursive, ::updateCreateMovieFolders,
        ::updateMoveInsteadOfCopy, ::updateDownloadImages, ::updateDownloadPreviewImages, ::updateDownloadWebPages, ::updateLockData, ::updateAutoScrape,
        ::updateFileLogging,
        ::toggleSite, ::resetSettings, ::updateFolderLayer, ::addLayer,
        ::removeLayer, ::updateFilenameFormat, ::updateMaxTitleLength,
        ::updateMaxFilenameLength, ::updateSuffixKeywords, ::updateRequestTimeoutMs, ::checkSites
    )
}
