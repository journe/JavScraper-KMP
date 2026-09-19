package javscraper.scrape.update

import javscraper.io.NfoFieldPresenceReader
import javscraper.io.NfoReader
import javscraper.models.ExistingVideoMetadata
import javscraper.models.ScannedFile
import java.nio.file.Files

sealed interface ExistingMetadataResult {
    data class Ready(val metadata: ExistingVideoMetadata) : ExistingMetadataResult
    data class Failed(val errors: List<String>) : ExistingMetadataResult
}

class UpdateModeExistingMetadataReader {
    fun read(files: List<ScannedFile>): ExistingMetadataResult {
        if (files.isEmpty()) return ExistingMetadataResult.Failed(listOf("Update mode requires source files"))
        val sourcePaths = files.map { it.path }.distinct()
        if (sourcePaths.size != files.size) {
            return ExistingMetadataResult.Failed(listOf("Update mode received duplicate source files"))
        }
        val parentCount = sourcePaths.map { java.nio.file.Path.of(it).toAbsolutePath().normalize().parent }
            .distinct().size
        if (parentCount != 1) {
            return ExistingMetadataResult.Failed(listOf("Update mode requires all source files in one folder"))
        }
        if (files.any { !Files.isRegularFile(java.nio.file.Path.of(it.path)) }) {
            return ExistingMetadataResult.Failed(listOf("Update mode source file is missing"))
        }
        val location = UpdateModeSources.locate(files)
            ?: return ExistingMetadataResult.Failed(listOf("Update mode requires an existing NFO file"))
        return try {
            val video = NfoReader.read(location.nfoPath)
                ?: return ExistingMetadataResult.Failed(listOf("Update mode could not read existing NFO file"))
            ExistingMetadataResult.Ready(
                ExistingVideoMetadata(
                    video = video.copy(path = files.first().path),
                    presentFields = NfoFieldPresenceReader.read(location.nfoPath)
                )
            )
        } catch (e: Exception) {
            ExistingMetadataResult.Failed(listOf("Update mode could not read existing NFO file: ${e.message}"))
        }
    }
}