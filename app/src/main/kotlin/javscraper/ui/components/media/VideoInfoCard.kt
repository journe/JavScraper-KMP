package javscraper.ui.components.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt
import javscraper.i18n.LocalTranslations
import javscraper.i18n.TranslationEn
import javscraper.i18n.TranslationZh
import javscraper.models.Review
import javscraper.models.Video
import javscraper.ui.VideoFieldItem
import javscraper.ui.previewVideoWithAllFields
import javscraper.ui.theme.JavScraperTheme
import javscraper.ui.videoFieldValues

internal data class VideoInfoRow(
    val label: String,
    val value: String,
    val isEmpty: Boolean,
    val items: List<VideoFieldItem>? = null
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoInfoCard(
    video: Video,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 420.dp
) {
    val translations = LocalTranslations.current
    var reviewsExpanded by remember(video) { mutableStateOf(false) }
    Card(modifier.fillMaxWidth()) {
        Box {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .heightIn(max = maxHeight)
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
            }
            ReviewsOverlay(
                expanded = reviewsExpanded,
                reviews = video.reviews,
                translations = translations,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                onToggle = { reviewsExpanded = !reviewsExpanded }
            )
        }
    }
}


@Composable
private fun ReviewsOverlay(
    expanded: Boolean,
    reviews: List<Review>,
    translations: TranslationEn,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit
) {
    if (reviews.isEmpty()) return
    val reviewsAnimationDuration = 100

    val revealProgress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(durationMillis = reviewsAnimationDuration),
        label = "revealProgress"
    )

    Box(modifier) {
        AnimatedVisibility(
            visible = revealProgress > 0f,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 48.dp, bottom = 56.dp),
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 3.dp,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .heightIn(max = 360.dp)
                    .then(
                        Modifier.drawWithContent {
                            val centerX = size.width
                            val centerY = size.height
                            val maxRadius = sqrt(size.width * size.width + size.height * size.height)
                            val radius = maxRadius * revealProgress
                            val path = Path().apply {
                                addOval(
                                    Rect(
                                        center = Offset(centerX, centerY),
                                        radius = radius
                                    )
                                )
                            }
                            clipPath(path) {
                                this@drawWithContent.drawContent()
                            }
                        }
                    )
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReviewsSection(reviews, translations)
                }
            }
        }
        ReviewsToggleButton(
            expanded = expanded,
            reviewsCount = reviews.size,
            translations = translations,
            modifier = Modifier.align(Alignment.BottomEnd)
        ) { onToggle() }
    }
}

@Composable
private fun ReviewsToggleButton(
    expanded: Boolean,
    reviewsCount: Int,
    translations: TranslationEn,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.size(48.dp)
    ) {
        Icon(
            imageVector = if (expanded) Icons.Filled.Close else Icons.Filled.RateReview,
            contentDescription = if (expanded) {
                translations.videoReviewsCollapse
            } else {
                translations.videoReviewsExpand(reviewsCount)
            },

            modifier = Modifier.padding(12.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun ReviewsSection(reviews: List<Review>, translations: TranslationEn) {
    if (reviews.isEmpty()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = translations.videoFieldReviews,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
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
            VideoInfoCard(previewVideoWithAllFields(), maxHeight = 900.dp)
        }
    }
}
