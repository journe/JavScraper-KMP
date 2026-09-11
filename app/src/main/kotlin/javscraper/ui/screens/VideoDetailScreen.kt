package javscraper.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
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
import javscraper.ui.components.PosterSource
import javscraper.ui.components.VideoInfoCard
import javscraper.ui.components.cropSourceModel
import javscraper.ui.components.localPosterPath
import javscraper.ui.previewVideoWithAllFields
import javscraper.ui.theme.JavScraperTheme
import java.io.File

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalLayoutApi::class)
@Composable
fun VideoDetailScreen(
    video: Video,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    // 由 App 层持有并注入:与图库页共享同一裁剪版本号,两侧 PosterCard 同步刷新
    posterRefreshKey: Any? = null,
    onPosterCropped: () -> Unit = {}
) {
    val translations = LocalTranslations.current
    var cropVisible by remember { mutableStateOf(false) }
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
                        text = video.number,
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
                    sharedTransitionScope = sharedTransitionScope,
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
                    modifier = Modifier.weight(1f, fill = false).widthIn(min = 360.dp)
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
                AnimatedContent(targetState = false, label = "video-detail-preview") { _ ->
                    VideoDetailScreen(
                        video = previewVideoWithAllFields(),
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
