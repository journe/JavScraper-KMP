package javscraper.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import javscraper.i18n.LocalTranslations
import javscraper.i18n.TranslationZh
import javscraper.models.Video
import javscraper.ui.LocalSharedTransitionScope
import javscraper.ui.components.ExtraFanartCarousel
import javscraper.ui.components.ExtraFanartViewer
import javscraper.ui.components.PosterCard
import javscraper.ui.components.PosterCropDialog
import javscraper.ui.components.PosterSource
import javscraper.ui.components.VideoInfoCard
import javscraper.ui.components.cropSourceModel
import javscraper.ui.components.listExtraFanartImages
import javscraper.ui.components.localPosterPath
import javscraper.ui.previewVideoWithAllFields
import javscraper.ui.theme.JavScraperTheme
import javscraper.ui.screens.detail.VideoDetailActions
import javscraper.ui.screens.detail.VideoDetailHeader
import javscraper.ui.screens.detail.VideoEditDialog
import javscraper.io.metadata.VideoMetadataEditResult
import java.io.File

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalLayoutApi::class)
@Composable
fun VideoDetailScreen(
    video: Video,
    actions: VideoDetailActions,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    // 由 App 层持有并注入:与图库页共享同一裁剪版本号,两侧 PosterCard 同步刷新
    posterRefreshKey: Any? = null,
    onPosterCropped: () -> Unit = {}
) {
    val translations = LocalTranslations.current
    var cropVisible by remember { mutableStateOf(false) }
    var editVisible by remember { mutableStateOf(false) }
    // extrafanart 目录下的预览图:IO 读取后驱动 Carousel
    var extraFanartImages by remember(video.path) { mutableStateOf(listExtraFanartImages(video)) }
    // 当前展开的大图索引;null 表示未打开。点击 Carousel 卡片时设置,
    // 大图与卡片通过 SharedTransition(文件路径 key)联动缩放
    var viewerImageIndex by remember { mutableStateOf<Int?>(null) }
    // Esc 退出:大图打开时先关大图,否则返回图库。焦点链方案与
    // ExtraFanartViewer 一致:可聚焦+主动抢焦点才能收到按键
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    // 大图关闭后 Compose 不会自动归还焦点(Viewer 移除时焦点被清空),
    // 监听到关闭即重新抢回,保证后续 Esc 仍能返回图库
    LaunchedEffect(viewerImageIndex) {
        if (viewerImageIndex == null) focusRequester.requestFocus()
    }
    Surface(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if ((event.type == KeyEventType.KeyDown) && (event.key == Key.Escape)) {
                    when {
                        viewerImageIndex != null -> viewerImageIndex = null
                        else -> actions.onBack()
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
            // Flow 自适应布局:宽度足够时封面与信息卡同一行,不够时自动换行,
            // 无需手动阈值切换,任意窗口宽度下 VideoInfoCard 都可见
            FlowRow(
                modifier = Modifier.fillMaxWidth().weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val posterModifier = galleryPosterModifier(
                    video = video,
                    sharedTransitionScope = LocalSharedTransitionScope.current,
                    animatedVisibilityScope = animatedVisibilityScope
                )
                PosterCard(
                    video = video,
                    onClick = { cropVisible = true },
                    modifier = posterModifier,
                    cardWidth = 320.dp,
                    posterRefreshKey = posterRefreshKey,
                    // 详情页展示目录下的横版封面 fanart,卡片宽高比随图片自适应
                    source = PosterSource.FANART
                )
                VideoInfoCard(
                    video = video,
                    // FlowRow 中 weight 表示占满该行剩余宽度(实验 API)
                    modifier = Modifier.weight(1f, fill = false).widthIn(min = 160.dp,max = 360.dp)
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
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(220)),
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
    }

    if (editVisible) {
        VideoEditDialog(
            video = video,
            onSaveMetadata = actions.onSaveMetadata,
            onDismiss = { editVisible = false }
        )
    }

    if (cropVisible) {
        val source = remember(video.path) { cropSourceModel(video) }
        if (source != null) {
            PosterCropDialog(
                videoNumber = video.number,
                sourceFile = source,
                posterFile = File(localPosterPath(video)),
                onCropped = onPosterCropped,
                onDismiss = { cropVisible = false }
            )
        } else {
            // 无本地封面可裁剪:自动关闭,不打断用户
            LaunchedEffect(Unit) { cropVisible = false }
        }
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
                                onSaveMetadata = { VideoMetadataEditResult.NfoMissing }
                            ),
                            animatedVisibilityScope = this@AnimatedContent
                        )
                    }
                }
            }
        }
    }
}
