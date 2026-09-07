package javscraper.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import javscraper.i18n.LocalTranslations

/**
 * Modal dialog shown when the scraper worker fails to start (e.g. the
 * scraper-worker.exe path is misconfigured). The user picks the correct
 * worker executable via [onSelect]; the dialog stays open until the worker
 * starts successfully or the user cancels.
 */
@Composable
fun WorkerSetupDialog(
    onSelect: () -> Unit,
    onCancel: () -> Unit,
    errorMessage: String? = null
) {
    val t = LocalTranslations.current
    AlertDialog(
        onDismissRequest = onCancel,
        icon = { Icon(Icons.Default.Warning, contentDescription = null) },
        title = { Text(t.workerSetupTitle) },
        text = {
            Column {
                Text(t.workerSetupMessage)
                if (errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onSelect) { Text(t.workerSetupSelect) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(t.commonCancel) }
        }
    )
}