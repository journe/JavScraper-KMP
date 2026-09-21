package javscraper.io.image

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import java.awt.image.BufferedImage
import java.nio.file.StandardCopyOption
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import java.awt.Color
import javscraper.models.WatermarkMark
import javscraper.models.WatermarkOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PosterCropperTest {

    // --- initialCropRect ---

    @Test
    fun `initial crop rect on landscape image sticks to right edge with 1_5 aspect`() {
        // 800x533 横图:aspect 533/800≈0.67 < 1.5 → 高度占满,宽度=533/1.5≈355,贴右
        val rect = PosterCropper.initialCropRect(800, 533)
        assertEquals(533, rect.height)
        assertEquals(0, rect.y)
        assertEquals((533 / PosterCropper.POSTER_ASPECT).toInt(), rect.width)
        assertEquals(800 - rect.width, rect.x)
    }

    @Test
    fun `initial crop rect on portrait image centers vertically`() {
        // 600x1200 高图:aspect 2 > 1.5 → 宽度占满,高度=600*1.5=900,垂直居中
        val rect = PosterCropper.initialCropRect(600, 1200)
        assertEquals(600, rect.width)
        assertEquals(0, rect.x)
        assertEquals((600 * PosterCropper.POSTER_ASPECT).toInt(), rect.height)
        assertEquals((1200 - rect.height) / 2, rect.y)
    }

    @Test
    fun `initial crop rect never exceeds image bounds`() {
        val cases = listOf(
            800 to 533, 600 to 1200, 1000 to 1000, 100 to 100,
            1600 to 900, 400 to 2000
        )
        for ((w, h) in cases) {
            val rect = PosterCropper.initialCropRect(w, h)
            assertTrue(rect.x >= 0 && rect.y >= 0, "rect origin must be non-negative for ${w}x$h")
            assertTrue(rect.x + rect.width <= w, "rect right must be within width for ${w}x$h")
            assertTrue(rect.y + rect.height <= h, "rect bottom must be within height for ${w}x$h")
        }
    }

    // --- initialCropRect(aspect) ---

    @Test
    fun `initial crop rect honors custom aspect on landscape image`() {
        // aspect=1.0(正方形):横图框宽=高=533,贴右
        val square = PosterCropper.initialCropRect(800, 533, aspect = 1.0f)
        assertEquals(533, square.width)
        assertEquals(533, square.height)
        assertEquals(800 - 533, square.x)

        // aspect=2.0(细长竖版):横图框宽=533/2≈266,贴右
        val tall = PosterCropper.initialCropRect(800, 533, aspect = 2.0f)
        assertEquals((533 / 2f).toInt(), tall.width)
        assertEquals(533, tall.height)
        assertEquals(800 - tall.width, tall.x)
    }

    @Test
    fun `initial crop rect honors custom aspect on portrait image`() {
        // aspect=1.0:600x1200 高图 aspect 2>1 → 框占满宽 600,高 600,垂直居中
        val square = PosterCropper.initialCropRect(600, 1200, aspect = 1.0f)
        assertEquals(600, square.width)
        assertEquals(600, square.height)
        assertEquals(300, square.y)
    }

    // --- resizeRectToAspect ---

    @Test
    fun `resize rect keeps right edge anchor on landscape image`() {
        // 800x533 横图,初始框 355x533 贴右(x=445,右缘 800);改 aspect=1.0 → 框 533x533,
        // 右缘锚定 800 → x = 800-533 = 267
        val current = PosterCropper.initialCropRect(800, 533)
        val resized = PosterCropper.resizeRectToAspect(current, 1.0f, 800, 533)

        assertEquals(533, resized.width)
        assertEquals(533, resized.height)
        assertEquals(0, resized.y)
        val oldRightEdge = current.x + current.width
        assertEquals(oldRightEdge - resized.width, resized.x)
        assertEquals(800 - 533, resized.x)
    }

    @Test
    fun `resize rect keeps bottom edge anchor on portrait image`() {
        // 600x1200 高图,初始框 600x900 居中(y=150,底缘 1050);改 aspect=2.0 → 框 600x1200,
        // 底缘锚定 1050 但新框高 1200 → y = 1050-1200 = -150 → 钳制到 0(占满)
        val current = PosterCropper.initialCropRect(600, 1200)
        val resized = PosterCropper.resizeRectToAspect(current, 2.0f, 600, 1200)

        assertEquals(600, resized.width)
        assertEquals(1200, resized.height)
        assertEquals(0, resized.x)
        assertEquals(0, resized.y)
    }

    @Test
    fun `resize rect clamps anchor when new rect would overflow`() {
        // 800x533 横图,框拖到最左(x=0,宽355,右缘 355);改 aspect=1.0 → 新框宽533,
        // 右缘锚定 355 → x = 355-533 = -178 → 钳制到 0
        val current = PosterCropper.Rect(0, 0, 355, 533)
        val resized = PosterCropper.resizeRectToAspect(current, 1.0f, 800, 533)

        assertEquals(0, resized.x)
        assertTrue(resized.x + resized.width <= 800)
    }

    @Test
    fun `resize rect widens leftward keeping right edge on landscape image`() {
        // 拖到中间的框(x=300,宽355,右缘 655);改 aspect=1.333 → 框宽≈400,
        // 右缘锚定 655 → x = 655-400 = 255
        val current = PosterCropper.Rect(300, 0, 355, 533)
        val resized = PosterCropper.resizeRectToAspect(current, 1.333f, 800, 533)

        val oldRightEdge = current.x + current.width
        assertEquals(oldRightEdge, resized.x + resized.width)
        assertEquals((533 / 1.333f).toInt(), resized.width)
    }

    // --- translateRect ---

    @Test
    fun `translate rect moves within bounds`() {
        val rect = PosterCropper.Rect(x = 100, y = 0, width = 200, height = 400)
        val moved = PosterCropper.translateRect(rect, 50, 0, 800, 533)
        assertEquals(150, moved.x)
        assertEquals(200, moved.width)
    }

    @Test
    fun `translate rect clamps at both edges`() {
        val rect = PosterCropper.Rect(x = 100, y = 0, width = 200, height = 400)
        val leftClamped = PosterCropper.translateRect(rect, -500, 0, 800, 533)
        assertEquals(0, leftClamped.x)
        val rightClamped = PosterCropper.translateRect(rect, 500, 0, 800, 533)
        assertEquals(800 - 200, rightClamped.x)
    }

    @Test
    fun `translate rect clamps vertically on portrait image`() {
        val rect = PosterCropper.Rect(x = 0, y = 100, width = 300, height = 450)
        val clamped = PosterCropper.translateRect(rect, 0, -999, 300, 600)
        assertEquals(0, clamped.y)
        val bottom = PosterCropper.translateRect(rect, 0, 999, 300, 600)
        assertEquals(600 - 450, bottom.y)
    }

    // --- clampRect ---

    @Test
    fun `clamp rect clamps oversized input`() {
        val rect = PosterCropper.Rect(x = -10, y = -10, width = 99999, height = 99999)
        val clamped = PosterCropper.clampRect(rect, 800, 533)
        assertEquals(800, clamped.width)
        assertEquals(533, clamped.height)
        assertEquals(0, clamped.x)
        assertEquals(0, clamped.y)
    }

    // --- cropToFile ---

    @Test
    fun `crop to file decodes webp source and logs key steps`() {
        val dir = Files.createTempDirectory("javscraper-crop-webp-")
        val logger = LoggerFactory.getLogger(PosterCropper::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>()
        appender.start()
        logger.addAppender(appender)
        try {
            val source = dir.resolve("fanart.webp").toFile()
            javaClass.getResourceAsStream("/watermark/4k.webp")!!.use { input ->
                Files.copy(input, source.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            val sourceImage = assertNotNull(ImageIO.read(source), "bundled webp should be decodable")
            val dest = dir.resolve("poster.jpg").toFile()
            val rect = PosterCropper.initialCropRect(sourceImage.width, sourceImage.height)

            val ok = PosterCropper.cropToFile(source, dest, rect)

            assertTrue(ok, "webp crop should succeed")
            val result = assertNotNull(ImageIO.read(dest), "cropped jpeg should be readable")
            assertEquals(rect.width, result.width)
            assertEquals(rect.height, result.height)
            val messages = appender.list.map { it.formattedMessage }
            listOf(
                "Poster crop start",
                "Poster crop decoded",
                "Poster crop JPEG written",
                "Poster crop completed"
            ).forEach { expected ->
                assertTrue(messages.any { it.contains(expected) }, "missing log: $expected, logs=$messages")
            }
        } finally {
            logger.detachAppender(appender)
            appender.stop()
            cleanup(dir)
        }
    }

    @Test
    fun `crop to file writes cropped jpeg with expected size`() {
        val dir = Files.createTempDirectory("javscraper-crop-")
        try {
            val source = writeTestImage(dir.resolve("fanart.jpg").toFile(), 800, 533)
            val dest = dir.resolve("poster.jpg").toFile()
            val rect = PosterCropper.initialCropRect(800, 533)

            val ok = PosterCropper.cropToFile(source, dest, rect)

            assertTrue(ok, "crop should succeed")
            assertTrue(dest.isFile, "poster.jpg should be written")
            val result = ImageIO.read(dest)
            assertNotNull(result)
            assertEquals(rect.width, result.width)
            assertEquals(rect.height, result.height)
            assertFalse(File(dir.toFile(), "poster.jpg.tmp.jpg").isFile, "temp file should be cleaned")
        } finally {
            cleanup(dir)
        }
    }

    @Test
    fun `crop to file preserves existing poster on invalid source`() {
        val dir = Files.createTempDirectory("javscraper-crop-")
        try {
            val source = dir.resolve("not-an-image.jpg").toFile()
            source.writeBytes(byteArrayOf(1, 2, 3))
            val dest = writeTestImage(dir.resolve("poster.jpg").toFile(), 10, 10)
            val before = dest.readBytes()

            val ok = PosterCropper.cropToFile(source, dest, PosterCropper.Rect(0, 0, 5, 5))

            assertFalse(ok, "crop should fail on unreadable source")
            assertTrue(dest.readBytes().contentEquals(before), "existing poster must stay intact")
        } finally {
            cleanup(dir)
        }
    }

    @Test
    fun `crop to file clamps out-of-bounds rect`() {
        val dir = Files.createTempDirectory("javscraper-crop-")
        try {
            val source = writeTestImage(dir.resolve("fanart.jpg").toFile(), 400, 300)
            val dest = dir.resolve("poster.jpg").toFile()
            val outOfBounds = PosterCropper.Rect(x = -50, y = -50, width = 500, height = 500)

            val ok = PosterCropper.cropToFile(source, dest, outOfBounds)

            assertTrue(ok, "clamped crop should succeed")
            val result = ImageIO.read(dest)
            assertNotNull(result)
            assertEquals(400, result.width)
            assertEquals(300, result.height)
        } finally {
            cleanup(dir)
        }
    }

    @Test
    fun `crop to file applies watermark to cropped poster`() {
        val dir = Files.createTempDirectory("javscraper-crop-")
        try {
            val source = writeTestImage(dir.resolve("fanart.jpg").toFile(), 800, 533)
            val dest = dir.resolve("poster.jpg").toFile()
            val rect = PosterCropper.initialCropRect(800, 533)
            val options = WatermarkOptions(listOf(WatermarkMark.HD_4K), size = 5)

            val ok = PosterCropper.cropToFile(source, dest, rect, options)

            assertTrue(ok, "watermarked crop should succeed")
            val result = ImageIO.read(dest)
            assertNotNull(result)
            assertEquals(rect.width, result.width)
            val placement = WatermarkRenderer.layout(options, rect.width, rect.height).first().rect
            val hasMarkPixel = (placement.x until placement.x + placement.width).step(2).any { x ->
                (placement.y until placement.y + placement.height).step(2).any { y ->
                    result.getRGB(x, y) != Color.BLACK.rgb
                }
            }
            assertTrue(hasMarkPixel, "watermark pixels should be drawn on the cropped poster")
            assertFalse(File(dir.toFile(), "poster.jpg.tmp.jpg").isFile, "temp file should be cleaned")
        } finally {
            cleanup(dir)
        }
    }

    @Test
    fun `crop to file writes higher quality jpeg than imageio default`() {
        val dir = Files.createTempDirectory("javscraper-crop-")
        try {
            val source = writeGradientImage(dir.resolve("fanart.jpg").toFile(), 400, 300)
            val dest = dir.resolve("poster.jpg").toFile()
            val plain = dir.resolve("plain.jpg").toFile()
            assertTrue(ImageIO.write(gradientImage(400, 300), "jpg", plain), "plain jpeg write should succeed")

            val ok = PosterCropper.cropToFile(source, dest, PosterCropper.Rect(0, 0, 400, 300))

            assertTrue(ok, "crop should succeed")
            assertTrue(
                dest.length() > plain.length(),
                "expected high-quality output (${dest.length()} B) to exceed default quality (${plain.length()} B)"
            )
        } finally {
            cleanup(dir)
        }
    }

    // --- writeFullImage ---

    @Test
    fun `write full image saves original poster without cropping`() {
        val dir = Files.createTempDirectory("javscraper-full-")
        try {
            // 600x1200 竖图:若被裁剪,输出尺寸必然不等于原图
            val poster = writeTestImage(dir.resolve("poster.jpg").toFile(), 600, 1200)

            val ok = PosterCropper.writeFullImage(poster, poster)

            assertTrue(ok, "full-image write should succeed")
            val result = assertNotNull(ImageIO.read(poster))
            assertEquals(600, result.width)
            assertEquals(1200, result.height)
            assertFalse(File(dir.toFile(), "poster.jpg.tmp.jpg").isFile, "temp file should be cleaned")
        } finally {
            cleanup(dir)
        }
    }

    @Test
    fun `write full image applies watermark to original poster`() {
        val dir = Files.createTempDirectory("javscraper-full-")
        try {
            val poster = writeTestImage(dir.resolve("poster.jpg").toFile(), 600, 900)
            val options = WatermarkOptions(listOf(WatermarkMark.HD_4K), size = 5)

            val ok = PosterCropper.writeFullImage(poster, poster, options)

            assertTrue(ok, "watermarked full-image write should succeed")
            val result = assertNotNull(ImageIO.read(poster))
            assertEquals(600, result.width)
            val placement = WatermarkRenderer.layout(options, 600, 900).first().rect
            val hasMarkPixel = (placement.x until placement.x + placement.width).step(2).any { x ->
                (placement.y until placement.y + placement.height).step(2).any { y ->
                    result.getRGB(x, y) != Color.BLACK.rgb
                }
            }
            assertTrue(hasMarkPixel, "watermark pixels should be drawn on the original poster")
        } finally {
            cleanup(dir)
        }
    }

    @Test
    fun `write full image preserves existing poster on invalid source`() {
        val dir = Files.createTempDirectory("javscraper-full-")
        try {
            val source = dir.resolve("not-an-image.jpg").toFile()
            source.writeBytes(byteArrayOf(1, 2, 3))
            val dest = writeTestImage(dir.resolve("poster.jpg").toFile(), 10, 10)
            val before = dest.readBytes()

            val ok = PosterCropper.writeFullImage(source, dest)

            assertFalse(ok, "full-image write should fail on unreadable source")
            assertTrue(dest.readBytes().contentEquals(before), "existing poster must stay intact")
        } finally {
            cleanup(dir)
        }
    }

    // --- helpers (appended) ---
    // --- helpers ---

    private fun writeTestImage(file: File, width: Int, height: Int): File {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        assertTrue(ImageIO.write(image, "jpg", file), "test image write should succeed")
        return file
    }

    private fun writeGradientImage(file: File, width: Int, height: Int): File {
        assertTrue(ImageIO.write(gradientImage(width, height), "jpg", file), "test image write should succeed")
        return file
    }

    private fun gradientImage(width: Int, height: Int): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val red = x * 255 / width
                val green = y * 255 / height
                image.setRGB(x, y, (red shl 16) or (green shl 8) or 0x40)
            }
        }
        return image
    }

    private fun cleanup(dir: Path) {
        Files.walk(dir).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
    }
}
