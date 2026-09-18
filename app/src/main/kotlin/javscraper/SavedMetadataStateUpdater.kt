package javscraper

import javscraper.models.ScannedFile
import javscraper.models.Video
import java.nio.file.Path

internal data class SavedMetadataState(
    val scannedFiles: List<ScannedFile>,
    val results: List<Video>
)

internal object SavedMetadataStateUpdater {
    fun apply(
        scannedFiles: List<ScannedFile>,
        results: List<Video>,
        savedVideo: Video,
        previousPath: String
    ): SavedMetadataState {
        val previousFolder = Path.of(previousPath).parent
        val updatedFiles = scannedFiles.map { file ->
            when {
                file.path == previousPath -> file.copy(
                    path = savedVideo.path,
                    number = savedVideo.number,
                    isScraped = true,
                    metadata = savedVideo
                )
                Path.of(file.path).parent == previousFolder -> file.copy(path = remapFolder(file.path, savedVideo.path))
                else -> file
            }
        }
        val updatedResults = results.map { current ->
            when {
                current.path == previousPath -> savedVideo
                Path.of(current.path).parent == previousFolder -> current.copy(path = remapFolder(current.path, savedVideo.path))
                else -> current
            }
        }
        return SavedMetadataState(updatedFiles, updatedResults)
    }

    private fun remapFolder(path: String, savedVideoPath: String): String {
        val oldFolder = folderText(path)
        val newFolder = folderText(savedVideoPath)
        return newFolder + path.drop(oldFolder.length)
    }

    private fun folderText(path: String): String {
        val separatorIndex = maxOf(path.lastIndexOf('/'), path.lastIndexOf('\\'))
        return if (separatorIndex == -1) path else path.substring(0, separatorIndex)
    }
}
