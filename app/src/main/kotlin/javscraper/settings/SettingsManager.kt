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
    val scanRecursive: Boolean = true, val createMovieFolders: Boolean = true, val hardlinkInsteadOfCopy: Boolean = true,
    val downloadImages: Boolean = true, val downloadPreviewImages: Boolean = false,
    val downloadWebPages: Boolean = false, val autoScrape: Boolean = false, val fileLoggingEnabled: Boolean = false,
    val language: String = "en",
    val enabledSites: List<String> = listOf("javbus","javdb","jav321","dmm","javlibrary","avsox","d2pass","fc2", "fc2mirror","heyzo","mmtv"),
    val folderLayers: List<String> = listOf("{num} {title}"),
    val filenameFormat: String = "{num} {title}",
    val maxTitleLength: Int = 50,
    val maxFilenameLength: Int = 60,
    val suffixKeywords: List<String> = listOf("-cd1", "-cd2", "-4k", "-uc"),
    /** 封面裁剪高宽比(高/宽),默认 1.5 = 2:3 海报标准,范围见 PosterCropper。 */
    val posterCropAspect: Float = 1.5f
)

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
