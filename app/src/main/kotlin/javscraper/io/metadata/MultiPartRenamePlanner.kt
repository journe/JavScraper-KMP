package javscraper.io.metadata

import javscraper.io.FileScanner
import javscraper.io.RenameFormatter
import javscraper.models.Video
import javscraper.settings.MultiPartSuffix
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

internal data class FileMove(
    val source: Path,
    val target: Path
)

internal data class MultiPartRenamePlan(
    val sourceFolder: Path,
    val targetFolder: Path,
    val videoMoves: List<FileMove>,
    val fileMoves: List<FileMove>
) {
    val targetMasterNfo: Path = targetFolder.resolve("movie.nfo")
    val targetPrimaryVideo: Path = videoMoves.first().target
    val shouldMoveFolder: Boolean = sourceFolder != targetFolder
    private var folderMoved = false

    fun apply(): List<FileMove> {
        val completed = mutableListOf<FileMove>()
        try {
            if (shouldMoveFolder) {
                Files.createDirectories(targetFolder.parent)
                Files.move(sourceFolder, targetFolder)
                folderMoved = true
            }
            fileMoves.forEach { move ->
                if (move.source != move.target) {
                    Files.move(move.source, move.target)
                    completed.add(move)
                }
            }
            return completed
        } catch (e: Exception) {
            rollback(completed)
            throw e
        }
    }

    fun rollback(completed: List<FileMove>) {
        completed.reversed().forEach { move ->
            runCatching { Files.move(move.target, move.source, StandardCopyOption.REPLACE_EXISTING) }
        }
        if (folderMoved && !Files.exists(sourceFolder)) {
            runCatching { Files.move(targetFolder, sourceFolder) }
        }
        folderMoved = false
    }
}

internal object MultiPartRenamePlanner {
    fun plan(
        video: Video,
        videoPath: Path,
        masterNfo: Path,
        folderLayers: List<String>,
        scanDir: String,
        multiPartSuffix: MultiPartSuffix
    ): MultiPartRenamePlan? {
        val sourceFolder = videoPath.parent ?: return null
        if (!masterNfo.fileName.toString().equals("movie.nfo", ignoreCase = true)) return null
        val siblings = FileScanner.scanDirectory(sourceFolder, recursive = false)
            .filter { it.number.equals(video.number, ignoreCase = true) }
        if (siblings.size < 2) return null
        val siblingLabels = siblings.map { FileScanner.parseFileName(it.fileName).versionLabel }
        if (siblingLabels.none(FileScanner::isMultiPartLabel)) return null

        val targetFolder = resolveTargetFolder(video, sourceFolder, folderLayers, scanDir)
        if (targetFolder != sourceFolder && targetFolder.startsWith(sourceFolder)) {
            throw IllegalArgumentException("Target folder must not be inside the current folder")
        }
        if (targetFolder != sourceFolder && Files.exists(targetFolder)) {
            throw FileAlreadyExistsException(targetFolder.toFile(), null, "Target folder already exists")
        }

        val sourceVideos = siblings.map { Path.of(it.path).toAbsolutePath().normalize() }
        val orderedVideos = sourceVideos
            .mapIndexed { index, path -> IndexedVideoPath(path, FileScanner.parseFileName(path.fileName.toString()).versionLabel, index) }
            .sortedWith(
                compareBy<IndexedVideoPath> { partNumber(it.label) ?: Int.MAX_VALUE }
                    .thenBy { it.index }
            )
            .map { it.path }
        val oldNames = orderedVideos.map { it.fileName.toString() }
        val labels = oldNames.mapIndexed { index, name -> partLabel(index, name, multiPartSuffix) }
        val newBase = targetFolder.fileName.toString()
        val moves = mutableListOf<FileMove>()
        val videoMoves = oldNames.mapIndexed { index, oldName ->
            val ext = oldName.substringAfterLast('.', "")
            val newName = buildString {
                append(newBase)
                append(" - ")
                append(labels[index])
                if (ext.isNotBlank()) append('.').append(ext)
            }
            FileMove(targetFolder.resolve(oldName), targetFolder.resolve(newName))
        }
        moves.addAll(videoMoves)

        oldNames.forEachIndexed { index, oldName ->
            val oldBase = oldName.substringBeforeLast('.')
            val newBaseWithName = videoMoves[index].target.fileName.toString().substringBeforeLast('.')
            val oldNfo = sourceFolder.resolve("$oldBase.nfo")
            if (Files.isRegularFile(oldNfo)) {
                moves.add(
                    FileMove(
                        targetFolder.resolve("$oldBase.nfo"),
                        targetFolder.resolve("$newBaseWithName.nfo")
                    )
                )
            }
            moves.addAll(partArtMoves(sourceFolder, targetFolder, oldBase, newBaseWithName))
        }

        if (targetFolder == sourceFolder) {
            moves.filter { it.source != it.target }.forEach { move ->
                if (Files.exists(move.target)) {
                    throw FileAlreadyExistsException(move.target.toFile(), null, "Target file already exists")
                }
            }
        }

        return MultiPartRenamePlan(
            sourceFolder = sourceFolder,
            targetFolder = targetFolder,
            videoMoves = videoMoves,
            fileMoves = moves
        )
    }

    private fun resolveTargetFolder(
        video: Video,
        sourceFolder: Path,
        folderLayers: List<String>,
        scanDir: String
    ): Path {
        val layers = RenameFormatter.formatFolder(video, folderLayers, "")
            .ifEmpty { listOf(RenameFormatter.sanitize("${video.number} ${video.title}")) }
        val configuredScanDir = scanDir.takeIf { it.isNotBlank() }
            ?.let { Path.of(it).toAbsolutePath().normalize() }
        val base = when {
            configuredScanDir != null && sourceFolder.startsWith(configuredScanDir) -> configuredScanDir
            sourceFolder.parent != null -> sourceFolder.parent
            else -> sourceFolder
        }
        return layers.fold(base) { path, layer -> path.resolve(layer) }
    }

    private fun partNumber(label: String): Int? = FileScanner.multiPartLabelNumber(label)

    private data class IndexedVideoPath(
        val path: Path,
        val label: String,
        val index: Int
    )

    private fun partLabel(index: Int, fileName: String, suffix: MultiPartSuffix): String {
        val raw = FileScanner.parseFileName(fileName).versionLabel
        val number = FileScanner.multiPartLabelNumber(raw) ?: index + 1
        return "${suffix.value}$number"
    }


    private fun partArtMoves(
        sourceFolder: Path,
        targetFolder: Path,
        oldBase: String,
        newBase: String
    ): List<FileMove> {
        if (!Files.isDirectory(sourceFolder)) return emptyList()
        return Files.list(sourceFolder).use { stream ->
            stream.filter { file ->
                val name = file.fileName.toString()
                (name.startsWith("$oldBase-poster.") || name.startsWith("$oldBase-fanart.")) &&
                    name.substringAfterLast(".", "") in setOf("jpg", "jpeg", "png")
            }.map { file ->
                val name = file.fileName.toString()
                val kind = if (name.startsWith("$oldBase-poster.")) "poster" else "fanart"
                val ext = name.substringAfterLast(".")
                FileMove(targetFolder.resolve(name), targetFolder.resolve("$newBase-$kind.$ext"))
            }.toList()
        }
    }
}
