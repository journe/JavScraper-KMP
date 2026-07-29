package javscraper.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import javscraper.i18n.LocalTranslations
import javscraper.models.ScannedFile
import javscraper.models.SingleScrapeDialogState
import javscraper.models.SiteInfo
import javscraper.ui.components.SiteItem
import javscraper.ui.components.SiteSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileScanScreen(
    scannedFiles: List<ScannedFile>,
    scanDir: String,
    isScanning: Boolean,
    onSelectDirectory: () -> Unit,
    onStartScan: () -> Unit,
    onStartScrape: () -> Unit,
    onSingleScrapeClick: (ScannedFile) -> Unit,
    singleScrapeDialogState: SingleScrapeDialogState,
    singleScrapeNumber: String,
    singleScrapeTask: ScrapeTask?,
    sites: List<SiteInfo>,
    onSingleScrapeNumberChange: (String) -> Unit,
    onSingleScrapeSiteChange: (String?) -> Unit,
    onStartSingleScrape: () -> Unit,
    onCloseSingleScrape: () -> Unit,
    onConfirmScrapeResult: () -> Unit,
    modifier: Modifier = Modifier
) {
    val t = LocalTranslations.current
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(t.scanTitle, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = scanDir,
            onValueChange = {},
            label = { Text(t.scanDirectoryLabel) },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = onSelectDirectory) {
                    Icon(
                        Icons.Default.FolderOpen,
                        t.commonBrowse
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onStartScan, enabled = !isScanning && scanDir.isNotBlank()) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isScanning) t.scanButtonScanning else t.scanButtonScan)
            }
            Button(
                onClick = onStartScrape,
                enabled = scannedFiles.isNotEmpty() && !isScanning,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(t.scanButtonScrape)
            }
        }
        Spacer(Modifier.height(16.dp))
        if (scannedFiles.isNotEmpty()) {
            Text(
                t.scanFound(scannedFiles.count { it.number.isNotBlank() }),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                items(scannedFiles) { file ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (file.number.isNotBlank()) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer.copy(
                                alpha = 0.3f
                            )
                        )
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                file.fileName,
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (file.number.isNotBlank()) {
                                IconButton(
                                    onClick = { onSingleScrapeClick(file) },
                                    enabled = !isScanning
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = t.singleScrapeTitle,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if (file.number.isNotBlank()) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        file.number,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                Text(
                                    t.scanNoNumber,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        } else if (!isScanning) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    t.scanEmptyHint,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }

        // --- Single scrape dialog ---
        when (singleScrapeDialogState) {
            SingleScrapeDialogState.Closed -> { }
            SingleScrapeDialogState.Input -> {
                AlertDialog(
                    onDismissRequest = onCloseSingleScrape,
                    title = { Text(t.singleScrapeTitle) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = singleScrapeNumber,
                                onValueChange = onSingleScrapeNumberChange,
                                label = { Text(t.singleScrapeNumberLabel) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            SiteSelector(
                                sites = listOf(SiteItem("", t.singleScrapeSiteAuto, true)) +
                                    sites.map { SiteItem(it.id, it.name, true) },
                                onToggle = { id: String, _: Boolean -> onSingleScrapeSiteChange(id.ifEmpty { null }) }
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = onStartSingleScrape,
                            enabled = singleScrapeNumber.isNotBlank()
                        ) { Text(t.singleScrapeStart) }
                    },
                    dismissButton = {
                        TextButton(onClick = onCloseSingleScrape) { Text(t.progressCancel) }
                    }
                )
            }
            SingleScrapeDialogState.Scraping -> {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text(t.singleScrapeTitle) },
                    text = {
                        val task = singleScrapeTask
                        if (task != null) {
                            Card(Modifier.fillMaxWidth()) {
                                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(task.number, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(t.singleScrapeInProgress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = onCloseSingleScrape) { Text(t.progressCancel) }
                    }
                )
            }
            is SingleScrapeDialogState.Result -> {
                val resultState = singleScrapeDialogState
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text(t.singleScrapeResultTitle) },
                    text = {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                if (resultState.video != null) {
                                    val v = resultState.video
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.tertiary)
                                        Spacer(Modifier.width(8.dp))
                                        Text(v.number, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text("Title: " + v.title, style = MaterialTheme.typography.bodySmall)
                                    Text("Maker: " + v.maker, style = MaterialTheme.typography.bodySmall)
                                    Text("Actresses: " + v.actresses.joinToString(", "), style = MaterialTheme.typography.bodySmall)
                                    if (v.date.isNotBlank()) Text("Date: " + v.date, style = MaterialTheme.typography.bodySmall)
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                                        Spacer(Modifier.width(8.dp))
                                        Text(singleScrapeNumber, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        resultState.error ?: t.singleScrapeResultError,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = onConfirmScrapeResult) { Text(t.commonConfirm) }
                    },
                    dismissButton = {}
                )
            }
        }
    }
}
