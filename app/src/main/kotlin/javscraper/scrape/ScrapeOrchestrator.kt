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

class ScrapeOrchestrator(
    private val sidecar: SidecarManager,
    private val outputDir: String,
    private val createMovieFolders: Boolean = true,
    private val hardlinkInsteadOfCopy: Boolean = true,
    private val downloadImages: Boolean = true,
    private val folderLayers: List<String> = listOf("{num} {title}"),
    private val filenameFormat: String = "{num} {title}",
    private val maxTitleLength: Int = 50,
    private val maxFilenameLength: Int = 60,
    private val suffixKeywords: List<String> = listOf("-cd1", "-cd2", "-4k", "-uc")
) {
    private val log = KotlinLogging.logger {}

    suspend fun process(sf: ScannedFile, site: String? = null): ScrapeResult {
        if (sf.number.isBlank()) return ScrapeResult(false, error = ScrapeError(-1, "No number"))
        log.info { "Processing ${sf.number}" }
        val result = sidecar.scrape(sf.number, site)
        if (!result.success || result.data == null) return result
        val video = result.data
        val paths = resolveOutputPaths(sf, video)
        Files.createDirectories(paths.folder)
        try {
            Files.writeString(paths.folder.resolve("${video.number}.nfo"), NfoWriter.generate(video))
        } catch (e: Exception) { log.error(e) { "NFO failed" } }
        if (downloadImages) try {
            ImageSaver.download(paths.folder, video.coverUrl, video.posterUrl, video.sampleImages)
        } catch (e: Exception) { log.warn(e) { "Images failed" } }
        try {
            val src = Path.of(sf.path)
            val tgt = paths.fullPath
            if (!Files.exists(tgt)) {
                if (hardlinkInsteadOfCopy) {
                    try { Files.createLink(tgt, src) }
                    catch (_: Exception) { Files.copy(src, tgt, StandardCopyOption.REPLACE_EXISTING) }
                } else Files.copy(src, tgt, StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (e: Exception) { log.warn(e) { "File move failed" } }
        return result
    }

    private fun resolveOutputPaths(sf: ScannedFile, video: Video): OutputPaths {
        val ext = sf.fileName.substringAfterLast('.')
        if (outputDir.isBlank()) return OutputPaths(Path.of(""), sf.fileName, Path.of(sf.fileName))

        val base = Path.of(outputDir)
        if (!createMovieFolders) return OutputPaths(base, sf.fileName, base.resolve(sf.fileName))

        val suffix = RenameFormatter.detectSuffix(sf.fileName, suffixKeywords)
        val layers = RenameFormatter.formatFolder(video, folderLayers, suffix)
        val folder = layers.fold(base) { acc, layer -> acc.resolve(layer) }
        val rawFilename = RenameFormatter.formatFilename(
            video, filenameFormat, suffix, maxFilenameLength, maxTitleLength
        )
        return OutputPaths(folder, "$rawFilename.$ext", folder.resolve("$rawFilename.$ext"))
    }

    private data class OutputPaths(val folder: Path, val filename: String, val fullPath: Path)
}
