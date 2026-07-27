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
    val downloadImages: Boolean = true, val autoScrape: Boolean = false,
    val enabledSites: List<String> = listOf("javbus","javdb","jav321","dmm","javlibrary","avsox","d2pass","fc2","heyzo")
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