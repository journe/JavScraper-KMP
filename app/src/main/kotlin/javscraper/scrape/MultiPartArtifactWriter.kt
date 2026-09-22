package javscraper.scrape

import javscraper.io.FileScanner
import javscraper.io.metadata.PartNfoWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

internal object MultiPartArtifactWriter {
    private val artExtensions = setOf("jpg", "jpeg", "png")

    fun writePartNfos(
        folder: Path,
        masterNfo: Path,
        videos: List<Path>,
        errors: MutableList<String>
    ) {
        if (!masterNfo.fileName.toString().equals("movie.nfo", ignoreCase = true)) return
        partVideos(videos).drop(1).forEach { video ->
            val label = partSuffix(video) ?: return@forEach
            val target = withExtension(video, "nfo")
            if (Files.exists(target)) return@forEach
            try {
                PartNfoWriter.write(masterNfo, target, label)
            } catch (e: Exception) {
                errors += "Part NFO failed for ${video.fileName}: ${e.message}"
            }
        }
    }

    fun writeDefaultPartImages(
        folder: Path,
        videos: List<Path>,
        errors: MutableList<String>
    ) {
        val poster = findDefaultArt(folder, "poster")
        val fanart = findDefaultArt(folder, "fanart")
        partVideos(videos).drop(1).forEach { video ->
            poster?.let { copyDefaultArt(it, withSuffix(video, "poster", extensionOf(it)), errors) }
            fanart?.let { copyDefaultArt(it, withSuffix(video, "fanart", extensionOf(it)), errors) }
        }
    }

    fun applyExtraFanartImages(
        folder: Path,
        videos: List<Path>,
        overwrite: Boolean
    ) {
        val previewFolder = folder.resolve("extrafanart")
        partVideos(videos).drop(1).forEach { video ->
            val number = partSuffix(video)?.let(FileScanner::multiPartLabelNumber) ?: return@forEach
            val source = previewFolder.resolve("fanart$number.jpg")
            if (!Files.isRegularFile(source)) return@forEach
            copyExtraFanart(source, withSuffix(video, "poster", extensionOf(source)), overwrite)
            copyExtraFanart(source, withSuffix(video, "fanart", extensionOf(source)), overwrite)
        }
    }

    private fun copyDefaultArt(source: Path, target: Path, errors: MutableList<String>) {
        if (Files.exists(target)) return
        try {
            Files.createDirectories(target.parent)
            Files.copy(source, target)
        } catch (e: Exception) {
            errors += "Part image failed for ${target.fileName}: ${e.message}"
        }
    }

    private fun copyExtraFanart(source: Path, target: Path, overwrite: Boolean) {
        if (!overwrite && Files.exists(target)) return
        try {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: Exception) {
            // Preview-derived art remains best effort, matching extrafanart download behavior.
        }
    }

    private fun ordered(videos: List<Path>): List<Path> {
        return videos.mapIndexed { index, path -> IndexedVideo(path, partSuffix(path), index) }
            .sortedWith(
                compareBy<IndexedVideo> { it.partSuffix?.let(FileScanner::multiPartLabelNumber) ?: Int.MAX_VALUE }
                    .thenBy { it.index }
            )
            .map { it.path }
    }

    private fun findDefaultArt(folder: Path, kind: String): Path? {
        val candidates = artExtensions.map { folder.resolve("$kind.$it") }
        return candidates.firstOrNull(Files::isRegularFile)
    }

    private fun partVideos(videos: List<Path>): List<Path> =
        ordered(videos).filter { partSuffix(it) != null }

    private fun partSuffix(video: Path): String? =
        FileScanner.parseFileName(video.fileName.toString()).versionLabel
            .takeIf(FileScanner::isMultiPartLabel)

    private fun withSuffix(video: Path, kind: String, extension: String): Path {
        val base = video.fileName.toString().substringBeforeLast('.')
        return video.resolveSibling("$base-$kind.$extension")
    }

    private fun extensionOf(path: Path): String =
        path.fileName.toString().substringAfterLast('.', "jpg")

    private fun withExtension(video: Path, extension: String): Path {
        val base = video.fileName.toString().substringBeforeLast('.')
        return video.resolveSibling("$base.$extension")
    }

    private data class IndexedVideo(
        val path: Path,
        val partSuffix: String?,
        val index: Int
    )
}
