package javscraper.ui.components.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import javscraper.i18n.LocalTranslations
import javscraper.io.image.PosterCropper
import javscraper.models.Video
import javscraper.settings.AppSettings
import javscraper.settings.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val log = mu.KotlinLogging.logger("PosterCropDialog")

/**
 * 封面裁剪对话框:对齐 mdcx CutWindow 交互——
 * 等比显示原图,叠加固定 1.5 高宽比的可拖动裁剪框,遮罩暗化框外区域,
 * 实时预览裁剪结果;确认后裁剪写盘 poster.jpg。
 */
@Composable
fun PosterCropDialog(
    video: Video,
    sourceFile: File,
    posterFile: File,
    onCropped: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val translations = LocalTranslations.current
    val scope = rememberCoroutineScope()
    val dismissDialog: (String) -> Unit = { reason ->
        log.info {
            "dismiss requested: reason=$reason, video=${video.number}, " +
                    "source=${sourceFile.absolutePath}"
        }
        onDismiss()
    }

    val bitmap = remember(sourceFile) { loadBitmap(sourceFile) }
    if (bitmap == null) {
        log.warn { "poster bitmap load failed: source=${sourceFile.absolutePath}" }
        PosterCropDialogFrame(
            title = translations.cropTitle(video.number),
            onDismissRequest = { dismissDialog("load-failed.onDismissRequest") },
            confirmButton = {
                TextButton(onClick = { dismissDialog("load-failed.confirm") }) {
                    Text(translations.commonConfirm)
                }
            },
            dismissButton = {}
        ) {
            Text(translations.cropLoadFailed)
        }
        return
    }

    val savedAspect = remember(sourceFile) {
        SettingsManager.get().posterCropAspect
            .coerceIn(PosterCropper.MIN_ASPECT, PosterCropper.MAX_ASPECT)
    }
    val cropRectState = remember(sourceFile) {
        mutableStateOf(PosterCropper.initialCropRect(bitmap.width, bitmap.height, savedAspect))
    }
    var aspect by remember(sourceFile) { mutableFloatStateOf(savedAspect) }
    val savedWatermarkEnabled = remember(sourceFile) {
        SettingsManager.get().posterWatermarkEnabled
    }
    var watermarkSize by remember(sourceFile) {
        mutableStateOf(
            SettingsManager.get().posterWatermarkSize.coerceIn(
                AppSettings.MIN_POSTER_WATERMARK_SIZE,
                AppSettings.MAX_POSTER_WATERMARK_SIZE
            )
        )
    }
    var watermarkState by remember(sourceFile, video) {
        mutableStateOf(defaultWatermarkState(video, savedWatermarkEnabled))
    }
    var errorMessage by remember(sourceFile) { mutableStateOf<String?>(null) }

    PosterCropDialogFrame(
        title = translations.cropTitle(video.number),
        onDismissRequest = { dismissDialog("custom-dialog.onDismissRequest") },
        confirmButton = {
            TextButton(
                onClick = {
                    log.info {
                        "confirm clicked: video=${video.number}, " +
                                "watermarkEnabled=${watermarkState.enabled}, " +
                                "watermarkMarks=${watermarkState.marks}"
                    }
                    scope.launch {
                        val watermarkOptions = watermarkState.toOptions(watermarkSize)
                        val ok = withContext(Dispatchers.IO) {
                            log.info {
                                "crop started: source=${sourceFile.absolutePath}, " +
                                        "poster=${posterFile.absolutePath}, " +
                                        "watermark=${watermarkOptions != null}"
                            }
                            PosterCropper.cropToFile(
                                sourceFile,
                                posterFile,
                                cropRectState.value,
                                watermarkOptions
                            )
                        }
                        log.info { "crop finished: video=${video.number}, success=$ok" }
                        if (ok) {
                            onCropped()
                            dismissDialog("crop-success")
                        } else {
                            errorMessage = translations.cropWriteFailed
                        }
                    }
                }
            ) {
                Text(translations.cropConfirm)
            }
        },
        dismissButton = {
            TextButton(onClick = { dismissDialog("cancel-button") }) {
                Text(translations.commonCancel)
            }
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    CropCanvas(bitmap, cropRectState)
                    Slider(
                        value = aspect,
                        onValueChange = { newAspect ->
                            aspect = newAspect
                            cropRectState.value = PosterCropper.resizeRectToAspect(
                                cropRectState.value,
                                newAspect,
                                bitmap.width,
                                bitmap.height
                            )
                        },
                        onValueChangeFinished = {
                            // 松手时才持久化,避免拖动过程中每帧写盘
                            SettingsManager.update { it.copy(posterCropAspect = aspect) }
                        },
                        valueRange = PosterCropper.MIN_ASPECT..PosterCropper.MAX_ASPECT,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = translations.cropAspectRatio(aspect),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = translations.cropSourceSize(bitmap.width, bitmap.height),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = translations.cropOutputSize(
                                cropRectState.value.width, cropRectState.value.height
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    errorMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    PosterWatermarkControls(
                        state = watermarkState,
                        size = watermarkSize,
                        onStateChange = { newState ->
                            log.info {
                                "watermark state change: enabled=" +
                                        "${watermarkState.enabled}->${newState.enabled}, " +
                                        "hd=${watermarkState.hdMark}->${newState.hdMark}, " +
                                        "subtitle=${watermarkState.subtitle}->${newState.subtitle}, " +
                                        "mosaic=${watermarkState.mosaicMark}->${newState.mosaicMark}"
                            }
                            watermarkState = newState
                            val persistedEnabled = SettingsManager.get().posterWatermarkEnabled
                            if (persistedEnabled != newState.enabled) {
                                SettingsManager.update {
                                    it.copy(posterWatermarkEnabled = newState.enabled)
                                }
                                log.info {
                                    "posterWatermarkEnabled persisted: " +
                                            "${SettingsManager.get().posterWatermarkEnabled}"
                                }
                            }
                        },
                        onSizeChange = { watermarkSize = it },
                        onSizeChangeFinished = {
                            SettingsManager.update { it.copy(posterWatermarkSize = watermarkSize) }
                        }
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    PreviewThumbnail(
                        bitmap,
                        cropRectState,
                        aspect,
                        watermarkState.toOptions(watermarkSize)
                    )
                }
            }
        }
    }
}
