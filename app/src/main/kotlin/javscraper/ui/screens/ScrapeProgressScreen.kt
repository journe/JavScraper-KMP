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
import javscraper.models.SingleScrapeDialogState
import javscraper.models.SiteInfo
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

/** Bundled scrape-progress state to reduce parameter count on [ScrapeProgressScreen]. */
data class ScrapeProgressState(
    val tasks: List<ScrapeTask>,
    val isRunning: Boolean,
    val singleScrapeDialogState: SingleScrapeDialogState,
    val singleScrapeNumber: String,
    val singleScrapeSite: String?,
    val singleScrapeTask: ScrapeTask?,
    val sites: List<SiteInfo>,
    val outputDir: String,
    val singleScrapeError: String?,
    val showMissingOutputDir: Boolean
)

data class ScrapeProgressActions(
    val onStartAll: () -> Unit,
    val onCancel: () -> Unit,
    val onSingleScrapeClick: (ScrapeTask) -> Unit,
    val onSingleScrapeNumberChange: (String) -> Unit,
    val onSingleScrapeSiteChange: (String?) -> Unit,
    val onStartSingleScrape: () -> Unit,
    val onCloseSingleScrape: () -> Unit,
    val onCancelSingleScrape: () -> Unit,
    val onConfirmPreviewWrite: () -> Unit,
    val onSelectPreviewCandidate: (Int) -> Unit,
    val onCancelPreviewWrite: () -> Unit,
    val onDismissMissingOutputDir: () -> Unit,
    val onConfirmScrapeResult: () -> Unit
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ScrapeProgressScreen(
    state: ScrapeProgressState,
    actions: ScrapeProgressActions,
    modifier: Modifier = Modifier
) {
    val t = LocalTranslations.current
    val tasks = state.tasks
    val isRunning = state.isRunning
    val onStartAll = actions.onStartAll
    val onCancel = actions.onCancel
    val onSingleScrapeClick = actions.onSingleScrapeClick

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
    SingleScrapeDialog(
        state = state,
        actions = actions
    )
    }
}
