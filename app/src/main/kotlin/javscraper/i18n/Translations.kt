package javscraper.i18n

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.MutableState
import java.text.MessageFormat
import java.util.Locale
import java.util.ResourceBundle

object Translations {
    private var bundle: ResourceBundle = ResourceBundle.getBundle("strings", Locale("en"))
    val version: MutableState<Int> = mutableStateOf(0)

    fun init(language: String) {
        val locale = if (language == "zh") Locale("zh") else Locale("en")
        bundle = ResourceBundle.getBundle("strings", locale)
        version.value++
    }

    // App
    val appTitle get(): String { version.value; return bundle.getString("app.title") }
    val statusInitializing get(): String { version.value; return bundle.getString("app.status.initializing") }
    val statusStarting get(): String { version.value; return bundle.getString("app.status.starting") }
    fun statusReady(sites: Int): String { version.value; return MessageFormat.format(bundle.getString("app.status.ready"), sites) }
    val statusFailed get(): String { version.value; return bundle.getString("app.status.failed") }
    fun statusError(msg: String): String { version.value; return MessageFormat.format(bundle.getString("app.status.error"), msg) }
    fun statusDirError(msg: String): String { version.value; return MessageFormat.format(bundle.getString("app.status.dir_error"), msg) }
    fun statusScanError(msg: String): String { version.value; return MessageFormat.format(bundle.getString("app.status.scan_error"), msg) }
    fun statusScrapeError(msg: String): String { version.value; return MessageFormat.format(bundle.getString("app.status.scrape_error"), msg) }
    val statusFileNotFound get(): String { version.value; return bundle.getString("app.status.file_not_found") }

    // Navigation
    val navScan get(): String { version.value; return bundle.getString("nav.scan") }
    val navScrape get(): String { version.value; return bundle.getString("nav.scrape") }
    val navGallery get(): String { version.value; return bundle.getString("nav.gallery") }
    val navSettings get(): String { version.value; return bundle.getString("nav.settings") }

    // Scan screen
    val scanTitle get(): String { version.value; return bundle.getString("scan.title") }
    val scanDirectoryLabel get(): String { version.value; return bundle.getString("scan.directory_label") }
    val scanButtonScanning get(): String { version.value; return bundle.getString("scan.button.scanning") }
    val scanButtonScan get(): String { version.value; return bundle.getString("scan.button.scan") }
    val scanButtonScrape get(): String { version.value; return bundle.getString("scan.button.scrape") }
    fun scanFound(count: Int): String { version.value; return MessageFormat.format(bundle.getString("scan.found"), count) }
    val scanNoNumber get(): String { version.value; return bundle.getString("scan.no_number") }
    val scanEmptyHint get(): String { version.value; return bundle.getString("scan.empty_hint") }

    // Progress screen
    val progressTitle get(): String { version.value; return bundle.getString("progress.title") }
    val progressCancel get(): String { version.value; return bundle.getString("progress.cancel") }
    val progressStartAll get(): String { version.value; return bundle.getString("progress.start_all") }
    fun progressCompleted(ok: Int, total: Int): String { version.value; return MessageFormat.format(bundle.getString("progress.completed"), ok, total) }

    // Gallery screen
    val galleryTitle get(): String { version.value; return bundle.getString("gallery.title") }
    fun galleryCount(n: Int): String { version.value; return MessageFormat.format(bundle.getString("gallery.count"), n) }
    val galleryOpenOutput get(): String { version.value; return bundle.getString("gallery.open_output") }
    val galleryClear get(): String { version.value; return bundle.getString("gallery.clear") }
    val galleryEmptyHint get(): String { version.value; return bundle.getString("gallery.empty_hint") }

    // Settings screen
    val settingsTitle get(): String { version.value; return bundle.getString("settings.title") }
    val settingsWorkerPath get(): String { version.value; return bundle.getString("settings.worker_path") }
    val settingsScanDir get(): String { version.value; return bundle.getString("settings.scan_dir") }
    val settingsScanRecursive get(): String { version.value; return bundle.getString("settings.scan_recursive") }
    val settingsOutputDir get(): String { version.value; return bundle.getString("settings.output_dir") }
    val settingsMovieFolders get(): String { version.value; return bundle.getString("settings.movie_folders") }
    val settingsHardlinks get(): String { version.value; return bundle.getString("settings.hardlinks") }
    val settingsDownloadImages get(): String { version.value; return bundle.getString("settings.download_images") }
    val settingsScraperSites get(): String { version.value; return bundle.getString("settings.scraper_sites") }
    val settingsReset get(): String { version.value; return bundle.getString("settings.reset") }
    val settingsLanguage get(): String { version.value; return bundle.getString("settings.language") }
    val settingsLanguageEn get(): String { version.value; return bundle.getString("settings.language.en") }
    val settingsLanguageZh get(): String { version.value; return bundle.getString("settings.language.zh") }
    val settingsLanguageRestartHint get(): String { version.value; return bundle.getString("settings.language.restart_hint") }

    // StatusBar
    val statusbarTitle get(): String { version.value; return bundle.getString("statusbar.title") }

    // Common
    val commonBrowse get(): String { version.value; return bundle.getString("common.browse") }
}