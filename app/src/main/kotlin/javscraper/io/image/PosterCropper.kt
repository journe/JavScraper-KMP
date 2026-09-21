package javscraper.io.image

import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import javscraper.models.WatermarkOptions
import mu.KotlinLogging
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier
import javax.imageio.ImageWriteParam
import javax.imageio.metadata.IIOMetadata
import javax.imageio.stream.ImageOutputStream

/**
 * 封面裁剪:从横版封面(fanart.jpg,缺省回退 poster.jpg)裁出竖版 poster。
 * 交互逻辑对齐 mdcx 的 cut_window:裁剪框高宽比固定 1.5(与 mdcx 默认
 * pic_h_w_ratio 一致),横图上只允许水平平移,高图上只允许垂直平移。
 */
object PosterCropper {
    private val log = KotlinLogging.logger {}

    /** 竖版海报默认高宽比(高/宽),对应 2:3 海报标准,与 mdcx 默认一致。 */
    const val POSTER_ASPECT = 1.5f

    /** 允许用户调整的高宽比范围(高/宽)。 */
    const val MIN_ASPECT = 1.0f
    const val MAX_ASPECT = 2.0f

    data class Rect(val x: Int, val y: Int, val width: Int, val height: Int)

    /**
     * 计算初始裁剪框(原图像素坐标):比例固定为 [aspect],位置贴右侧/垂直居中,
     * 与 mdcx showimage 的默认行为一致(横图裁右侧,高图垂直居中)。
     */
    fun initialCropRect(imageWidth: Int, imageHeight: Int, aspect: Float = POSTER_ASPECT): Rect {
        require(imageWidth > 0 && imageHeight > 0) { "image size must be positive" }
        require(aspect > 0f) { "aspect must be positive" }
        val imageAspect = imageHeight.toFloat() / imageWidth
        return if (imageAspect <= aspect) {
            // 横图:裁剪框占满高度,宽度按比例,水平位置默认贴右
            val h = imageHeight
            val w = (h / aspect).toInt().coerceAtMost(imageWidth)
            Rect(imageWidth - w, 0, w, h)
        } else {
            // 高图:裁剪框占满宽度,高度按比例,垂直居中
            val w = imageWidth
            val h = (w * aspect).toInt().coerceAtMost(imageHeight)
            Rect(0, (imageHeight - h) / 2, w, h)
        }
    }

    /**
     * 按新高宽比重算裁剪框:尺寸沿用 [initialCropRect] 的最大化规则,
     * 位置以 [current] 的右缘为锚点(高图时以底缘为锚点)——JAV 封面正面
     * 通常贴原图右缘,锚定右缘可避免调比例时裁剪内容漂移。
     */
    fun resizeRectToAspect(
        current: Rect,
        aspect: Float,
        imageWidth: Int,
        imageHeight: Int
    ): Rect {
        require(aspect > 0f) { "aspect must be positive" }
        val sized = initialCropRect(imageWidth, imageHeight, aspect)
        val imageAspect = imageHeight.toFloat() / imageWidth
        return if (imageAspect <= aspect) {
            // 横向模式:y 占满不变,右缘对齐旧框右缘
            val rightEdge = current.x + current.width
            val x = (rightEdge - sized.width)
                .coerceIn(0, imageWidth - sized.width)
            Rect(x, 0, sized.width, sized.height)
        } else {
            // 纵向模式:x 占满不变,底缘对齐旧框底缘
            val bottomEdge = current.y + current.height
            val y = (bottomEdge - sized.height)
                .coerceIn(0, imageHeight - sized.height)
            Rect(0, y, sized.width, sized.height)
        }
    }

    /**
     * 平移裁剪框并钳制在原图范围内(移动轴与 [initialCropRect] 相同维度)。
     * dx/dy 为原图像素位移,正值向右/向下。
     */
    fun translateRect(rect: Rect, dx: Int, dy: Int, imageWidth: Int, imageHeight: Int): Rect =
        Rect(
            x = (rect.x + dx).coerceIn(0, imageWidth - rect.width),
            y = (rect.y + dy).coerceIn(0, imageHeight - rect.height),
            width = rect.width,
            height = rect.height
        )

