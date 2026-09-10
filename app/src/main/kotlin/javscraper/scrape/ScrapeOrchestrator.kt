package javscraper.scrape

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
        return sidecar.scrape(sf.number, site, options.enabledSites?.toList(), options.downloadWebPages)
    }

    /** Fetch all metadata candidates without any file IO. */
    suspend fun fetchCandidates(sf: ScannedFile, site: String? = null): List<Video> {
        if (sf.number.isBlank()) return emptyList()
        return sidecar.searchCandidates(sf.number, site, options.enabledSites?.toList(), options.downloadWebPages)
    }
    /** Write scraped metadata (NFO/images) and organize files to the output directory. */
    suspend fun writeToDisk(files: List<ScannedFile>, video: Video): ScrapeResult {
        if (files.isEmpty())
            return ScrapeResult(false, error = ScrapeError(-1, "No files"))
        if (options.outputDir.isBlank())
            return ScrapeResult(false, error = ScrapeError(-1, "Output directory is empty"))

        val ioErrors = mutableListOf<String>()
        val firstPaths = resolveOutputPaths(files.first(), video)
        try {
            Files.createDirectories(firstPaths.folder)
        } catch (e: Exception) {
            log.error(e) { "Directory creation failed" }
            ioErrors += "Directory creation failed: ${e.message}"
        }
        try {
            Files.writeString(
                firstPaths.folder.resolve(firstPaths.nfoBase + ".nfo"),
                NfoWriter.generate(video, options.lockData)
            )
        } catch (e: Exception) {
            log.error(e) { "NFO failed" }
            ioErrors += "NFO failed: ${e.message}"
        }
        var mhtmlPath: Path? = null
        if (options.downloadWebPages) {
            try {
                mhtmlPath = writeWebpage(firstPaths.folder, video)
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
                            firstPaths.folder,
                            video.copy(sampleImages = emptyList())
                        )
                        if (!imageResult.success) ioErrors += "Images failed: ${imageResult.message}"
                    } catch (e: Exception) {
                        log.warn(e) { "Images failed" }
                        ioErrors += "Images failed: ${e.message}"
                    }
                }
                if (options.downloadPreviewImages) {
                    try {
                        ImageSaver.download(firstPaths.folder, sampleImages = video.sampleImages)
                    } catch (e: Exception) {
                        log.warn(e) { "Preview images failed" }
                        ioErrors += "Preview images failed: ${e.message}"
                    }
                }
            } else try {
                val previewImages = if (options.downloadPreviewImages) video.sampleImages else emptyList()
                ImageSaver.download(firstPaths.folder, video.coverUrl, video.posterUrl, previewImages)
            } catch (e: Exception) {
                log.warn(e) { "Images failed" }
                ioErrors += "Images failed: ${e.message}"
            }
        }

        files.forEach { sf ->
            try {
                val paths = resolveOutputPaths(sf, video)
                val src = Path.of(sf.path)
                val tgt = paths.fullPath
                if (!Files.exists(tgt)) {
                    if (options.hardlinkInsteadOfCopy) {
                        try {
                            Files.createLink(tgt, src)
                        } catch (e: Exception) {
                            log.warn(e) { "Hardlink failed, falling back to copy" }
                            Files.copy(src, tgt, StandardCopyOption.REPLACE_EXISTING)
                        }
                    } else {
                        Files.copy(src, tgt, StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            } catch (e: Exception) {
                log.warn(e) { "File move failed for ${sf.fileName}" }
                ioErrors += "File move failed for ${sf.fileName}: ${e.message}"
            }
        }

        return if (ioErrors.isEmpty()) {
            ScrapeResult(true, data = video.copy(path = firstPaths.fullPath.toString()))
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
    private fun resolveOutputPaths(sf: ScannedFile, video: Video): OutputPaths {
        val ext = sf.fileName.substringAfterLast('.')
        if (options.outputDir.isBlank()) return OutputPaths(Path.of(""), sf.fileName, Path.of(sf.fileName), sf.fileName.substringBeforeLast("."))

        val base = Path.of(options.outputDir)
        if (!options.createMovieFolders) return OutputPaths(base, sf.fileName, base.resolve(sf.fileName), sf.fileName.substringBeforeLast("."))

        val suffix = RenameFormatter.detectSuffix(sf.fileName, options.suffixKeywords)
        val layers = RenameFormatter.formatFolder(video, options.folderLayers, suffix)
        val folder = layers.fold(base) { acc, layer -> acc.resolve(layer) }
        val rawFilename = RenameFormatter.formatFilename(
            video, options.filenameFormat, suffix, options.maxFilenameLength, options.maxTitleLength
        )
        val nfoBase = RenameFormatter.stripPartSuffix(rawFilename, suffix)
        val filename = "$rawFilename.$ext"
        return OutputPaths(folder, filename, folder.resolve(filename), nfoBase)
    }

    private data class OutputPaths(
        val folder: Path, val filename: String, val fullPath: Path, val nfoBase: String
    )
}