package javscraper.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import javscraper.i18n.LocalTranslations
import javscraper.i18n.TranslationZh
import javscraper.models.Video
import javscraper.ui.components.PosterCard
import javscraper.ui.components.PosterCropDialog
import javscraper.ui.components.VideoInfoCard
import javscraper.ui.components.cropSourceModel
import javscraper.ui.components.localPosterPath
import javscraper.ui.theme.JavScraperTheme
import java.io.File

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun VideoDetailScreen(
    video: Video,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
) {
    val translations = LocalTranslations.current
    var cropVisible by remember { mutableStateOf(false) }
    // 裁剪成功后自增,驱动 PosterCard 的 poster 缓存重读(mtime 变化)
    var posterVersion by remember { mutableLongStateOf(0L) }
    Surface(
        modifier = modifier.fillMaxSize().padding(16.dp),
        color = MaterialTheme.colorScheme.background
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = translations.galleryDetailTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = video.title.ifBlank { video.number },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                FilledIconButton(
                    onClick = onRefresh,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = translations.galleryDetailRefresh
                    )
                }
                FilledIconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = translations.galleryDetailBack
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                val posterModifier = galleryPosterModifier(
                    video = video,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
                PosterCard(
                    video = video,
                    onClick = { cropVisible = true },
                    modifier = posterModifier,
                    cardWidth = 320.dp,
                    posterRefreshKey = posterVersion
                )
                VideoInfoCard(
                    video = video,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (cropVisible) {
        val source = remember(video.path) { cropSourceModel(video) }
        if (source != null) {
            PosterCropDialog(
                videoNumber = video.number,
                sourceFile = source,
                posterFile = File(localPosterPath(video)),
                onCropped = { posterVersion++ },
                onDismiss = { cropVisible = false }
            )
        } else {
            // 无本地封面可裁剪:自动关闭,不打断用户
            LaunchedEffect(Unit) { cropVisible = false }
        }
    }
}

@Preview
@Composable
private fun VideoDetailScreenPreview() {
    CompositionLocalProvider(LocalTranslations provides TranslationZh()) {
        JavScraperTheme {
            SharedTransitionLayout {
                AnimatedContent(targetState = false, label = "video-detail-preview") { _ ->
                    VideoDetailScreen(
                        video = Video(
                            number = "SONE-001",
                            title = "已刮削元数据标题",
                            path = "F:/Videos/SONE-001.mp4"
                        ),
                        onBack = {},
                        onRefresh = {},
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedContent
                    )
                }
            }
        }
    }
}
