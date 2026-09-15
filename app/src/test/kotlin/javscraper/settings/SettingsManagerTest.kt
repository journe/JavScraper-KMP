package javscraper.settings

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SettingsManagerTest {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    @Test
    fun `AppSettings default values`() {
        val settings = AppSettings()
        assertEquals("worker/scraper-worker.exe", settings.workerPath)
        assertEquals("", settings.outputDir)
        assertEquals(true, settings.scanRecursive)
        assertEquals(emptyList(), settings.scanDirHistory)
        assertEquals(true, settings.createMovieFolders)
        assertEquals(true, settings.moveInsteadOfCopy)
        assertEquals(emptyMap(), settings.siteMirrorUrls)
        assertEquals(true, settings.downloadImages)
        assertEquals(false, settings.downloadPreviewImages)
        assertEquals(false, settings.downloadWebPages)
        assertEquals(false, settings.autoScrape)
        assertEquals(false, settings.fileLoggingEnabled)
        assertEquals("en", settings.language)
        assertEquals(11, settings.enabledSites.size)
        assertTrue(settings.enabledSites.contains("mmtv"))
        assertEquals(1.5f, settings.posterCropAspect)
        assertEquals(true, settings.lockData)
        assertEquals(15_000, settings.requestTimeoutMs)
    }

    @Test
    fun `AppSettings default sites include expected scrapers`() {
        val settings = AppSettings()
        assertTrue(settings.enabledSites.contains("javbus"))
        assertTrue(settings.enabledSites.contains("javdb"))
        assertTrue(settings.enabledSites.contains("fc2"))
        assertTrue(settings.enabledSites.contains("fc2mirror"))
        assertTrue(settings.enabledSites.contains("javlibrary"))
    }

    @Test
    fun `AppSettings JSON serialization round-trip`() {
        val original = AppSettings(
            workerPath = "C:\\worker\\scraper-worker.exe",
            outputDir = "C:\\output",
            scanDir = "C:\\videos",
            scanDirHistory = listOf("C:\\videos", "D:\\media"),
            scanRecursive = false,
            createMovieFolders = false,
            moveInsteadOfCopy = false,
            siteMirrorUrls = mapOf("javbus" to "https://www.dmmsee.casa/"),
            downloadImages = false,
            downloadPreviewImages = true,
            downloadWebPages = true,
            autoScrape = true,
            fileLoggingEnabled = true,
            language = "zh",
            enabledSites = listOf("javbus", "javdb"),
            posterCropAspect = 1.8f,
            lockData = false,
            requestTimeoutMs = 60_000
        )
        val jsonStr = json.encodeToString(AppSettings.serializer(), original)
        val decoded = json.decodeFromString(AppSettings.serializer(), jsonStr)
        assertEquals(original.workerPath, decoded.workerPath)
        assertEquals(original.outputDir, decoded.outputDir)
        assertEquals(original.scanDir, decoded.scanDir)
        assertEquals(original.scanDirHistory, decoded.scanDirHistory)
        assertEquals(false, decoded.scanRecursive)
        assertEquals(false, decoded.createMovieFolders)
        assertEquals(false, decoded.moveInsteadOfCopy)
        assertEquals(mapOf("javbus" to "https://www.dmmsee.casa/"), decoded.siteMirrorUrls)
        assertEquals(false, decoded.downloadImages)
        assertEquals(true, decoded.downloadPreviewImages)
        assertEquals(true, decoded.downloadWebPages)
        assertEquals(true, decoded.autoScrape)
        assertEquals(true, decoded.fileLoggingEnabled)
        assertEquals("zh", decoded.language)
        assertEquals(2, decoded.enabledSites.size)
        assertEquals(1.8f, decoded.posterCropAspect)
        assertEquals(false, decoded.lockData)
        assertEquals(60_000, decoded.requestTimeoutMs)
    }

    @Test
    fun `AppSettings ignores legacy hardlink key and defaults to move`() {
        val jsonStr = """{"hardlinkInsteadOfCopy": false}"""
        val settings = json.decodeFromString(AppSettings.serializer(), jsonStr)

        assertTrue(settings.moveInsteadOfCopy)
    }
    @Test
    fun `AppSettings scan directory history falls back to empty on missing key`() {
        val jsonStr = """{"workerPath": "custom.exe"}"""
        val settings = json.decodeFromString(AppSettings.serializer(), jsonStr)

        assertEquals(emptyList(), settings.scanDirHistory)
    }

    @Test
    fun `AppSettings posterCropAspect falls back to default on missing key`() {
        val jsonStr = """{"workerPath": "custom.exe"}"""
        val settings = json.decodeFromString(AppSettings.serializer(), jsonStr)
        assertEquals(1.5f, settings.posterCropAspect)
    }

    @Test
    fun `AppSettings request timeout options and fallback`() {
        // 选项恰为用户要求的四个档位
        assertEquals(listOf(5_000, 15_000, 30_000, 60_000), AppSettings.REQUEST_TIMEOUT_OPTIONS)
        assertTrue(AppSettings.REQUEST_TIMEOUT_OPTIONS.contains(AppSettings().requestTimeoutMs))
        // 旧配置缺少 requestTimeoutMs 键时回退默认值
        val jsonStr = """{"workerPath": "custom.exe"}"""
        val settings = json.decodeFromString(AppSettings.serializer(), jsonStr)
        assertEquals(15_000, settings.requestTimeoutMs)
    }

    @Test
    fun `AppSettings JSON handles unknown keys gracefully`() {
        val jsonStr = """{"unknown_key": "value", "workerPath": "custom.exe"}"""
        val settings = json.decodeFromString(AppSettings.serializer(), jsonStr)
        assertEquals("custom.exe", settings.workerPath)
    }

    @Test
    fun `SettingsManager get returns non-null settings`() {
        val settings = SettingsManager.get()
        assertNotNull(settings)
        assertTrue(settings.enabledSites.isNotEmpty())
    }
}
