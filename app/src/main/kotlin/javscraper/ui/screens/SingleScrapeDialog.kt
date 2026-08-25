package javscraper.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import javscraper.i18n.LocalTranslations
import javscraper.models.SingleScrapeDialogState
import javscraper.models.SiteInfo

/** Dialog shown when the user scrapes a single task from the progress screen. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SingleScrapeDialog(
    state: SingleScrapeDialogState,
    number: String,
    site: String?,
    task: ScrapeTask?,
    sites: List<SiteInfo>,
    onNumberChange: (String) -> Unit,
    onSiteChange: (String?) -> Unit,
    onStart: () -> Unit,
    onClose: () -> Unit,
    onConfirm: () -> Unit
) {
    val t = LocalTranslations.current
    when (state) {
        SingleScrapeDialogState.Closed -> { }
        SingleScrapeDialogState.Input -> {
            AlertDialog(
                onDismissRequest = onClose,
                title = { Text(t.singleScrapeTitle) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = number,
                            onValueChange = onNumberChange,
                            label = { Text(t.singleScrapeNumberLabel) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        val selectedSiteName = if (site == null) t.singleScrapeSiteAuto
                            else sites.find { it.id == site }?.name ?: site ?: ""
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
                                        onSiteChange(null)
                                        expanded = false
                                    }
                                )
                                sites.forEach { siteItem ->
                                    DropdownMenuItem(
                                        text = { Text(siteItem.name) },
                                        onClick = {
                                            onSiteChange(siteItem.id)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = onStart,
                        enabled = number.isNotBlank()
                    ) { Text(t.singleScrapeStart) }
                },
                dismissButton = {
                    TextButton(onClick = onClose) { Text(t.progressCancel) }
                }
            )
        }
        SingleScrapeDialogState.Scraping -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(t.singleScrapeTitle) },
                text = {
                    val current = task
                    if (current != null) {
                        Card(Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(current.number, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(t.singleScrapeInProgress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = onClose) { Text(t.progressCancel) }
                }
            )
        }
        is SingleScrapeDialogState.Result -> {
            val resultState = state as SingleScrapeDialogState.Result
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
                                    Text(number, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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
                    Button(onClick = onConfirm) { Text(t.commonConfirm) }
                },
                dismissButton = {}
            )
        }
    }
}