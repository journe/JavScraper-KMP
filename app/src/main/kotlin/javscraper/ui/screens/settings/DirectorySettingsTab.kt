package javscraper.ui.screens.settings

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
import androidx.compose.ui.tooling.preview.Preview
import javscraper.i18n.LocalTranslations
import javscraper.settings.MultiPartSuffix
import javscraper.ui.components.ScanDirectoryField

/** Scan/output directories and file naming rules. */
@Composable
fun DirectorySettingsTab(state: SettingsState, actions: SettingsActions) {
    val t = LocalTranslations.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(t.commonScanDirectory, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        ScanDirectoryField(
            value = state.scanDir,
            history = state.scanDirHistory,
            onHistorySelect = actions.onSelectScanDirFromHistory,
            onBrowse = actions.onSelectScanDir,
            label = t.commonScanDirectory,
            modifier = Modifier.fillMaxWidth()
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
        SettingsSwitchRow(t.settingsMoveOriginalVideo, state.moveInsteadOfCopy, actions.onMoveInsteadOfCopyChange)

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
                        placeholder = { Text(t.commonNamingExample) }
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
                placeholder = { Text(t.commonNamingExample) }
            )
            VariableInsertButton { v -> actions.onFilenameFormatChange(state.filenameFormat + v) }
        }
        Spacer(Modifier.height(12.dp))
        Text(t.settingsMultiPartSuffix, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        MultiPartSuffixSelector(
            selected = state.multiPartSuffix,
            onSelect = actions.onMultiPartSuffixChange
        )
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
                    text = { Text(v, fontFamily = FontFamily.Monospace) },
                    onClick = { onInsert(v); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiPartSuffixSelector(
    selected: MultiPartSuffix,
    onSelect: (MultiPartSuffix) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MultiPartSuffix.entries.forEach { suffix ->
                DropdownMenuItem(
                    text = { Text(suffix.displayName) },
                    onClick = {
                        onSelect(suffix)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview
@Composable
fun DirectorySettingsTabPreview() {
    SettingsPreview {
        DirectorySettingsTab(previewSettingsState(), previewSettingsActions())
    }
}
