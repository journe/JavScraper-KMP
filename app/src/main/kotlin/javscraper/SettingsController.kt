package javscraper

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import javscraper.i18n.TranslationEn
import javscraper.i18n.TranslationZh
import javscraper.io.pickDirectory
import javscraper.settings.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Holds settings UI state and persistence, independent of worker/scrape logic. */
class SettingsController(private val scope: CoroutineScope) {

    /** Invoked when scraper-related settings change so the orchestrator can be rebuilt. */
    var onScrapeSettingsChanged: () -> Unit = {}
    /** Invoked to surface errors to the app status bar. */
    var onStatusChange: (String) -> Unit = {}
    /** Invoked when file logging changes so the app log sink can be updated. */
    var onFileLoggingChanged: (Boolean) -> Unit = {}

    var scanDir by mutableStateOf(SettingsManager.get().scanDir)
    var outputDir by mutableStateOf(SettingsManager.get().outputDir)
    var workerPath by mutableStateOf(SettingsManager.get().workerPath)
    var currentLanguage by mutableStateOf(SettingsManager.get().language)
    var showRestartHint by mutableStateOf(false)
    var enabledSites by mutableStateOf(SettingsManager.get().enabledSites)
    var scanRecursive by mutableStateOf(SettingsManager.get().scanRecursive)
    var createMovieFolders by mutableStateOf(SettingsManager.get().createMovieFolders)
    var hardlinkInsteadOfCopy by mutableStateOf(SettingsManager.get().hardlinkInsteadOfCopy)
    var downloadImages by mutableStateOf(SettingsManager.get().downloadImages)
    var downloadPreviewImages by mutableStateOf(SettingsManager.get().downloadPreviewImages)
    var downloadWebPages by mutableStateOf(SettingsManager.get().downloadWebPages)
    var lockData by mutableStateOf(SettingsManager.get().lockData)
    var autoScrape by mutableStateOf(SettingsManager.get().autoScrape)
    var fileLoggingEnabled by mutableStateOf(SettingsManager.get().fileLoggingEnabled)
    var folderLayers by mutableStateOf(SettingsManager.get().folderLayers)
    var filenameFormat by mutableStateOf(SettingsManager.get().filenameFormat)
    var maxTitleLength by mutableStateOf(SettingsManager.get().maxTitleLength)
    var maxFilenameLength by mutableStateOf(SettingsManager.get().maxFilenameLength)
    var suffixKeywords by mutableStateOf(SettingsManager.get().suffixKeywords)

    val strings: TranslationEn
        get() = if (currentLanguage == "zh") TranslationZh() else TranslationEn()

    fun updateLanguage(lang: String) {
        currentLanguage = lang
        SettingsManager.update { it.copy(language = lang) }
        // The UI recomposition is driven by currentLanguage change;
        // composition-local [LocalTranslations] is updated by the App composable.
    }

    // --- Directory selection ---

