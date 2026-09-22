package javscraper.io.metadata

import javscraper.io.FileScanner
import javscraper.io.InvalidNfoException
import javscraper.io.NfoReader
import javscraper.io.NfoUpdater
import javscraper.models.Video
import javscraper.settings.MultiPartSuffix
import java.nio.file.Files
import java.nio.file.Paths

/** Edits master movie metadata and keeps all part assets names in sync. */
internal object MultiPartMetadataEditor {
    fun update(
        video: Video,
        lockData: Boolean,
        folderLayers: List<String>,
        scanDir: String,
        multiPartSuffix: MultiPartSuffix,
        mergeTags: Boolean
    ): VideoMetadataEditResult? {
        if (video.path.isBlank()) return null
        val videoPath = Paths.get(video.path).toAbsolutePath().normalize()
        val masterNfo = FileScanner.findMatchingNfo(videoPath) ?: return null
        val plan = try {
            MultiPartRenamePlanner.plan(video, videoPath, masterNfo, folderLayers, scanDir, multiPartSuffix)
        } catch (e: Exception) {
            return VideoMetadataEditResult.Failed(e.message.orEmpty())
        } ?: return null

        val originalNfo = try {
            Files.readString(masterNfo).also {
                try {
                    NfoUpdater.validate(masterNfo)
                } catch (e: InvalidNfoException) {
                    return VideoMetadataEditResult.Failed(e.message.orEmpty())
                }
            }
        } catch (e: Exception) {
            return VideoMetadataEditResult.Failed(e.message.orEmpty())
        }

        val completed = try {
            plan.apply()
        } catch (e: Exception) {
            return VideoMetadataEditResult.Failed(e.message.orEmpty())
        }

        return try {
            val changed = NfoUpdater.update(
                path = plan.targetMasterNfo,
                video = video,
                lockData = lockData,
                insertMissingFields = true,
                mergeTags = mergeTags
            )
            val saved = NfoReader.read(plan.targetMasterNfo)
                ?.copy(path = plan.targetPrimaryVideo.toString())
                ?: throw IllegalStateException("NFO read-back failed")
            VideoMetadataEditResult.Success(
                video = saved,
                changed = changed,
                previousPath = video.path
            )
        } catch (e: Exception) {
            plan.rollback(completed)
            if (Files.isRegularFile(masterNfo)) {
                runCatching { Files.writeString(masterNfo, originalNfo) }
            }
            VideoMetadataEditResult.Failed(e.message.orEmpty())
        }
    }
}
