package javscraper.ui.screens.settings

import androidx.compose.runtime.Composable
import javscraper.i18n.LocalTranslations
import javscraper.auth.JavdbLoginState
import javscraper.i18n.TranslationZh
import javscraper.models.SiteCategory
import javscraper.models.SiteInfo
import javscraper.settings.MultiPartSuffix
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
    scanDirHistory = listOf("D:/Downloads", "E:/Movies"),
    scanRecursive = true,
    createMovieFolders = true,
    moveInsteadOfCopy = false,
    downloadImages = true,
    downloadPreviewImages = false,
    downloadWebPages = false,
    lockData = true,
    updateMode = false,
    autoScrape = false,
    fileLoggingEnabled = true,
    sites = listOf(
        SiteInfo(
            "javbus",
            "JavBus",
            SiteCategory.CENSORED,
            "https://www.javbus.com",
            listOf("https://www.dmmsee.casa", "https://www.buscdn.casa")
        ),
        SiteInfo("javdb", "JavDB", SiteCategory.MIXED),
        SiteInfo("dmm", "DMM", SiteCategory.CENSORED)
    ),
    enabledSiteIds = listOf("javbus", "javdb"),
    siteMirrorUrls = mapOf("javbus" to "https://www.dmmsee.casa"),
    javdbSessionCookie = "",
    javdbLoginState = JavdbLoginState(),
    language = "zh",
    showRestartHint = false,
    folderLayers = listOf("{num}", "{title}"),
    filenameFormat = "{num} {title}",
    multiPartSuffix = MultiPartSuffix.CD,
    maxTitleLength = 200,
    maxFilenameLength = 150,
    suffixKeywords = listOf("-cd1", "-cd2"),
    requestTimeoutMs = 15_000,
    siteCheckRunning = false,
    siteCheckResults = null
)

/** No-op actions so previews stay interactive-free. */
internal fun previewSettingsActions(): SettingsActions = SettingsActions(
    onLanguageChange = {},
    onSelectOutputDir = {},
    onSelectScanDir = {},
    onSelectScanDirFromHistory = {},
    onSelectWorkerPath = {},
    onWorkerPathChange = {},
    onScanRecursiveChange = {},
    onCreateMovieFoldersChange = {},
    onMoveInsteadOfCopyChange = {},
    onDownloadImagesChange = {},
    onDownloadPreviewImagesChange = {},
    onDownloadWebPagesChange = {},
    onLockDataChange = {},
    onUpdateModeChange = {},
    onAutoScrapeChange = {},
    onFileLoggingChange = {},
    onToggleSite = { _, _ -> },
    onSiteMirrorChange = { _, _ -> },
    onJavdbSessionCookieChange = {},
    onStartJavdbLogin = {},
    onCancelJavdbLogin = {},
    onReset = {},
    onFolderLayerChange = { _, _ -> },
    onAddLayer = {},
    onRemoveLayer = {},
    onFilenameFormatChange = {},
    onMultiPartSuffixChange = {},
    onMaxTitleLengthChange = {},
    onMaxFilenameLengthChange = {},
    onSuffixKeywordsChange = {},
    onRequestTimeoutMsChange = {},
    onCheckSites = {}
)
