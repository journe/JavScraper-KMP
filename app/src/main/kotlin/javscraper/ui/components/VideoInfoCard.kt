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
                        modifier = Modifier.width(116.dp),
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
    val fields: List<Pair<String, String?>> = listOf(
        t.commonNumber to video.number,
        t.videoFieldTitle to video.title,
        t.videoFieldActresses to video.actresses.joinToString(", "),
        t.videoFieldDate to video.date,
        t.videoFieldMaker to video.maker,
        t.videoFieldLabel to video.label,
        t.videoFieldSeries to video.series,
        t.videoFieldDirector to video.director,
        t.videoFieldDuration to video.duration?.toString(),
        t.videoFieldRating to video.rating?.toString(),
        t.videoFieldTags to video.tags.joinToString(", "),
        t.videoFieldCoverUrl to video.coverUrl,
        t.videoFieldPosterUrl to video.posterUrl,
        t.videoFieldSampleImages to video.sampleImages.joinToString("\n"),
        t.videoFieldSummary to video.summary,
        t.videoFieldSource to video.source,
        t.videoFieldDetailUrl to video.detailUrl
    )
    return fields.map { (label, value) ->
        val normalizedValue = value.orEmpty()
        val empty = normalizedValue.isBlank()
        VideoInfoRow(
            label = label,
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

@Preview
@Composable
fun VideoInfoCardPreviewSparse() {
    CompositionLocalProvider(LocalTranslations provides TranslationZh()) {
        JavScraperTheme {
            VideoInfoCard(Video(number = "SONE-002"))
        }
    }
}

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