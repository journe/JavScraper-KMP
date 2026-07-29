package javscraper.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import javscraper.i18n.LocalTranslations
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
    val showRestartHint: Boolean,
    val folderLayers: List<String>,
    val filenameFormat: String,
    val maxTitleLength: Int,
    val maxFilenameLength: Int,
    val suffixKeywords: List<String>
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
    val onReset: () -> Unit,
    val onFolderLayerChange: (Int, String) -> Unit,
    val onAddLayer: () -> Unit,
    val onRemoveLayer: (Int) -> Unit,
    val onFilenameFormatChange: (String) -> Unit,
    val onMaxTitleLengthChange: (Int) -> Unit,
    val onMaxFilenameLengthChange: (Int) -> Unit,
    val onSuffixKeywordsChange: (List<String>) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    actions: SettingsActions,
    modifier: Modifier = Modifier
) {
    val t = LocalTranslations.current
    Column(modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(t.settingsTitle, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        Text(t.settingsWorkerPath, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = state.workerPath,
            onValueChange = actions.onWorkerPathChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = actions.onSelectWorkerPath) {
                    Icon(Icons.Default.FileOpen, t.commonBrowse)
                }
            }
        )
        Spacer(Modifier.height(16.dp))
        Text(t.settingsScanDir, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = state.scanDir, onValueChange = {}, readOnly = true,
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            trailingIcon = {
                IconButton(onClick = actions.onSelectScanDir) {
                    Icon(Icons.Default.FolderOpen, t.commonBrowse)
                }
            }
        )
        Spacer(Modifier.height(8.dp))
        SettingsSwitchRow(t.settingsScanRecursive, state.scanRecursive, actions.onScanRecursiveChange)
        Spacer(Modifier.height(16.dp))
        Text(t.settingsOutputDir, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = state.outputDir, onValueChange = {}, readOnly = true,
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            trailingIcon = {
                IconButton(onClick = actions.onSelectOutputDir) {
                    Icon(Icons.Default.FolderOpen, t.commonBrowse)
                }
            }
        )
        Spacer(Modifier.height(12.dp))
        SettingsSwitchRow(t.settingsMovieFolders, state.createMovieFolders, actions.onCreateMovieFoldersChange)
        SettingsSwitchRow(t.settingsHardlinks, state.hardlinkInsteadOfCopy, actions.onHardlinkChange)
        SettingsSwitchRow(t.settingsDownloadImages, state.downloadImages, actions.onDownloadImagesChange)

        // --- Naming Rules ---
        Spacer(Modifier.height(20.dp))
        Text(t.settingsRenameTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        Text(t.settingsFolderLayers, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        Column {
            state.folderLayers.forEachIndexed { index, layer ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = layer,
                        onValueChange = { actions.onFolderLayerChange(index, it) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text(t.settingsFolderLayerPlaceholder) }
                    )
                    VariableInsertButton { v -> actions.onFolderLayerChange(index, layer + v) }
                    IconButton(onClick = { actions.onRemoveLayer(index) }) {
                        Icon(Icons.Default.RemoveCircleOutline, t.settingsRemoveLayer)
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            OutlinedButton(onClick = actions.onAddLayer, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text(t.settingsAddLayer)
            }
        }
        Spacer(Modifier.height(12.dp))

        Text(t.settingsFilenameFormat, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = state.filenameFormat,
                onValueChange = actions.onFilenameFormatChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text(t.settingsFilenamePlaceholder) }
            )
            VariableInsertButton { v -> actions.onFilenameFormatChange(state.filenameFormat + v) }
        }
        Spacer(Modifier.height(16.dp))

        var advancedExpanded by remember { mutableStateOf(false) }
        OutlinedButton(
            onClick = { advancedExpanded = !advancedExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(if (advancedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
            Spacer(Modifier.width(8.dp))
            Text(t.settingsAdvanced)
        }
        AnimatedVisibility(visible = advancedExpanded) {
            Column(Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = state.maxTitleLength.toString(),
                    onValueChange = { it.toIntOrNull()?.let(actions.onMaxTitleLengthChange) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(t.settingsMaxTitleLength) }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.maxFilenameLength.toString(),
                    onValueChange = { it.toIntOrNull()?.let(actions.onMaxFilenameLengthChange) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(t.settingsMaxFilenameLength) }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.suffixKeywords.joinToString(", "),
                    onValueChange = {
                        actions.onSuffixKeywordsChange(
                            it.split(",").map { s -> s.trim() }.filter { s -> s.isNotBlank() }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(t.settingsSuffixKeywords) },
                    placeholder = { Text(t.settingsSuffixKeywordsHint) }
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(t.settingsScraperSites, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        SiteSelector(
            sites = state.sites.map { SiteItem(it.id, it.name, it.id in state.enabledSiteIds) },
            onToggle = actions.onToggleSite
        )
        Spacer(Modifier.height(20.dp))
        Text(t.settingsLanguage, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        LanguageSelector(state.language, state.showRestartHint, t, actions.onLanguageChange)
        Spacer(Modifier.height(24.dp))
        OutlinedButton(
            onClick = actions.onReset,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) { Icon(Icons.Default.SettingsBackupRestore, null); Spacer(Modifier.width(8.dp)); Text(t.settingsReset) }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun VariableInsertButton(onInsert: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val variables = listOf(
        "{num}", "{title}", "{actor}", "{actors}",
        "{maker}", "{label}", "{series}", "{director}",
        "{date}", "{year}", "{month}", "{day}", "{suffix}"
    )

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Add, "Insert variable")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            variables.forEach { v ->
                DropdownMenuItem(
                    text = { Text(v, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace) },
                    onClick = { onInsert(v); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(label)
        Switch(checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LanguageSelector(
    language: String,
    showRestartHint: Boolean,
    t: javscraper.i18n.TranslationEn,
    onLanguageChange: (String) -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(t.settingsLanguageEn)
            RadioButton(selected = language == "en", onClick = { onLanguageChange("en") })
        }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(t.settingsLanguageZh)
            RadioButton(selected = language == "zh", onClick = { onLanguageChange("zh") })
        }
        if (showRestartHint) {
            Spacer(Modifier.height(4.dp))
            Text(
                t.settingsLanguageRestartHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}
