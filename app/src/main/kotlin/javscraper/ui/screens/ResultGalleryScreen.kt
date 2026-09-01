package javscraper.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    val results: List<Video>,
    val scrapedFiles: List<ScannedFile>,
    val outputDir: String
)

data class GalleryActions(
    val onClear: () -> Unit,
    val onOpenOutputDir: () -> Unit
)

@Composable
fun ResultGalleryScreen(
    state: GalleryState,
    actions: GalleryActions,
    modifier: Modifier = Modifier
) {
    val t = LocalTranslations.current
    val results = state.results
    val scrapedFiles = state.scrapedFiles
    val hasContent = results.isNotEmpty() || scrapedFiles.isNotEmpty()
    val onClear = actions.onClear
    val onOpenOutputDir = actions.onOpenOutputDir
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column {
                Text(t.galleryTitle, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                if (results.isNotEmpty()) Text(
                    t.galleryCount(results.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (scrapedFiles.isNotEmpty()) Text(
                    t.galleryScrapedCount(scrapedFiles.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (results.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenOutputDir) {
                    Icon(Icons.Default.Folder, null); Spacer(Modifier.width(8.dp)); Text(t.galleryOpenOutput)
                }
                Button(
                    onClick = onClear,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, null); Spacer(Modifier.width(8.dp)); Text(t.galleryClear)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (!hasContent) {
            EmptyGallery()
        } else {
            if (results.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(190.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(results) { PosterCard(it) }
                }
            }
            if (scrapedFiles.isNotEmpty()) {
                if (results.isNotEmpty()) Spacer(Modifier.height(16.dp))
                Text(
                    t.galleryScrapedFiles,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(if (results.isEmpty()) 1f else 0.3f)
                ) {
                    lazyItems(scrapedFiles) { file -> ScrapedFileCard(file) }
                }
            }
        }
    }
}

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

@Composable
private fun ScrapedFileCard(file: ScannedFile) {
    val metadata = file.metadata
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    metadata?.title?.takeIf { it.isNotBlank() } ?: file.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    file.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (metadata != null) {
                    val details = listOfNotNull(
                        metadata.actresses.takeIf { it.isNotEmpty() }?.joinToString(", "),
                        metadata.maker.takeIf { it.isNotBlank() },
                        metadata.date.takeIf { it.isNotBlank() },
                        metadata.duration?.takeIf { it > 0 }?.let { "$it min" }
                    ).joinToString(" · ")
                    if (details.isNotBlank()) Text(
                        details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            val number = metadata?.number?.takeIf { it.isNotBlank() } ?: file.number
            if (number.isNotBlank()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        number,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun ScrapedFileCardPreview() {
    CompositionLocalProvider(LocalTranslations provides TranslationZh()) {
        JavScraperTheme {
            Column(
                modifier = Modifier.padding(16.dp).width(420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScrapedFileCard(
                    ScannedFile(
                        path = "F:/Videos/SONE-001.mp4",
                        fileName = "SONE-001.mp4",
                        number = "SONE-001",
                        isScraped = true
                    )
                )
                ScrapedFileCard(
                    ScannedFile(
                        path = "F:/Movies/这是一个用于预览的超长视频文件名称并且没有识别出番号.mp4",
                        fileName = "这是一个用于预览的超长视频文件名称并且没有识别出番号.mp4"
                    )
                )
            }
        }
    }
}
