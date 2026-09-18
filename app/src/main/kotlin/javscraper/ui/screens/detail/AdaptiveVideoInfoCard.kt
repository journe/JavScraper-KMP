package javscraper.ui.screens.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import javscraper.models.Video
import javscraper.ui.components.VideoInfoCard

private val MIN_VIDEO_INFO_WIDTH = 160.dp
private val MAX_VIDEO_INFO_WIDTH = 500.dp
private val DETAIL_ITEM_SPACING = 16.dp

internal data class PosterSpace(
    val width: Int,
    val height: Int
)

internal fun reservedPosterSpace(
    measured: PosterSpace?,
    cached: PosterSpace
): PosterSpace = measured ?: cached

private class PosterSpaceCache {
    var value = PosterSpace(width = 0, height = 0)
        private set

    fun update(space: PosterSpace) {
        if (space.width > 0 && space.height > 0) value = space
    }
}

@Composable
internal fun AdaptiveVideoInfoCard(
    video: Video,
    poster: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val posterSpaceCache = remember { PosterSpaceCache() }

    Layout(
        content = {
            poster()
            VideoInfoCard(video = video)
        },
        modifier = modifier
    ) { measurables, constraints ->
        // AnimatedVisibility 完全隐藏后，poster 槽位不会产生 measurable。
        check(measurables.size in 1..2) {
            "AdaptiveVideoInfoCard expects an info card and an optional poster."
        }

        val hasPoster = measurables.size == 2
        val posterPlaceable = if (hasPoster) {
            measurables[0].measure(constraints.copy(minWidth = 0, minHeight = 0))
        } else {
            null
        }
        val measuredPosterSpace = posterPlaceable?.let {
            PosterSpace(width = it.width, height = it.height)
        }
        measuredPosterSpace?.let(posterSpaceCache::update)
        val posterSpace = reservedPosterSpace(
            measured = measuredPosterSpace,
            cached = posterSpaceCache.value
        ).let {
            PosterSpace(
                width = it.width.coerceIn(0, constraints.maxWidth),
                height = it.height.coerceIn(0, constraints.maxHeight)
            )
        }
        val infoPlaceable = measurables.last().measure(
            Constraints.fixedWidth(
                adaptiveVideoInfoCardWidth(
                    availableWidth = constraints.maxWidth.toDp(),
                    posterWidth = posterSpace.width.toDp()
                ).roundToPx()
            )
        )
        val hasPosterSpace = posterSpace.width > 0
        val spacing = if (hasPosterSpace) DETAIL_ITEM_SPACING.roundToPx() else 0
        val fitsBesidePoster = !hasPosterSpace ||
            posterSpace.width + spacing + MIN_VIDEO_INFO_WIDTH.roundToPx() <= constraints.maxWidth
        val layoutWidth = constraints.maxWidth.coerceIn(constraints.minWidth, constraints.maxWidth)

        if (fitsBesidePoster) {
            val layoutHeight = maxOf(posterSpace.height, infoPlaceable.height)
                .coerceIn(constraints.minHeight, constraints.maxHeight)
            layout(layoutWidth, layoutHeight) {
                posterPlaceable?.placeRelative(0, 0)
                infoPlaceable.placeRelative(posterSpace.width + spacing, 0)
            }
        } else {
            val layoutHeight = (posterSpace.height + spacing + infoPlaceable.height)
                .coerceIn(constraints.minHeight, constraints.maxHeight)
            layout(layoutWidth, layoutHeight) {
                posterPlaceable?.placeRelative(0, 0)
                infoPlaceable.placeRelative(0, posterSpace.height + spacing)
            }
        }
    }
}

internal fun adaptiveVideoInfoCardWidth(
    availableWidth: Dp,
    posterWidth: Dp,
    spacing: Dp = DETAIL_ITEM_SPACING
): Dp {
    if (posterWidth > 0.dp && posterWidth + spacing + MIN_VIDEO_INFO_WIDTH <= availableWidth) {
        return (availableWidth - posterWidth - spacing).coerceAtMost(MAX_VIDEO_INFO_WIDTH)
    }

    return MAX_VIDEO_INFO_WIDTH.coerceAtMost(availableWidth)
}