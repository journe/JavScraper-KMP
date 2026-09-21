package javscraper

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import javscraper.i18n.LocalTranslations
import javscraper.i18n.TranslationEn
import javscraper.i18n.TranslationZh
import javscraper.io.external.SystemFileLauncher
import javscraper.models.Video
import javscraper.ui.LocalSharedTransitionScope
import javscraper.ui.components.CollapsibleNavRail
import javscraper.ui.components.LogsDialog
import javscraper.ui.components.WorkerSetupDialog
import javscraper.ui.components.media.PosterCropDialog
import javscraper.ui.components.media.cropSourceModel
import javscraper.ui.components.media.localPosterPath
import javscraper.ui.screens.FileScanScreen
import javscraper.ui.screens.NetworkPreviewScreen
import javscraper.ui.screens.ResultGalleryScreen
import javscraper.ui.screens.ScrapeProgressScreen
import javscraper.ui.screens.SingleScrapeDialog
import javscraper.ui.screens.VideoDetailScreen
import javscraper.ui.screens.detail.VideoDetailActions
import javscraper.ui.screens.settings.SettingsScreen
import javscraper.ui.theme.JavScraperTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

enum class Screen { SCAN, PROGRESS, GALLERY, NETWORK_PREVIEW, SETTINGS }

private data class PosterCropRequest(
    val video: Video,
    val sourceFile: File
)


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
            var cropRequest by remember { mutableStateOf<PosterCropRequest?>(null) }
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
                            if (viewModel.updateMode) {
                                IconButton(onClick = { }) {
                                    Icon(
                                        Icons.Default.Upload,
                                        contentDescription = t.settingsUpdateMode
                                    )
                                }
                            }
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
                                        val cropSource =
                                            remember(video.path) { cropSourceModel(video) }
                                        VideoDetailScreen(
                                            video = video,
                                            actions = VideoDetailActions(
                                                onBack = { selectedVideoPath = null },
                                                onRefresh = {
                                                    galleryState.fileByPath(video.path)
                                                        ?.let(viewModel::openSingleScrape)
                                                },
                                                onSaveMetadata = viewModel::saveVideoMetadata,
                                                onMetadataSaved = { saved ->
                                                    selectedVideoPath = saved.path
                                                },
                                                onCropPoster = cropSource?.let { source ->
                                                    {
                                                        cropRequest =
                                                            PosterCropRequest(video, source)
                                                    }
                                                },
                                                onPlayVideo = {
                                                    scope.launch(Dispatchers.IO) {
                                                        systemFileLauncher.openVideo(video.path)
                                                    }
                                                },
                                                onOpenFolder = {
                                                    scope.launch(Dispatchers.IO) {
                                                        systemFileLauncher.openContainingDirectory(
                                                            video.path
                                                        )
                                                    }
                                                }
                                            ),
                                            animatedVisibilityScope = this@AnimatedContent,
                                            posterRefreshKey = posterRefreshVersion
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

            cropRequest?.let { request ->
                PosterCropDialog(
                    video = request.video,
                    sourceFile = request.sourceFile,
                    posterFile = File(localPosterPath(request.video)),
                    onCropped = {
                        posterRefreshVersion++
                        cropRequest = null
                    },
                    onDismiss = { cropRequest = null }
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

