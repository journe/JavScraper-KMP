package javscraper.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import javscraper.i18n.LocalTranslations
import javscraper.io.metadata.VideoMetadataEditResult
import javscraper.models.Video
import javscraper.ui.VideoFieldValue
import kotlinx.coroutines.launch

@Composable
fun VideoEditDialog(
    video: Video,
    onSaveMetadata: suspend (Video) -> VideoMetadataEditResult,
    onDismiss: () -> Unit
) {
    val translations = LocalTranslations.current
    val scope = rememberCoroutineScope()
    var fields by remember(video) { mutableStateOf(videoEditFields(video, translations)) }
    var saving by remember(video) { mutableStateOf(false) }
    var saveError by remember(video) { mutableStateOf<String?>(null) }
    val editedVideo = videoFromEditFields(video, fields)

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(translations.galleryEditTitle) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                fields.forEachIndexed { index, field ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = field.label,
                            modifier = Modifier.width(54.dp).padding(top = 13.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        MetadataTextField(
                            field = field,
                            multiline = index == 4 || index == 16,
                            numeric = index == 9 || index == 10,
                            onValueChange = { value ->
                                fields = fields.mapIndexed { fieldIndex, current ->
                                    if (fieldIndex == index) current.copy(value = value) else current
                                }
                            }
                        )
                    }
                }
                if (editedVideo == null) {
                    Text(
                        text = translations.metadataEditInvalidFields,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                saveError?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = editedVideo != null && !saving,
                onClick = {
                    val candidate = editedVideo ?: return@Button
                    saving = true
                    saveError = null
                    scope.launch {
                        val result = onSaveMetadata(candidate)
                        saving = false
                        when (result) {
                            is VideoMetadataEditResult.Success -> onDismiss()
                            VideoMetadataEditResult.NfoMissing ->
                                saveError = translations.metadataEditNfoMissing
                            is VideoMetadataEditResult.Failed ->
                                saveError = translations.metadataEditSaveFailed(result.message)
                        }
                    }
                }
            ) {
                Text(if (saving) translations.commonSaving else translations.commonSave)
            }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onDismiss) {
                Text(translations.commonCancel)
            }
        }
    )
}

@Composable
private fun MetadataTextField(
    field: VideoFieldValue,
    multiline: Boolean,
    numeric: Boolean,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = field.value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = !multiline,
        minLines = if (multiline) 2 else 1,
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
        textStyle = MaterialTheme.typography.bodySmall
    )
}
