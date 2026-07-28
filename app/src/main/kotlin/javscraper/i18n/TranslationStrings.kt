package javscraper.i18n

/**
 * Type-safe translation strings for the JavScraper UI.
 * Extend this class and override properties for each locale.
 */
open class TranslationStrings {

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
    open val settingsWorkerPath: String = "Worker Path"
    open val settingsScanDir: String = "Scan Directory"
    open val settingsScanRecursive: String = "Scan subdirectories"
    open val settingsOutputDir: String = "Output Directory"
    open val settingsMovieFolders: String = "Movie folders"
    open val settingsHardlinks: String = "Hardlinks"
    open val settingsDownloadImages: String = "Download images"
    open val settingsScraperSites: String = "Scraper Sites"
    open val settingsReset: String = "Reset to Defaults"
    open val settingsLanguage: String = "Language"
    open val settingsLanguageEn: String = "English"
    open val settingsLanguageZh: String = "中文"
    open val settingsLanguageRestartHint: String = "Restart to apply language change"

    // --- StatusBar ---
    open val statusbarTitle: String = "Scraper Sites"

    // --- Common ---
    open val commonBrowse: String = "Browse"
}
