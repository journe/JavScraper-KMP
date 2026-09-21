package javscraper.scrape

import javscraper.io.InvalidNfoException
import javscraper.io.MediaArtPaths
import javscraper.io.NfoUpdater
import javscraper.io.RenameFormatter
import javscraper.models.ScannedFile
import javscraper.models.Video
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory

/** 更新模式基准目录：源文件夹位于配置的扫描目录内时取扫描目录；否则退回源文件夹父目录。
 *  源文件夹可能与扫描目录相同（视频直接放在扫描根下），此时返回扫描目录本身，
 *  绝不能取其 parent（那是盘符根，会把目标解析到库外）。 */
internal fun resolveUpdateModeScanBase(sourceFolder: Path, configuredScanDir: Path?): Path {
    if (configuredScanDir != null && sourceFolder.startsWith(configuredScanDir)) return configuredScanDir
    return sourceFolder.parent ?: sourceFolder
}

/** Moves an already-scraped movie folder and exposes assets reused by update mode. */
internal class UpdateModeWriter(private val options: ScrapeOptions) {
    fun prepare(files: List<ScannedFile>, video: Video): UpdateModeResult {
        return try {
            val sourcePaths = files.map { Path.of(it.path).toAbsolutePath().normalize() }
            if (sourcePaths.map { it.parent }.distinct().size != 1) {
                return UpdateModeResult.Failed(listOf("Update mode requires all source files in one folder"))
            }
            val sourceFolder = sourcePaths.first().parent
                ?: return UpdateModeResult.Failed(listOf("Update mode source folder is missing"))
            if (sourcePaths.any { !Files.isRegularFile(it) }) {
                return UpdateModeResult.Failed(listOf("Update mode source file is missing"))
            }

            val nfo = findNfo(sourceFolder, sourcePaths)
                ?: return UpdateModeResult.NoExistingNfo(sourceFolder)
            // 旧 NFO 无法解析时，在任何文件夹移动/写盘之前失败，保持用户输入态。
            try {
                NfoUpdater.validate(nfo)
            } catch (e: InvalidNfoException) {
                return UpdateModeResult.Failed(listOf(e.message.orEmpty()))
            }
            // fanart 是封面本体，poster 是副本（用户可能手动裁剪/编辑过），分开判断：
            // fanart 缺失时重新下载封面；poster 缺失时本地补副本，不覆盖已编辑的 poster。
            // 两类图片均同时接受通用名（fanart.jpg/poster.jpg）与同名变体（<视频基准名>-fanart.jpg 等）。
            // 基准名取首个视频文件名去扩展名；多文件时各视频基准名不同，但旧库通常一套图共用通用名或首片名。
            val artBaseName = MediaArtPaths.videoBaseName(sourcePaths.first().fileName.toString())
            val fanartPath = MediaArtPaths.findFanart(sourceFolder, artBaseName)
            val posterPath = MediaArtPaths.findPoster(sourceFolder, artBaseName)
            val fanartReusable = fanartPath != null
            val posterPresent = posterPath != null
            val previewsReusable = hasPreviewImages(sourceFolder)
            val target = resolveTargetFolder(sourceFolder, files, video)
            if (target == sourceFolder) {
                return UpdateModeResult.Ready(
                    folder = sourceFolder,
                    files = files,
                    nfoPath = nfo,
                    fanartPath = fanartPath,
                    posterPath = posterPath,
                    fanartReusable = fanartReusable,
                    posterPresent = posterPresent,
                    previewsReusable = previewsReusable
                )
            }
            if (target.startsWith(sourceFolder)) {
                return UpdateModeResult.Failed(listOf("Update mode target folder must not be inside the source folder"))
            }

            moveFolderContents(sourceFolder, target)
            UpdateModeResult.Ready(
                folder = target,
                files = files.mapIndexed { index, file ->
                    file.copy(path = target.resolve(sourcePaths[index].fileName).toString())
                },
                nfoPath = target.resolve(nfo.fileName),
                fanartPath = fanartPath?.let { target.resolve(it.fileName) },
                posterPath = posterPath?.let { target.resolve(it.fileName) },
                fanartReusable = fanartReusable,
                posterPresent = posterPresent,
                previewsReusable = previewsReusable
            )
        } catch (e: Exception) {
            UpdateModeResult.Failed(listOf("Update mode folder move failed: ${e.message}"))
        }
    }

