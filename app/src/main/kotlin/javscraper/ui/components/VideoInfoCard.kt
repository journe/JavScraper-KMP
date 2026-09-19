package javscraper.ui.components

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import javscraper.i18n.LocalTranslations
import javscraper.i18n.TranslationEn
import javscraper.i18n.TranslationZh
import javscraper.models.Review
import javscraper.models.Video
import javscraper.ui.VideoFieldItem
import javscraper.ui.videoFieldValues
import javscraper.ui.previewVideoWithAllFields
import javscraper.ui.theme.JavScraperTheme

internal data class VideoInfoRow(
    val label: String,
    val value: String,
    val isEmpty: Boolean,
    val items: List<VideoFieldItem>? = null
)

@OptIn(ExperimentalLayoutApi::class)
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
                    if (row.items.isNullOrEmpty()) {
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
                    } else {
                        FlowRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            row.items.forEach { item ->
                                AssistChip(
                                    modifier = Modifier.height(24.dp),
                                    onClick = {},
                                    label = {
                                        Text(
                                            text = item.value,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
            ReviewsSection(video.reviews, translations)
        }
    }
}

@Composable
private fun ReviewsSection(reviews: List<Review>, translations: TranslationEn) {
    if (reviews.isEmpty()) return
    var expanded by remember(reviews) { mutableStateOf(false) }
    HorizontalDivider()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = translations.videoFieldReviews,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = { expanded = !expanded }) {
            Text(
                text = if (expanded) {
                    translations.videoReviewsCollapse
                } else {
                    translations.videoReviewsExpand(reviews.size)
                },
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
    if (!expanded) return
    reviews.forEachIndexed { index, review ->
        if (index > 0) HorizontalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val metadata = buildList {
                review.author.takeIf { it.isNotBlank() }?.let(::add)
                review.date.takeIf { it.isNotBlank() }?.let(::add)
                review.score?.let { add(translations.videoReviewScore(it)) }
                review.likes?.let { add(translations.videoReviewLikes(it)) }
            }
            Text(
                text = metadata.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = review.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
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
            isEmpty = empty,
            items = field.items
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
