package javscraper.ui.components.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.ResizeMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.CarouselState
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.PlatformContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import javscraper.i18n.LocalTranslations
import javscraper.i18n.TranslationZh
import javscraper.models.Video
import javscraper.ui.LocalSharedTransitionScope
import javscraper.ui.PreviewImageProvider
import javscraper.ui.theme.JavScraperTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

/**
 * 视频目录下 extrafanart/ 图片轮播组件。
 *
 * 使用 Material3 HorizontalMultiBrowseCarousel:同时展示多张大小不一的图片,
 * 宽度不足时自动折衷为大/中/小混合排布,支持拖拽/滚轮翻页。
 * 目录规则与刮削落盘约定一致(ImageSaver 保存 sampleImages 到 extrafanart/fanartN.jpg):
 * - 图片列表为空时不渲染任何内容;
 * - 有图片时展示 Carousel,大项显示完整图片,小项裁切遮罩;
 * - 点击卡片通过 [onImageClick] 回调,由上层用 SharedTransition 弹出大图。
 *
 * 卡片端必须用本地 [AnimatedVisibility] 在选中时"退出":sharedBounds 的
 * 初始 bounds 来自 exiting 元素,目标 bounds 来自 incoming 元素。若卡片始终可见
 * (绑到外部不变的 scope),则没有 exiting bounds 可插值,动画退化为直接淡入。
 */
@Composable
fun ExtraFanartCarousel(
    images: List<File>,
    modifier: Modifier = Modifier,
    height: Dp = 240.dp,
    selectedIndex: Int? = null,
    onImageClick: (index: Int) -> Unit = {},
) {
    if (images.isEmpty()) return
    val translations = LocalTranslations.current
    // SharedTransitionLayout 的 scope 经 CompositionLocal 下发,免参数透传
    val sharedTransitionScope = LocalSharedTransitionScope.current
    // 桌面端已知问题:Carousel 内置手势在 JVM 目标上不生效(YouTrack 反馈),
    // CarouselState 实现 ScrollableState,手动转发滚轮/拖拽增量实现翻页
    val state = rememberCarouselState { images.size }
    val scope = rememberCoroutineScope()

    Column {
        Box(Modifier.padding(8.dp)) {
            Text(
                text = translations.videoFieldSampleImages,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        HorizontalMultiBrowseCarousel(
            state = state,
            modifier = modifier
                .fillMaxWidth()
                .carouselDesktopScrollInput(state, scope),
            preferredItemWidth = 320.dp,
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) { page ->
            // 选中项退出:提供 exiting bounds 供 SharedTransition 插值
            AnimatedVisibility(
                visible = selectedIndex != page,
                enter = fadeIn(tween(220)),
                exit = fadeOut(tween(220)),
            ) {
                Card(
                    onClick = { onImageClick(page) },
                    modifier = Modifier
                        .then(
                            carouselImageBoundsModifier(
                                image = images[page],
                                sharedTransitionScope = sharedTransitionScope,
                                // 用本项自己的 AnimatedVisibility scope:
                                // 只有它会随选中状态变化,才能与 viewer 端配对成过渡
                                animatedVisibilityScope = this@AnimatedVisibility,
                            )
                        )
                        .fillMaxSize()
                        .height(height),
                    // 背景透明:图片本身铺满卡片,避免 Card 默认底色在图片加载前/圆角处露出
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(PlatformContext.INSTANCE)
                            .data(images[page])
                            .build(),
                        contentDescription = "${translations.galleryDetailExtraFanart} ${page + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .maskClip(MaterialTheme.shapes.medium),
                    )
                }
            }
        }
    }
}

/** 桌面端 Carousel 内置手势在 JVM 目标上不稳定,统一转发滚轮与拖拽增量。 */
internal fun Modifier.carouselDesktopScrollInput(
    state: CarouselState,
    scope: CoroutineScope,
): Modifier = this
    .pointerInput(state) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Scroll) {
                    val delta = event.changes.first().scrollDelta.y
                    scope.launch { state.scrollBy(delta * 64f) }
                }
            }
        }
    }
    .pointerInput(state) {
        detectDragGestures { change, dragAmount ->
            change.consume()
            scope.launch { state.scrollBy(-dragAmount.x) }
        }
    }

/** SharedTransition bounds key:按文件路径唯一标识 extrafanart 图片。 */
internal fun carouselImageKey(image: File): String = "extrafanart-${image.absolutePath}"

/** 卡片与大图共用的 sharedBounds modifier;作用域缺失时退化为普通 Modifier。 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun carouselImageBoundsModifier(
    image: File,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    sharedContentKey: String = carouselImageKey(image),
): Modifier {
    if ((sharedTransitionScope == null) || (animatedVisibilityScope == null)) return Modifier
    return with(sharedTransitionScope) {
        Modifier.sharedBounds(
            rememberSharedContentState(key = sharedContentKey),
            animatedVisibilityScope = animatedVisibilityScope,
            enter = fadeIn(),
            exit = fadeOut(),
            // 与 PosterCard 转场一致:内容随 bounds 缩放,避免卡片 Crop 图
            // 与大图 Fit 内容比例差异导致的跳变
            resizeMode = ResizeMode.scaleToBounds(),
        )
    }
}

/** File.extension 不带点 */
private val extraFanartExtensions = listOf("jpg", "jpeg", "png", "webp")

/**
 * 读取视频同目录下 extrafanart/ 内的全部图片文件。
 *
 * 返回按文件名自然排序（fanart1、fanart2…fanart10 顺序正确）的列表；
 * 目录不存在或没有图片时返回空列表。
 */
fun listExtraFanartImages(video: Video): List<File> {
    val directory = File(video.path).parentFile ?: return emptyList()
    val extraDir = directory.resolve("extrafanart")
    if (!extraDir.isDirectory) return emptyList()
    return extraDir.listFiles()
        ?.asSequence()
        ?.filter { file ->
            file.isFile && (file.extension.lowercase() in extraFanartExtensions)
        }
        ?.sortedWith(compareBy({ it.name.length }, { it.name }))
        ?.toList()
        .orEmpty()
}

@Preview(widthDp = 900, heightDp = 400)
@Composable
private fun ExtraFanartCarouselPreview() {
    CompositionLocalProvider(LocalTranslations provides TranslationZh()) {
        JavScraperTheme {
            val images = remember { PreviewImageProvider.previewImages(3) }
            Card(modifier = Modifier.height(360.dp).width(800.dp)) {
                ExtraFanartCarousel(images = images)
            }
        }
    }
}
