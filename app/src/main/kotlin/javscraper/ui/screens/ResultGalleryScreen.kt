package javscraper.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.ResizeMode
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import javscraper.i18n.LocalTranslations
import javscraper.i18n.TranslationZh
import javscraper.models.ScannedFile
import javscraper.models.Video
import javscraper.ui.components.media.PosterCard
import javscraper.ui.theme.JavScraperTheme
import java.io.File
import io.github.oikvpqya.compose.fastscroller.VerticalScrollbar
import io.github.oikvpqya.compose.fastscroller.material3.defaultMaterialScrollbarStyle
import io.github.oikvpqya.compose.fastscroller.rememberScrollbarAdapter

/** Bundled gallery state to reduce parameter count on [ResultGalleryScreen]. */
data class GalleryState(
    val scrapedFiles: List<ScannedFile>,
    val outputDir: String,
    val sessionResults: List<Video> = emptyList()
) {
    val videos: List<Video>
        get() {
            val byKey = LinkedHashMap<String, Video>()
            scrapedFiles.map(::toGalleryVideo).forEach { video ->
                byKey[video.number.ifBlank { video.path }] = video
            }
            sessionResults.forEach { video ->
                byKey[video.number.ifBlank { video.path }] = video
            }
            return byKey.values.toList()
        }

    fun videoByPath(path: String): Video? = videos.firstOrNull { it.path == path }

    fun fileByPath(path: String): ScannedFile? {
        scrapedFiles.firstOrNull { it.path == path }?.let { return it }
        val video = videos.firstOrNull { it.path == path } ?: return null
        return ScannedFile(
            path = video.path,
            fileName = File(video.path).name,
            number = video.number,
            isScraped = true,
            metadata = video
        )
    }
}

private fun toGalleryVideo(file: ScannedFile): Video {
    val metadata = file.metadata
    return metadata?.copy(
        number = metadata.number.ifBlank { file.number },
        title = metadata.title.ifBlank { file.fileName },
        path = file.path
    ) ?: Video(number = file.number, title = file.fileName, path = file.path)
}

data class GalleryActions(
    val onClear: () -> Unit,
    val onOpenOutputDir: () -> Unit,
    val onVideoClick: (Video) -> Unit
)

@Composable
fun ResultGalleryScreen(
    state: GalleryState,
    actions: GalleryActions,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    listState: LazyGridState,
    posterRefreshKey: Any? = null
) {
    val translations = LocalTranslations.current
    val videos = state.videos
    val onClear = actions.onClear
    val onOpenOutputDir = actions.onOpenOutputDir
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column {
                Text(
                    translations.galleryTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                if (videos.isNotEmpty()) Text(
                    translations.galleryCount(videos.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (videos.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenOutputDir) {
                    Icon(Icons.Default.Folder, null)
                    Spacer(Modifier.width(8.dp))
                    Text(translations.galleryOpenOutput)
                }
                Button(
                    onClick = onClear,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Icon(Icons.Default.Delete, null)
                    Spacer(Modifier.width(8.dp))
                    Text(translations.galleryClear)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (videos.isEmpty()) {
            EmptyGallery()
        } else {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyVerticalGrid(
                    state = listState,
                    columns = GridCells.Adaptive(190.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize().padding(end = 20.dp)
                ) {
                    items(videos, key = { galleryListItemKey(it) }) { video ->
                        PosterCard(
                            video = video,
                            onClick = { actions.onVideoClick(video) },
                            // 裁剪后文件内容变化但路径不变:refreshKey 参与 Coil 缓存 key,
                            // 返回图库也能重读新 poster 而不是命中旧缓存
                            posterRefreshKey = posterRefreshKey,
                            modifier = galleryPosterModifier(
                                video = video,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        )
                    }
                }
                if (galleryShowsFastScrollbar(videos.size)) {
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(scrollState = listState),
                        style = defaultMaterialScrollbarStyle(),
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        enablePressToScroll = true
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun galleryPosterModifier(
    video: Video,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?
): Modifier {
    if (sharedTransitionScope == null || animatedVisibilityScope == null) return Modifier

    return with(sharedTransitionScope) {
        Modifier.sharedBounds(
            rememberSharedContentState(key = galleryPosterKey(video)),
            animatedVisibilityScope = animatedVisibilityScope,
            enter = fadeIn(),
            exit = fadeOut(),
            resizeMode = ResizeMode.scaleToBounds()
        )
    }
}

internal fun galleryListItemKey(video: Video): String =
    video.number.ifBlank { video.path }

internal fun galleryShowsFastScrollbar(videoCount: Int): Boolean = videoCount > 1

internal fun galleryPosterKey(video: Video): String =
    "gallery-poster-${video.path.ifBlank { video.number }}"

@Composable
private fun EmptyGallery() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.PhotoLibrary,
                null,
                Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                LocalTranslations.current.galleryEmptyHint,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
private fun ResultGalleryScreenPreview() {
    CompositionLocalProvider(LocalTranslations provides TranslationZh()) {
        JavScraperTheme {
            ResultGalleryScreen(
                state = GalleryState(
                    scrapedFiles = listOf(
                        ScannedFile(
                            path = "F:/Videos/SONE-001.mp4",
                            fileName = "SONE-001.mp4",
                            number = "SONE-001",
                            isScraped = true,
                            metadata = Video(
                                number = "SONE-001",
                                title = "已刮削元数据标题"
                            )
                        )
                    ),
                    outputDir = "F:/Output"
                ),
                actions = GalleryActions(onClear = {}, onOpenOutputDir = {}, onVideoClick = {}),
                listState = rememberLazyGridState()
            )
        }
    }
}
