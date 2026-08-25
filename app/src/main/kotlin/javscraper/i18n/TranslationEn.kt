package javscraper.i18n

/**
 * Type-safe translation strings for the JavScraper UI.
 * Extend this class and override properties for each locale.
 */
open class TranslationEn {

    // --- App ---
    open val appTitle: String = "JavScraper - JAV Video Metadata Scraper"
    open val statusInitializing: String = "Initializing..."
    open val statusStarting: String = "Starting..."
    open val statusFailed: String = "Failed"
    open val statusFileNotFound: String = "File not found"

    open fun statusReady(sites: Int): String = "Ready ($sites sites)"
    open fun statusError(msg: String): String = "Error: $msg"
    open fun statusDirError(msg: String): String = "Directory selection error: $msg"
    open fun statusScanError(msg: String): String = "Scan error: $msg"
    open fun statusScrapeError(msg: String): String = "Scrape error: $msg"

    // --- Navigation ---
    open val navScan: String = "Scan"
    open val navScrape: String = "Scrape"
    open val navGallery: String = "Gallery"
    open val navSettings: String = "Settings"

    // --- Scan screen ---
    open val scanTitle: String = "File Scanner"
    open val scanDirectoryLabel: String = "Scan Directory"
    open val scanButtonScanning: String = "Scanning..."
    open val scanButtonScan: String = "Scan for Videos"
    open val scanButtonScrape: String = "Start Scraping"
    open val scanNoNumber: String = "No number"
    open val scanEmptyHint: String = "Select a directory and scan"

    open fun scanFound(count: Int): String = "Found $count videos with JAV numbers"

    // --- Progress screen ---
    open val progressTitle: String = "Scraping Progress"
    open val progressCancel: String = "Cancel"
    open val progressStartAll: String = "Start All"

    open fun progressCompleted(ok: Int, total: Int): String = "$ok / $total completed"

    // --- Gallery screen ---
    open val galleryTitle: String = "Scrape Results"
    open val galleryOpenOutput: String = "Open Output"
    open val galleryClear: String = "Clear"
    open val galleryEmptyHint: String = "No results yet"

    open fun galleryCount(n: Int): String = "$n movies scraped"

    // --- Settings screen ---
    open val settingsTitle: String = "Settings"
    open val settingsTabDirectories: String = "Directories"
    open val settingsTabScraping: String = "Scraping"
    open val settingsTabOther: String = "Other"
    open val settingsWorkerPath: String = "Worker Path"
    open val settingsScanDir: String = "Scan Directory"
    open val settingsScanRecursive: String = "Scan subdirectories"
    open val settingsOutputDir: String = "Output Directory"
    open val settingsMovieFolders: String = "Movie folders"
    open val settingsHardlinks: String = "Hardlinks"
    open val settingsDownloadImages: String = "Download images"
    open val settingsScraperSites: String = "Scraper Sites"
    open val settingsTestSites: String = "Test Sites"
    open val settingsTestSitesTitle: String = "Site Connectivity Test"
    open val settingsTestSitesRunning: String = "Testing site connections..."
    open val settingsTestSitesEmpty: String = "No results yet"
    open val settingsTestSitesRetry: String = "Retry"
    open val settingsTestSitesClose: String = "Close"
    open val settingsTestSitesOk: String = "OK"
    open val settingsTestSitesFail: String = "Unreachable"
    open fun settingsTestSitesLatency(ms: Int): String = "$ms ms"
    open val settingsReset: String = "Reset to Defaults"
    open val settingsLanguage: String = "Language"
    open val settingsLanguageEn: String = "English"
    open val settingsLanguageZh: String = "Chinese"
    open val settingsLanguageRestartHint: String = "Restart to apply language change"

    // --- Rename rules ---
    open val settingsRenameTitle: String = "Naming Rules"
    open val settingsFolderLayers: String = "Folder Layers"
    open val settingsAddLayer: String = "Add Layer"
    open val settingsRemoveLayer: String = "Remove"
    open val settingsFilenameFormat: String = "Filename Format"
    open val settingsMaxTitleLength: String = "Max Title Length"
    open val settingsMaxFilenameLength: String = "Max Filename Length"
    open val settingsSuffixKeywords: String = "Suffix Keywords"
    open val settingsSuffixKeywordsHint: String = "Comma-separated, e.g. -cd1,-cd2,-4k"
    open val settingsRenameVariables: String = "Available: {num} {title} {actor} {actors} {maker} {label} {series} {director} {date} {year} {month} {day} {suffix}"
    open val settingsAdvanced: String = "Advanced"
    open val settingsFolderLayerPlaceholder: String = "e.g. {num} {title}"
    open val settingsFilenamePlaceholder: String = "e.g. {num} {title}"


    // --- StatusBar ---
    open val statusbarTitle: String = "Scraper Sites"

    // --- Common ---
    open val commonBrowse: String = "Browse"

    // --- Single Scrape Dialog ---
    open val singleScrapeTitle: String = "Single Scrape"
    open val singleScrapeNumberLabel: String = "Number"
    open val singleScrapeSiteLabel: String = "Scraper Site"
    open val singleScrapeSiteAuto: String = "Auto"
    open val singleScrapeStart: String = "Start Scraping"
    open val singleScrapeInProgress: String = "Scraping in progress..."
    open val singleScrapeResultTitle: String = "Scrape Result"
    open val singleScrapeResultError: String = "Error"
    open val singleScrapeOutputDirMissing: String = "Not set"
    open val singleScrapeMissingOutputTitle: String = "Output Directory Not Set"
    open val singleScrapeMissingOutputMessage: String = "Please set the output directory in Settings before scraping."
    open val singleScrapeCancelJob: String = "Cancel Scraping"
    open val singleScrapePreviewTitle: String = "Scrape Result Preview"
    open val singleScrapeWriteConfirm: String = "Confirm & Write"
    open val singleScrapeWriteProgress: String = "Writing files..."
    open val singleScrapeFailedTitle: String = "Scrape Failed"
    // --- Worker setup dialog ---
    open val workerSetupTitle: String = "Worker Not Running"
    open val workerSetupMessage: String = "Unable to start scraper-worker.exe. Please select the correct path to the worker executable."
    open val workerSetupSelect: String = "Select File..."
    open val workerSetupSelectTitle: String = "Select scraper-worker.exe"
    open val workerSetupCancel: String = "Cancel"

    open val commonConfirm: String = "Confirm"
}

