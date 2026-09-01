package javscraper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import javscraper.models.Video
import javscraper.i18n.LocalTranslations
import javscraper.i18n.TranslationZh
import javscraper.ui.theme.JavScraperTheme

@Composable
fun PosterCard(video: Video, onClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.width(180.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            Box(
                Modifier.fillMaxWidth().height(250.dp).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (video.coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = video.coverUrl,
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
            Column(Modifier.padding(8.dp)) {
                Text(
                    video.number,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (video.title.isNotBlank()) Text(
                    video.title,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (video.actresses.isNotEmpty()) Text(
                    video.actresses.joinToString(", "),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.tertiary
                )
                if (video.maker.isNotBlank()) Text(
                    video.maker,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
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
                    Video(
                        number = "SONE-001",
                        title = "包含标题、演员与片商的完整卡片",
                        actresses = listOf("演员 A", "演员 B"),
                        maker = "片商",
                        coverUrl = "https://i0.hdslb.com/bfs/archive/44a5f313c93dfdf46ced70b83fcba987ac38b236.jpg@672w_378h_1c_!web-home-common-cover.avif"
                    )
                )
                PosterCard(
                    Video(
                        number = "ABP-123",
                        title = "这是一个用于预览超长标题换行与省略效果的测试文本"
                    )
                )
            }
        }
    }
}
