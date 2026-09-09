package javscraper.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import java.io.File
import javax.imageio.ImageIO
import javscraper.i18n.LocalTranslations
import javscraper.i18n.TranslationZh
import javscraper.io.PosterCropper
import javscraper.settings.SettingsManager
import javscraper.ui.theme.JavScraperTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.image.BufferedImage

/**
 * 封面裁剪对话框:对齐 mdcx CutWindow 交互——
 * 等比显示原图,叠加固定 1.5 高宽比的可拖动裁剪框,遮罩暗化框外区域,
 * 实时预览裁剪结果;确认后裁剪写盘 poster.jpg。
 */
@Composable
fun PosterCropDialog(
    videoNumber: String,
    sourceFile: File,
    posterFile: File,
    onCropped: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val translations = LocalTranslations.current
    val scope = rememberCoroutineScope()

    val bitmap = remember(sourceFile) { loadBitmap(sourceFile) }
    if (bitmap == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(translations.cropTitle(videoNumber)) },
            text = { Text(translations.cropLoadFailed) },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(translations.commonConfirm) }
            },
        )
        return
    }
    // 持久化的高宽比(默认 1.5 = 2:3 海报标准);初值与裁剪框保持一致
    val savedAspect = remember(sourceFile) {
        SettingsManager.get().posterCropAspect
            .coerceIn(PosterCropper.MIN_ASPECT, PosterCropper.MAX_ASPECT)
    }
    // 用 MutableState 直接持有裁剪框:手势 lambda 逐事件读写 .value,
    // 每次读到的都是上一次写入的最新值,拖动才能与鼠标位移 1:1 累计。
    val cropRectState = remember(sourceFile) {
        mutableStateOf(PosterCropper.initialCropRect(bitmap.width, bitmap.height, savedAspect))
    }
    // 高宽比可调,变更时以框右缘为锚重算尺寸,松手后写回设置。
    var aspect by remember(sourceFile) { mutableFloatStateOf(savedAspect) }
    var errorMessage by remember(sourceFile) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(translations.cropTitle(videoNumber)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CropCanvas(
                    bitmap = bitmap,
                    rectState = cropRectState,
                )
                Column {
                    Text(
                        text = translations.cropAspectRatio(aspect),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PreviewThumbnail(bitmap, cropRectState, aspect)
                    Column {
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
                        errorMessage?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            PosterCropper.cropToFile(sourceFile, posterFile, cropRectState.value)
                        }
                        if (ok) {
                            onCropped()
                            onDismiss()
                        } else {
                            errorMessage = translations.cropWriteFailed
                        }
                    }
                }
            ) { Text(translations.cropConfirm) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(translations.commonCancel) }
        }
    )
}

/** 等比适配比例(letterbox):画布尺寸相对原图的缩放系数。 */
private fun fitScale(canvasWidth: Float, canvasHeight: Float, imageWidth: Int, imageHeight: Int): Float {
    if ((imageWidth == 0) || (imageHeight == 0)) return 1f
    return minOf(canvasWidth / imageWidth, canvasHeight / imageHeight)
}

