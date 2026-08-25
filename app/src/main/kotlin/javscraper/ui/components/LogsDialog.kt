package javscraper.ui.components

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javscraper.i18n.LocalTranslations
import javscraper.i18n.TranslationZh
import javscraper.io.logging.LogEntry
import javscraper.ui.theme.JavScraperTheme

@Composable
fun LogsDialog(
    entries: List<LogEntry>,
    filePath: String,
    onDismiss: () -> Unit
) {
    val translations = LocalTranslations.current
    val listState = rememberLazyListState()

    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.lastIndex)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(translations.logsTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = translations.logsFilePath + ": " + filePath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (entries.isEmpty()) {
                    Text(translations.logsEmpty)
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(entries) { entry ->
                            LogEntryItem(entry)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(translations.commonConfirm) }
        }
    )
}

@Composable
private fun LogEntryItem(entry: LogEntry) {
    val timestamp = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        .withZone(ZoneOffset.UTC)
        .format(Instant.ofEpochMilli(entry.timestampMillis))
    Column {
        Text(
            text = "$timestamp [${entry.level}] ${entry.loggerName} - ${entry.message}",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
        entry.throwableStack?.let { stack ->
            Text(
                text = stack,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Preview
@Composable
fun LogsDialogPreview() {
    val entries = listOf(
        LogEntry(1, 1_769_171_200_000, "INFO", "javscraper.app", "应用已启动"),
        LogEntry(2, 1_769_171_201_000, "WARN", "javscraper.sidecar", "Worker 启动失败")
    )
    CompositionLocalProvider(LocalTranslations provides TranslationZh()) {
        JavScraperTheme {
            LogsDialog(entries, "C:/Users/demo/.javscraper/logs/javscraper.log", onDismiss = {})
        }
    }
}
