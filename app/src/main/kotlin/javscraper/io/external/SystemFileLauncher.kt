package javscraper.io.external

import java.awt.Desktop
import java.io.File

/** Opens files and directories with the operating system's default handler. */
class SystemFileLauncher(
    private val openWithSystemHandler: (File) -> Unit = { file ->
        Desktop.getDesktop().open(file)
    }
) {
    fun openVideo(videoPath: String): Boolean {
        val video = File(videoPath)
        if (!video.isFile) return false
        return runCatching { openWithSystemHandler(video) }.isSuccess
    }

    fun openContainingDirectory(path: String): Boolean {
        val target = File(path)
        val directory = if (target.isDirectory) target else target.parentFile
        if (directory?.isDirectory != true) return false
        return runCatching { openWithSystemHandler(directory) }.isSuccess
    }
}