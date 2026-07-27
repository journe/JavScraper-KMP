package javscraper.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import javscraper.models.SiteInfo
import javscraper.ui.components.SiteItem
import javscraper.ui.components.SiteSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    workerPath: String, outputDir: String, scanDir: String, scanRecursive: Boolean, createMovieFolders: Boolean,
    hardlinkInsteadOfCopy: Boolean, downloadImages: Boolean, autoScrape: Boolean, sites: List<SiteInfo>,
    enabledSiteIds: List<String>, onWorkerPathChange: (String) -> Unit, onOutputDirChange: (String) -> Unit,
    onSelectOutputDir: () -> Unit, onSelectScanDir: () -> Unit, onSelectWorkerPath: () -> Unit,
    onScanRecursiveChange: (Boolean) -> Unit, onCreateMovieFoldersChange: (Boolean) -> Unit,
    onHardlinkChange: (Boolean) -> Unit, onDownloadImagesChange: (Boolean) -> Unit, onAutoScrapeChange: (Boolean) -> Unit,
    onToggleSite: (String, Boolean) -> Unit, onReset: () -> Unit, modifier: Modifier = Modifier
) {
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Spacer(Modifier.height(20.dp))
        Text("Worker Path", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = workerPath, onValueChange = onWorkerPathChange, modifier = Modifier.fillMaxWidth(), singleLine = true, trailingIcon = { IconButton(onClick = onSelectWorkerPath) { Icon(Icons.Default.FileOpen, "Browse") } })
        Spacer(Modifier.height(16.dp))
        Text("Scan Directory", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = scanDir, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth(), singleLine = true, trailingIcon = { IconButton(onClick = onSelectScanDir) { Icon(Icons.Default.FolderOpen, "Browse") } })
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text("Scan subdirectories"); Switch(scanRecursive, onCheckedChange = onScanRecursiveChange) }
        Spacer(Modifier.height(16.dp))
        Text("Output Directory", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = outputDir, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth(), singleLine = true, trailingIcon = { IconButton(onClick = onSelectOutputDir) { Icon(Icons.Default.FolderOpen, "Browse") } })
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text("Movie folders"); Switch(createMovieFolders, onCheckedChange = onCreateMovieFoldersChange) }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text("Hardlinks"); Switch(hardlinkInsteadOfCopy, onCheckedChange = onHardlinkChange) }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text("Download images"); Switch(downloadImages, onCheckedChange = onDownloadImagesChange) }
        Spacer(Modifier.height(20.dp))
        Text("Scraper Sites", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp))
        SiteSelector(sites.map { SiteItem(it.id, it.name, it.id in enabledSiteIds) }, onToggle = onToggleSite)
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Icon(Icons.Default.SettingsBackupRestore, null); Spacer(Modifier.width(8.dp)); Text("Reset to Defaults") }
        Spacer(Modifier.height(32.dp))
    }
}