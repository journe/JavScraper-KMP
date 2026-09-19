package javscraper.ui.screens.single

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import javscraper.i18n.LocalTranslations
import javscraper.i18n.TranslationEn
import javscraper.models.SingleScrapeDialogState
import javscraper.models.VideoFieldUpdateChoice
import javscraper.models.VideoFieldUpdateChoiceStatus
import javscraper.models.VideoUpdateField

@Composable
internal fun FieldUpdateSelectionDialog(
    state: SingleScrapeDialogState.FieldUpdateSelection,
    onToggleField: (VideoUpdateField, Boolean) -> Unit,
    onConfirm: () -> Unit,
    onBackToPreview: () -> Unit
) {
    val t = LocalTranslations.current
    AlertDialog(
        onDismissRequest = onBackToPreview,
        title = { Text(t.singleScrapeFieldUpdateTitle) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.choices.forEach { choice ->
                    FieldUpdateChoiceRow(choice, t) { selected ->
                        onToggleField(choice.field, selected)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = state.choices.any { it.selected }
            ) { Text(t.singleScrapeFieldUpdateNext) }
        },
        dismissButton = {
            TextButton(onClick = onBackToPreview) {
                Text(t.singleScrapeFieldUpdateBackPreview)
            }
        }
    )
}

@Composable
private fun FieldUpdateChoiceRow(
    choice: VideoFieldUpdateChoice,
    t: TranslationEn,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(
            checked = choice.selected,
            onCheckedChange = onToggle,
            enabled = choice.enabled
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = fieldUpdateLabel(choice.field, t),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = fieldUpdateStatusText(choice.status, t),
                    style = MaterialTheme.typography.labelSmall,
                    color = when (choice.status) {
                        VideoFieldUpdateChoiceStatus.NEW_FIELD -> MaterialTheme.colorScheme.tertiary
                        VideoFieldUpdateChoiceStatus.OVERWRITE -> MaterialTheme.colorScheme.primary
                        VideoFieldUpdateChoiceStatus.SAME -> MaterialTheme.colorScheme.onSurfaceVariant
                        VideoFieldUpdateChoiceStatus.EMPTY_INCOMING -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            ValueLine(t.singleScrapeFieldUpdateOldValue, choice.oldValue, t)
            ValueLine(t.singleScrapeFieldUpdateNewValue, choice.newValue, t)
        }
    }
}

@Composable
private fun ValueLine(label: String, value: String, t: TranslationEn) {
    Text(
        text = label + ": " + value.ifBlank { t.commonNotSet },
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}
