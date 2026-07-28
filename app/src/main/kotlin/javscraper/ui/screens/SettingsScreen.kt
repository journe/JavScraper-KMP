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
import javscraper.i18n.Translations
import javscraper.models.SiteInfo
import javscraper.ui.components.SiteItem
import javscraper.ui.components.SiteSelector

/** Bundled settings state to reduce parameter count on [SettingsScreen]. */
data class SettingsState(
    val workerPath: String,
    val outputDir: String,
    val scanDir: String,
    val scanRecursive: Boolean,
    val createMovieFolders: Boolean,
    val hardlinkInsteadOfCopy: Boolean,
    val downloadImages: Boolean,
    val autoScrape: Boolean,
    val sites: List<SiteInfo>,
    val enabledSiteIds: List<String>,
    val language: String,
    val showRestartHint: Boolean
)

data class SettingsActions(
    val onLanguageChange: (String) -> Unit,
    val onSelectOutputDir: () -> Unit,
    val onSelectScanDir: () -> Unit,
    val onSelectWorkerPath: () -> Unit,
    val onWorkerPathChange: (String) -> Unit,
    val onScanRecursiveChange: (Boolean) -> Unit,
    val onCreateMovieFoldersChange: (Boolean) -> Unit,
    val onHardlinkChange: (Boolean) -> Unit,
    val onDownloadImagesChange: (Boolean) -> Unit,
    val onAutoScrapeChange: (Boolean) -> Unit,
    val onToggleSite: (String, Boolean) -> Unit,
    val onReset: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    actions: SettingsActions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
    ) {
        Text(
            Translations.settingsTitle,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(20.dp))
        Text(
            Translations.settingsWorkerPath,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            value = state.workerPath,
            onValueChange = actions.onWorkerPathChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = actions.onSelectWorkerPath) {
                    Icon(Icons.Default.FileOpen, Translations.commonBrowse)
                }
            }
        )
        Spacer(Modifier.height(16.dp))
        Text(
            Translations.settingsScanDir,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            value = state.scanDir,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = actions.onSelectScanDir) {
                    Icon(Icons.Default.FolderOpen, Translations.commonBrowse)
                }
            }
        )
        Spacer(Modifier.height(8.dp))
        SettingsSwitchRow(
            label = Translations.settingsScanRecursive,
            checked = state.scanRecursive,
            onCheckedChange = actions.onScanRecursiveChange
        )
        Spacer(Modifier.height(16.dp))
        Text(
            Translations.settingsOutputDir,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            value = state.outputDir,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = actions.onSelectOutputDir) {
                    Icon(Icons.Default.FolderOpen, Translations.commonBrowse)
                }
            }
        )
        Spacer(Modifier.height(12.dp))
        SettingsSwitchRow(
            label = Translations.settingsMovieFolders,
            checked = state.createMovieFolders,
            onCheckedChange = actions.onCreateMovieFoldersChange
        )
        SettingsSwitchRow(
            label = Translations.settingsHardlinks,
            checked = state.hardlinkInsteadOfCopy,
            onCheckedChange = actions.onHardlinkChange
        )
        SettingsSwitchRow(
            label = Translations.settingsDownloadImages,
            checked = state.downloadImages,
            onCheckedChange = actions.onDownloadImagesChange
        )
        Spacer(Modifier.height(20.dp))
        Text(
            Translations.settingsScraperSites,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        SiteSelector(
            sites = state.sites.map {
                SiteItem(it.id, it.name, it.id in state.enabledSiteIds)
            },
            onToggle = actions.onToggleSite
        )
        Spacer(Modifier.height(20.dp))
        Text(
            Translations.settingsLanguage,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        LanguageSelector(
            language = state.language,
            showRestartHint = state.showRestartHint,
            onLanguageChange = actions.onLanguageChange
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(
            onClick = actions.onReset,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.SettingsBackupRestore, null)
            Spacer(Modifier.width(8.dp))
            Text(Translations.settingsReset)
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        Arrangement.SpaceBetween,
        Alignment.CenterVertically
    ) {
        Text(label)
        Switch(checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LanguageSelector(
    language: String,
    showRestartHint: Boolean,
    onLanguageChange: (String) -> Unit
) {
    Column {
        Row(
            Modifier.fillMaxWidth(),
            Arrangement.SpaceBetween,
            Alignment.CenterVertically
        ) {
            Text(Translations.settingsLanguageEn)
            RadioButton(
                selected = language == "en",
                onClick = { onLanguageChange("en") }
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            Arrangement.SpaceBetween,
            Alignment.CenterVertically
        ) {
            Text(Translations.settingsLanguageZh)
            RadioButton(
                selected = language == "zh",
                onClick = { onLanguageChange("zh") }
            )
        }
        if (showRestartHint) {
            Spacer(Modifier.height(4.dp))
            Text(
                Translations.settingsLanguageRestartHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}
