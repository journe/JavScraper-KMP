package javscraper

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith

import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import javscraper.ui.LocalSharedTransitionScope
import javscraper.io.external.SystemFileLauncher
import javscraper.ui.components.CollapsibleNavRail
import javscraper.ui.components.LogsDialog
import javscraper.ui.components.WorkerSetupDialog
import javscraper.ui.screens.*
import javscraper.ui.screens.settings.*

import javscraper.ui.theme.JavScraperTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class Screen { SCAN, PROGRESS, GALLERY, NETWORK_PREVIEW, SETTINGS }


@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
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
            var selectedVideoPath by remember { mutableStateOf<String?>(null) }
            val systemFileLauncher = remember { SystemFileLauncher() }
            val galleryListState = rememberLazyGridState()
            // 裁剪成功后自增:提升到详情/图库共同父级,两侧 PosterCard 共用同一
            // refreshKey 绕过 Coil 缓存重读,返回图库也能看到裁剪后的 poster
            var posterRefreshVersion by remember { mutableLongStateOf(0L) }
            LaunchedEffect(viewModel.currentScreen) {
                if (viewModel.currentScreen != Screen.GALLERY) selectedVideoPath = null
            }
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
                                    contentDescription = t.commonApplicationLogs
                                )
                            }
                            IconButton(onClick = { viewModel.navigate(Screen.SETTINGS) }) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = t.commonSettings,
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
                        galleryEnabled = viewModel.scrapedFiles.isNotEmpty() || viewModel.results.isNotEmpty()
                    )
                    Box(Modifier.weight(1f)) {
                        val galleryState = viewModel.galleryState
                        val detailVideo = if (viewModel.currentScreen == Screen.GALLERY) {
                            selectedVideoPath?.let { path -> galleryState.videoByPath(path) }
                        } else {
                            null
                        }
                        SharedTransitionLayout {
                            // scope 通过 CompositionLocal 下发,深层组件免参数透传
                            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                            AnimatedContent(
                                targetState = detailVideo,
                                transitionSpec = {
                                    fadeIn(tween(220)) togetherWith fadeOut(tween(120))
                                },
                                label = "gallery-detail-transition"
                            ) { video ->
                                if (video == null) {
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
                                            state = galleryState,
                                            posterRefreshKey = posterRefreshVersion,
                                            actions = viewModel.galleryActions.copy(
                                                onClear = {
                                                    selectedVideoPath = null
                                                    galleryListState.requestScrollToItem(0)
                                                    viewModel.clearResults()
                                                },
                                                onVideoClick = { video ->
                                                    selectedVideoPath = video.path
                                                }
                                            ),
                                            listState = galleryListState,
                                            sharedTransitionScope = this@SharedTransitionLayout,
                                            animatedVisibilityScope = this@AnimatedContent
                                        )

                                        Screen.NETWORK_PREVIEW -> NetworkPreviewScreen(
                                            state = viewModel.networkPreviewState,
                                            actions = viewModel.networkPreviewActions
                                        )
                                        Screen.SETTINGS -> SettingsScreen(
                                            state = viewModel.settingsState,
                                            actions = viewModel.settingsActions
                                        )
                                    }
                                } else {
                                    VideoDetailScreen(
                                        video = video,
                                        onBack = { selectedVideoPath = null },
                                        onRefresh = {
                                            galleryState.fileByPath(video.path)
                                                ?.let(viewModel::openSingleScrape)
                                        },
                                        onPlayVideo = {
                                            scope.launch(Dispatchers.IO) {
                                                systemFileLauncher.openVideo(video.path)
                                            }
                                        },
                                        onOpenFolder = {
                                            scope.launch(Dispatchers.IO) {
                                                systemFileLauncher.openContainingDirectory(video.path)
                                            }
                                        },
                                        animatedVisibilityScope = this@AnimatedContent,
                                        posterRefreshKey = posterRefreshVersion,
                                        onPosterCropped = { posterRefreshVersion++ }
                                    )
                                }
                            }
                            }
                        }
                    }
                }
            }
            if (viewModel.currentScreen == Screen.GALLERY && selectedVideoPath != null) {
                SingleScrapeDialog(
                    state = viewModel.scrapeProgressState,
                    actions = viewModel.scrapeProgressActions
                )
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


