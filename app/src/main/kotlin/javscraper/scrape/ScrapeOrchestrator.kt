package javscraper.scrape

import javscraper.io.FileScanner
import javscraper.io.ImageSaver
import javscraper.io.NfoWriter
import javscraper.io.RenameFormatter
import javscraper.models.*
import javscraper.sidecar.SidecarManager
import mu.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Base64

/** 依赖项：[SidecarManager] 负责抓取，[ScrapeOptions] 承载全部输出配置。 */
class ScrapeOrchestrator(
    private val sidecar: SidecarManager,
    private val options: ScrapeOptions,
    private val webpageArchiver: WebpageArchiver? = null
) {
    private val activeWebpageArchiver: WebpageArchiver by lazy { webpageArchiver ?: SidecarWebpageArchiver(sidecar) }
    private val log = KotlinLogging.logger {}

    suspend fun process(sf: ScannedFile, site: String? = null): ScrapeResult {
        return processParts(listOf(sf), site)
    }

    /** Fetch metadata only, without any file IO. */
    suspend fun fetch(sf: ScannedFile, site: String? = null): ScrapeResult {
        if (sf.number.isBlank())
            return ScrapeResult(false, error = ScrapeError(-1, "No number"))
        val result = sidecar.scrape(
            sf.number, site, options.enabledSites?.toList(), options.siteMirrorUrls, options.downloadWebPages
        )
        return result.copy(data = result.data?.copy(version = FileScanner.parseFileName(sf.fileName).version))
    }

    /** Fetch all metadata candidates without any file IO. */
    suspend fun fetchCandidates(sf: ScannedFile, site: String? = null): List<Video> {
        if (sf.number.isBlank()) return emptyList()
        val version = FileScanner.parseFileName(sf.fileName).version
        return sidecar.searchCandidates(
            sf.number, site, options.enabledSites?.toList(), options.siteMirrorUrls, options.downloadWebPages
        ).map { it.copy(version = version) }
    }
    /** Write scraped metadata (NFO/images) and organize files to the output directory. */
    suspend fun writeToDisk(files: List<ScannedFile>, video: Video): ScrapeResult {
        if (files.isEmpty())
            return ScrapeResult(false, error = ScrapeError(-1, "No files"))
        if (options.outputDir.isBlank())
            return ScrapeResult(false, error = ScrapeError(-1, "Output directory is empty"))

        val outputVideo = video.withFileVersion(files)
        val ioErrors = mutableListOf<String>()
        val writePlan = resolveWritePlan(files, video)
        try {
            Files.createDirectories(writePlan.folder)
        } catch (e: Exception) {
            log.error(e) { "Directory creation failed" }
            ioErrors += "Directory creation failed: ${e.message}"
        }
        try {
            Files.writeString(
                writePlan.folder.resolve(writePlan.nfoBase + ".nfo"),
                NfoWriter.generate(outputVideo, options.lockData)
            )
        } catch (e: Exception) {
            log.error(e) { "NFO failed" }
            ioErrors += "NFO failed: ${e.message}"
        }
        var mhtmlPath: Path? = null
        if (options.downloadWebPages) {
            try {
                mhtmlPath = writeWebpage(writePlan.folder, outputVideo)
                if (mhtmlPath == null) ioErrors += "Webpage content missing"
            } catch (e: Exception) {
                log.error(e) { "Webpage failed" }
                ioErrors += "Webpage failed: ${e.message}"
            }
        }
        if (options.downloadImages) {
            if (options.downloadWebPages) {
                val path = mhtmlPath
                if (path != null) {
                    try {
                        val imageResult = activeWebpageArchiver.extractImages(
                            path,
                            writePlan.folder,
                            outputVideo.copy(sampleImages = emptyList())
                        )
                        if (!imageResult.success) ioErrors += "Images failed: ${imageResult.message}"
                    } catch (e: Exception) {
                        log.warn(e) { "Images failed" }
                        ioErrors += "Images failed: ${e.message}"
                    }
                }
                if (options.downloadPreviewImages) {
                    try {
                        ImageSaver.download(writePlan.folder, sampleImages = outputVideo.sampleImages)
                    } catch (e: Exception) {
                        log.warn(e) { "Preview images failed" }
                        ioErrors += "Preview images failed: ${e.message}"
                    }
                }
            } else try {
                val previewImages = if (options.downloadPreviewImages) outputVideo.sampleImages else emptyList()
                ImageSaver.download(writePlan.folder, outputVideo.coverUrl, outputVideo.posterUrl, previewImages)
            } catch (e: Exception) {
                log.warn(e) { "Images failed" }
                ioErrors += "Images failed: ${e.message}"
            }
        }

        writePlan.files.forEach { planned ->
            try {
                val src = Path.of(planned.source.path)
                val tgt = planned.target
                if (!Files.exists(tgt)) {
                    if (options.moveInsteadOfCopy) {
                        Files.move(src, tgt, StandardCopyOption.REPLACE_EXISTING)
                    } else {
                        Files.copy(src, tgt, StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            } catch (e: Exception) {
                log.warn(e) { "File move failed for ${planned.source.fileName}" }
                ioErrors += "File move failed for ${planned.source.fileName}: ${e.message}"
            }
        }

        return if (ioErrors.isEmpty()) {
            ScrapeResult(true, data = outputVideo.copy(path = writePlan.files.first().target.toString()))
        } else {
            ScrapeResult(false, error = ScrapeError(-20, ioErrors.joinToString("; ")))
        }
    }

    suspend fun processParts(files: List<ScannedFile>, site: String? = null): ScrapeResult {
        if (files.isEmpty() || files.first().number.isBlank())
            return ScrapeResult(false, error = ScrapeError(-1, "No number"))

        val result = fetch(files.first(), site)
        if (!result.success || result.data == null) return result
        return writeToDisk(files, result.data)
    }

    private fun writeWebpage(folder: Path, video: Video): Path? {
        if (!options.downloadWebPages || video.webpage.isBlank()) return null
        val target = folder.resolve(webpageFileName(video))
        Files.write(target, Base64.getMimeDecoder().decode(video.webpage))
        return target
    }

    private fun webpageFileName(video: Video): String {
        val invalid = Regex("""[\\/:*?"<>|]""")
        val number = invalid.replace(video.number, "_").trim('.', ' ')
        val site = invalid.replace(video.source.uppercase(), "_").trim('.', ' ')
        val safeNumber = number.ifBlank { "UNKNOWN" }
        val safeSite = site.ifBlank { "SITE" }
        return "$safeNumber-$safeSite.mhtml"
    }

    private fun resolveWritePlan(files: List<ScannedFile>, video: Video): FileWritePlan {
        if (files.size == 1) {
            val paths = resolveOutputPaths(files.first(), video)
            return FileWritePlan(
                folder = paths.folder,
                nfoBase = paths.nfoBase,
                files = listOf(PlannedFile(files.first(), paths.fullPath))
            )
        }

        val ordered = files.sortedWith(
            compareBy<ScannedFile> {
                val label = FileScanner.parseFileName(it.fileName).versionLabel
                when {
                    label.isBlank() -> 0
                    label.toIntOrNull() != null -> 1
                    else -> 2
                }
            }.thenBy {
                FileScanner.parseFileName(it.fileName).versionLabel.toIntOrNull() ?: Int.MAX_VALUE
            }.thenBy { it.fileName.lowercase() }
        )
        val kind = detectMultiFileKind(ordered)
        val labels = buildMultiFileLabels(ordered, kind)
        val labelSuffixes = labels.map { " - $it" }
        val layers = RenameFormatter.formatFolder(video, options.folderLayers, "")
        val baseName = RenameFormatter.sanitize(video.number).ifBlank { "UNKNOWN" }

        val base = Path.of(options.outputDir)
        val parentLayers = if (options.createMovieFolders && layers.size > 1) {
            layers.dropLast(1)
        } else {
            emptyList()
        }
        val layeredFolder = parentLayers.fold(base) { path, layer -> path.resolve(layer) }
        val folder = if (options.createMovieFolders) layeredFolder.resolve(baseName) else base
        val plannedFiles = ordered.mapIndexed { index, source ->
            val ext = source.fileName.substringAfterLast('.', "")
            val namedBase = appendVersionSuffix(
                baseName + labelSuffixes[index],
                FileScanner.parseFileName(source.fileName).version
            )
            val filename = buildString {
                append(namedBase)
                if (ext.isNotBlank()) append('.').append(ext)
            }
            PlannedFile(source, folder.resolve(filename))
        }
        return FileWritePlan(folder = folder, nfoBase = baseName, files = plannedFiles)
    }

    private fun detectMultiFileKind(files: List<ScannedFile>): MultiFileKind {
        val labels = files.map { FileScanner.parseFileName(it.fileName).versionLabel }
        if (labels.any(::isPartLabel)) return MultiFileKind.PARTS
        if (labels.any { it.toIntOrNull() != null }) return MultiFileKind.PARTS
        return MultiFileKind.VERSIONS
    }

    private fun buildMultiFileLabels(
        files: List<ScannedFile>,
        kind: MultiFileKind
    ): List<String> {
        val used = mutableSetOf<String>()
        return files.mapIndexed { index, file ->
            val fileInfo = FileScanner.parseFileName(file.fileName)
            val label = when (kind) {
                MultiFileKind.PARTS -> partLabel(fileInfo.versionLabel, index)
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

    private fun partLabel(rawLabel: String, index: Int): String = when {
        rawLabel.isBlank() -> "part${index + 1}"
        rawLabel.toIntOrNull() != null -> "part${rawLabel.toInt()}"
        isPartLabel(rawLabel) -> RenameFormatter.sanitize(rawLabel)
        else -> RenameFormatter.sanitize(rawLabel).ifBlank { "part${index + 1}" }
    }

    private fun versionLabel(rawLabel: String, index: Int, version: String): String = when {
        rawLabel.isBlank() && version.isNotBlank() -> version
        rawLabel.isBlank() -> "version${index + 1}"
        rawLabel.toIntOrNull() != null -> "v${rawLabel.toInt()}"
        else -> RenameFormatter.sanitize(rawLabel).ifBlank { "version${index + 1}" }
    }

    private fun isPartLabel(value: String): Boolean =
        value.replace(" ", "").matches(partLabelRegex)

    private fun resolveOutputPaths(sf: ScannedFile, video: Video): OutputPaths {
        val ext = sf.fileName.substringAfterLast('.')
        if (options.outputDir.isBlank()) return OutputPaths(Path.of(""), sf.fileName, Path.of(sf.fileName), sf.fileName.substringBeforeLast("."))

        val base = Path.of(options.outputDir)
        if (!options.createMovieFolders) return OutputPaths(base, sf.fileName, base.resolve(sf.fileName), sf.fileName.substringBeforeLast("."))

        val suffix = RenameFormatter.detectSuffix(sf.fileName, options.suffixKeywords)
        val fileInfo = FileScanner.parseFileName(sf.fileName)
        val layers = RenameFormatter.formatFolder(video, options.folderLayers, suffix)
        val folder = layers.fold(base) { acc, layer -> acc.resolve(layer) }
        val rawFilename = RenameFormatter.formatFilename(
            video, options.filenameFormat, suffix, options.maxFilenameLength, options.maxTitleLength
        )
        val nfoBase = RenameFormatter.stripPartSuffix(rawFilename, suffix)
        val filename = "${appendVersionSuffix(rawFilename, fileInfo.version)}.$ext"
        return OutputPaths(folder, filename, folder.resolve(filename), nfoBase)
    }

    private fun Video.withFileVersion(files: List<ScannedFile>): Video {
        val versions = listOf(version) + files.map { FileScanner.parseFileName(it.fileName).version }
        return copy(version = mergeVersions(versions))
    }

    private fun mergeVersions(values: List<String>): String {
        val chars = values.flatMap { it.uppercase().asSequence() }.toSet()
        return buildString {
            if ('C' in chars) append('C')
            if ('U' in chars) append('U')
        }
    }

    private fun appendVersionSuffix(name: String, version: String): String {
        if (version.isBlank() || name.endsWith("-$version", ignoreCase = true)) return name
        return "$name-$version"
    }

    private data class OutputPaths(
        val folder: Path, val filename: String, val fullPath: Path, val nfoBase: String
    )

    private data class PlannedFile(val source: ScannedFile, val target: Path)

    private data class FileWritePlan(
        val folder: Path,
        val nfoBase: String,
        val files: List<PlannedFile>
    )

    private enum class MultiFileKind {
        PARTS,
        VERSIONS
    }
}

private val partLabelRegex = Regex("""(?i)^(?:cd|dvd|part|pt|disc|disk)\d+$""")
