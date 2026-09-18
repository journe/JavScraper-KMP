package javscraper.io.metadata

import javscraper.io.RenameFormatter
import javscraper.models.Video
import java.nio.file.Files
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Path

internal data class MetadataFolderMove(
    val sourceFolder: Path,
    val targetFolder: Path,
    val targetVideoPath: Path,
    val targetNfoPath: Path
) {
    val shouldMove: Boolean = sourceFolder != targetFolder
}

internal object MetadataFolderRenamer {
    fun plan(
        video: Video,
        videoPath: Path,
        nfoPath: Path,
        folderLayers: List<String>,
        scanDir: String
    ): MetadataFolderMove {
        if (folderLayers.isEmpty()) {
            return MetadataFolderMove(
                sourceFolder = videoPath.parent ?: throw IllegalArgumentException("Video folder is missing"),
                targetFolder = videoPath.parent ?: throw IllegalArgumentException("Video folder is missing"),
                targetVideoPath = videoPath,
                targetNfoPath = nfoPath
            )
        }
        val sourceFolder = videoPath.parent
            ?: throw IllegalArgumentException("Video folder is missing")
        val configuredScanDir = scanDir.takeIf { it.isNotBlank() }
            ?.let { Path.of(it).toAbsolutePath().normalize() }
        val base = when {
            configuredScanDir != null && sourceFolder.startsWith(configuredScanDir) -> configuredScanDir
            sourceFolder.parent != null -> sourceFolder.parent
            else -> throw IllegalArgumentException("Video folder parent is missing")
        }
        val layers = RenameFormatter.formatFolder(video, folderLayers, suffix = "")
            .ifEmpty { listOf(RenameFormatter.sanitize("${video.number} ${video.title}")) }
        val targetFolder = layers.fold(base) { path, layer -> path.resolve(layer) }

        if (targetFolder == sourceFolder) {
            return MetadataFolderMove(sourceFolder, targetFolder, videoPath, nfoPath)
        }
        if (targetFolder.startsWith(sourceFolder)) {
            throw IllegalArgumentException("Target folder must not be inside the current folder")
        }
        if (Files.exists(targetFolder)) {
            throw FileAlreadyExistsException(targetFolder.toFile(), null, "Target folder already exists")
        }
        return MetadataFolderMove(
            sourceFolder = sourceFolder,
            targetFolder = targetFolder,
            targetVideoPath = targetFolder.resolve(videoPath.fileName),
            targetNfoPath = targetFolder.resolve(nfoPath.fileName)
        )
    }
}
