package javscraper.io

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object ImageSaver {
    private val log = KotlinLogging.logger {}

    // 下载单张图片的连接/读取超时，防止网络无响应时无限期卡住写盘流程。
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 15_000

    // fanart → poster 复制失败时的重试次数与重试间隔（毫秒）。
    private const val POSTER_COPY_ATTEMPTS = 3
    private const val POSTER_COPY_RETRY_DELAY_MS = 300L

    /**
     * 下载封面/海报图片到 [outputDir]。
     *
     * 图片落盘规则（与 scraper-worker/core/webpage_archive.py 的 extract_images 保持一致）：
     * - [coverUrl]（封面，对应 Video.coverUrl）：下载为 fanart.jpg；
     * - [posterUrl]（海报，对应 Video.posterUrl）：下载为 poster.jpg；
     * - 当 posterUrl 为空或与 coverUrl 相同时，先下载封面为 fanart.jpg，
     *   再复制同一份内容为 poster.jpg（不做二次下载）；
     * - [sampleImages]（预览图）：保存到 extrafanart/fanartN.jpg。
     */
    suspend fun download(
        outputDir: Path,
        coverUrl: String = "",
        posterUrl: String = "",
        sampleImages: List<String> = emptyList(),
        /** 为 false 时禁止从 fanart 复制生成 poster（保护已存在的用户编辑版 poster）。 */
        copyPosterFromFanart: Boolean = true
    ): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val jobs = mutableListOf<Job>()
        Files.createDirectories(outputDir)

        val fanart = outputDir.resolve("fanart.jpg")
        val poster = outputDir.resolve("poster.jpg")

        // 封面 → fanart.jpg
        val fanartJob = if (coverUrl.isNotBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    dl(coverUrl, fanart)
                    result["fanart"] = fanart.toString()
                } catch (e: Exception) {
                    log.warn(e) { "fanart download failed" }
                }
            }
        } else {
            null
        }
        if (fanartJob != null) jobs.add(fanartJob)

        // 海报 → poster.jpg。
        // 有独立地址时直接下载；否则（或与封面相同）由 fanart 复制。
        // copyPosterFromFanart=false 时不做任何 poster 写入（已有用户编辑版）。
        val hasIndependentPoster = posterUrl.isNotBlank() && normalizeUrl(posterUrl) != normalizeUrl(coverUrl)
        if (hasIndependentPoster) {
            jobs.add(CoroutineScope(Dispatchers.IO).launch {
                try {
                    dl(posterUrl, poster)
                    result["poster"] = poster.toString()
                } catch (e: Exception) {
                    log.warn(e) { "poster download failed" }
                }
            })
        } else if (fanartJob != null && copyPosterFromFanart) {
            jobs.add(CoroutineScope(Dispatchers.IO).launch {
                fanartJob.join()
                if (!Files.isRegularFile(fanart)) return@launch
                try {
                    copyFanartToPoster(fanart, poster)
                    result["poster"] = poster.toString()
                } catch (e: Exception) {
                    log.warn(e) { "poster copy failed" }
                }
            })
        }

        if (sampleImages.isNotEmpty()) {
            val ed = outputDir.resolve("extrafanart")
            Files.createDirectories(ed)
            sampleImages.forEachIndexed { i, u ->
                jobs.add(
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            dl(u, ed.resolve("fanart${i + 1}.jpg"))
                        } catch (_: Exception) {
                        }
                    })
            }
        }
        jobs.joinAll()
        return result
    }

    /** 将 fanart 复制为 poster，短暂重试以应对媒体服务器短暂锁文件。 */
    private suspend fun copyFanartToPoster(fanart: Path, poster: Path) {
        var lastError: Exception? = null
        repeat(POSTER_COPY_ATTEMPTS) { attempt ->
            try {
                Files.copy(fanart, poster, StandardCopyOption.REPLACE_EXISTING)
                return
            } catch (e: Exception) {
                lastError = e
                if (attempt < POSTER_COPY_ATTEMPTS - 1) {
                    delay(POSTER_COPY_RETRY_DELAY_MS)
                }
            }
        }
        throw lastError ?: IllegalStateException("poster copy failed")
    }

    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.startsWith("//")) "https:$trimmed" else trimmed
    }

    private fun dl(url: String, target: Path) {
        val u = normalizeUrl(url)
        if (u.startsWith("data:")) return
        val connection = URL(u).openConnection()
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.getInputStream().use {
            Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}