package javscraper.ui.components

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import javscraper.i18n.LocalTranslations
import javscraper.i18n.TranslationEn
import javscraper.i18n.TranslationZh
import javscraper.models.Video
import javscraper.ui.videoFieldValues
import javscraper.ui.previewVideoWithAllFields
import javscraper.ui.theme.JavScraperTheme

internal data class VideoInfoRow(
    val label: String,
    val value: String,
    val isEmpty: Boolean
)

@Composable
fun VideoInfoCard(video: Video, modifier: Modifier = Modifier) {
    val translations = LocalTranslations.current
    Card(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            videoInfoRows(video, translations).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = row.label,
                        modifier = Modifier.width(54.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = row.value,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (row.isEmpty) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}

internal fun videoInfoRows(video: Video, t: TranslationEn): List<VideoInfoRow> {
    return videoFieldValues(video, t).map { field ->
        val normalizedValue = field.value
        val empty = normalizedValue.isBlank()
        VideoInfoRow(
            label = field.label,
            value = if (empty) t.commonNotSet else normalizedValue,
            isEmpty = empty
        )
    }
}

@Preview
@Composable
fun VideoInfoCardPreviewFull() {
    CompositionLocalProvider(LocalTranslations provides TranslationZh()) {
        JavScraperTheme {
            VideoInfoCard(previewVideoWithAllFields())
        }
    }
}
