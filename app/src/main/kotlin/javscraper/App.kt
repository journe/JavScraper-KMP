package javscraper

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import javscraper.i18n.LocalTranslations
import javscraper.i18n.TranslationEn
import javscraper.i18n.TranslationZh
import javscraper.ui.components.CollapsibleNavRail
import javscraper.ui.components.WorkerSetupDialog
import javscraper.ui.screens.*
import javscraper.ui.screens.settings.*

import javscraper.ui.theme.JavScraperTheme

enum class Screen { SCAN, PROGRESS, GALLERY, SETTINGS }


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val scope = rememberCoroutineScope()
    val viewModel = remember { AppViewModel(scope) }

    LaunchedEffect(Unit) { viewModel.init() }
    DisposableEffect(Unit) {
        onDispose { viewModel.dispose() }
    }

    val localeStrings: TranslationEn = remember(viewModel.currentLanguage) {
        if (viewModel.currentLanguage == "zh") TranslationZh() else TranslationEn()
    }

    CompositionLocalProvider(LocalTranslations provides localeStrings) {
        JavScraperTheme {
            var navExpanded by remember { mutableStateOf(false) }
            Scaffold(
                topBar = {
                    val t = LocalTranslations.current
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = { navExpanded = !navExpanded }) {
                                Icon(
                                    if (navExpanded) Icons.AutoMirrored.Filled.MenuOpen
                                    else Icons.Default.Menu,
                                    contentDescription = "Toggle navigation"
                                )
                            }
                        },
                        title = { Text("JavScraper") },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        actions = {
                            Text(
                                viewModel.status,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            IconButton(onClick = { viewModel.navigate(Screen.SETTINGS) }) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = t.navSettings,
                                    tint = if (viewModel.currentScreen == Screen.SETTINGS)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    )
                }
            ) { padding ->
                Row(Modifier.padding(padding)) {
                    CollapsibleNavRail(
                        expanded = navExpanded,
                        currentScreen = viewModel.currentScreen,
                        onNavigate = viewModel::navigate,
                        scrapeEnabled = viewModel.tasks.isNotEmpty(),
                        galleryEnabled = viewModel.results.isNotEmpty()
                    )
                    Box(Modifier.weight(1f)) {
                        when (viewModel.currentScreen) {
                            Screen.SCAN -> FileScanScreen(
                                state = FileScanState(
                                    viewModel.scannedFiles, viewModel.scanDir, viewModel.scanning
                                ),
                                actions = FileScanActions(
                                    viewModel::selectScanDir,
                                    viewModel::startScan,
                                    { viewModel.navigate(Screen.PROGRESS) }
                                )
                            )

                            Screen.PROGRESS -> ScrapeProgressScreen(
                                state = ScrapeProgressState(
                                    viewModel.tasks, viewModel.scraping,
                                    viewModel.singleScrapeDialogState, viewModel.singleScrapeNumber,
                                    viewModel.singleScrapeSite, viewModel.singleScrapeTask,
                                    viewModel.enabledSiteInfos
                                ),
                                actions = ScrapeProgressActions(
                                    viewModel::startAllScraping,
                                    viewModel::cancelScraping,
                                    viewModel::openSingleScrapeFromTask,
                                    viewModel::updateSingleScrapeNumber,
                                    viewModel::updateSingleScrapeSite,
                                    viewModel::startSingleScrape,
                                    viewModel::closeSingleScrape,
                                    viewModel::confirmSingleScrape
                                )
                            )

                            Screen.GALLERY -> ResultGalleryScreen(
                                state = GalleryState(viewModel.results, viewModel.outputDir),
                                actions = GalleryActions(viewModel::clearResults, {})
                            )

                            Screen.SETTINGS -> SettingsScreen(
                                state = SettingsState(
                                    workerPath = viewModel.workerPath,
                                    outputDir = viewModel.outputDir,
                                    scanDir = viewModel.scanDir,
                                    scanRecursive = viewModel.scanRecursive,
                                    createMovieFolders = viewModel.createMovieFolders,
                                    hardlinkInsteadOfCopy = viewModel.hardlinkInsteadOfCopy,
                                    downloadImages = viewModel.downloadImages,
                                    autoScrape = viewModel.autoScrape,
                                    sites = viewModel.sites,
                                    enabledSiteIds = viewModel.enabledSites,
                                    language = viewModel.currentLanguage,
                                    showRestartHint = viewModel.showRestartHint,
                                    folderLayers = viewModel.folderLayers,
                                    filenameFormat = viewModel.filenameFormat,
                                    maxTitleLength = viewModel.maxTitleLength,
                                    maxFilenameLength = viewModel.maxFilenameLength,
                                    suffixKeywords = viewModel.suffixKeywords,
                                    siteCheckRunning = viewModel.siteCheckRunning,
                                    siteCheckResults = viewModel.siteCheckResults
                                ),
                                actions = SettingsActions(
                                    onLanguageChange = viewModel::updateLanguage,
                                    onSelectOutputDir = viewModel::selectOutputDir,
                                    onSelectScanDir = viewModel::selectScanDir,
                                    onSelectWorkerPath = viewModel::selectWorkerPath,
                                    onWorkerPathChange = viewModel::updateWorkerPath,
                                    onScanRecursiveChange = viewModel::updateScanRecursive,
                                    onCreateMovieFoldersChange = viewModel::updateCreateMovieFolders,
                                    onHardlinkChange = viewModel::updateHardlink,
                                    onDownloadImagesChange = viewModel::updateDownloadImages,
                                    onAutoScrapeChange = viewModel::updateAutoScrape,
                                    onToggleSite = viewModel::toggleSite,
                                    onReset = viewModel::resetSettings,
                                    onFolderLayerChange = viewModel::updateFolderLayer,
                                    onAddLayer = viewModel::addLayer,
                                    onRemoveLayer = viewModel::removeLayer,
                                    onFilenameFormatChange = viewModel::updateFilenameFormat,
                                    onMaxTitleLengthChange = viewModel::updateMaxTitleLength,
                                    onMaxFilenameLengthChange = viewModel::updateMaxFilenameLength,
                                    onSuffixKeywordsChange = viewModel::updateSuffixKeywords,
                                    onCheckSites = viewModel::checkSites
                                )
                            )
                        }
                    }
                }
            }
            if (viewModel.workerSetupVisible) {
                WorkerSetupDialog(
                    errorMessage = viewModel.workerSetupError,
                    onSelect = viewModel::selectWorkerPath,
                    onCancel = viewModel::dismissWorkerSetup
                )
            }
        }
    }
}


