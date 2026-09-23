package javscraper.ui

import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * 预览专用图片提供者。
 *
 * 优先从 classpath 读取应用图标；读取失败时生成占位图，保证所有 @Preview
 * 都能拿到可显示的本地图片文件。仅供预览与 UI 测试使用。
 */
internal object PreviewImageProvider {
    fun previewImage(): File {
        val directory = File(System.getProperty("java.io.tmpdir"), "javscraper-preview-images").apply { mkdirs() }
        val target = File(directory, "app_icon.png")
        if (target.isFile && target.length() > 0L) return target
        val copied = runCatching {
            javaClass.getResourceAsStream("/icon/app_icon.png")?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }.getOrNull()
        if (copied == null || !target.isFile || target.length() == 0L) {
            writePlaceholder(target)
        }
        return target
    }

    fun previewImages(count: Int): List<File> = List(count) { previewImage() }

    private fun writePlaceholder(file: File) {
        val image = BufferedImage(300, 450, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.color = Color(55, 71, 79)
        graphics.fillRect(0, 0, image.width, image.height)
        graphics.color = Color.WHITE
        graphics.font = Font(Font.SANS_SERIF, Font.BOLD, 28)
        graphics.drawString("POSTER", 66, 232)
        graphics.dispose()
        ImageIO.write(image, "png", file)
    }
}