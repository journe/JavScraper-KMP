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
import javscraper.ui.components.LogsDialog
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
            var logsVisible by remember { mutableStateOf(false) }
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
                            IconButton(onClick = { logsVisible = true }) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = t.navLogs
                                )
                            }
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
                                state = viewModel.scanState,
                                actions = viewModel.scanActions
                            )

                            Screen.PROGRESS -> ScrapeProgressScreen(
                                state = viewModel.scrapeProgressState,
                                actions = viewModel.scrapeProgressActions
                            )

                            Screen.GALLERY -> ResultGalleryScreen(
                                state = viewModel.galleryState,
                                actions = viewModel.galleryActions
                            )

                            Screen.SETTINGS -> SettingsScreen(
                                state = viewModel.settingsState,
                                actions = viewModel.settingsActions
                            )
                        }
                    }
                }
            }
            if (logsVisible) {
                LogsDialog(
                    entries = viewModel.lifecycleLogEntries,
                    filePath = viewModel.lifecycleLogFilePath,
                    onDismiss = { logsVisible = false }
                )
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


