package javscraper.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import javscraper.i18n.LocalTranslations
import javscraper.i18n.TranslationEn

/** Worker path, language and reset actions. */
@Composable
fun OtherSettingsTab(state: SettingsState, actions: SettingsActions) {
    val t = LocalTranslations.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelector(
    language: String,
    showRestartHint: Boolean,
    t: TranslationEn,
    onLanguageChange: (String) -> Unit
) {
    val languages = listOf("en" to t.settingsLanguageEn, "zh" to t.settingsLanguageZh)
    val currentLabel = languages.firstOrNull { it.first == language }?.second ?: language
    var expanded by remember { mutableStateOf(false) }
    Column {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = currentLabel,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                languages.forEach { (id, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onLanguageChange(id)
                            expanded = false
                        }
                    )
                }
            }
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

@Preview
@Composable
fun OtherSettingsTabPreview() {
    SettingsPreview {
        OtherSettingsTab(previewSettingsState(), previewSettingsActions())
    }
}