    private fun resolveTargetFolder(
        sourceFolder: Path,
        files: List<ScannedFile>,
        video: Video
    ): Path {
        if (!options.createMovieFolders) return sourceFolder
        val configuredScanDir = options.scanDir.takeIf { it.isNotBlank() }
            ?.let { Path.of(it).toAbsolutePath().normalize() }
        if (configuredScanDir == sourceFolder) return sourceFolder
        val scanBase = resolveUpdateModeScanBase(sourceFolder, configuredScanDir)
        if (files.size > 1) {
            val layers = RenameFormatter.formatFolder(video, options.folderLayers, "")
            val parent = layers.dropLast(1).fold(scanBase) { path, layer -> path.resolve(layer) }
            val baseName = RenameFormatter.sanitize(video.number).ifBlank { "UNKNOWN" }
            return parent.resolve(baseName)
        }

        val suffix = RenameFormatter.detectSuffix(files.first().fileName, options.suffixKeywords)
        return RenameFormatter.formatFolder(video, options.folderLayers, suffix)
            .fold(scanBase) { path, layer -> path.resolve(layer) }
    }

    private fun findNfo(folder: Path, videos: List<Path>): Path? {
        val explicit = (
            videos.map { video ->
                video.resolveSibling(video.fileName.toString().substringBeforeLast('.') + ".nfo")
            } + folder.resolve(folder.fileName.toString() + ".nfo")
            ).distinct()
        explicit.firstOrNull { Files.isRegularFile(it) }?.let { return it }

        return Files.list(folder).use { stream ->
            stream.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".nfo", ignoreCase = true) }
                .toList()
        }.singleOrNull()
    }

    private fun hasPreviewImages(folder: Path): Boolean {
        val previews = folder.resolve("extrafanart")
        if (!Files.isDirectory(previews)) return false
        return Files.list(previews).use { stream ->
            stream.anyMatch { file ->
                Files.isRegularFile(file) && file.fileName.toString().lowercase().endsWith(".jpg")
            }
        }
    }

    private fun moveFolderContents(source: Path, target: Path) {
        if (Files.exists(target) && !target.isDirectory()) {
            throw FileAlreadyExistsException(target.toFile(), null, "target is not a directory")
        }
        Files.createDirectories(target)
        Files.newDirectoryStream(source).use { entries ->
            entries.forEach { entry ->
                val destination = target.resolve(entry.fileName.toString())
                if (Files.exists(destination)) {
                    throw FileAlreadyExistsException(destination.toFile(), null, "target already exists")
                }
                Files.move(entry, destination)
            }
        }
        Files.deleteIfExists(source)
    }
}

internal sealed interface UpdateModeResult {
    /** 旧文件夹中找不到 NFO（视频从未刮削过）：不算失败，调用方回退标准写盘流程，基准为扫描目录。 */
    data class NoExistingNfo(val sourceFolder: Path) : UpdateModeResult

    data class Ready(
        val folder: Path,
        val files: List<ScannedFile>,
        val nfoPath: Path,
        /** 已存在的 fanart 路径（可能是同名变体命名）；无则 null。 */
        val fanartPath: Path?,
        /** 已存在的 poster 路径（可能是同名变体命名，也可能是用户编辑版）；无则 null。 */
        val posterPath: Path?,
        /** fanart（封面本体）已存在，可跳过封面下载。 */
        val fanartReusable: Boolean,
        /** poster 是否已存在（可能是用户编辑过的版本，不应被覆盖）。 */
        val posterPresent: Boolean,
        val previewsReusable: Boolean
    ) : UpdateModeResult

    data class Failed(val errors: List<String>) : UpdateModeResult
}
