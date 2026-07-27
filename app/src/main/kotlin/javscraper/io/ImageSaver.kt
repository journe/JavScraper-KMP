package javscraper.io

import kotlinx.coroutines.*
import mu.KotlinLogging
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object ImageSaver {
    private val log = KotlinLogging.logger {}

    suspend fun download(outputDir: Path, coverUrl: String = "", fanartUrl: String = "", sampleImages: List<String> = emptyList()): Map<String, String> {
        val result = mutableMapOf<String, String>(); val jobs = mutableListOf<Job>()
        Files.createDirectories(outputDir)
        if (coverUrl.isNotBlank()) jobs.add(CoroutineScope(Dispatchers.IO).launch { try { val t = outputDir.resolve("poster.jpg"); dl(coverUrl, t); result["poster"] = t.toString() } catch (e: Exception) { log.warn(e) { "poster failed" } } })
        if (fanartUrl.isNotBlank()) jobs.add(CoroutineScope(Dispatchers.IO).launch { try { val t = outputDir.resolve("fanart.jpg"); dl(fanartUrl, t); result["fanart"] = t.toString() } catch (e: Exception) { log.warn(e) { "fanart failed" } } })
        if (sampleImages.isNotEmpty()) { val ed = outputDir.resolve("extrafanart"); Files.createDirectories(ed); sampleImages.forEachIndexed { i, u -> jobs.add(CoroutineScope(Dispatchers.IO).launch { try { dl(u, ed.resolve("fanart${i+1}.jpg")) } catch (_: Exception) {} }) } }
        jobs.joinAll(); return result
    }
    private fun dl(url: String, target: Path) { val u = if (url.startsWith("//")) "https:$url" else url; if (u.startsWith("data:")) return; URL(u).openStream().use { Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING) } }
}