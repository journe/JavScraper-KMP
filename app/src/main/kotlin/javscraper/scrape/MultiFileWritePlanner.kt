package javscraper.scrape

import javscraper.io.FileScanner
import javscraper.io.RenameFormatter
import javscraper.models.ScannedFile
import javscraper.models.Video
import java.nio.file.Path

internal data class PlannedFile(
    val source: ScannedFile,
    val target: Path,
    val label: String
)

internal data class FileWritePlan(
    val folder: Path,
    val nfoBase: String,
    val files: List<PlannedFile>,
    val kind: MultiFileKind
)

internal enum class MultiFileKind {
    PARTS,
    VERSIONS
}

/** Plans one movie folder for a same-number file group. */
internal class MultiFileWritePlanner(private val options: ScrapeOptions) {
    fun plan(
        files: List<ScannedFile>,
        video: Video,
        fallbackBase: Path? = null
    ): FileWritePlan {
        val ordered = files.sortedWith(
            compareBy<ScannedFile> {
                val label = FileScanner.parseFileName(it.fileName).versionLabel
                when {
                    label.isBlank() -> 0
                    FileScanner.multiPartLabelNumber(label) != null -> 1
                    else -> 2
                }
            }.thenBy {
                FileScanner.parseFileName(it.fileName).versionLabel.let(FileScanner::multiPartLabelNumber)
                    ?: Int.MAX_VALUE
            }.thenBy { it.fileName.lowercase() }
        )
        val kind = detectMultiFileKind(ordered)
        val labels = buildMultiFileLabels(ordered, kind)
        val layers = RenameFormatter.formatFolder(video, options.folderLayers, "")
        val numberBase = RenameFormatter.sanitize(video.number).ifBlank { "UNKNOWN" }
        val base = fallbackBase ?: Path.of(options.outputDir)

        val folder = when {
            options.createMovieFolders && layers.isNotEmpty() && kind == MultiFileKind.PARTS ->
                layers.fold(base) { path, layer -> path.resolve(layer) }
            options.createMovieFolders && layers.isNotEmpty() -> {
                val parentLayers = layers.dropLast(1)
                parentLayers.fold(base) { path, layer -> path.resolve(layer) }.resolve(numberBase)
            }
            options.createMovieFolders -> base.resolve(numberBase)
            else -> base
        }
        val videoBase = when {
            options.createMovieFolders && layers.isNotEmpty() && kind == MultiFileKind.PARTS ->
                folder.fileName.toString()
            else -> numberBase
        }
        val plannedFiles = ordered.mapIndexed { index, source ->
            val fileInfo = FileScanner.parseFileName(source.fileName)
            val suffix = appendVersionSuffix(labels[index], fileInfo.version)
            val ext = source.fileName.substringAfterLast('.', "")
            val filename = buildString {
                append(videoBase)
                append(" - ")
                append(suffix)
                if (ext.isNotBlank()) append('.').append(ext)
            }
            PlannedFile(source, folder.resolve(filename), labels[index])
        }
        val nfoBase = if (kind == MultiFileKind.PARTS) "movie" else videoBase
        return FileWritePlan(folder, nfoBase, plannedFiles, kind)
    }

    private fun detectMultiFileKind(files: List<ScannedFile>): MultiFileKind {
        val labels = files.map { FileScanner.parseFileName(it.fileName).versionLabel }
        if (labels.any(FileScanner::isMultiPartLabel)) return MultiFileKind.PARTS
        return MultiFileKind.VERSIONS
    }

    private fun buildMultiFileLabels(
        files: List<ScannedFile>,
        kind: MultiFileKind
    ): List<String> {
        val used = mutableSetOf<String>()
        val partNumbers = if (kind == MultiFileKind.PARTS) buildPartNumbers(files) else emptyList()
        return files.mapIndexed { index, file ->
            val fileInfo = FileScanner.parseFileName(file.fileName)
            val label = when (kind) {
                MultiFileKind.PARTS -> partLabel(partNumbers[index])
                MultiFileKind.VERSIONS -> versionLabel(fileInfo.versionLabel, index, fileInfo.version)
            }
            var candidate = label
            var duplicateIndex = 2
            while (!used.add(candidate.lowercase())) {
                candidate = "$label $duplicateIndex"
                duplicateIndex++
            }
            candidate
        }
    }

    private fun buildPartNumbers(files: List<ScannedFile>): List<Int> {
        val explicit = files.map { file ->
            FileScanner.multiPartLabelNumber(FileScanner.parseFileName(file.fileName).versionLabel)
        }
        val used = explicit.filterNotNull().toMutableSet()
        val seen = mutableSetOf<Int>()
        return explicit.map { requested ->
            if (requested != null && seen.add(requested)) requested
            else nextPartNumber(used).also(used::add)
        }
    }

    private fun nextPartNumber(used: Set<Int>): Int =
        generateSequence(1) { it + 1 }.first { it !in used }

    private fun partLabel(number: Int): String = "${options.multiPartSuffix.value}$number"

    private fun versionLabel(rawLabel: String, index: Int, version: String): String = when {
        rawLabel.isBlank() && version.isNotBlank() -> version
        rawLabel.isBlank() -> "version${index + 1}"
        rawLabel.toIntOrNull() != null -> "v${rawLabel.toInt()}"
        else -> RenameFormatter.sanitize(rawLabel).ifBlank { "version${index + 1}" }
    }

    private fun appendVersionSuffix(name: String, version: String): String {
        if (version.isBlank() || name.endsWith("-$version", ignoreCase = true)) return name
        return "$name-$version"
    }
}
