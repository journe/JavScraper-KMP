package javscraper.io.metadata

import javscraper.io.FileScanner
import javscraper.io.InvalidNfoException
import javscraper.io.NfoReader
import javscraper.io.NfoUpdater
import javscraper.models.Video
import javscraper.settings.MultiPartSuffix
import java.nio.file.Files
import java.nio.file.Paths

sealed interface VideoMetadataEditResult {
    data class Success(
        val video: Video,
        val changed: Boolean,
        val previousPath: String = video.path
    ) : VideoMetadataEditResult
    data object NfoMissing : VideoMetadataEditResult
    data class Failed(val message: String) : VideoMetadataEditResult
}

object VideoMetadataEditor {
    fun update(
        video: Video,
        lockData: Boolean,
        folderLayers: List<String> = emptyList(),
        scanDir: String = "",
        multiPartSuffix: MultiPartSuffix = MultiPartSuffix.CD,
        mergeTags: Boolean = false
    ): VideoMetadataEditResult {
        if (video.path.isBlank()) return VideoMetadataEditResult.NfoMissing
        MultiPartMetadataEditor.update(video, lockData, folderLayers, scanDir, multiPartSuffix, mergeTags)?.let { return it }
        val videoPath = Paths.get(video.path).toAbsolutePath().normalize()
        val nfoPath = FileScanner.findMatchingNfo(videoPath)
            ?: return VideoMetadataEditResult.NfoMissing
        var originalNfo = ""
        var plannedMove: MetadataFolderMove? = null
        var folderMoved = false

        return try {
            originalNfo = Files.readString(nfoPath)
            // 旧 NFO 非法时在移动文件夹前直接失败，避免先 move 再回滚。
            try {
                NfoUpdater.validate(nfoPath)
            } catch (e: InvalidNfoException) {
                return VideoMetadataEditResult.Failed(e.message.orEmpty())
            }
            val move = MetadataFolderRenamer.plan(video, videoPath, nfoPath, folderLayers, scanDir)
                .also { plannedMove = it }
            if (move.shouldMove) {
                Files.createDirectories(move.targetFolder.parent)
                Files.move(move.sourceFolder, move.targetFolder)
                folderMoved = true
            }
            val changed = NfoUpdater.update(
                path = move.targetNfoPath,
                video = video,
                lockData = lockData,
                insertMissingFields = true,
                mergeTags = mergeTags
            )
            val savedVideo = NfoReader.read(move.targetNfoPath)
                ?.copy(path = move.targetVideoPath.toString())
                ?: throw IllegalStateException("NFO read-back failed")
            VideoMetadataEditResult.Success(savedVideo, changed, previousPath = video.path)
        } catch (e: Exception) {
            if (folderMoved) rollback(plannedMove!!, originalNfo, e)
            VideoMetadataEditResult.Failed(e.message.orEmpty())
        }
    }

    private fun rollback(
        move: MetadataFolderMove,
        originalNfo: String,
        failure: Exception
    ) {
        try {
            if (Files.isRegularFile(move.targetNfoPath)) {
                Files.writeString(move.targetNfoPath, originalNfo)
            }
            if (!Files.exists(move.sourceFolder)) {
                Files.move(move.targetFolder, move.sourceFolder)
            }
        } catch (rollbackFailure: Exception) {
            failure.addSuppressed(rollbackFailure)
        }
    }
}
