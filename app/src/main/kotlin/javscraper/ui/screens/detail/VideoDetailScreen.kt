package javscraper.ui.screens.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import javscraper.i18n.LocalTranslations
import javscraper.i18n.TranslationZh
import javscraper.io.metadata.VideoMetadataEditResult
import javscraper.models.Video
import javscraper.ui.LocalSharedTransitionScope
import javscraper.ui.components.media.ExtraFanartCarousel
import javscraper.ui.components.media.ExtraFanartViewer
import javscraper.ui.components.media.PosterCard
import javscraper.ui.components.media.PosterSource
import javscraper.ui.components.media.carouselImageBoundsModifier
import javscraper.ui.components.media.listExtraFanartImages
import javscraper.ui.components.media.localFanartModel
import javscraper.ui.components.media.posterViewerKey
import javscraper.ui.components.media.partposter.PartPosterCarousel
import javscraper.ui.components.media.partposter.listPartPosters
import javscraper.ui.previewVideoWithAllFields
import javscraper.ui.screens.galleryPosterModifier
import javscraper.ui.theme.JavScraperTheme
import java.io.File

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun VideoDetailScreen(
    video: Video,
    actions: VideoDetailActions,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    // 由 App 层持有并注入:与图库页共享同一裁剪版本号,两侧 PosterCard 同步刷新
    posterRefreshKey: Any? = null,
) {
    var editVisible by remember { mutableStateOf(false) }
    // extrafanart 目录下的预览图:IO 读取后驱动 Carousel
    var extraFanartImages by remember(video.path) { mutableStateOf(listExtraFanartImages(video)) }
    val partPosterImages = remember(video.path) { listPartPosters(video) }
    var posterCardWidth by remember(video.path) { mutableStateOf(190.dp) }
    // 当前展开的大图索引;null 表示未打开。点击 Carousel 卡片时设置,
    // 大图与卡片通过 SharedTransition(文件路径 key)联动缩放
    var viewerImageIndex by remember { mutableStateOf<Int?>(null) }
    var posterViewerState by remember { mutableStateOf(PosterViewerState.HIDDEN) }
    val posterViewerTransitionState = remember { MutableTransitionState(false) }
    posterViewerTransitionState.targetState = posterViewerState == PosterViewerState.VISIBLE
    val posterImage = remember(video.path, posterRefreshKey) { localFanartModel(video) }
    // Esc 退出:大图打开时先关大图,否则返回图库。焦点链方案与
    // ExtraFanartViewer 一致:可聚焦+主动抢焦点才能收到按键
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    // 大图关闭后 Compose 不会自动归还焦点(Viewer 移除时焦点被清空),
    // 监听到关闭即重新抢回,保证后续 Esc 仍能返回图库
    LaunchedEffect(viewerImageIndex, posterViewerState) {
        if (shouldRestoreDetailFocus(viewerImageIndex, posterViewerState)) {
            focusRequester.requestFocus()
        }
    }
    // AnimatedVisibility 的布尔值只表达"正在退出",不知道退出何时结束。
    // 等转场真正停稳后再回到 HIDDEN,避免焦点提前回到详情页后,
    // 第二下 Esc 穿透成页面返回
    LaunchedEffect(
        posterViewerState,
        posterViewerTransitionState.currentState,
        posterViewerTransitionState.isIdle
    ) {
        if (
            posterViewerState == PosterViewerState.EXITING &&
            !posterViewerTransitionState.targetState &&
            posterViewerTransitionState.isIdle
        ) {
            posterViewerState = PosterViewerState.HIDDEN
        }
    }
    Surface(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if ((event.type == KeyEventType.KeyDown) && (event.key == Key.Escape)) {
                    when (detailEscapeAction(viewerImageIndex, posterViewerState)) {
                        DetailEscapeAction.CLOSE_EXTRA_FANART_VIEWER -> viewerImageIndex = null
                        DetailEscapeAction.CLOSE_POSTER_VIEWER ->
                            posterViewerState = PosterViewerState.EXITING

                        DetailEscapeAction.IGNORE -> Unit
                        DetailEscapeAction.BACK -> actions.onBack()
                    }
                    true
                } else {
                    false
                }
            },
        color = MaterialTheme.colorScheme.background
    ) {
        Column {
            VideoDetailHeader(
                video = video,
                actions = actions,
                onEdit = { editVisible = true }
            )
            Spacer(Modifier.height(16.dp))
            // 先测量 Poster 的实际宽度，再决定信息卡同行还是换行；
            // 信息卡同行时从最小宽度开始吃剩余宽度，最大不超过 360dp
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val posterModifier = galleryPosterModifier(
                    video = video,
                    sharedTransitionScope = LocalSharedTransitionScope.current,
                    animatedVisibilityScope = animatedVisibilityScope
                )
                AdaptiveVideoInfoCard(
                    video = video,
                    modifier = Modifier.fillMaxWidth(),
                    poster = {
                        AnimatedVisibility(
                            visible = posterViewerState != PosterViewerState.VISIBLE,
                            enter = fadeIn(tween(120)),
                            exit = fadeOut(tween(120)),
                        ) {
                            val image = posterImage
                            val viewerBoundsModifier = image?.let {
                                carouselImageBoundsModifier(
                                    image = image,
                                    sharedTransitionScope = LocalSharedTransitionScope.current,
                                    animatedVisibilityScope = this@AnimatedVisibility,
                                    sharedContentKey = posterViewerKey(image)
                                )
                            } ?: Modifier
                            PosterCard(
                                video = video,
                                onClick = {
                                    if (image != null) posterViewerState = PosterViewerState.VISIBLE
                                },
                                modifier = posterModifier.then(viewerBoundsModifier)
                                    .onSizeChanged { posterCardWidth = it.width.dp },
                                posterRefreshKey = posterRefreshKey,
                                // 详情页展示目录下的横版封面 fanart,卡片宽高比随图片自适应
                                source = PosterSource.FANART
                            )
                        }
                    }
                )
                // 多分段海报在信息卡后展示;单分段时收集结果为空,不改变现状。
                PartPosterCarousel(
                    posters = partPosterImages,
                    width = posterCardWidth,
                    posterRefreshKey = posterRefreshKey,
                )

                // extrafanart 预览图 Carousel:占满一整行,无图时不渲染。
                // selectedIndex 驱动选中卡片"退出",为 SharedTransition 提供
                // exiting bounds;viewer 弹层作为 incoming 端与之配对
                if (extraFanartImages.isNotEmpty()) {
                    ExtraFanartCarousel(
                        images = extraFanartImages,
                        modifier = Modifier.fillMaxWidth(),
                        selectedIndex = viewerImageIndex,
                        onImageClick = { index -> viewerImageIndex = index },
                    )
                }
            }
        }

        // 大图查看层:叠在详情页之上,AnimatedVisibility 提供进出场作用域供
        // SharedTransition 计算卡片→大图的 bounds 动画;tween 与 PosterCard
        // 转场节奏对齐,避免遮罩与图片缩放不同步产生闪现感。
        // 退出期间内容 lambda 会重组,此时 viewerImageIndex 已为 null,若直接
        // 条件渲染会把 Viewer 摘出组合树,退出动画来不及播——
        // 用 remember 保留最后一个非空索引,让退出动画完整播放
        var lastViewedImageIndex by remember { mutableStateOf<Int?>(null) }
        if (viewerImageIndex != null) lastViewedImageIndex = viewerImageIndex
        AnimatedVisibility(
            visible = viewerImageIndex != null,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(120)),
        ) {
            val index = lastViewedImageIndex
            if (index != null && index < extraFanartImages.size) {
                ExtraFanartViewer(
                    image = extraFanartImages[index],
                    onDismiss = { viewerImageIndex = null },
                    animatedVisibilityScope = this@AnimatedVisibility,
                )
            }
        }
        var lastPosterImage by remember { mutableStateOf<File?>(null) }
        if (posterViewerState == PosterViewerState.VISIBLE) lastPosterImage = posterImage
        AnimatedVisibility(
            visibleState = posterViewerTransitionState,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(120)),
        ) {
            val image = lastPosterImage
            if (image != null) {
                ExtraFanartViewer(
                    image = image,
                    onDismiss = { posterViewerState = PosterViewerState.EXITING },
                    animatedVisibilityScope = this@AnimatedVisibility,
                    sharedContentKey = posterViewerKey(image),
                )
            }
        }
    }

    if (editVisible) {
        VideoEditDialog(
            video = video,
            onSaveMetadata = actions.onSaveMetadata,
            onMetadataSaved = actions.onMetadataSaved,
            onDismiss = { editVisible = false }
        )
    }
}

@Preview(widthDp = 1280, heightDp = 720, name = "Wide")
@Preview(widthDp = 600, heightDp = 800, name = "Narrow")
@Composable
private fun VideoDetailScreenPreview() {
    CompositionLocalProvider(LocalTranslations provides TranslationZh()) {
        JavScraperTheme {
            SharedTransitionLayout {
                // Preview 中也通过同一 CompositionLocal 提供 scope
                CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                    AnimatedContent(targetState = false, label = "video-detail-preview") { _ ->
                        VideoDetailScreen(
                            video = previewVideoWithAllFields(),
                            actions = VideoDetailActions(
                                onBack = {},
                                onRefresh = {},
                                onSaveMetadata = { _, _ -> VideoMetadataEditResult.NfoMissing }
                            ),
                            animatedVisibilityScope = this@AnimatedContent
                        )
                    }
                }
            }
        }
    }
}