@Composable
private fun CropCanvas(
    bitmap: ImageBitmap,
    rectState: MutableState<PosterCropper.Rect>
) {
    val density = LocalDensity.current
    val scrim = MaterialTheme.colorScheme.scrim
    val border = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(bitmap.width.toFloat() / bitmap.height)
            .heightIn(max = 420.dp)
            .pointerInput(bitmap) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    // 屏幕位移换算为原图像素位移
                    val scale = fitScale(
                        size.width.toFloat(), size.height.toFloat(), bitmap.width, bitmap.height
                    )
                    if (scale > 0f) {
                        // 读 MutableState.value 是同步最新值:逐事件在上次结果上累计,
                        // 保证框与鼠标 1:1(rememberUpdatedState 只在重组时刷新,
                        // 帧内多个拖动事件会互相覆盖,导致框明显慢于鼠标)。
                        rectState.value = PosterCropper.translateRect(
                            rectState.value,
                            (dragAmount.x / scale).toInt(),
                            (dragAmount.y / scale).toInt(),
                            bitmap.width,
                            bitmap.height
                        )
                    }
                }
            }
    ) {
        val cropRect = rectState.value
        val canvasSize = size
        val scale = fitScale(canvasSize.width, canvasSize.height, bitmap.width, bitmap.height)
        val drawWidth = bitmap.width * scale
        val drawHeight = bitmap.height * scale
        val offsetX = (canvasSize.width - drawWidth) / 2f
        val offsetY = (canvasSize.height - drawHeight) / 2f

        // 原图等比居中绘制
        drawImage(
            image = bitmap,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(bitmap.width, bitmap.height),
            dstOffset = IntOffset(offsetX.toInt(), offsetY.toInt()),
            dstSize = IntSize(drawWidth.toInt(), drawHeight.toInt())
        )

        val rectLeft = offsetX + (cropRect.x * scale)
        val rectTop = offsetY + (cropRect.y * scale)
        val rectRight = offsetX + ((cropRect.x + cropRect.width) * scale)
        val rectBottom = offsetY + ((cropRect.y + cropRect.height) * scale)

        // 框外四侧遮罩
        val scrimColor = scrim.copy(alpha = 0.6f)
        drawRect(color = scrimColor, topLeft = Offset.Zero, size = Size(rectLeft, canvasSize.height))
        drawRect(
            color = scrimColor,
            topLeft = Offset(rectRight, 0f),
            size = Size(canvasSize.width - rectRight, canvasSize.height)
        )
        drawRect(
            color = scrimColor,
            topLeft = Offset(rectLeft, 0f),
            size = Size(rectRight - rectLeft, rectTop)
        )
        drawRect(
            color = scrimColor,
            topLeft = Offset(rectLeft, rectBottom),
            size = Size(rectRight - rectLeft, canvasSize.height - rectBottom)
        )

        // 裁剪框边框
        drawRect(
            color = border,
            topLeft = Offset(rectLeft, rectTop),
            size = Size(rectRight - rectLeft, rectBottom - rectTop),
            style = Stroke(width = with(density) { 2.dp.toPx() })
        )
    }
}

@Composable
private fun PreviewThumbnail(
    bitmap: ImageBitmap,
    rectState: MutableState<PosterCropper.Rect>,
    aspect: Float
) {
    // 裁剪区域实时预览:按裁剪框原图坐标截取显示(在 draw 内读状态,仅重绘);
    // 宽高比与当前 aspect 同步,比例调整时预览形状随之变化
    Canvas(modifier = Modifier.width(72.dp).aspectRatio(1f / aspect)) {
        val cropRect = rectState.value
        drawImage(
            image = bitmap,
            srcOffset = IntOffset(cropRect.x, cropRect.y),
            srcSize = IntSize(cropRect.width, cropRect.height),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.toInt(), size.height.toInt())
        )
    }
}

private fun loadBitmap(file: File): ImageBitmap? = try {
    Image.makeFromEncoded(file.readBytes()).toComposeImageBitmap()
} catch (_: Exception) {
    null
}

@Preview
@Composable
private fun PosterCropDialogPreview() {
    val tempFile = File.createTempFile("preview", ".jpg").apply {
        val img = BufferedImage(800, 533, BufferedImage.TYPE_INT_RGB)
        ImageIO.write(img, "jpg", this)
        deleteOnExit()
    }
    CompositionLocalProvider(LocalTranslations provides TranslationZh()) {
        JavScraperTheme {
            PosterCropDialog(
                videoNumber = "SONE-001",
                sourceFile = tempFile,
                posterFile = File(tempFile.parentFile, "poster.jpg"),
                onDismiss = {}
            )
        }
    }
}
