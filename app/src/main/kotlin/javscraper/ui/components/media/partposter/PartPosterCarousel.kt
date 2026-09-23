package javscraper.ui.components.media.partposter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.PlatformContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import javscraper.i18n.LocalTranslations
import javscraper.io.FileNameInfo
import javscraper.io.FileScanner
import javscraper.io.MediaArtPaths
import javscraper.models.Video
import javscraper.ui.PreviewImageProvider
import javscraper.ui.components.media.carouselDesktopScrollInput
import javscraper.ui.theme.JavScraperTheme
import java.io.File

private val posterExtensions = listOf("jpg", "jpeg", "png")

/**
 * 收集同一番号多个分段视频的 poster。
 *
 * 第一段通常没有专属图片，但通用 `poster.jpg` 就是它的海报；第二段起使用
 * `<分段视频基准名>-poster.jpg`。专属文件优先匹配，避免所有分段都回退到通用图。
 */
fun listPartPosters(video: Video): List<File> {
    val currentFile = File(video.path)
    val directory = currentFile.parentFile ?: return emptyList()
    if (!directory.isDirectory) return emptyList()

    val parts = directory.listFiles(File::isFile)
        ?.filter { file -> FileScanner.isVideo(file.toPath()) }
        ?.map { file -> PartVideo(file, FileScanner.parseFileName(file.name)) }
        ?.filter { part -> part.info.number.equals(video.number, ignoreCase = true) }
        .orEmpty()
    if (parts.size < 2) return emptyList()

    return parts
        .sortedWith(
            compareBy<PartVideo> {
                FileScanner.multiPartLabelNumber(it.info.versionLabel) ?: Int.MAX_VALUE
            }
                .thenBy { it.file.name }
        )
        .mapNotNull { part -> findPartPoster(directory, part.file) }
}

private data class PartVideo(
    val file: File,
    val info: FileNameInfo
)

private fun findPartPoster(directory: File, videoFile: File): File? {
    val baseName = MediaArtPaths.videoBaseName(videoFile.name)
    val specificPoster = posterExtensions
        .map { extension -> directory.resolve("$baseName-poster.$extension") }
        .firstOrNull(File::isFile)
    return specificPoster ?: MediaArtPaths.findPoster(directory.toPath(), baseName)?.toFile()
}

/**
 * 单影片多分段的海报轮播。
 *
 * [listPartPosters] 只在多分段时返回内容；空列表和单文件在此直接不渲染。
 * 桌面端 Carousel 内置手势在 JVM 目标上不稳定，因此与额外图集一样手动
 * 转发滚轮和拖拽增量。
 */
@Composable
fun PartPosterCarousel(
    posters: List<File>,
    modifier: Modifier = Modifier,
    width: Dp = 190.dp,
    height: Dp = 270.dp,
    posterRefreshKey: Any? = null,
) {
    if (posters.isEmpty()) return
    val state = rememberCarouselState { posters.size }
    val scope = rememberCoroutineScope()
    val translations = LocalTranslations.current

    Column {
        Box(Modifier.padding(8.dp)) {
            Text(
                text = translations.commonPartVideos,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        HorizontalMultiBrowseCarousel(
            state = state,
            modifier = modifier
                .width(width)
                .carouselDesktopScrollInput(state, scope),
            preferredItemWidth = 190.dp,
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) { page ->
            val poster = posters[page]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .maskClip(MaterialTheme.shapes.medium)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(PlatformContext.INSTANCE)
                        .data(poster)
                        .memoryCacheKey("${poster.absolutePath}#$posterRefreshKey")
                        .diskCacheKey("${poster.absolutePath}#$posterRefreshKey")
                        .build(),
                    contentDescription = poster.nameWithoutExtension,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}


@Preview(widthDp = 900, heightDp = 360, name = "Part Posters")
@Composable
private fun PartPosterCarouselPreview() {
    JavScraperTheme {
        val posters = remember { PreviewImageProvider.previewImages(4) }
        Card(modifier = Modifier.height(360.dp).width(600.dp)) {
            PartPosterCarousel(
                posters = posters,
                width = 600.dp,
                height = 100.dp,
            )
        }
    }
}
