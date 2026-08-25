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
import javscraper.AppViewModel
import javscraper.i18n.LocalTranslations
import javscraper.ui.LocalAppViewModel
import javscraper.models.SingleScrapeDialogState
import javscraper.models.Video

enum class ScrapeTaskStatus { PENDING, SCRAPING, SUCCESS, FAILED }
data class ScrapeTask(
    val number: String,
    val fileName: String,
    val partCount: Int = 1,
    val path: String = "",
    val status: ScrapeTaskStatus = ScrapeTaskStatus.PENDING,
    val video: Video? = null,
    val error: String = ""
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ScrapeProgressScreen(
    viewModel: AppViewModel = LocalAppViewModel.current,
    modifier: Modifier = Modifier
) {
    val t = LocalTranslations.current
    val tasks = viewModel.tasks
    val isRunning = viewModel.scraping
    val onStartAll = viewModel::startAllScraping
    val onCancel = viewModel::cancelScraping
    val onSingleScrapeClick = viewModel::openSingleScrapeFromTask
    val singleScrapeDialogState = viewModel.singleScrapeDialogState
    val singleScrapeNumber = viewModel.singleScrapeNumber
    val singleScrapeSite = viewModel.singleScrapeSite
    val singleScrapeTask = viewModel.singleScrapeTask
    val sites = viewModel.sites
    val onSingleScrapeNumberChange = viewModel::updateSingleScrapeNumber
    val onSingleScrapeSiteChange = viewModel::updateSingleScrapeSite
    val onStartSingleScrape = viewModel::startSingleScrape
    val onCloseSingleScrape = viewModel::closeSingleScrape
    val onConfirmScrapeResult = viewModel::confirmSingleScrape
    val done = tasks.count { it.status == ScrapeTaskStatus.SUCCESS || it.status == ScrapeTaskStatus.FAILED }
    val ok = tasks.count { it.status == ScrapeTaskStatus.SUCCESS }
    val progress = if (tasks.isNotEmpty()) done.toFloat() / tasks.size else 0f
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(t.progressTitle, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (isRunning) Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Icon(Icons.Default.Stop, null); Spacer(Modifier.width(8.dp)); Text(t.progressCancel) }
            else if (tasks.isNotEmpty()) Button(onClick = onStartAll) {
                Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text(t.progressStartAll)
            }
        }
        Spacer(Modifier.height(16.dp))
        if (tasks.isNotEmpty()) {
            LinearProgressIndicator({ progress }, Modifier.fillMaxWidth().height(8.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                t.progressCompleted(ok, tasks.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            items(tasks) { task ->
                val bg = when (task.status) {
                    ScrapeTaskStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
                    ScrapeTaskStatus.SCRAPING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ScrapeTaskStatus.SUCCESS -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    ScrapeTaskStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                }
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = bg)) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(task.number, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                if (task.partCount > 1) {
                                    Spacer(Modifier.width(6.dp))
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            " parts",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                task.fileName,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (task.error.isNotBlank()) Text(
                                task.error,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        if (task.status == ScrapeTaskStatus.PENDING && !isRunning) {
                            IconButton(
                                onClick = { onSingleScrapeClick(task) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Single scrape",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        when (task.status) {
                            ScrapeTaskStatus.PENDING -> Icon(
                                Icons.Default.HourglassEmpty,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            ScrapeTaskStatus.SCRAPING -> CircularProgressIndicator(
                                Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )

                            ScrapeTaskStatus.SUCCESS -> Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )

                            ScrapeTaskStatus.FAILED -> Icon(
                                Icons.Default.Error,
                                null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
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
                            val selectedSiteName = if (singleScrapeSite == null) t.singleScrapeSiteAuto
                                else sites.find { it.id == singleScrapeSite }?.name ?: singleScrapeSite ?: ""
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
                                            onSingleScrapeSiteChange(null)
                                            expanded = false
                                        }
                                    )
                                    sites.forEach { site ->
                                        DropdownMenuItem(
                                            text = { Text(site.name) },
                                            onClick = {
                                                onSingleScrapeSiteChange(site.id)
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
                val resultState = singleScrapeDialogState as SingleScrapeDialogState.Result
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
