package javscraper.ui.components.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import javscraper.i18n.LocalTranslations
import javscraper.i18n.TranslationEn
import javscraper.io.image.WatermarkRenderer
import javscraper.models.Video
import javscraper.models.WatermarkMark
import javscraper.models.WatermarkOptions
import javscraper.models.inferWatermarkMarks
import org.jetbrains.skia.Image as SkiaImage

/** 对话框内可编辑的水印状态:清晰度、字幕、马赛克三组互斥/独立选择。 */
internal data class PosterWatermarkState(
    val enabled: Boolean = true,
    val hdMark: WatermarkMark? = null,
    val subtitle: Boolean = false,
    val mosaicMark: WatermarkMark? = null
) {
    val marks: List<WatermarkMark>
        get() = listOfNotNull(
            hdMark,
            WatermarkMark.SUB.takeIf { subtitle },
            mosaicMark
        )

    fun toOptions(size: Int): WatermarkOptions? =
        if (!enabled || marks.isEmpty()) null else WatermarkOptions(marks = marks, size = size)
}

/** 有码默认不推断,用户仍可在对话框手动选择;其余马赛克按 mdcx 优先级取一种。 */
private val mosaicInferencePriority = listOf(
    WatermarkMark.UMR,
    WatermarkMark.LEAK,
    WatermarkMark.UNCENSORED
)

internal fun defaultWatermarkState(video: Video, enabled: Boolean): PosterWatermarkState {
    val inferred = inferWatermarkMarks(video)
    return PosterWatermarkState(
        enabled = enabled,
        hdMark = inferred.firstOrNull { it.isHd },
        subtitle = WatermarkMark.SUB in inferred,
        mosaicMark = mosaicInferencePriority.firstOrNull { it in inferred }
    )
}

@Composable
internal fun PosterWatermarkControls(
    state: PosterWatermarkState,
    size: Int,
    onStateChange: (PosterWatermarkState) -> Unit,
    onSizeChange: (Int) -> Unit,
    onSizeChangeFinished: () -> Unit = {}
) {
    val translations = LocalTranslations.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = translations.cropWatermarkSectionTitle,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Switch(
                checked = state.enabled,
                onCheckedChange = { checked -> onStateChange(state.copy(enabled = checked)) }
            )
        }
        if (state.enabled) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = translations.cropWatermarkHdLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                MarkChipRow(
                    options = listOf(WatermarkMark.HD_4K, WatermarkMark.HD_8K, null),
                    selected = state.hdMark,
                    onSelect = { mark -> onStateChange(state.copy(hdMark = mark)) }
                )
                FilterChip(
                    selected = state.subtitle,
                    onClick = { onStateChange(state.copy(subtitle = !state.subtitle)) },
                    label = {
                        Text(
                            text = translations.cropWatermarkSub,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
                Text(
                    text = translations.cropWatermarkMosaicLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                MarkChipRow(
                    options = listOf(
                        WatermarkMark.CENSORED,
                        WatermarkMark.UMR,
                        WatermarkMark.LEAK,
                        WatermarkMark.UNCENSORED,
                        null
                    ),
                    selected = state.mosaicMark,
                    onSelect = { mark -> onStateChange(state.copy(mosaicMark = mark)) }
                )
                Text(
                    text = translations.cropWatermarkSize(size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = size.toFloat(),
                    onValueChange = { value -> onSizeChange(value.toInt()) },
                    onValueChangeFinished = onSizeChangeFinished,
                    valueRange = 1f..10f,
                    steps = 8
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MarkChipRow(
    options: List<WatermarkMark?>,
    selected: WatermarkMark?,
    onSelect: (WatermarkMark?) -> Unit
) {
    val translations = LocalTranslations.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = {
                    Text(
                        text = markLabel(translations, option),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )
        }
    }
}

private fun markLabel(translations: TranslationEn, mark: WatermarkMark?): String = when (mark) {
    WatermarkMark.HD_4K -> translations.cropWatermarkHd4k
    WatermarkMark.HD_8K -> translations.cropWatermarkHd8k
    WatermarkMark.SUB -> translations.cropWatermarkSub
    WatermarkMark.CENSORED -> translations.cropWatermarkCensored
    WatermarkMark.UMR -> translations.cropWatermarkUmr
    WatermarkMark.LEAK -> translations.cropWatermarkLeak
    WatermarkMark.UNCENSORED -> translations.cropWatermarkUncensored
    null -> translations.cropWatermarkNone
}

/** 预览用:按当前选中的水印加载位图,与渲染器使用同一份类路径素材。 */
@Composable
internal fun rememberWatermarkBitmaps(marks: List<WatermarkMark>): Map<WatermarkMark, ImageBitmap> =
    remember(marks) {
        marks.mapNotNull { mark ->
            val bytes = runCatching {
                WatermarkRenderer::class.java
                    .getResourceAsStream("/watermark/${mark.assetName}")
                    ?.use { stream -> stream.readBytes() }
            }.getOrNull() ?: return@mapNotNull null
            runCatching {
                mark to SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
            }.getOrNull()
        }.toMap()
    }