    /** 将裁剪框钳制到有效范围(防御越界输入)。 */
    fun clampRect(rect: Rect, imageWidth: Int, imageHeight: Int): Rect {
        val w = rect.width.coerceIn(1, imageWidth)
        val h = rect.height.coerceIn(1, imageHeight)
        return Rect(
            x = rect.x.coerceIn(0, imageWidth - w),
            y = rect.y.coerceIn(0, imageHeight - h),
            width = w,
            height = h
        )
    }

    /**
     * 执行裁剪并写入 [destFile](JPEG,quality 95,对齐 mdcx 的 quality=95)。
     * 返回写入是否成功;失败时 [destFile] 保持原状(先写临时文件再原子替换)。
     */
    fun cropToFile(
        sourceFile: File,
        destFile: File,
        rect: Rect,
        watermark: WatermarkOptions? = null
    ): Boolean {
        log.info {
            "Poster crop start: source=${sourceFile.absolutePath}, sourceBytes=${sourceFile.length()}, " +
                "destination=${destFile.absolutePath}, rect=$rect, " +
                "watermarkMarks=${watermark?.marks?.size ?: 0}"
        }
        val image = decodeImage(sourceFile) ?: return false

        log.info {
            "Poster crop decoded: source=${sourceFile.absolutePath}, size=${image.width}x${image.height}, " +
                "type=${image.type}, readers=${imageReaderClasses(sourceFile)}"
        }
        val clamped = clampRect(rect, image.width, image.height)
        val cropped = BufferedImage(clamped.width, clamped.height, BufferedImage.TYPE_INT_RGB)
        val graphics = cropped.createGraphics()
        try {
            graphics.drawImage(
                image,
                0, 0, clamped.width, clamped.height,
                clamped.x, clamped.y, clamped.x + clamped.width, clamped.y + clamped.height,
                null
            )
        } finally {
            graphics.dispose()
        }
        val output = applyWatermark(cropped, watermark)
        return writeImageToFile(sourceFile, destFile, output, "crop")
    }

    /**
     * 直接以整图（不裁剪）写入 [destFile]，可选叠加水印。
     * 用于“使用原图”模式：把当前 poster 原图重新保存（可带水印），
     * 与 [cropToFile] 共享同一套 JPEG/原子替换写盘逻辑。
     */
    fun writeFullImage(
        sourceFile: File,
        destFile: File,
        watermark: WatermarkOptions? = null
    ): Boolean {
        log.info {
            "Poster full-image write start: source=${sourceFile.absolutePath}, " +
                "sourceBytes=${sourceFile.length()}, destination=${destFile.absolutePath}, " +
                "watermarkMarks=${watermark?.marks?.size ?: 0}"
        }
        val image = decodeImage(sourceFile) ?: return false

        log.info {
            "Poster full-image decoded: source=${sourceFile.absolutePath}, " +
                "size=${image.width}x${image.height}, type=${image.type}"
        }
        val output = applyWatermark(image, watermark)
        return writeImageToFile(sourceFile, destFile, output, "full")
    }

    private fun decodeImage(sourceFile: File): BufferedImage? {
        val image = try {
            val decoded = ImageIO.read(sourceFile)
            if (decoded == null) {
                log.warn {
                    "Poster decode returned null: source=${sourceFile.absolutePath}, " +
                        "sourceBytes=${sourceFile.length()}, readers=${imageReaderClasses(sourceFile)}"
                }
            }
            decoded
        } catch (e: Exception) {
            log.warn(e) {
                "Poster decode failed: source=${sourceFile.absolutePath}, " +
                    "sourceBytes=${sourceFile.length()}, readers=${imageReaderClasses(sourceFile)}"
            }
            null
        }
        return image
    }

    private fun applyWatermark(image: BufferedImage, watermark: WatermarkOptions?): BufferedImage =
        if ((watermark != null) && watermark.marks.isNotEmpty()) {
            WatermarkRenderer.render(image, watermark)
        } else {
            image
        }

