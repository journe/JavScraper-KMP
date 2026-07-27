package javscraper.scrape

import javscraper.io.ImageSaver
import javscraper.io.NfoWriter
import javscraper.models.*
import javscraper.sidecar.SidecarManager
import mu.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class ScrapeOrchestrator(
    private val sidecar: SidecarManager, private val outputDir: String,
    private val createMovieFolders: Boolean = true, private val hardlinkInsteadOfCopy: Boolean = true,
    private val downloadImages: Boolean = true
) {
    private val log = KotlinLogging.logger {}

    suspend fun process(sf: ScannedFile, site: String? = null): ScrapeResult {
        if (sf.number.isBlank()) return ScrapeResult(false, error = ScrapeError(-1, "No number"))
        log.info { "Processing ${sf.number}" }
        val result = sidecar.scrape(sf.number, site)
        if (!result.success || result.data == null) return result
        val video = result.data; val out = getOut(sf.number, video); Files.createDirectories(out)
        try { Files.writeString(out.resolve("${sf.number}.nfo"), NfoWriter.generate(video)) } catch (e: Exception) { log.error(e) { "NFO failed" } }
        if (downloadImages) try { ImageSaver.download(out, video.coverUrl, video.posterUrl, video.sampleImages) } catch (e: Exception) { log.warn(e) { "Images failed" } }
        try { val src = Path.of(sf.path); val tgt = out.resolve(sf.fileName); if (!Files.exists(tgt)) { if (hardlinkInsteadOfCopy) { try { Files.createLink(tgt, src) } catch (_: Exception) { Files.copy(src, tgt, StandardCopyOption.REPLACE_EXISTING) } } else Files.copy(src, tgt, StandardCopyOption.REPLACE_EXISTING) } } catch (e: Exception) { log.warn(e) { "File move failed" } }
        return result
    }
    private fun getOut(number: String, v: Video): Path {
        if (outputDir.isBlank()) return Path.of(number)
        val base = Path.of(outputDir)
        return if (createMovieFolders) base.resolve(buildString { append(number); if (v.maker.isNotBlank()) append(" [${v.maker}]"); if (v.actresses.isNotEmpty()) append(" ${v.actresses.joinToString(" ")}") }) else base
    }
}