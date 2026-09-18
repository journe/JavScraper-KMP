package javscraper.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import javscraper.i18n.LocalTranslations
import javscraper.io.metadata.VideoMetadataEditResult
import javscraper.models.Video

data class VideoDetailActions(
    val onBack: () -> Unit,
    val onRefresh: () -> Unit,
    val onSaveMetadata: suspend (Video, Boolean) -> VideoMetadataEditResult,
    val onMetadataSaved: (Video) -> Unit = {},
    val onCropPoster: (() -> Unit)? = null,
    val onPlayVideo: () -> Unit = {},
    val onOpenFolder: () -> Unit = {}
)

@Composable
fun VideoDetailHeader(
    video: Video,
    actions: VideoDetailActions,
    onEdit: () -> Unit
) {
    val translations = LocalTranslations.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = translations.galleryDetailTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = video.number,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }

        DetailIconButton(actions.onRefresh, Icons.Filled.Refresh, translations.galleryDetailRefresh)
        actions.onCropPoster?.let { onCropPoster ->
            DetailIconButton(
                onClick = onCropPoster,
                icon = Icons.Filled.Crop,
                contentDescription = translations.galleryDetailCropPoster
            )
        }
        DetailIconButton(onEdit, Icons.Filled.Edit, translations.galleryDetailEdit)
        DetailIconButton(actions.onOpenFolder, Icons.Filled.FolderOpen, translations.galleryDetailOpenFolder)
        DetailIconButton(actions.onPlayVideo, Icons.Filled.PlayArrow, translations.galleryDetailPlay)
        DetailIconButton(actions.onBack, Icons.Filled.Close, translations.galleryDetailBack)
    }
}

@Composable
private fun DetailIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String
) {
    FilledIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}
