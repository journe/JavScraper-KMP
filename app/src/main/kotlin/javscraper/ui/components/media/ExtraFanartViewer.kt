package javscraper.ui.components.media

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.unit.dp
import coil3.PlatformContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import javscraper.ui.LocalSharedTransitionScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * extrafanart 大图查看器:半透明全屏遮罩 + 居中大图。
 *
 * 与 Carousel 卡片通过 SharedTransition(相同 key)联动:
 * 打开时从卡片 bounds 放大到居中大图,关闭时缩回原卡片位置。
 * 点击遮罩任意处或按 Esc 关闭。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ExtraFanartViewer(
    image: File,
    onDismiss: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    sharedContentKey: String = carouselImageKey(image),
) {
    // SharedTransitionLayout 的 scope 经 CompositionLocal 下发,免参数透传
    val sharedTransitionScope = LocalSharedTransitionScope.current
    // Compose Desktop 按键事件沿焦点链分发:必须可聚焦并主动抢焦点,
    // onPreviewKeyEvent 才能收到 Esc
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .focusRequester(focusRequester)
            .focusable()
            // Esc 关闭;点击遮罩关闭
            .onPreviewKeyEvent { event ->
                if ((event.type == KeyEventType.KeyDown) && (event.key == Key.Escape)) {
                    onDismiss()
                    true
                } else {
                    false
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { onDismiss() }
            },
        contentAlignment = Alignment.Center,
    ) {
        // bounds 动画与 PosterCard 一致:scaleToBounds 让图片内容
        // 随 bounds 一起缩放,避免 Crop 小图→Fit 大图的内容跳变。
        // scope 经 CompositionLocal 下发,Preview 等未提供时退化为普通 Modifier
        val boundsModifier = if (sharedTransitionScope != null) {
            with(sharedTransitionScope) {
                Modifier.sharedBounds(
                    rememberSharedContentState(key = sharedContentKey),
                    animatedVisibilityScope = animatedVisibilityScope,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
                )
            }
        } else {
            Modifier
        }
        // 先读图片真实宽高比再定 Card 尺寸:加载前用横版典型比例 2/3 占位,
        // 读到后精确约束,Card 与图片同比例,图片铺满无留白
        var imageAspect by remember(image) { mutableFloatStateOf(2f / 3f) }
        LaunchedEffect(image) {
            withContext(Dispatchers.IO) { readImageDimensions(image) }?.let { (w, h) ->
                imageAspect = w.toFloat() / h.toFloat()
            }
        }
        // 用 BoxWithConstraints 拿到遮罩实际可用宽高,计算图片比例下的
        // 最大内接矩形(Fit 逻辑作用于 Card 尺寸),避免固定 max 值
        BoxWithConstraints(contentAlignment = Alignment.Center) {
            val maxW = maxWidth - 64.dp   // 两侧各 32dp 边距
            val maxH = maxHeight - 64.dp
            Card(
                modifier = boundsModifier
                    .widthIn(max = maxW)
                    .heightIn(max = maxH)
                    .aspectRatio(imageAspect),
                elevation = CardDefaults.cardElevation(4.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(PlatformContext.INSTANCE)
                        .data(image)
                        .build(),
                    contentDescription = image.nameWithoutExtension,
                    // Card 已与图片同比例,Crop 即铺满且不裁内容
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
