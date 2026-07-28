package javscraper

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import javscraper.i18n.LocalTranslations
import javscraper.i18n.TranslationEn
import javscraper.i18n.TranslationZh
import javscraper.ui.screens.*
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
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("JavScraper") },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        actions = {
                            Text(
                                viewModel.status,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    )
                },
                bottomBar = {
                    val t = LocalTranslations.current
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Search, null) },
                            label = { Text(t.navScan) },
                            selected = viewModel.currentScreen == Screen.SCAN,
                            onClick = { viewModel.navigate(Screen.SCAN) }
                        )
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (viewModel.tasks.isNotEmpty()) Icons.Default.CloudDownload
                                    else Icons.Default.CloudOff, null
                                )
                            },
                            label = { Text(t.navScrape) },
                            selected = viewModel.currentScreen == Screen.PROGRESS,
                            onClick = { viewModel.navigate(Screen.PROGRESS) },
                            enabled = viewModel.tasks.isNotEmpty()
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.PhotoLibrary, null) },
                            label = { Text(t.navGallery) },
                            selected = viewModel.currentScreen == Screen.GALLERY,
                            onClick = { viewModel.navigate(Screen.GALLERY) },
                            enabled = viewModel.results.isNotEmpty()
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Settings, null) },
                            label = { Text(t.navSettings) },
                            selected = viewModel.currentScreen == Screen.SETTINGS,
                            onClick = { viewModel.navigate(Screen.SETTINGS) }
                        )
                    }
                }
            ) { padding ->
                Box(Modifier.padding(padding)) {
                    when (viewModel.currentScreen) {
                        Screen.SCAN -> FileScanScreen(
                            scannedFiles = viewModel.scannedFiles,
                            scanDir = viewModel.scanDir,
                            isScanning = viewModel.scanning,
                            onSelectDirectory = viewModel::selectScanDir,
                            onStartScan = viewModel::startScan,
                            onStartScrape = { viewModel.navigate(Screen.PROGRESS) }
                        )

                        Screen.PROGRESS -> ScrapeProgressScreen(
                            tasks = viewModel.tasks,
                            isRunning = viewModel.scraping,
                            onStartAll = viewModel::startAllScraping,
                            onCancel = viewModel::cancelScraping
                        )

                        Screen.GALLERY -> ResultGalleryScreen(
                            results = viewModel.results,
                            onClear = viewModel::clearResults,
                            onOpenOutputDir = {},
                            outputDir = viewModel.outputDir
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
                                showRestartHint = viewModel.showRestartHint
                            ),
                            actions = SettingsActions(
                                onLanguageChange = viewModel::updateLanguage,
                                onSelectOutputDir = viewModel::selectOutputDir,
                                onSelectScanDir = viewModel::selectScanDir,
                                onSelectWorkerPath = {},
                                onWorkerPathChange = viewModel::updateWorkerPath,
                                onScanRecursiveChange = viewModel::updateScanRecursive,
                                onCreateMovieFoldersChange = viewModel::updateCreateMovieFolders,
                                onHardlinkChange = viewModel::updateHardlink,
                                onDownloadImagesChange = viewModel::updateDownloadImages,
                                onAutoScrapeChange = viewModel::updateAutoScrape,
                                onToggleSite = viewModel::toggleSite,
                                onReset = viewModel::resetSettings
                            )
                        )
                    }
                }
            }
        }
    }
}
