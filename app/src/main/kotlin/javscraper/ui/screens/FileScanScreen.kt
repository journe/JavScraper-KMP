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
import javscraper.models.ScannedFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileScanScreen(scannedFiles: List<ScannedFile>, scanDir: String, isScanning: Boolean, onSelectDirectory: () -> Unit, onStartScan: () -> Unit, onStartScrape: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("File Scanner", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = scanDir, onValueChange = {}, label = { Text("Scan Directory") }, readOnly = true, trailingIcon = { IconButton(onClick = onSelectDirectory) { Icon(Icons.Default.FolderOpen, "Browse") } }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onStartScan, enabled = !isScanning && scanDir.isNotBlank()) {
                Icon(Icons.Default.Search, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(if (isScanning) "Scanning..." else "Scan for Videos")
            }
            Button(onClick = onStartScrape, enabled = scannedFiles.isNotEmpty() && !isScanning, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                Icon(Icons.Default.CloudDownload, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Start Scraping")
            }
        }
        Spacer(Modifier.height(16.dp))
        if (scannedFiles.isNotEmpty()) {
            Text("Found ${scannedFiles.count { it.number.isNotBlank() }} videos with JAV numbers", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                items(scannedFiles) { file ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (file.number.isNotBlank()) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(file.fileName, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (file.number.isNotBlank()) Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) { Text(file.number, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) }
                            else Text("No number", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        } else if (!isScanning) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Select a directory and scan", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        else { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
    }
}