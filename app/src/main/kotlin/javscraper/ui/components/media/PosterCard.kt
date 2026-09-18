package javscraper.ui.components.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.PlatformContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import javscraper.i18n.LocalTranslations
import javscraper.i18n.TranslationZh
import javscraper.models.Video
import javscraper.ui.previewVideoWithAllFields
import javscraper.ui.theme.JavScraperTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.imageio.ImageIO

/** PosterCard 展示的本地图片:竖版 poster(默认)或目录下的横版 fanart(详情页)。 */
enum class PosterSource { POSTER, FANART }

@Composable
fun PosterCard(
    video: Video,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    cardWidth: Dp = 190.dp,
    posterRefreshKey: Any? = null,
    source: PosterSource = PosterSource.POSTER
) {
    val imageFile = remember(video.path, posterRefreshKey, source) {
        when (source) {
            PosterSource.POSTER -> localPosterModel(video)
            PosterSource.FANART -> localFanartModel(video)
        }
    }
    // FANART 模式:高度固定为 posterHeight(与 poster 一致),
    // 宽度按图片真实高宽比自适应,使 Box 与图片同比例,
    // ContentScale.Crop 不裁切内容;读取完成前用缺省横版比例占位
    val fanartAspect by produceState(DEFAULT_FANART_ASPECT, imageFile) {
        if (source == PosterSource.FANART && imageFile != null) {
            val dimension = withContext(Dispatchers.IO) { readImageDimensions(imageFile) }
            if (dimension != null) value = dimension.second.toFloat() / dimension.first
        }
    }
    val imageHeight =
        when (source) {
            PosterSource.POSTER -> 270.dp
            PosterSource.FANART -> 452.dp
        }
    val imageWidth = when (source) {
        PosterSource.POSTER -> cardWidth
        PosterSource.FANART -> imageHeight / fanartAspect
    }
    Card(
        onClick = onClick,
        modifier = modifier.width(imageWidth),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            Box(
                Modifier.fillMaxWidth().height(imageHeight)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (imageFile != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(PlatformContext.INSTANCE)
                            .data(imageFile)
                            // 裁剪后文件内容变化但路径不变:用 refreshKey 参与缓存 key,
                            // 强制 Coil 重新解码,否则命中旧缓存不刷新
                            .memoryCacheKey("${imageFile.absolutePath}#$posterRefreshKey")
                            .diskCacheKey("${imageFile.absolutePath}#$posterRefreshKey")
                            .build(),
                        contentDescription = video.title.ifBlank { video.number },
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        video.number,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            when (source) {
                PosterSource.POSTER -> Column(Modifier.padding(8.dp)) {
                    Text(
                        video.number,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        video.title,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        video.actresses.joinToString(", "),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        video.maker,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                PosterSource.FANART -> {}
            }


        }
    }
}

@Preview
@Composable
private fun PosterCardPreview() {
    CompositionLocalProvider(LocalTranslations provides TranslationZh()) {
        JavScraperTheme {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PosterCard(
                    previewVideoWithAllFields()
                )
                PosterCard(
                    Video(
                        number = "ABP-123",
                        title = "这是一个用于预览超长标题换行与省略效果的测试文本这是一个用于预览超长标题换行与省略"
                    )
                )
            }
        }
    }
}


private val posterFileNames = listOf("poster.jpg", "poster.png")

internal fun localPosterPath(video: Video): String {
    val directory = File(video.path).parentFile ?: return ""
    val candidates = posterFileNames.map(directory::resolve)
    return (candidates.firstOrNull(File::isFile) ?: candidates.first()).path
}

internal fun localPosterModel(video: Video): File? =
    localPosterPath(video).takeIf { it.isNotBlank() }
        ?.let(::File)
        ?.takeIf(File::isFile)

private val fanartFileNames = listOf("fanart.jpg", "fanart.png")

internal fun localFanartPath(video: Video): String {
    val directory = File(video.path).parentFile ?: return ""
    val candidates = fanartFileNames.map(directory::resolve)
    return candidates.firstOrNull(File::isFile)?.path ?: ""
}

/** 详情页横版封面来源:取视频同目录的 fanart,缺省回退 poster(与裁剪源优先级一致)。 */
internal fun localFanartModel(video: Video): File? =
    localFanartPath(video).takeIf { it.isNotBlank() }
        ?.let(::File)
        ?.takeIf(File::isFile)
    ?: localPosterModel(video)

/** 详情页封面放大层的 SharedTransition key,与图库转场和 extrafanart key 隔离。 */
internal fun posterViewerKey(image: File): String =
    "detail-poster-${image.absolutePath}"

/** fanart 缺省高宽比(高/宽):读取真实尺寸前的占位,取横版封面典型比例(如 800x538)。 */
internal const val DEFAULT_FANART_ASPECT = 2f / 3f

/** 仅解析图片头部读取宽高,不做整图解码;非图片或读取失败返回 null。 */
internal fun readImageDimensions(file: File): Pair<Int, Int>? {
    val input = try {
        ImageIO.createImageInputStream(file)
    } catch (_: Exception) {
        null
    } ?: return null
    try {
        val readers = ImageIO.getImageReaders(input)
        if (!readers.hasNext()) return null
        val reader = readers.next()
        reader.input = input
        return try {
            val width = reader.getWidth(0)
            val height = reader.getHeight(0)
            if (width > 0 && height > 0) width to height else null
        } finally {
            reader.dispose()
        }
    } catch (_: Exception) {
        return null
    } finally {
        runCatching { input.close() }
    }
}

private val cropSourceFileNames = listOf("fanart.jpg", "fanart.png", "poster.jpg", "poster.png")

/** 裁剪输入源:优先横版封面 fanart,缺省回退 poster(与刮削落盘约定一致)。 */
internal fun cropSourceModel(video: Video): File? {
    val directory = File(video.path).parentFile ?: return null
    return cropSourceFileNames
        .map(directory::resolve)
        .firstOrNull(File::isFile)
}
