package javscraper.io.metadata

import javscraper.io.FileScanner
import javscraper.io.NfoReader
import javscraper.io.NfoUpdater
import javscraper.models.Video
import java.nio.file.Paths

sealed interface VideoMetadataEditResult {
    data class Success(val video: Video, val changed: Boolean) : VideoMetadataEditResult
    data object NfoMissing : VideoMetadataEditResult
    data class Failed(val message: String) : VideoMetadataEditResult
}

object VideoMetadataEditor {
    fun update(video: Video, lockData: Boolean): VideoMetadataEditResult {
        if (video.path.isBlank()) return VideoMetadataEditResult.NfoMissing
        val nfoPath = FileScanner.findMatchingNfo(Paths.get(video.path))
            ?: return VideoMetadataEditResult.NfoMissing

        return try {
            val changed = NfoUpdater.update(
                path = nfoPath,
                video = video,
                lockData = lockData,
                insertMissingFields = true
            )
            val savedVideo = NfoReader.read(nfoPath)
                ?.copy(path = video.path)
                ?: return VideoMetadataEditResult.Failed("NFO read-back failed")
            VideoMetadataEditResult.Success(savedVideo, changed)
        } catch (e: Exception) {
            VideoMetadataEditResult.Failed(e.message.orEmpty())
        }
    }
}
