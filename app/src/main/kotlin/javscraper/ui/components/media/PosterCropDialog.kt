package javscraper.ui.components.media

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import javscraper.i18n.LocalTranslations
import javscraper.i18n.TranslationZh
import javscraper.io.image.PosterCropper
import javscraper.io.image.WatermarkRenderer
import javscraper.models.Video
import javscraper.models.WatermarkOptions
import javscraper.settings.AppSettings
import javscraper.settings.SettingsManager
import javscraper.ui.theme.JavScraperTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

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

    val bitmap = remember(sourceFile) { loadBitmap(sourceFile) }
    if (bitmap == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(translations.cropTitle(video.number)) },
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
    val log = mu.KotlinLogging.logger {}

    AlertDialog(
        modifier = Modifier.requiredWidthIn(min = 780.dp, max = 1080.dp),
        onDismissRequest = onDismiss,
        title = { Text(translations.cropTitle(video.number)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        CropCanvas(bitmap, cropRectState)
                        Column {
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
                        PosterWatermarkControls(
                            state = watermarkState,
                            size = watermarkSize,
                            onStateChange = { newState ->
                                watermarkState = newState
                                if (SettingsManager.get().posterWatermarkEnabled != newState.enabled) {
                                    SettingsManager.update {
                                        it.copy(posterWatermarkEnabled = newState.enabled)
                                    }
                                }
                            },
                            onSizeChange = { watermarkSize = it },
                            onSizeChangeFinished = {
                                SettingsManager.update { it.copy(posterWatermarkSize = watermarkSize) }
                            }
                        )
                    }
                    PreviewThumbnail(
                        bitmap,
                        cropRectState,
                        aspect,
                        watermarkState.toOptions(watermarkSize)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        val watermarkOptions = watermarkState.toOptions(watermarkSize)
                        val ok = withContext(Dispatchers.IO) {
                            log.info { "PosterCropper.cropToFile" }
                            PosterCropper.cropToFile(
                                sourceFile,
                                posterFile,
                                cropRectState.value,
                                watermarkOptions
                            )
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
private fun fitScale(
    canvasWidth: Float,
    canvasHeight: Float,
    imageWidth: Int,
    imageHeight: Int
): Float {
    if ((imageWidth == 0) || (imageHeight == 0)) return 1f
    return minOf(canvasWidth / imageWidth, canvasHeight / imageHeight)
}

@Composable
private fun CropCanvas(
    bitmap: ImageBitmap,
    rectState: MutableState<PosterCropper.Rect>,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val scrim = MaterialTheme.colorScheme.scrim
    val border = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier
            // 不用 fillMaxWidth:它会把宽度钉死为父宽,aspectRatio 只能靠高度满足比例,
            // 竖图(如 380x530)时所需高度 = 父宽/0.717 会突破 420 上限,约束冲突导致布局错乱。
            // 正确做法:aspectRatio 在「宽≤父宽、高≤420」双约束内自由取最大等比尺寸,
            // 画布自身宽=图宽×scale;wrapContent 让画布在父宽内水平居中。
            .heightIn(max = 420.dp)
            .aspectRatio(bitmap.width.toFloat() / bitmap.height)
            .clip(RectangleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
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
        drawRect(
            color = scrimColor,
            topLeft = Offset.Zero,
            size = Size(rectLeft, canvasSize.height)
        )
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
    aspect: Float,
    watermarkOptions: WatermarkOptions?
) {
    val watermarkBitmaps = rememberWatermarkBitmaps(watermarkOptions?.marks.orEmpty())
    // 裁剪区域实时预览:按裁剪框原图坐标截取显示(在 draw 内读状态,仅重绘);
    // 高度固定,宽度随 aspect 变化(高/宽=aspect → 宽=高/aspect)
    Card(
        modifier = Modifier.height(412.dp).aspectRatio(1f / aspect),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cropRect = rectState.value
            drawImage(
                image = bitmap,
                srcOffset = IntOffset(cropRect.x, cropRect.y),
                srcSize = IntSize(cropRect.width, cropRect.height),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.toInt(), size.height.toInt())
            )
            watermarkOptions?.let { options ->
                WatermarkRenderer
                    .layout(options, size.width.toInt(), size.height.toInt())
                    .forEach { placement ->
                        val markBitmap = watermarkBitmaps[placement.mark] ?: return@forEach
                        drawImage(
                            image = markBitmap,
                            dstOffset = IntOffset(placement.rect.x, placement.rect.y),
                            dstSize = IntSize(placement.rect.width, placement.rect.height)
                        )
                    }
            }
        }
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
                video = Video(number = "SONE-001"),
                sourceFile = tempFile,
                posterFile = File(tempFile.parentFile, "poster.jpg"),
                onDismiss = {}
            )
        }
    }
}
