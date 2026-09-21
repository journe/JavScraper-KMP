package javscraper.scrape

import javscraper.io.RenameFormatter
import javscraper.models.ScannedFile
import javscraper.models.Video
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory

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
                ?: return UpdateModeResult.Failed(listOf("Update mode requires an existing NFO file"))
            // fanart 是封面本体，poster 是副本（用户可能手动裁剪/编辑过），分开判断：
            // fanart 缺失时重新下载封面；poster 缺失时本地补副本，不覆盖已编辑的 poster。
            val fanartReusable = Files.isRegularFile(sourceFolder.resolve("fanart.jpg"))
            val posterPresent = Files.isRegularFile(sourceFolder.resolve("poster.jpg"))
            val previewsReusable = hasPreviewImages(sourceFolder)
            val target = resolveTargetFolder(sourceFolder, files, video)
            if (target == sourceFolder) {
                return UpdateModeResult.Ready(
                    folder = sourceFolder,
                    files = files,
                    nfoPath = nfo,
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
        val scanBase = when {
            configuredScanDir == null -> sourceFolder.parent ?: return sourceFolder
            configuredScanDir == sourceFolder -> return sourceFolder
            sourceFolder.startsWith(configuredScanDir) -> configuredScanDir
            else -> sourceFolder.parent ?: return sourceFolder
        }
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
    data class Ready(
        val folder: Path,
        val files: List<ScannedFile>,
        val nfoPath: Path,
        /** fanart（封面本体）已存在，可跳过封面下载。 */
        val fanartReusable: Boolean,
        /** poster.jpg 是否已存在（可能是用户编辑过的版本，不应被覆盖）。 */
        val posterPresent: Boolean,
        val previewsReusable: Boolean
    ) : UpdateModeResult

    data class Failed(val errors: List<String>) : UpdateModeResult
}
