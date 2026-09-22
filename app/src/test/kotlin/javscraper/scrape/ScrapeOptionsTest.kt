package javscraper.scrape

import javscraper.settings.AppSettings
import javscraper.settings.MultiPartSuffix
import kotlin.test.Test
import kotlin.test.assertEquals

class ScrapeOptionsTest {

    @Test
    fun `from projects every settings field`() {
        val settings = AppSettings(
            scanDir = "D:/Scan",
            outputDir = "D:/Media",
            createMovieFolders = false,
            moveInsteadOfCopy = false,
            siteMirrorUrls = mapOf("javbus" to "https://www.dmmsee.casa"),
            downloadImages = false,
            downloadPreviewImages = true,
            downloadWebPages = true,
            updateMode = true,
            lockData = false,
            folderLayers = listOf("{num}"),
            filenameFormat = "{num}",
            maxTitleLength = 12,
            maxFilenameLength = 34,
            suffixKeywords = listOf("-cd1"),
            multiPartSuffix = MultiPartSuffix.DISC,
            enabledSites = listOf("javbus", "dmm")
        )

        val options = ScrapeOptions.from(settings)

        assertEquals("D:/Scan", options.scanDir)
        assertEquals("D:/Media", options.outputDir)
        assertEquals(false, options.createMovieFolders)
        assertEquals(false, options.moveInsteadOfCopy)
        assertEquals(mapOf("javbus" to "https://www.dmmsee.casa"), options.siteMirrorUrls)
        assertEquals(false, options.downloadImages)
        assertEquals(true, options.downloadPreviewImages)
        assertEquals(true, options.downloadWebPages)
        assertEquals(true, options.updateMode)
        assertEquals(false, options.lockData)
        assertEquals(listOf("{num}"), options.folderLayers)
        assertEquals("{num}", options.filenameFormat)
        assertEquals(12, options.maxTitleLength)
        assertEquals(34, options.maxFilenameLength)
        assertEquals(listOf("-cd1"), options.suffixKeywords)
        assertEquals(MultiPartSuffix.DISC, options.multiPartSuffix)
        assertEquals(setOf("javbus", "dmm"), options.enabledSites)
    }

    @Test
    fun `from keeps AppSettings defaults as the single source of defaults`() {
        val defaults = AppSettings()

        val options = ScrapeOptions.from(defaults)

        assertEquals(defaults.folderLayers, options.folderLayers)
        assertEquals(defaults.filenameFormat, options.filenameFormat)
        assertEquals(defaults.maxTitleLength, options.maxTitleLength)
        assertEquals(defaults.maxFilenameLength, options.maxFilenameLength)
        assertEquals(defaults.suffixKeywords, options.suffixKeywords)
        assertEquals(defaults.lockData, options.lockData)
        assertEquals(defaults.siteMirrorUrls, options.siteMirrorUrls)
        assertEquals(defaults.enabledSites.toSet(), options.enabledSites)
    }
}
