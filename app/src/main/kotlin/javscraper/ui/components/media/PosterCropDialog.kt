package javscraper.ui.components.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import javscraper.i18n.LocalTranslations
import javscraper.io.image.PosterCropper
import javscraper.models.Video
import javscraper.settings.AppSettings
import javscraper.settings.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import java.io.File

/** 滚轮微调比例后停顿时长,超时无新输入才持久化,避免每格滚轮都写盘。 */
private val SCROLL_PERSIST_DEBOUNCE = 400.milliseconds

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
    var useOriginalPoster by remember(posterFile) { mutableStateOf(false) }
    val posterBitmap = remember(posterFile) {
        posterFile.takeIf(File::isFile)?.let(::loadBitmap)
    }
    // 原图模式预览:复用 PreviewThumbnail,裁剪框即整图,高宽比取 poster 真实值,
    // 卡片高度 540 固定、宽度按真实比例自适应
    val fullImageRectState = remember(posterBitmap) {
        mutableStateOf(
            posterBitmap?.let {
                PosterCropper.Rect(0, 0, it.width, it.height)
            } ?: PosterCropper.Rect(0, 0, 1, 1)
        )
    }
    val posterAspect = posterBitmap?.let { it.height.toFloat() / it.width } ?: aspect

    PosterCropDialogFrame(
        title = translations.cropTitle(video.number),
        onDismissRequest = { dismissDialog("custom-dialog.onDismissRequest") },
        confirmButton = {
            TextButton(
                // 原图模式下 poster 缺失/不可解码时禁止确认,避免必然失败的写盘
                enabled = !useOriginalPoster || posterBitmap != null,
                onClick = {
                    log.info {
                        "confirm clicked: video=${video.number}, " +
                                "watermarkEnabled=${watermarkState.enabled}, " +
                                "watermarkMarks=${watermarkState.marks}"
                    }
                    scope.launch {
                        val watermarkOptions = watermarkState.toOptions(watermarkSize)
                        // 原图 + 无水印:poster 已是目标内容,跳过重编码写盘,
                        // 直接回调刷新预览并关闭弹窗(幂等,避免 JPEG 再编码损耗)
                        if (useOriginalPoster && watermarkOptions == null) {
                            log.info {
                                "skip original poster rewrite: video=${video.number}, " +
                                        "poster=${posterFile.absolutePath}"
                            }
                            onCropped()
                            dismissDialog("original-skip")
                            return@launch
                        }
                        val ok = withContext(Dispatchers.IO) {
                            if (useOriginalPoster) {
                                log.info {
                                    "write original poster: video=${video.number}, " +
                                            "poster=${posterFile.absolutePath}, " +
                                            "watermark=${watermarkOptions != null}"
                                }
                                PosterCropper.writeFullImage(
                                    posterFile,
                                    posterFile,
                                    watermarkOptions
                                )
                            } else {
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
                        }
                        log.info { "save finished: video=${video.number}, success=$ok" }
                        if (ok) {
                            onCropped()
                            dismissDialog("save-success")
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
                    // 滚轮微调比例:上滚+步长、下滚-步长,停顿后按防抖持久化;
                    // 拖动路径维持原有松手持久化语义
                    var scrollPersistJob by remember(sourceFile) {
                        mutableStateOf<Job?>(null)
                    }
                    val applyAspect: (Float, Boolean) -> Unit = { newAspect, persistAfterScroll ->
                        aspect = newAspect
                        cropRectState.value = PosterCropper.resizeRectToAspect(
                            cropRectState.value,
                            newAspect,
                            bitmap.width,
                            bitmap.height
                        )
                        if (persistAfterScroll) {
                            scrollPersistJob?.cancel()
                            scrollPersistJob = scope.launch {
                                delay(SCROLL_PERSIST_DEBOUNCE)
                                SettingsManager.update {
                                    it.copy(posterCropAspect = aspect)
                                }
                            }
                        }
                    }
                    Slider(
                        value = aspect,
                        onValueChange = { newAspect ->
                            applyAspect(newAspect, false)
                        },
                        onValueChangeFinished = {
                            // 松手时才持久化,避免拖动过程中每帧写盘
                            scrollPersistJob?.cancel()
                            SettingsManager.update { it.copy(posterCropAspect = aspect) }
                        },
                        valueRange = PosterCropper.MIN_ASPECT..PosterCropper.MAX_ASPECT,
                        modifier = Modifier.pointerInput(bitmap) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.type == PointerEventType.Scroll) {
                                        val step = 0.01f * event.changes.first().scrollDelta.y
                                        val next = (aspect + step).coerceIn(
                                            PosterCropper.MIN_ASPECT,
                                            PosterCropper.MAX_ASPECT
                                        )
                                        applyAspect(next, true)
                                    }
                                }
                            }
                        }
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
                    when {
                        useOriginalPoster && posterBitmap != null -> PreviewThumbnail(
                            posterBitmap,
                            fullImageRectState,
                            posterAspect,
                            watermarkState.toOptions(watermarkSize)
                        )
                        useOriginalPoster -> Text(
                            text = translations.cropPosterNotFound,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        else -> PreviewThumbnail(
                            bitmap,
                            cropRectState,
                            aspect,
                            watermarkState.toOptions(watermarkSize)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(onClick = { useOriginalPoster = true }) {
                            Text(translations.cropUseOriginal)
                        }
                        OutlinedButton(onClick = { useOriginalPoster = false }) {
                            Text(translations.cropUseCropped)
                        }
                    }
                }
            }
        }
    }
}
