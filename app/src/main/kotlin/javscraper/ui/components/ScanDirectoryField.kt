package javscraper.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

/** Read-only scan-directory field with a recent-directory dropdown. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanDirectoryField(
    value: String,
    history: List<String>,
    onHistorySelect: (String) -> Unit,
    onBrowse: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val hasHistory = history.isNotEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { requested -> if (hasHistory) expanded = requested }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = {
                IconButton(onClick = onBrowse) {
                    Icon(Icons.Default.FolderOpen, contentDescription = label)
                }
            },
            modifier = modifier
                .fillMaxWidth()
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            history.forEach { directory ->
                DropdownMenuItem(
                    text = {
                        Text(directory, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                    onClick = {
                        onHistorySelect(directory)
                        expanded = false
                    }
                )
            }
        }
    }
}