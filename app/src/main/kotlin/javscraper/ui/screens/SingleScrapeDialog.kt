package javscraper.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import javscraper.i18n.LocalTranslations
import javscraper.models.SingleScrapeDialogState
import javscraper.ui.components.VideoInfoCard
import javscraper.ui.screens.single.FieldUpdateConfirmDialog
import javscraper.ui.screens.single.FieldUpdateSelectionDialog

/** Dialog shown when the user scrapes a single task from the progress screen. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SingleScrapeDialog(
    state: ScrapeProgressState,
    actions: ScrapeProgressActions,
    modifier: Modifier = Modifier
) {
    val t = LocalTranslations.current
    when (state.singleScrapeDialogState) {
        SingleScrapeDialogState.Closed -> { }
        SingleScrapeDialogState.Input -> InputDialog(state, actions, t)
        SingleScrapeDialogState.Scraping -> ScrapingDialog(state, actions, t)
        is SingleScrapeDialogState.Preview -> PreviewDialog(state.singleScrapeDialogState, state.updateMode, actions, t)
        is SingleScrapeDialogState.FieldUpdateSelection -> FieldUpdateSelectionDialog(
            state.singleScrapeDialogState,
            actions.onToggleFieldUpdateField,
            actions.onConfirmFieldUpdateSelection,
            actions.onBackToPreviewFromFieldUpdate
        )
        is SingleScrapeDialogState.FieldUpdateConfirm -> FieldUpdateConfirmDialog(
            state.singleScrapeDialogState,
            actions.onConfirmFieldUpdateWrite,
            actions.onBackToFieldUpdateSelection
        )
        is SingleScrapeDialogState.Result -> ResultDialog(state.singleScrapeDialogState, actions, t)
    }
    if (state.showMissingOutputDir) {
        AlertDialog(
            onDismissRequest = actions.onDismissMissingOutputDir,
            title = { Text(t.singleScrapeMissingOutputTitle) },
            text = { Text(t.singleScrapeMissingOutputMessage) },
            confirmButton = {
                TextButton(onClick = actions.onDismissMissingOutputDir) { Text(t.commonConfirm) }
            }
        )
    }
}

@Composable
private fun InputDialog(
    state: ScrapeProgressState,
    actions: ScrapeProgressActions,
    t: javscraper.i18n.TranslationEn
) {
    val number = state.singleScrapeNumber
    val site = state.singleScrapeSite
    AlertDialog(
        onDismissRequest = actions.onCloseSingleScrape,
        title = { Text(t.singleScrapeTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 输出目录提示
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        t.settingsOutputDir + ": ",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        state.outputDir.ifBlank { t.commonNotSet },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.outputDir.isBlank()) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                state.singleScrapeError?.let { error ->
                    val stageMessage = when (state.singleScrapeErrorStage) {
                        "webpage_request" -> t.singleScrapeErrorWebpageRequestTimeout
                        "webpage_archive" -> t.singleScrapeErrorWebpageArchiveTimeout
                        null, "" -> if (error.contains("Timed out waiting", ignoreCase = true))
                            t.singleScrapeErrorWorkerTimeout else null

                        else -> null
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        if (stageMessage != null) {
                            Column {
                                Text(
                                    stageMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    error,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                                )
                            }
                        } else {
                            Text(
                                error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = number,
                    onValueChange = actions.onSingleScrapeNumberChange,
                    label = { Text(t.commonNumber) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                SiteDropdown(state, actions, t)
            }
        },
        confirmButton = {
            Button(
                onClick = actions.onStartSingleScrape,
                enabled = number.isNotBlank()
            ) { Text(t.commonStartScraping) }
        },
        dismissButton = {
            TextButton(onClick = actions.onCloseSingleScrape) { Text(t.commonCancel) }
        }
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SiteDropdown(
    state: ScrapeProgressState,
    actions: ScrapeProgressActions,
    t: javscraper.i18n.TranslationEn
) {
    val site = state.singleScrapeSite
    val selectedSiteName = if (site == null) t.singleScrapeSiteAuto
        else state.sites.find { it.id == site }?.name ?: site ?: ""
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedSiteName,
            onValueChange = {},
            label = { Text(t.singleScrapeSiteLabel) },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(t.singleScrapeSiteAuto) },
                onClick = {
                    actions.onSingleScrapeSiteChange(null)
                    expanded = false
                }
            )
            state.sites.forEach { siteItem ->
                DropdownMenuItem(
                    text = { Text(siteItem.name) },
                    onClick = {
                        actions.onSingleScrapeSiteChange(siteItem.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ScrapingDialog(
    state: ScrapeProgressState,
    actions: ScrapeProgressActions,
    t: javscraper.i18n.TranslationEn
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(t.singleScrapeTitle) },
        text = {
            val task = state.singleScrapeTask
            if (task != null) {
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(task.number, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                t.singleScrapeInProgress,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = actions.onCancelSingleScrape,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text(t.singleScrapeCancelJob) }
        },
        dismissButton = {}
    )
}

@Composable
private fun PreviewDialog(
    dialogState: SingleScrapeDialogState.Preview,
    updateMode: Boolean,
    actions: ScrapeProgressActions,
    t: javscraper.i18n.TranslationEn
) {
    AlertDialog(
        onDismissRequest = actions.onCancelPreviewWrite,
        title = { Text(t.singleScrapePreviewTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                dialogState.error?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                PreviewCandidatesSection(
                    candidates = dialogState.candidates,
                    selectedIndex = dialogState.selectedIndex,
                    onSelect = actions.onSelectPreviewCandidate
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (updateMode) {
                    Button(onClick = actions.onConfigureFieldUpdate) {
                        Text(t.singleScrapeFieldUpdateAction)
                    }
                }
                Button(onClick = actions.onConfirmPreviewWrite) {
                    Text(t.singleScrapeWriteConfirm)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = actions.onCancelPreviewWrite) { Text(t.commonCancel) }
        }
    )
}

@Composable
private fun ResultDialog(
    dialogState: SingleScrapeDialogState.Result,
    actions: ScrapeProgressActions,
    t: javscraper.i18n.TranslationEn
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(if (dialogState.video != null) t.singleScrapeResultTitle else t.singleScrapeFailedTitle) },
        text = {
            val video = dialogState.video
            if (video != null) {
                VideoInfoCard(video)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        dialogState.error ?: t.singleScrapeResultError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = actions.onConfirmScrapeResult) { Text(t.commonConfirm) }
        },
        dismissButton = {}
    )
}
