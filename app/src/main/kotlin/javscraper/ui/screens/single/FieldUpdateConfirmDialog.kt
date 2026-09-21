package javscraper.ui.screens.single

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import javscraper.i18n.LocalTranslations
import javscraper.models.SingleScrapeDialogState
import javscraper.ui.components.media.VideoInfoCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FieldUpdateConfirmDialog(
    state: SingleScrapeDialogState.FieldUpdateConfirm,
    onConfirmWrite: () -> Unit,
    onBackToSelection: () -> Unit
) {
    val t = LocalTranslations.current
    AlertDialog(
        onDismissRequest = onBackToSelection,
        title = { Text(t.singleScrapeFieldUpdateConfirmTitle) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = t.singleScrapeFieldUpdateSelected(state.selectedFields.size),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    state.selectedFields.forEach { field ->
                        AssistChip(
                            modifier = Modifier.height(24.dp),
                            onClick = {},
                            label = {
                                Text(
                                    text = fieldUpdateLabel(field, t),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
                VideoInfoCard(state.merged)
            }
        },
        confirmButton = {
            Button(onClick = onConfirmWrite) { Text(t.singleScrapeWriteConfirm) }
        },
        dismissButton = {
            TextButton(onClick = onBackToSelection) {
                Text(t.singleScrapeFieldUpdateBackSelection)
            }
        }
    )
}