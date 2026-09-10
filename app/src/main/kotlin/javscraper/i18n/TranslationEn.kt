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
    open val navNetworkPreview: String = "Network"

    // --- Scan screen ---
    open val scanTitle: String = "File Scanner"
    open val scanButtonScanning: String = "Scanning..."
    open val scanButtonScan: String = "Scan for Videos"
    open val scanNoNumber: String = "No number"
    open val scanScraped: String = "Scraped"
    open val scanEmptyHint: String = "Select a directory and scan"

    open fun scanFound(count: Int): String = "Found $count videos with JAV numbers"
    open fun scanSkipped(count: Int): String = "$count videos already scraped and will be skipped"

    // --- Progress screen ---
    open val progressTitle: String = "Scraping Progress"
    open val progressStartAll: String = "Start All"

    open fun progressCompleted(ok: Int, total: Int): String = "$ok / $total completed"

    // --- Gallery screen ---
    open val galleryTitle: String = "Scrape Results"
    open val galleryOpenOutput: String = "Open Output"
    open val galleryClear: String = "Clear Results"
    open val galleryDetailTitle: String = "Video Details"
    open val galleryDetailBack: String = "Back to results"
    open val galleryDetailRefresh: String = "Refresh metadata"
    open val galleryEmptyHint: String = "No results yet"

    open fun galleryCount(n: Int): String = "$n movies scraped"


    // --- Network preview screen ---
    open val networkPreviewTitle: String = "Network Results"
    open val networkPreviewClear: String = "Clear"
    open val networkPreviewEmptyHint: String = "No network results yet"

    open fun networkPreviewCount(n: Int): String = "$n network results"
    // --- Settings screen ---
    open val settingsTabDirectories: String = "Directories"
    open val settingsTabScraping: String = "Scraping"
    open val settingsTabOther: String = "Other"
    open val settingsWorkerPath: String = "Worker Path"
    open val settingsScanRecursive: String = "Scan subdirectories"
    open val settingsOutputDir: String = "Output Directory"
    open val settingsMovieFolders: String = "Movie folders"
    open val settingsHardlinks: String = "Hardlinks"
    open val settingsDownloadImages: String = "Download images"
    open val settingsDownloadPreviewImages: String = "Download preview images"
    open val settingsDownloadWebPages: String = "Download webpage"
    open val settingsNfoLockData: String = "Lock NFO metadata (lockdata)"
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


    // --- StatusBar ---

    // --- Common ---
    open val commonBrowse: String = "Browse"
    open val commonApplicationLogs: String = "Application Logs"
    open val commonCancel: String = "Cancel"
    open val commonNamingExample: String = "e.g. {num} {title}"
    open val commonNotSet: String = "Not set"
    open val commonNumber: String = "Number"
    open val commonScanDirectory: String = "Scan Directory"
    open val commonScraperSites: String = "Scraper Sites"
    open val commonSettings: String = "Settings"
    open val commonStartScraping: String = "Start Scraping"

    // --- Single Scrape Dialog ---
    open val singleScrapeTitle: String = "Single Scrape"
    open val singleScrapeSiteLabel: String = "Scraper Site"
    open val singleScrapeSiteAuto: String = "Auto"
    open val singleScrapeInProgress: String = "Scraping in progress..."
    open val singleScrapeResultTitle: String = "Scrape Result"
    open val singleScrapeResultError: String = "Error"
    open val singleScrapeMissingOutputTitle: String = "Output Directory Not Set"
    open val singleScrapeMissingOutputMessage: String = "Please set the output directory in Settings before scraping."
    open val singleScrapeCancelJob: String = "Cancel Scraping"
    open val singleScrapePreviewTitle: String = "Scrape Result Preview"
    open val singleScrapeWriteConfirm: String = "Confirm & Write"
    open val singleScrapeWriteProgress: String = "Writing files..."
    open val singleScrapeFailedTitle: String = "Scrape Failed"
    open val logsEmpty: String = "No logs in the current application lifecycle."
    open val logsFilePath: String = "Log file"
    open val settingsFileLogging: String = "File Logging"
    open val settingsFileLoggingDescription: String = "Append lifecycle logs to the local log file in real time."
    open val videoFieldTitle: String = "Title"
    open val videoFieldActresses: String = "Actresses"
    open val videoFieldDate: String = "Date"
    open val videoFieldMaker: String = "Maker"
    open val videoFieldLabel: String = "Label"
    open val videoFieldSeries: String = "Series"
    open val videoFieldDirector: String = "Director"
    open val videoFieldDuration: String = "Duration"
    open val videoFieldRating: String = "Rating"
    open val videoFieldTags: String = "Tags"
    open val videoFieldCoverUrl: String = "Cover URL"
    open val videoFieldPosterUrl: String = "Poster URL"
    open val videoFieldSampleImages: String = "Sample Images"
    open val videoFieldSummary: String = "Summary"
    open val videoFieldSource: String = "Source"
    open val videoFieldDetailUrl: String = "Detail URL"
    // --- Worker setup dialog ---
    open val workerSetupTitle: String = "Worker Not Running"
    open val workerSetupMessage: String = "Unable to start scraper-worker.exe. Please select the correct path to the worker executable."
    open val workerSetupSelect: String = "Select File..."
    open val workerSetupSelectTitle: String = "Select scraper-worker.exe"

    open val commonConfirm: String = "Confirm"

    // --- Poster crop dialog ---
    open fun cropTitle(number: String): String = "Crop Poster - $number"
    open val cropConfirm: String = "Crop & Save"
    open val cropLoadFailed: String = "Failed to load the cover image."
    open val cropWriteFailed: String = "Failed to write the cropped poster."
    open fun cropSourceSize(w: Int, h: Int): String = "Source: $w x $h"
    open fun cropOutputSize(w: Int, h: Int): String = "Output: $w x $h"
    open fun cropAspectRatio(aspect: Float): String = "Aspect ratio (H/W): " + String.format("%.2f", aspect)
}

