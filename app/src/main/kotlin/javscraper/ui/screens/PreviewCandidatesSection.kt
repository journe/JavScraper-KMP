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

private fun previewVideoWithAllFields(): Video = Video(
    number = "SONE-001",
    title = "包含完整字段的刮削结果标题",
    actresses = listOf("演员 A", "演员 B"),
    date = "2026-08-26",
    maker = "片商",
    label = "发行标签",
    series = "系列",
    director = "导演",
    duration = 120,
    rating = 9.2,
    tags = listOf("标签 A", "标签 B"),
    coverUrl = "https://example.com/cover.jpg",
    posterUrl = "https://example.com/poster.jpg",
    sampleImages = listOf(
        "https://example.com/sample-1.jpg",
        "https://example.com/sample-2.jpg"
    ),
    summary = "这是完整刮削结果预览的简介内容，用于检查长文本滚动和换行效果。",
    source = "JavBus",
    detailUrl = "https://example.com/detail"
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