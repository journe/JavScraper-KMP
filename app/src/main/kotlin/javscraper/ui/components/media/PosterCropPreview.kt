package javscraper.ui.components.media

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
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
import javscraper.ui.theme.JavScraperTheme
import org.jetbrains.skia.Image
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

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
internal fun CropCanvas(
    bitmap: ImageBitmap,
    rectState: MutableState<PosterCropper.Rect>,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val scrim = MaterialTheme.colorScheme.scrim
    val border = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier
            // aspectRatio 在「宽≤420、高受父约束」内取最大等比尺寸。
            .widthIn(max = 540.dp)
            .aspectRatio(bitmap.width.toFloat() / bitmap.height)
            .clip(RectangleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(bitmap) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val scale = fitScale(
                        size.width.toFloat(), size.height.toFloat(), bitmap.width, bitmap.height
                    )
                    if (scale > 0f) {
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
        drawRect(
            color = border,
            topLeft = Offset(rectLeft, rectTop),
            size = Size(rectRight - rectLeft, rectBottom - rectTop),
            style = Stroke(width = with(density) { 2.dp.toPx() })
        )
    }
}

@Composable
internal fun PreviewThumbnail(
    bitmap: ImageBitmap,
    rectState: MutableState<PosterCropper.Rect>,
    aspect: Float,
    watermarkOptions: WatermarkOptions?
) {
    val watermarkBitmaps = rememberWatermarkBitmaps(watermarkOptions?.marks.orEmpty())
    Card(
        modifier = Modifier.height(540.dp).aspectRatio(1f / aspect),
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

internal fun loadBitmap(file: File): ImageBitmap? = try {
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