    private fun writeImageToFile(
        sourceFile: File,
        destFile: File,
        output: BufferedImage,
        mode: String
    ): Boolean {
        val startedAt = System.currentTimeMillis()
        log.info {
            "Poster $mode write start: source=${sourceFile.absolutePath}, " +
                "destination=${destFile.absolutePath}, output=${output.width}x${output.height}"
        }
        val tempFile = File(destFile.parentFile, destFile.name + ".tmp.jpg")
        return try {
            val ok = writeJpeg(output, tempFile)
            if (!ok) {
                log.warn {
                    "Poster $mode JPEG write failed: temp=${tempFile.absolutePath}, " +
                        "outputSize=${output.width}x${output.height}"
                }
                tempFile.delete()
                return false
            }
            log.info {
                "Poster $mode JPEG written: file=${tempFile.absolutePath}, bytes=${tempFile.length()}, " +
                    "size=${output.width}x${output.height}"
            }

            if (destFile.exists() && !destFile.delete()) {
                log.warn {
                    "Poster $mode destination delete failed: destination=${destFile.absolutePath}, " +
                        "destinationBytes=${destFile.length()}"
                }
                tempFile.delete()
                return false
            }
            Files.move(tempFile.toPath(), destFile.toPath())
            log.info {
                "Poster $mode completed: destination=${destFile.absolutePath}, bytes=${destFile.length()}, " +
                    "elapsedMs=${System.currentTimeMillis() - startedAt}"
            }
            true
        } catch (e: Exception) {
            log.warn(e) {
                "Poster $mode failed: source=${sourceFile.absolutePath}, " +
                    "destination=${destFile.absolutePath}, temp=${tempFile.absolutePath}"
            }
            tempFile.delete()
            false
        }
    }

    private fun imageReaderClasses(file: File): String =
        try {
            val input = ImageIO.createImageInputStream(file) ?: return "unavailable"
            try {
                val readers = ImageIO.getImageReaders(input)
                buildList {
                    while (readers.hasNext()) {
                        add(readers.next().javaClass.simpleName)
                    }
                }.joinToString(",", prefix = "[", postfix = "]")
            } finally {
                input.close()
            }
        } catch (e: Exception) {
            log.warn(e) { "Poster crop reader inspection failed: file=${file.absolutePath}" }
            "unavailable"
        }

    /** JPEG 输出参数对齐 mdcx:quality=95 且色度不二次抽样(4:4:4)。 */
    private const val JPEG_QUALITY = 0.95f
    private const val JPEG_NATIVE_METADATA = "javax_imageio_jpeg_image_1.0"

    private fun writeJpeg(image: BufferedImage, file: File): Boolean {
        val writerIterator = ImageIO.getImageWritersByFormatName("jpg")
        if (!writerIterator.hasNext()) {
            log.warn { "Poster crop JPEG writer unavailable: format=jpg" }
            return false
        }
        val writer = writerIterator.next()
        val stream = try {
            ImageIO.createImageOutputStream(file)
        } catch (e: Exception) {
            log.warn(e) { "Poster crop JPEG output stream failed: file=${file.absolutePath}" }
            null
        } ?: return false
        return try {
            writer.output = stream
            val param = writer.defaultWriteParam
            param.compressionMode = ImageWriteParam.MODE_EXPLICIT
            param.compressionQuality = JPEG_QUALITY
            val metadata = writer.getDefaultImageMetadata(ImageTypeSpecifier(image), param)
            disableChromaSubsampling(metadata)
            writer.write(null, IIOImage(image, null, metadata), param)
            true
        } catch (e: Exception) {
            log.warn(e) { "Poster crop JPEG encode failed: file=${file.absolutePath}" }
            false
        } finally {
            writer.dispose()
            stream.close()
        }
    }

    /** 标准 JPEG 元数据树没有直接 API,尝试把两个色度分量采样改为 1x1;失败时保持默认。 */
    private fun disableChromaSubsampling(metadata: IIOMetadata) {
        runCatching {
            val tree = metadata.getAsTree(JPEG_NATIVE_METADATA) as? org.w3c.dom.Element ?: return@runCatching
            val componentSpecs = tree.getElementsByTagName("componentSpec")
            for (index in 0 until componentSpecs.length) {
                val node = componentSpecs.item(index) as? org.w3c.dom.Element ?: continue
                val componentId = node.getAttribute("componentId")
                if (componentId == "2" || componentId == "3") {
                    node.setAttribute("HsamplingFactor", "1")
                    node.setAttribute("VsamplingFactor", "1")
                }
            }
            metadata.setFromTree(JPEG_NATIVE_METADATA, tree)
        }
    }
}
