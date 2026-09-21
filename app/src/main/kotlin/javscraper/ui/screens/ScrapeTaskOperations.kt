package javscraper.ui.screens

import javscraper.models.ScannedFile
import javscraper.models.Video
import java.nio.file.Path

internal fun filesForScrapeTask(
    task: ScrapeTask,
    scannedFiles: List<ScannedFile>
): List<ScannedFile> {
    val pendingGroup = scannedFiles
        .filter { it.number == task.number && !it.isScraped }
        .distinctBy { it.path.ifBlank { it.fileName } }
    if (pendingGroup.isNotEmpty()) return pendingGroup

    val fallback = scannedFiles.find { it.fileName == task.fileName }
        ?: ScannedFile(path = task.path, fileName = task.fileName, number = task.number)
    return listOf(fallback)
}

internal fun sameFolderSiblings(
    file: ScannedFile,
    scannedFiles: List<ScannedFile>
): List<ScannedFile> {
    if (file.path.isBlank()) return listOf(file)
    val folder = siblingFolder(file.path) ?: return listOf(file)
    val siblings = scannedFiles
        .filter { candidate ->
            candidate.number.isNotBlank() &&
                candidate.number.equals(file.number, ignoreCase = true) &&
                siblingFolder(candidate.path) == folder
        }
        .distinctBy { candidate -> candidate.path.ifBlank { candidate.fileName } }
    if (siblings.isNotEmpty()) return siblings
    val sourceBase = scannedFiles.firstOrNull { candidate ->
        candidate.number.isNotBlank() && candidate.number.equals(file.number, ignoreCase = true)
    } ?: return listOf(file)
    return sameFolderSiblings(sourceBase, scannedFiles)
}

private fun siblingFolder(path: String): String? =
    Path.of(path).parent?.toAbsolutePath()?.normalize()?.toString()

internal fun upsertScrapeTask(
    tasks: List<ScrapeTask>,
    updated: ScrapeTask
): List<ScrapeTask> {
    val index = tasks.indexOfFirst { task ->
        (task.path.isNotBlank() && updated.path.isNotBlank() && task.path == updated.path) ||
            task.fileName == updated.fileName
    }
    return if (index < 0) {
        tasks + updated
    } else {
        tasks.toMutableList().apply { set(index, updated) }
    }
}

internal fun upsertVideo(
    videos: List<Video>,
    updated: Video
): List<Video> {
    val index = videos.indexOfFirst { it.number == updated.number }
    return if (index < 0) {
        videos + updated
    } else {
        videos.toMutableList().apply { set(index, updated) }
    }
}
