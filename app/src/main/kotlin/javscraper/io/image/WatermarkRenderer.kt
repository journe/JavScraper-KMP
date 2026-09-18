package javscraper.io.image

import javscraper.models.WatermarkMark
import javscraper.models.WatermarkOptions
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

/** 单个水印在目标图上的绘制区域。 */
data class WatermarkPlacement(val mark: WatermarkMark, val rect: PosterCropper.Rect)

/**
 * 海报水印渲染:布局规则对齐 mdcx 默认 not_fixed 配置——
 * 4K/8K 固定右下角,其余水印从左上角起顺时针排布并跳过已占用角。
 */
object WatermarkRenderer {

    /** 渲染顺序:高清 → 字幕 → 马赛克,与 mdcx mark_list 顺序一致。 */
    private val renderOrder = listOf(
        WatermarkMark.HD_4K,
        WatermarkMark.HD_8K,
        WatermarkMark.SUB,
        WatermarkMark.CENSORED,
        WatermarkMark.UMR,
        WatermarkMark.LEAK,
        WatermarkMark.UNCENSORED
    )

    /** 顺时针四角序:左上(0) → 右上(1) → 右下(2) → 左下(3)。 */
    private const val HD_START_SLOT = 2
    private const val OTHER_START_SLOT = 0
    private const val SLOT_COUNT = 4

    private val markImageCache = ConcurrentHashMap<WatermarkMark, Optional<BufferedImage>>()

    fun render(image: BufferedImage, options: WatermarkOptions): BufferedImage =
        render(image, options, ::loadMarkImage)

    internal fun render(
        image: BufferedImage,
        options: WatermarkOptions,
        loader: (WatermarkMark) -> BufferedImage?
    ): BufferedImage {
        val markImages = options.marks
            .mapNotNull { mark -> loader(mark)?.let { mark to it } }
            .toMap()
        val drawableOptions = options.copy(marks = markImages.keys.toList())
        val placements = layout(drawableOptions, image.width, image.height) { markImages[it] }

        val output = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
        val graphics = output.createGraphics()
        try {
            graphics.drawImage(image, 0, 0, null)
            placements.forEach { placement ->
                drawMark(graphics, markImages.getValue(placement.mark), placement.rect)
            }
        } finally {
            graphics.dispose()
        }
        return output
    }

    fun layout(
        options: WatermarkOptions,
        imageWidth: Int,
        imageHeight: Int
    ): List<WatermarkPlacement> = layout(options, imageWidth, imageHeight, ::loadMarkImage)

    internal fun layout(
        options: WatermarkOptions,
        imageWidth: Int,
        imageHeight: Int,
        loader: (WatermarkMark) -> BufferedImage?
    ): List<WatermarkPlacement> {
        if (options.marks.isEmpty() || imageWidth <= 0 || imageHeight <= 0) return emptyList()

        val size = WatermarkOptions.normalizedSize(options.size)
        val markHeight = (imageHeight * size / 40).coerceIn(1, imageHeight)
        val occupiedSlots = mutableSetOf<Int>()
        val placements = mutableListOf<WatermarkPlacement>()
        var otherSlot = OTHER_START_SLOT

        for (mark in renderOrder) {
            if (mark !in options.marks) continue
            val asset = loader(mark) ?: continue
            val markWidth = (markHeight * asset.width / asset.height).coerceIn(1, imageWidth)
            val slot = if (mark.isHd) {
                HD_START_SLOT
            } else {
                nextFreeSlot(otherSlot, occupiedSlots).also { otherSlot = (it + 1) % SLOT_COUNT }
            }
            occupiedSlots += slot
            placements += WatermarkPlacement(
                mark = mark,
                rect = PosterCropper.Rect(
                    x = if (slot == 1 || slot == 2) imageWidth - markWidth else 0,
                    y = if (slot == 2 || slot == 3) imageHeight - markHeight else 0,
                    width = markWidth,
                    height = markHeight
                )
            )
        }
        return placements
    }

    private fun nextFreeSlot(startSlot: Int, occupiedSlots: Set<Int>): Int {
        var slot = startSlot
        while (slot in occupiedSlots) slot = (slot + 1) % SLOT_COUNT
        return slot
    }

    private fun drawMark(graphics: Graphics2D, markImage: BufferedImage, rect: PosterCropper.Rect) {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.drawImage(markImage, rect.x, rect.y, rect.width, rect.height, null)
    }

    private fun loadMarkImage(mark: WatermarkMark): BufferedImage? =
        markImageCache.computeIfAbsent(mark) {
            Optional.ofNullable(
                runCatching {
                    WatermarkRenderer::class.java.getResourceAsStream("/watermark/${mark.assetName}")
                        ?.use(ImageIO::read)
                }.getOrNull()
            )
        }.orElse(null)
}