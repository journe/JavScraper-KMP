package javscraper.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import javscraper.models.Video
import javscraper.ui.components.VideoInfoCard
import javscraper.ui.previewVideoWithAllFields
import javscraper.ui.theme.JavScraperTheme

@Composable
internal fun PreviewCandidatesSection(
    candidates: List<Video>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        candidates.forEachIndexed { index, video ->
            val selected = index == selectedIndex
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(index) },
                border = BorderStroke(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selected, onClick = { onSelect(index) })
                    Column(Modifier.padding(start = 4.dp)) {
                        Text(
                            video.number,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            video.title.ifBlank { video.detailUrl },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        HorizontalDivider()
        VideoInfoCard(candidates[selectedIndex])
    }
}
private val previewCandidates = listOf(
    previewVideoWithAllFields(),
    Video(
        number = "ABC-123",
        title = "第一个候选结果",
        date = "2026-08-01",
        maker = "Example Studio",
        source = "mmtv",
        detailUrl = "https://www.7mmtv.sx/zh/example/123/content.html"
    ),

)

@Preview
@Composable
private fun PreviewCandidatesSectionPreview() {
    JavScraperTheme {
        PreviewCandidatesSection(
            candidates = previewCandidates,
            selectedIndex = 0,
            onSelect = { }
        )
    }
}