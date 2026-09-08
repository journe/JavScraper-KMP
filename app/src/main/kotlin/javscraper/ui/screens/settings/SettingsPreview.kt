package javscraper.ui.screens.settings

import androidx.compose.runtime.Composable
import javscraper.i18n.LocalTranslations
import javscraper.i18n.TranslationZh
import javscraper.models.SiteInfo
import javscraper.ui.theme.JavScraperTheme

/** Wraps previews with app theme and Chinese translations. */
@Composable
internal fun SettingsPreview(content: @Composable () -> Unit) {
    JavScraperTheme {
        androidx.compose.runtime.CompositionLocalProvider(LocalTranslations provides TranslationZh()) {
            content()
        }
    }
}

/** Sample settings state for previews. */
internal fun previewSettingsState(): SettingsState = SettingsState(
    workerPath = "C:/tools/scraper-worker.exe",
    outputDir = "D:/Media/JAV",
    scanDir = "D:/Downloads",
    scanRecursive = true,
    createMovieFolders = true,
    hardlinkInsteadOfCopy = false,
    downloadImages = true,
    downloadPreviewImages = false,
    downloadWebPages = false,
    autoScrape = false,
    fileLoggingEnabled = true,
    sites = listOf(
        SiteInfo("javbus", "JavBus"),
        SiteInfo("javdb", "JavDB"),
        SiteInfo("dmm", "DMM")
    ),
    enabledSiteIds = listOf("javbus", "javdb"),
    language = "zh",
    showRestartHint = false,
    folderLayers = listOf("{num}", "{title}"),
    filenameFormat = "{num} {title}",
    maxTitleLength = 200,
    maxFilenameLength = 150,
    suffixKeywords = listOf("-cd1", "-cd2"),
    siteCheckRunning = false,
    siteCheckResults = null
)

/** No-op actions so previews stay interactive-free. */
internal fun previewSettingsActions(): SettingsActions = SettingsActions(
    onLanguageChange = {},
    onSelectOutputDir = {},
    onSelectScanDir = {},
    onSelectWorkerPath = {},
    onWorkerPathChange = {},
    onScanRecursiveChange = {},
    onCreateMovieFoldersChange = {},
    onHardlinkChange = {},
    onDownloadImagesChange = {},
    onDownloadPreviewImagesChange = {},
    onDownloadWebPagesChange = {},
    onAutoScrapeChange = {},
    onFileLoggingChange = {},
    onToggleSite = { _, _ -> },
    onReset = {},
    onFolderLayerChange = { _, _ -> },
    onAddLayer = {},
    onRemoveLayer = {},
    onFilenameFormatChange = {},
    onMaxTitleLengthChange = {},
    onMaxFilenameLengthChange = {},
    onSuffixKeywordsChange = {},
    onCheckSites = {}
)
