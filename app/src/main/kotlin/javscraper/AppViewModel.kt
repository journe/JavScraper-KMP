package javscraper

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import javscraper.i18n.TranslationEn
import javscraper.i18n.TranslationZh
import javscraper.io.FileScanner
import javscraper.io.pickDirectory
import javscraper.models.ScannedFile
import javscraper.models.SiteInfo
import javscraper.models.Video
import javscraper.models.SingleScrapeDialogState
import javscraper.scrape.ScrapeOrchestrator
import javscraper.settings.SettingsManager
import javscraper.sidecar.SidecarManager
import javscraper.ui.screens.ScrapeTask
import javscraper.ui.screens.ScrapeTaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Paths

/**
 * MVVM ViewModel for the main application.
 * Encapsulates all UI state and business logic, keeping composables stateless.
 */
class AppViewModel(private val scope: CoroutineScope) {

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

    // --- Sites ---
    var sites by mutableStateOf<List<SiteInfo>>(emptyList())
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

    // --- Single scrape dialog setters ---
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
                val result = orch?.process(sf, site) ?: return@launch
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
    // --- Settings state ---
    var scanDir by mutableStateOf(SettingsManager.get().scanDir)
        private set
    var outputDir by mutableStateOf(SettingsManager.get().outputDir)
        private set
    var workerPath by mutableStateOf(SettingsManager.get().workerPath)
        private set
    var currentLanguage by mutableStateOf(SettingsManager.get().language)
        private set
    var showRestartHint by mutableStateOf(false)
        private set
    var enabledSites by mutableStateOf(SettingsManager.get().enabledSites)
        private set
    var scanRecursive by mutableStateOf(SettingsManager.get().scanRecursive)
        private set
    var createMovieFolders by mutableStateOf(SettingsManager.get().createMovieFolders)
        private set
    var hardlinkInsteadOfCopy by mutableStateOf(SettingsManager.get().hardlinkInsteadOfCopy)
        private set
    var downloadImages by mutableStateOf(SettingsManager.get().downloadImages)
        private set
    var autoScrape by mutableStateOf(SettingsManager.get().autoScrape)
        private set

    var folderLayers by mutableStateOf(SettingsManager.get().folderLayers)
        private set
    var filenameFormat by mutableStateOf(SettingsManager.get().filenameFormat)
        private set
    var maxTitleLength by mutableStateOf(SettingsManager.get().maxTitleLength)
        private set
    var maxFilenameLength by mutableStateOf(SettingsManager.get().maxFilenameLength)
        private set
    var suffixKeywords by mutableStateOf(SettingsManager.get().suffixKeywords)
        private set

    // --- Locale-aware strings ---
    private val strings: TranslationEn
        get() = if (currentLanguage == "zh") TranslationZh() else TranslationEn()

    // --- Dependencies ---
    private var mgr: SidecarManager? = null
    private var orch: ScrapeOrchestrator? = null

    /** Initialize the sidecar worker and load sites */
    fun init() {
        status = strings.statusInitializing
        scope.launch {
            try {
                status = strings.statusStarting
                val m = SidecarManager(
                    Paths.get(System.getProperty("user.dir"), workerPath).toString()
                )
                if (m.start()) {
                    mgr = m
                    sites = m.listSites()
                    status = strings.statusReady(sites.size)
                    orch = ScrapeOrchestrator(
                        m, outputDir,
                        createMovieFolders, hardlinkInsteadOfCopy, downloadImages
                    )
                } else {
                    status = strings.statusFailed
                }
            } catch (e: Exception) {
                status = strings.statusError(e.message ?: "")
            }
        }
    }

    /** Clean up the sidecar process */
    fun dispose() {
        scope.launch {
            try {
                mgr?.stop()
            } catch (_: Exception) {
            }
        }
    }

    // --- Navigation ---

    fun navigate(screen: Screen) {
        currentScreen = screen
    }

    // --- Directory selection ---

