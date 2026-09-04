package javscraper.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.ResizeMode
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

import androidx.compose.foundation.layout.*
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
import javscraper.ui.components.PosterCard
import javscraper.ui.theme.JavScraperTheme

/** Bundled gallery state to reduce parameter count on [ResultGalleryScreen]. */
data class GalleryState(
    val scrapedFiles: List<ScannedFile>,
    val outputDir: String
) {
    val videos: List<Video>
        get() = scrapedFiles.map(::toGalleryVideo)

    fun videoByPath(path: String): Video? = videos.firstOrNull { it.path == path }
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
    animatedVisibilityScope: AnimatedVisibilityScope? = null
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
            LazyVerticalGrid(
                columns = GridCells.Adaptive(190.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(videos) { video ->
                    PosterCard(
                        video = video,
                        onClick = { actions.onVideoClick(video) },
                        modifier = galleryPosterModifier(
                            video = video,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        )
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
                actions = GalleryActions(onClear = {}, onOpenOutputDir = {}, onVideoClick = {})
            )
        }
    }
}
