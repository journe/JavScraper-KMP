package javscraper.io

import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO

/**
 * 封面裁剪:从横版封面(fanart.jpg,缺省回退 poster.jpg)裁出竖版 poster。
 * 交互逻辑对齐 mdcx 的 cut_window:裁剪框高宽比固定 1.5(与 mdcx 默认
 * pic_h_w_ratio 一致),横图上只允许水平平移,高图上只允许垂直平移。
 */
object PosterCropper {

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
    fun cropToFile(sourceFile: File, destFile: File, rect: Rect): Boolean {
        val image = try {
            ImageIO.read(sourceFile) ?: return false
        } catch (_: Exception) {
            return false
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
        val tempFile = File(destFile.parentFile, destFile.name + ".tmp.jpg")
        return try {
            val ok = ImageIO.write(cropped, "jpg", tempFile)
            if (ok && (!destFile.exists() || destFile.delete())) {
                Files.move(tempFile.toPath(), destFile.toPath())
                true
            } else {
                tempFile.delete()
                false
            }
        } catch (_: Exception) {
            tempFile.delete()
            false
        }
    }
}
