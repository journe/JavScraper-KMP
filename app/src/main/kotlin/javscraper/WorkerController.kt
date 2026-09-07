package javscraper

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import javscraper.io.pickFile
import javscraper.models.SiteCheckResult
import javscraper.models.SiteInfo
import javscraper.scrape.ScrapeOrchestrator
import javscraper.settings.SettingsManager
import javscraper.sidecar.SidecarManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.file.Paths

/** Manages the scraper worker process, site list and site connectivity checks. */
class WorkerController(
    private val scope: CoroutineScope,
    private val settings: SettingsController
) {

    /** Invoked to surface progress or errors to the app status bar. */
    var onStatusChange: (String) -> Unit = {}

    var sites by mutableStateOf<List<SiteInfo>>(emptyList())
    var workerSetupVisible by mutableStateOf(false)
    var workerSetupError by mutableStateOf<String?>(null)
    var siteCheckRunning by mutableStateOf(false)
    var siteCheckResults by mutableStateOf<List<SiteCheckResult>?>(null)

    private var mgr: SidecarManager? = null
    var orch: ScrapeOrchestrator? = null
        private set

    /**
     * Starts (or restarts) the sidecar worker. If startup fails — for example when the
     * scraper-worker.exe path is misconfigured — shows a dialog asking the user to pick
     * the correct worker executable, and retries until it starts successfully or the
     * user cancels.
     */
    suspend fun ensureWorkerRunning() {
        try {
            onStatusChange(settings.strings.statusStarting)
            val m = SidecarManager(resolveWorkerPath())
            val started = withContext(Dispatchers.IO) { m.start() }
            if (started) {
                val siteList = withContext(Dispatchers.IO) { m.listSites() }
                mgr?.let { old -> if (old !== m) runCatching { old.stop() } }
                mgr = m
                sites = siteList
                orch = createScrapeOrchestrator(m)
                onStatusChange(settings.strings.statusReady(siteList.size))
                workerSetupVisible = false
                workerSetupError = null
            } else {
                workerSetupVisible = true
                workerSetupError = null
            }
        } catch (e: Exception) {
            onStatusChange(settings.strings.statusError(e.message ?: ""))
            workerSetupVisible = true
            workerSetupError = e.message
        }
    }

    private fun resolveWorkerPath(): String {
        val configured = Paths.get(settings.workerPath)
        // 绝对路径（如用户通过对话框选择的路径）直接使用，避免与 user.dir 拼接产生非法路径
        return if (configured.isAbsolute) configured.toString()
            else Paths.get(System.getProperty("user.dir"), settings.workerPath).toString()
    }

    /** Open a file picker and retry starting the worker with the selected path. */
    fun selectWorkerPath() {
        scope.launch(Dispatchers.IO) {
            try {
                val file = pickFile(settings.strings.workerSetupSelectTitle, listOf("exe", "bat"))
                if (file != null) {
                    withContext(Dispatchers.Main) {
                        settings.workerPath = file
                        SettingsManager.update { it.copy(workerPath = file) }
                        ensureWorkerRunning()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onStatusChange(settings.strings.statusDirError(e.message ?: ""))
                }
            }
        }
    }

    /** Dismiss the worker setup dialog without starting the worker. */
    fun dismissWorkerSetup() {
        workerSetupVisible = false
        workerSetupError = null
    }

    /** Clean up the sidecar process. */
    fun dispose() {
        runBlocking {
            withTimeoutOrNull(2_000) {
                try {
                    mgr?.stop()
                } catch (_: Exception) {
                }
            }
        }
    }

    /** Probe connectivity of all registered scraper sites. */
    fun checkSites() {
        val m = mgr
        if (m == null || siteCheckRunning) return
        scope.launch {
            siteCheckRunning = true
            siteCheckResults = null
            try {
                siteCheckResults = withContext(Dispatchers.IO) { m.checkSites(settings.enabledSites) }
            } catch (e: Exception) {
                siteCheckResults = emptyList()
                onStatusChange(settings.strings.statusError(e.message ?: "check failed"))
            } finally {
                siteCheckRunning = false
            }
        }
    }

    /** Rebuild the orchestrator after scraper-related settings change. */
    fun rebuildOrchestrator() {
        mgr?.let { m -> orch = createScrapeOrchestrator(m) }
    }

    private fun createScrapeOrchestrator(m: SidecarManager): ScrapeOrchestrator {
        return ScrapeOrchestrator(
            sidecar = m,
            outputDir = settings.outputDir,
            createMovieFolders = settings.createMovieFolders,
            hardlinkInsteadOfCopy = settings.hardlinkInsteadOfCopy,
            downloadImages = settings.downloadImages,
            downloadWebPages = settings.downloadWebPages,
            folderLayers = settings.folderLayers,
            filenameFormat = settings.filenameFormat,
            maxTitleLength = settings.maxTitleLength,
            maxFilenameLength = settings.maxFilenameLength,
            suffixKeywords = settings.suffixKeywords,
            enabledSites = settings.enabledSites.toSet()
        )
    }
}
