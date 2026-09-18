package javscraper.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import javscraper.i18n.LocalTranslations
import javscraper.ui.VideoFieldItem

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EditableChipField(
    items: List<VideoFieldItem>,
    onItemSelectionChange: (Int, Boolean) -> Unit,
    onItemAdd: (String) -> Unit
) {
    val translations = LocalTranslations.current
    var addDialogVisible by remember { mutableStateOf(false) }
    var newItem by remember { mutableStateOf("") }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEachIndexed { itemIndex, item ->
            FilterChip(
                modifier = Modifier.height(24.dp),
                selected = item.selected,
                onClick = { onItemSelectionChange(itemIndex, !item.selected) },
                label = {
                    Text(item.value, style = MaterialTheme.typography.labelSmall)
                }
            )
        }
        AssistChip(
            modifier = Modifier.height(24.dp),
            onClick = { addDialogVisible = true },
            label = {
                Text("+", style = MaterialTheme.typography.labelMedium)
            }
        )
    }

    if (addDialogVisible) {
        AlertDialog(
            onDismissRequest = {
                addDialogVisible = false
                newItem = ""
            },
            title = { Text(translations.metadataEditAddTagTitle) },
            text = {
                OutlinedTextField(
                    value = newItem,
                    onValueChange = { newItem = it },
                    singleLine = true,
                    placeholder = {
                        Text(translations.metadataEditAddTagPlaceholder)
                    }
                )
            },
            confirmButton = {
                Button(
                    enabled = newItem.isNotBlank(),
                    onClick = {
                        onItemAdd(newItem)
                        newItem = ""
                        addDialogVisible = false
                    }
                ) {
                    Text(translations.metadataEditAddTagAction)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        newItem = ""
                        addDialogVisible = false
                    }
                ) {
                    Text(translations.commonCancel)
                }
            }
        )
    }
}
