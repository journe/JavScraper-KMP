package javscraper.io

import java.nio.file.Files
import java.nio.file.Path

/**
 * 海报/封面图片的候选文件名解析。
 *
 * 读取（展示、复用、裁剪源）时同时接受两类命名：
 * 1. 通用名：`poster.jpg` / `fanart.jpg`（含 png 变体）；
 * 2. 与视频同名：`<视频基准名>-poster.jpg` / `<视频基准名>-fanart.jpg`，
 *    视频基准名指视频文件名去掉扩展名，如 `FC2-3264420.mp4` → `FC2-3264420-fanart.jpg`。
 *
 * 通用名优先（刮削落盘的标准产物），同名变体作为兼容旧库/外部工具命名。
 */
object MediaArtPaths {
    private val EXTENSIONS = listOf("jpg", "png")

    /** 返回目录下可接受的 poster 候选路径，按优先级排序（存在与否不判断）。 */
    fun posterCandidates(directory: Path, videoBaseName: String): List<Path> =
        candidates(directory, videoBaseName, "poster")

    /** 返回目录下可接受的 fanart 候选路径，按优先级排序（存在与否不判断）。 */
    fun fanartCandidates(directory: Path, videoBaseName: String): List<Path> =
        candidates(directory, videoBaseName, "fanart")

    /** 返回首个存在的 poster 文件；不存在时返回 null。 */
    fun findPoster(directory: Path, videoBaseName: String): Path? =
        posterCandidates(directory, videoBaseName).firstOrNull(Files::isRegularFile)

    /** 返回首个存在的 fanart 文件；不存在时返回 null。 */
    fun findFanart(directory: Path, videoBaseName: String): Path? =
        fanartCandidates(directory, videoBaseName).firstOrNull(Files::isRegularFile)

    /** 从视频文件名提取基准名（去扩展名），无法提取时返回空串。 */
    fun videoBaseName(videoFileName: String): String =
        videoFileName.substringBeforeLast(".").trim()

    private fun candidates(directory: Path, videoBaseName: String, kind: String): List<Path> {
        val names = buildList {
            for (ext in EXTENSIONS) add("$kind.$ext")
            if (videoBaseName.isNotBlank()) {
                for (ext in EXTENSIONS) add("$videoBaseName-$kind.$ext")
            }
        }
        return names.map(directory::resolve)
    }
}
