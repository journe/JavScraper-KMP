package javscraper.i18n

import java.text.MessageFormat
import java.util.Locale
import java.util.ResourceBundle

object Translations {
    private var bundle: ResourceBundle = ResourceBundle.getBundle("strings", Locale.forLanguageTag("en"))

    fun init(language: String) {
        val locale = if (language == "zh") Locale.forLanguageTag("zh") else Locale.forLanguageTag("en")
        bundle = ResourceBundle.getBundle("strings", locale)
    }

    // App
    val appTitle get() = bundle.getString("app.title")
    val statusInitializing get() = bundle.getString("app.status.initializing")
    val statusStarting get() = bundle.getString("app.status.starting")
    fun statusReady(sites: Int) = MessageFormat.format(bundle.getString("app.status.ready"), sites)
    val statusFailed get() = bundle.getString("app.status.failed")
    fun statusError(msg: String) = MessageFormat.format(bundle.getString("app.status.error"), msg)
    fun statusDirError(msg: String) = MessageFormat.format(bundle.getString("app.status.dir_error"), msg)
    fun statusScanError(msg: String) = MessageFormat.format(bundle.getString("app.status.scan_error"), msg)
    fun statusScrapeError(msg: String) = MessageFormat.format(bundle.getString("app.status.scrape_error"), msg)
    val statusFileNotFound get() = bundle.getString("app.status.file_not_found")

    // Navigation
    val navScan get() = bundle.getString("nav.scan")
    val navScrape get() = bundle.getString("nav.scrape")
    val navGallery get() = bundle.getString("nav.gallery")
    val navSettings get() = bundle.getString("nav.settings")

    // Scan screen
    val scanTitle get() = bundle.getString("scan.title")
    val scanDirectoryLabel get() = bundle.getString("scan.directory_label")
    val scanButtonScanning get() = bundle.getString("scan.button.scanning")
    val scanButtonScan get() = bundle.getString("scan.button.scan")
    val scanButtonScrape get() = bundle.getString("scan.button.scrape")
    fun scanFound(count: Int) = MessageFormat.format(bundle.getString("scan.found"), count)
    val scanNoNumber get() = bundle.getString("scan.no_number")
    val scanEmptyHint get() = bundle.getString("scan.empty_hint")

    // Progress screen
    val progressTitle get() = bundle.getString("progress.title")
    val progressCancel get() = bundle.getString("progress.cancel")
    val progressStartAll get() = bundle.getString("progress.start_all")
    fun progressCompleted(ok: Int, total: Int) = MessageFormat.format(bundle.getString("progress.completed"), ok, total)

    // Gallery screen
    val galleryTitle get() = bundle.getString("gallery.title")
    fun galleryCount(n: Int) = MessageFormat.format(bundle.getString("gallery.count"), n)
    val galleryOpenOutput get() = bundle.getString("gallery.open_output")
    val galleryClear get() = bundle.getString("gallery.clear")
    val galleryEmptyHint get() = bundle.getString("gallery.empty_hint")

    // Settings screen
    val settingsTitle get() = bundle.getString("settings.title")
    val settingsWorkerPath get() = bundle.getString("settings.worker_path")
    val settingsScanDir get() = bundle.getString("settings.scan_dir")
    val settingsScanRecursive get() = bundle.getString("settings.scan_recursive")
    val settingsOutputDir get() = bundle.getString("settings.output_dir")
    val settingsMovieFolders get() = bundle.getString("settings.movie_folders")
    val settingsHardlinks get() = bundle.getString("settings.hardlinks")
    val settingsDownloadImages get() = bundle.getString("settings.download_images")
    val settingsScraperSites get() = bundle.getString("settings.scraper_sites")
    val settingsReset get() = bundle.getString("settings.reset")
    val settingsLanguage get() = bundle.getString("settings.language")
    val settingsLanguageEn get() = bundle.getString("settings.language.en")
    val settingsLanguageZh get() = bundle.getString("settings.language.zh")
    val settingsLanguageRestartHint get() = bundle.getString("settings.language.restart_hint")

    // StatusBar
    val statusbarTitle get() = bundle.getString("statusbar.title")

    // Common
    val commonBrowse get() = bundle.getString("common.browse")
}
