package javscraper.io

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object ImageSaver {
    private val log = KotlinLogging.logger {}

    /**
     * 下载封面/海报图片到 [outputDir]。
     *
     * 图片落盘规则（与 scraper-worker/core/webpage_archive.py 的 extract_images 保持一致）：
     * - [coverUrl]（站点封面，对应 Video.coverUrl）：下载保存为 poster.jpg，
     *   随后复制同一份内容为 fanart.jpg（不做二次下载）；
     * - [fanartUrl]（独立海报，对应 Video.posterUrl）：当前所有刮削器均未填充 poster_url，
     *   此分支实际不会触发；若未来某站点提供独立海报，则按 URL 覆盖下载为 fanart.jpg；
     * - [sampleImages]（预览图）：保存到 extrafanart/fanartN.jpg。
     *
     * 因此正常刮削结果为：每个影片目录得到内容相同的 poster.jpg + fanart.jpg 两份封面。
     */
    suspend fun download(
        outputDir: Path,
        coverUrl: String = "",
        fanartUrl: String = "",
        sampleImages: List<String> = emptyList()
    ): Map<String, String> {
        val result = mutableMapOf<String, String>();
        val jobs = mutableListOf<Job>()
        Files.createDirectories(outputDir)
        if (coverUrl.isNotBlank()) jobs.add(CoroutineScope(Dispatchers.IO).launch {
            try {
                val t = outputDir.resolve("poster.jpg"); dl(coverUrl, t); result["poster"] =
                    t.toString();
                val fanart = outputDir.resolve("fanart.jpg"); Files.copy(
                    t,
                    fanart,
                    StandardCopyOption.REPLACE_EXISTING
                ); result["fanart"] = fanart.toString()
            } catch (e: Exception) {
                log.warn(e) { "poster failed" }
            }
        })
        // 兑底分支：仅当站点提供独立海报地址（Video.posterUrl 非空）时才会执行，
        // 用它覆盖下载 fanart.jpg；目前没有任何刮削器填充该字段，属子保留的兼容路径。
        if (fanartUrl.isNotBlank()) jobs.add(CoroutineScope(Dispatchers.IO).launch {
            try {
                val t = outputDir.resolve("fanart.jpg"); dl(fanartUrl, t); result["fanart"] =
                    t.toString()
            } catch (e: Exception) {
                log.warn(e) { "fanart failed" }
            }
        })
        if (sampleImages.isNotEmpty()) {
            val ed =
                outputDir.resolve("extrafanart"); Files.createDirectories(ed); sampleImages.forEachIndexed { i, u ->
                jobs.add(
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            dl(u, ed.resolve("fanart${i + 1}.jpg"))
                        } catch (_: Exception) {
                        }
                    })
            }
        }
        jobs.joinAll(); return result
    }

    private fun dl(url: String, target: Path) {
        val u =
            if (url.startsWith("//")) "https:$url" else url; if (u.startsWith("data:")) return; URL(
            u
        ).openStream().use { Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING) }
    }
}