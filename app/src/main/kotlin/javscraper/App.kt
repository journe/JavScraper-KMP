package javscraper

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import javscraper.io.FileScanner
import javscraper.models.ScannedFile
import javscraper.models.SiteInfo
import javscraper.models.Video
import javscraper.scrape.ScrapeOrchestrator
import javscraper.settings.AppSettings
import javscraper.settings.SettingsManager
import javscraper.sidecar.SidecarManager
import javscraper.ui.screens.*
import javscraper.ui.theme.JavScraperTheme
import kotlinx.coroutines.launch
import java.nio.file.Paths
import javax.swing.JFileChooser

enum class Screen { SCAN, PROGRESS, GALLERY, SETTINGS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val scope = rememberCoroutineScope()
    val settings = remember { SettingsManager.get() }
    val screen = remember { mutableStateOf(Screen.SCAN) }
    val status = remember { mutableStateOf("Initializing...") }
    val scannedFiles = remember { mutableStateOf<List<ScannedFile>>(emptyList()) }
    val results = remember { mutableStateOf<List<Video>>(emptyList()) }
    val tasks = remember { mutableStateOf<List<ScrapeTask>>(emptyList()) }
    val scanning = remember { mutableStateOf(false) }
    val scraping = remember { mutableStateOf(false) }
    val sites = remember { mutableStateOf<List<SiteInfo>>(emptyList()) }
    val scanDir = remember { mutableStateOf(settings.scanDir) }
    val outputDir = remember { mutableStateOf(settings.outputDir) }

    val mgr = remember { SidecarManager(Paths.get(System.getProperty("user.dir"), settings.workerPath).toString()) }
    val orch = remember { mutableStateOf<ScrapeOrchestrator?>(null) }

    fun saveBothDirs() {
        SettingsManager.update { it.copy(scanDir = scanDir.value, outputDir = outputDir.value) }
    }

    LaunchedEffect(Unit) {
        try {
            status.value = "Starting..."
            if (mgr.start()) {
                sites.value = mgr.listSites()
                status.value = "Ready (${sites.value.size} sites)"
                orch.value = ScrapeOrchestrator(mgr, outputDir.value, true, true, true)
            } else status.value = "Failed"
        } catch (e: Exception) { status.value = "Error: ${e.message}" }
    }

    DisposableEffect(Unit) { onDispose { scope.launch { mgr.stop() } } }

    val onSelectDir: () -> Unit = {
        scope.launch {
            val c = JFileChooser()
            c.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            if (c.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                val dir = c.selectedFile.absolutePath
                scanDir.value = dir
                saveBothDirs()
            }
        }
    }

    val onSelectOutDir: () -> Unit = {
        scope.launch {
            val c = JFileChooser()
            c.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            if (c.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                val dir = c.selectedFile.absolutePath
                outputDir.value = dir
                saveBothDirs()
            }
        }
    }

    JavScraperTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("JavScraper") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    actions = { Text(status.value, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 8.dp)) }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(icon = { Icon(Icons.Default.Search, null) }, label = { Text("Scan") }, selected = screen.value == Screen.SCAN, onClick = { screen.value = Screen.SCAN })
                    NavigationBarItem(icon = { Icon(Icons.Default.Download, null) }, label = { Text("Scrape") }, selected = screen.value == Screen.PROGRESS, onClick = { screen.value = Screen.PROGRESS })
                    NavigationBarItem(icon = { Icon(Icons.Default.PhotoLibrary, null) }, label = { Text("Gallery") }, selected = screen.value == Screen.GALLERY, onClick = { screen.value = Screen.GALLERY })
                    NavigationBarItem(icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") }, selected = screen.value == Screen.SETTINGS, onClick = { screen.value = Screen.SETTINGS })
                }
            }
        ) { pv -> Box(Modifier.padding(pv)) {
            when (screen.value) {
                Screen.SCAN -> FileScanScreen(
                    scannedFiles = scannedFiles.value, scanDir = scanDir.value, isScanning = scanning.value,
                    onSelectDirectory = onSelectDir,
                    onStartScan = { scope.launch { scanning.value = true; scannedFiles.value = FileScanner.scanDirectory(Paths.get(scanDir.value), true); scanning.value = false } },
                    onStartScrape = { tasks.value = scannedFiles.value.filter { it.number.isNotBlank() }.map { ScrapeTask(it.number, it.fileName) }; screen.value = Screen.PROGRESS }
                )
                Screen.PROGRESS -> ScrapeProgressScreen(
                    tasks = tasks.value, isRunning = scraping.value,
                    onStartAll = { scope.launch {
                        scraping.value = true
                        val o = orch.value ?: return@launch
                        for (i in tasks.value.indices) {
                            val t = tasks.value[i]
                            val updated = tasks.value.toMutableList()
                            updated[i] = t.copy(status = ScrapeTaskStatus.SCRAPING)
                            tasks.value = updated
                            val f = scannedFiles.value.find { it.number == t.number }
                            if (f != null) {
                                try {
                                    val r = o.process(f)
                                    val newTasks = tasks.value.toMutableList()
                                    if (r.success && r.data != null) {
                                        results.value = results.value + r.data
                                        newTasks[i] = t.copy(status = ScrapeTaskStatus.SUCCESS, video = r.data)
                                    } else newTasks[i] = t.copy(status = ScrapeTaskStatus.FAILED, error = r.error?.message ?: "Failed")
                                    tasks.value = newTasks
                                } catch (e: Exception) {
                                    val newTasks = tasks.value.toMutableList()
                                    newTasks[i] = t.copy(status = ScrapeTaskStatus.FAILED, error = e.message ?: "Error")
                                    tasks.value = newTasks
                                }
                            }
                        }
                        scraping.value = false
                    } },
                    onCancel = { scraping.value = false }
                )
                Screen.GALLERY -> ResultGalleryScreen(results = results.value, onClear = { results.value = emptyList(); tasks.value = emptyList() }, onOpenOutputDir = {}, outputDir = outputDir.value)
                Screen.SETTINGS -> SettingsScreen(
                    workerPath = settings.workerPath,
                    outputDir = outputDir.value,
                    scanDir = scanDir.value,
                    scanRecursive = true,
                    createMovieFolders = true,
                    hardlinkInsteadOfCopy = true,
                    downloadImages = true,
                    autoScrape = false,
                    sites = sites.value,
                    enabledSiteIds = settings.enabledSites,
                    onWorkerPathChange = { _ -> },
                    onOutputDirChange = { _ -> },
                    onSelectOutputDir = onSelectOutDir,
                    onSelectScanDir = onSelectDir,
                    onSelectWorkerPath = { },
                    onScanRecursiveChange = { _ -> },
                    onCreateMovieFoldersChange = { _ -> },
                    onHardlinkChange = { _ -> },
                    onDownloadImagesChange = { _ -> },
                    onAutoScrapeChange = { _ -> },
                    onToggleSite = { _: String, _: Boolean -> },
                    onReset = { SettingsManager.reset(); scanDir.value = ""; outputDir.value = "" }
                )
            }
        } }
    }
}