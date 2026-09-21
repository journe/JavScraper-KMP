package javscraper.settings

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Serializable
data class AppSettings(
    val workerPath: String = "worker/scraper-worker.exe", val outputDir: String = "", val scanDir: String = "",
    val scanDirHistory: List<String> = emptyList(),
    val scanRecursive: Boolean = true, val createMovieFolders: Boolean = true,
    /** true 表示移动原视频，false 表示复制并保留源文件。 */
    val moveInsteadOfCopy: Boolean = true,
    val downloadImages: Boolean = true, val downloadPreviewImages: Boolean = false,
    val downloadWebPages: Boolean = false, val autoScrape: Boolean = false, val fileLoggingEnabled: Boolean = false,
    /** NFO 中是否写入 `lockdata` 标记（媒体服务器据此锁定元数据）。 */
    val lockData: Boolean = true,
    /** 更新模式：复用旧媒体目录资产，并字段级修改既有 NFO。 */
    val updateMode: Boolean = false,
    val language: String = "en",
    val enabledSites: List<String> = listOf("javbus","javdb","jav321","dmm","javlibrary","avsox","d2pass","fc2", "fc2mirror","heyzo","mmtv","madouqu","mdtv","hdouban","cnmdb","javday"),
    val siteMirrorUrls: Map<String, String> = emptyMap(),
    val javdbSessionCookie: String = "",
    val folderLayers: List<String> = listOf("{num} {title}"),
    val filenameFormat: String = "{num} {title}",
    val maxTitleLength: Int = 50,
    val maxFilenameLength: Int = 60,
    val suffixKeywords: List<String> = listOf("-cd1", "-cd2", "-4k", "-uc"),
    /** 封面裁剪高宽比(高/宽),默认 1.5 = 2:3 海报标准,范围见 PosterCropper。 */
    val posterCropAspect: Float = 1.5f,
    /** 裁剪海报时是否添加水印,对齐 mdcx poster_mark 默认开启。 */
    val posterWatermarkEnabled: Boolean = true,
    /** 水印大小:水印高度 = 海报高度 * size / 40。 */
    val posterWatermarkSize: Int = DEFAULT_POSTER_WATERMARK_SIZE,
    /** 单次刮削请求等待 worker 响应的超时（毫秒），可选项见 [REQUEST_TIMEOUT_OPTIONS]。 */
    val requestTimeoutMs: Int = 15_000
) {
    companion object {
        const val MIN_POSTER_WATERMARK_SIZE = 1
        const val MAX_POSTER_WATERMARK_SIZE = 10
        const val DEFAULT_POSTER_WATERMARK_SIZE = 5
        /** 设置页可选的超时时间（毫秒）。 */
        val REQUEST_TIMEOUT_OPTIONS: List<Int> = listOf(5_000, 15_000, 30_000, 60_000)
    }
}

object SettingsManager {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val configDir: Path by lazy { val d = Paths.get(System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home"), "JavScraper"); Files.createDirectories(d); d }
    private val configFile: Path by lazy { configDir.resolve("config.json") }
    private var cached: AppSettings = load()
    fun get(): AppSettings = cached
    fun update(updater: (AppSettings) -> AppSettings) { cached = updater(cached); save() }
    fun reset() { cached = AppSettings(); save() }
    private fun load(): AppSettings = try { if (Files.exists(configFile)) json.decodeFromString(Files.readString(configFile)) else AppSettings().also { save() } } catch (e: Exception) { AppSettings() }
    private fun save() { try { Files.writeString(configFile, json.encodeToString(cached)) } catch (e: Exception) {} }
}
internal fun AppSettings.withNormalizedWatermarkSize(): AppSettings = copy(
    posterWatermarkSize = posterWatermarkSize.coerceIn(
        AppSettings.MIN_POSTER_WATERMARK_SIZE,
        AppSettings.MAX_POSTER_WATERMARK_SIZE
    )
)
