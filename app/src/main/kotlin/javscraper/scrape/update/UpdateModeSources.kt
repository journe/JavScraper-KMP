package javscraper.scrape.update

import javscraper.models.ScannedFile
import java.nio.file.Files
import java.nio.file.Path

internal data class UpdateModeSourceLocation(
    val folder: Path,
    val nfoPath: Path
)

internal object UpdateModeSources {
    fun locate(files: List<ScannedFile>): UpdateModeSourceLocation? {
        if (files.isEmpty()) return null
        val sourcePaths = files.map { Path.of(it.path).toAbsolutePath().normalize() }
        if (sourcePaths.map { it.parent }.distinct().size != 1) return null
        val folder = sourcePaths.first().parent ?: return null
        if (sourcePaths.any { !Files.isRegularFile(it) }) return null
        val nfo = findNfo(folder, sourcePaths) ?: return null
        return UpdateModeSourceLocation(folder, nfo)
    }

    fun findNfo(folder: Path, videos: List<Path>): Path? {
        folder.resolve("movie.nfo").takeIf { Files.isRegularFile(it) }?.let { return it }

        val explicit = videos.map { video ->
            video.resolveSibling(video.fileName.toString().substringBeforeLast('.') + ".nfo")
        } + folder.resolve(folder.fileName.toString() + ".nfo")
        explicit.firstOrNull { Files.isRegularFile(it) }?.let { return it }

        return Files.list(folder).use { stream ->
            stream.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".nfo", ignoreCase = true) }
                .toList()
        }.singleOrNull()
    }
}