    fun selectScanDir() {
        scope.launch(Dispatchers.IO) {
            try {
                val dir = pickDirectory(
                    strings.scanDirectoryLabel,
                    scanDir.ifBlank { null }
                )
                if (dir != null) {
                    withContext(Dispatchers.Main) {
                        scanDir = dir
                        saveBothDirs()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    status = strings.statusDirError(e.message ?: "")
                }
            }
        }
    }

    fun selectOutputDir() {
        scope.launch(Dispatchers.IO) {
            try {
                val dir = pickDirectory(
                    strings.commonBrowse,
                    outputDir.ifBlank { null }
                )
                if (dir != null) {
                    withContext(Dispatchers.Main) {
                        outputDir = dir
                        saveBothDirs()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    status = strings.statusDirError(e.message ?: "")
                }
            }
        }
    }

    private fun saveBothDirs() {
        SettingsManager.update { it.copy(scanDir = scanDir, outputDir = outputDir) }
    }

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
            val o = orch ?: run { scraping = false; return@launch }
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

    // --- Settings ---

    fun updateLanguage(lang: String) {
        currentLanguage = lang
        SettingsManager.update { it.copy(language = lang) }
        // The UI recomposition is driven by currentLanguage change;
        // composition-local [LocalTranslations] is updated by the App composable.
    }

    fun toggleSite(id: String, enabled: Boolean) {
        enabledSites = if (enabled) enabledSites + id else enabledSites - id
        SettingsManager.update { it.copy(enabledSites = enabledSites) }
    }

    fun resetSettings() {
        SettingsManager.reset()
        val fresh = SettingsManager.get()
        scanDir = fresh.scanDir
        outputDir = fresh.outputDir
        workerPath = fresh.workerPath
        scanRecursive = fresh.scanRecursive
        createMovieFolders = fresh.createMovieFolders
        hardlinkInsteadOfCopy = fresh.hardlinkInsteadOfCopy
        downloadImages = fresh.downloadImages
        autoScrape = fresh.autoScrape
        enabledSites = fresh.enabledSites
        currentLanguage = fresh.language
        folderLayers = fresh.folderLayers
        filenameFormat = fresh.filenameFormat
        maxTitleLength = fresh.maxTitleLength
        maxFilenameLength = fresh.maxFilenameLength
        suffixKeywords = fresh.suffixKeywords
        updateRenameOrchestrator()
    }

    fun updateScanRecursive(v: Boolean) {
        scanRecursive = v
        SettingsManager.update { it.copy(scanRecursive = v) }
    }

    fun updateCreateMovieFolders(v: Boolean) {
        createMovieFolders = v
        SettingsManager.update { it.copy(createMovieFolders = v) }
        mgr?.let { m ->
                    orch = createScrapeOrchestrator(m)
        }
    }

    fun updateHardlink(v: Boolean) {
        hardlinkInsteadOfCopy = v
        SettingsManager.update { it.copy(hardlinkInsteadOfCopy = v) }
        mgr?.let { m ->
            orch = ScrapeOrchestrator(m, outputDir, createMovieFolders, v, downloadImages)
        }
    }

    fun updateDownloadImages(v: Boolean) {
        downloadImages = v
        SettingsManager.update { it.copy(downloadImages = v) }
        mgr?.let { m ->
            orch = ScrapeOrchestrator(m, outputDir, createMovieFolders, hardlinkInsteadOfCopy, v)
        }
    }

    fun updateAutoScrape(v: Boolean) {
        autoScrape = v
        SettingsManager.update { it.copy(autoScrape = v) }
    }

    fun updateWorkerPath(v: String) {
        workerPath = v
        SettingsManager.update { it.copy(workerPath = v) }
    }

    fun updateFolderLayer(index: Int, value: String) {
        folderLayers = folderLayers.toMutableList().also { it[index] = value }
        SettingsManager.update { it.copy(folderLayers = folderLayers) }
        updateRenameOrchestrator()
    }

    fun addLayer() {
        folderLayers = folderLayers + ""
        SettingsManager.update { it.copy(folderLayers = folderLayers) }
    }

    fun removeLayer(index: Int) {
        folderLayers = folderLayers.toMutableList().also { it.removeAt(index) }
        SettingsManager.update { it.copy(folderLayers = folderLayers) }
        updateRenameOrchestrator()
    }

    fun updateFilenameFormat(v: String) {
        filenameFormat = v
        SettingsManager.update { it.copy(filenameFormat = v) }
        updateRenameOrchestrator()
    }

    fun updateMaxTitleLength(v: Int) {
        maxTitleLength = v
        SettingsManager.update { it.copy(maxTitleLength = v) }
    }

    fun updateMaxFilenameLength(v: Int) {
        maxFilenameLength = v
        SettingsManager.update { it.copy(maxFilenameLength = v) }
        updateRenameOrchestrator()
    }

    fun updateSuffixKeywords(v: List<String>) {
        suffixKeywords = v
        SettingsManager.update { it.copy(suffixKeywords = v) }
        updateRenameOrchestrator()
    }

    private fun createScrapeOrchestrator(m: SidecarManager): ScrapeOrchestrator {
        return ScrapeOrchestrator(
            m, outputDir, createMovieFolders, hardlinkInsteadOfCopy,
            downloadImages, folderLayers, filenameFormat,
            maxTitleLength, maxFilenameLength, suffixKeywords
        )
    }

    private fun updateRenameOrchestrator() {
        mgr?.let { m -> orch = createScrapeOrchestrator(m) }
    }
}
