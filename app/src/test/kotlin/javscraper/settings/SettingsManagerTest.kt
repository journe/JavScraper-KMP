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
        assertEquals(true, settings.createMovieFolders)
        assertEquals(true, settings.hardlinkInsteadOfCopy)
        assertEquals(true, settings.downloadImages)
        assertEquals(false, settings.autoScrape)
        assertEquals("en", settings.language)
        assertEquals(9, settings.enabledSites.size)
    }

    @Test
    fun `AppSettings default sites include expected scrapers`() {
        val settings = AppSettings()
        assertTrue(settings.enabledSites.contains("javbus"))
        assertTrue(settings.enabledSites.contains("javdb"))
        assertTrue(settings.enabledSites.contains("fc2"))
        assertTrue(settings.enabledSites.contains("javlibrary"))
    }

    @Test
    fun `AppSettings JSON serialization round-trip`() {
        val original = AppSettings(
            workerPath = "C:\\worker\\scraper-worker.exe",
            outputDir = "C:\\output",
            scanDir = "C:\\videos",
            scanRecursive = false,
            createMovieFolders = false,
            hardlinkInsteadOfCopy = false,
            downloadImages = false,
            autoScrape = true,
            language = "zh",
            enabledSites = listOf("javbus", "javdb")
        )
        val jsonStr = json.encodeToString(AppSettings.serializer(), original)
        val decoded = json.decodeFromString(AppSettings.serializer(), jsonStr)
        assertEquals(original.workerPath, decoded.workerPath)
        assertEquals(original.outputDir, decoded.outputDir)
        assertEquals(original.scanDir, decoded.scanDir)
        assertEquals(false, decoded.scanRecursive)
        assertEquals(false, decoded.createMovieFolders)
        assertEquals(false, decoded.hardlinkInsteadOfCopy)
        assertEquals(false, decoded.downloadImages)
        assertEquals(true, decoded.autoScrape)
        assertEquals("zh", decoded.language)
        assertEquals(2, decoded.enabledSites.size)
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