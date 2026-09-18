package javscraper.io.image

import javscraper.models.WatermarkMark
import javscraper.models.WatermarkOptions
import java.awt.Color
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class WatermarkRendererTest {

    @Test
    fun `layout scales mark height by image height and size`() {
        val options = WatermarkOptions(listOf(WatermarkMark.HD_4K), size = 5)

        val placements = WatermarkRenderer.layout(options, imageWidth = 400, imageHeight = 800)

        assertEquals(1, placements.size)
        val rect = placements.first().rect
        assertEquals(100, rect.height)
        val asset = ImageIO.read(javaClass.getResourceAsStream("/watermark/4k.webp"))
        assertEquals(100 * asset.width / asset.height, rect.width)
    }

    @Test
    fun `image io decodes every bundled webp asset`() {
        WatermarkMark.entries.forEach { mark ->
            val asset = ImageIO.read(javaClass.getResourceAsStream("/watermark/${mark.assetName}"))

            assertTrue(asset != null, "${mark.assetName} should be decodable")
            assertTrue(asset.width > 0 && asset.height > 0, "${mark.assetName} should have dimensions")
        }
    }

    @Test
    fun `layout clamps oversized size to max`() {
        val options = WatermarkOptions(listOf(WatermarkMark.SUB), size = 99)

        val placements = WatermarkRenderer.layout(options, imageWidth = 400, imageHeight = 800)

        assertEquals(200, placements.first().rect.height)
    }

    @Test
    fun `layout places hd bottom right and others clockwise from top left`() {
        val options = WatermarkOptions(
            listOf(WatermarkMark.UNCENSORED, WatermarkMark.SUB, WatermarkMark.HD_4K),
            size = 5
        )

        val byMark = WatermarkRenderer.layout(options, imageWidth = 400, imageHeight = 800)
            .associateBy { it.mark }

        val hd = byMark.getValue(WatermarkMark.HD_4K).rect
        assertEquals(400 - hd.width, hd.x)
        assertEquals(800 - hd.height, hd.y)

        val sub = byMark.getValue(WatermarkMark.SUB).rect
        assertEquals(0, sub.x)
        assertEquals(0, sub.y)

        val uncensored = byMark.getValue(WatermarkMark.UNCENSORED).rect
        assertEquals(400 - uncensored.width, uncensored.x)
        assertEquals(0, uncensored.y)
    }

    @Test
    fun `layout returns empty list without marks`() {
        val placements = WatermarkRenderer.layout(WatermarkOptions(), imageWidth = 400, imageHeight = 800)

        assertTrue(placements.isEmpty())
    }

    @Test
    fun `render draws watermark pixels into placement and keeps input intact`() {
        val image = whiteImage(400, 800)
        val options = WatermarkOptions(listOf(WatermarkMark.HD_4K), size = 5)

        val result = WatermarkRenderer.render(image, options)

        assertEquals(400, result.width)
        assertEquals(800, result.height)
        val rect = WatermarkRenderer.layout(options, imageWidth = 400, imageHeight = 800)
            .first().rect
        val hasDrawnPixel = (rect.x until rect.x + rect.width).step(2).any { x ->
            (rect.y until rect.y + rect.height).step(2).any { y ->
                result.getRGB(x, y) != Color.WHITE.rgb
            }
        }
        assertTrue(hasDrawnPixel, "watermark pixels should be visible inside its placement")
        assertEquals(Color.WHITE.rgb, image.getRGB(rect.x + rect.width / 2, rect.y + rect.height / 2))
        assertEquals(Color.WHITE.rgb, result.getRGB(200, 400))
    }

    @Test
    fun `render skips missing assets without failing`() {
        val image = whiteImage(400, 800)
        val options = WatermarkOptions(listOf(WatermarkMark.HD_4K), size = 5)

        val result = WatermarkRenderer.render(image, options) { _ -> null }

        assertEquals(400, result.width)
        assertEquals(800, result.height)
        assertTrue(
            (0 until 400).step(8).all { x -> (0 until 800).step(8).all { y -> result.getRGB(x, y) == Color.WHITE.rgb } },
            "output should stay unchanged when every asset is missing"
        )
    }

    private fun whiteImage(width: Int, height: Int): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.color = Color.WHITE
        graphics.fillRect(0, 0, width, height)
        graphics.dispose()
        return image
    }
}