    fun selectScanDir() {
        scope.launch(Dispatchers.IO) {
            try {
                val dir = pickDirectory(strings.commonScanDirectory, scanDir.ifBlank { null })
                if (dir != null) {
                    withContext(Dispatchers.Main) {
                        scanDir = dir
                        saveBothDirs()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onStatusChange(strings.statusDirError(e.message ?: ""))
                }
            }
        }
    }

    fun selectOutputDir() {
        scope.launch(Dispatchers.IO) {
            try {
                val dir = pickDirectory(strings.commonBrowse, outputDir.ifBlank { null })
                if (dir != null) {
                    withContext(Dispatchers.Main) {
                        outputDir = dir
                        saveBothDirs()
                        onScrapeSettingsChanged()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onStatusChange(strings.statusDirError(e.message ?: ""))
                }
            }
        }
    }

    private fun saveBothDirs() {
        SettingsManager.update { it.copy(scanDir = scanDir, outputDir = outputDir) }
    }

    // --- Settings updates ---

    fun updateWorkerPath(v: String) {
        workerPath = v
        SettingsManager.update { it.copy(workerPath = v) }
    }

    fun updateScanRecursive(v: Boolean) {
        scanRecursive = v
        SettingsManager.update { it.copy(scanRecursive = v) }
    }

    fun updateCreateMovieFolders(v: Boolean) {
        createMovieFolders = v
        SettingsManager.update { it.copy(createMovieFolders = v) }
        onScrapeSettingsChanged()
    }

    fun updateHardlink(v: Boolean) {
        hardlinkInsteadOfCopy = v
        SettingsManager.update { it.copy(hardlinkInsteadOfCopy = v) }
        onScrapeSettingsChanged()
    }

    fun updateDownloadImages(v: Boolean) {
        downloadImages = v
        SettingsManager.update { it.copy(downloadImages = v) }
        onScrapeSettingsChanged()
    }

    fun updateDownloadPreviewImages(v: Boolean) {
        downloadPreviewImages = v
        SettingsManager.update { it.copy(downloadPreviewImages = v) }
        onScrapeSettingsChanged()
    }

    fun updateDownloadWebPages(v: Boolean) {
        downloadWebPages = v
        SettingsManager.update { it.copy(downloadWebPages = v) }
        onScrapeSettingsChanged()
    }

    fun updateLockData(v: Boolean) {
        lockData = v
        SettingsManager.update { it.copy(lockData = v) }
        onScrapeSettingsChanged()
    }

    fun updateAutoScrape(v: Boolean) {
        autoScrape = v
        SettingsManager.update { it.copy(autoScrape = v) }
    }

    fun updateFileLogging(v: Boolean) {
        fileLoggingEnabled = v
        SettingsManager.update { it.copy(fileLoggingEnabled = v) }
        onFileLoggingChanged(v)
    }

    fun toggleSite(id: String, enabled: Boolean) {
        enabledSites = if (enabled) enabledSites + id else enabledSites - id
        SettingsManager.update { it.copy(enabledSites = enabledSites) }
        onScrapeSettingsChanged()
    }

    fun updateFolderLayer(index: Int, value: String) {
        folderLayers = folderLayers.toMutableList().also { it[index] = value }
        SettingsManager.update { it.copy(folderLayers = folderLayers) }
        onScrapeSettingsChanged()
    }

    fun addLayer() {
        folderLayers = folderLayers + ""
        SettingsManager.update { it.copy(folderLayers = folderLayers) }
    }

    fun removeLayer(index: Int) {
        folderLayers = folderLayers.toMutableList().also { it.removeAt(index) }
        SettingsManager.update { it.copy(folderLayers = folderLayers) }
        onScrapeSettingsChanged()
    }

    fun updateFilenameFormat(v: String) {
        filenameFormat = v
        SettingsManager.update { it.copy(filenameFormat = v) }
        onScrapeSettingsChanged()
    }

    fun updateMaxTitleLength(v: Int) {
        maxTitleLength = v
        SettingsManager.update { it.copy(maxTitleLength = v) }
        onScrapeSettingsChanged()
    }

    fun updateMaxFilenameLength(v: Int) {
        maxFilenameLength = v
        SettingsManager.update { it.copy(maxFilenameLength = v) }
        onScrapeSettingsChanged()
    }

    fun updateSuffixKeywords(v: List<String>) {
        suffixKeywords = v
        SettingsManager.update { it.copy(suffixKeywords = v) }
        onScrapeSettingsChanged()
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
        downloadPreviewImages = fresh.downloadPreviewImages
        downloadWebPages = fresh.downloadWebPages
        lockData = fresh.lockData
        autoScrape = fresh.autoScrape
        fileLoggingEnabled = fresh.fileLoggingEnabled
        enabledSites = fresh.enabledSites
        currentLanguage = fresh.language
        folderLayers = fresh.folderLayers
        filenameFormat = fresh.filenameFormat
        maxTitleLength = fresh.maxTitleLength
        maxFilenameLength = fresh.maxFilenameLength
        suffixKeywords = fresh.suffixKeywords
        onFileLoggingChanged(fileLoggingEnabled)
        onScrapeSettingsChanged()
    }
}
