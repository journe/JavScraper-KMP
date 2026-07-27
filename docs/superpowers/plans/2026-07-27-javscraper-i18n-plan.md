# JavScraper i18n Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add English/Chinese i18n support to the Compose Desktop UI with language selection in Settings.

**Architecture:** Java ResourceBundle loads `.properties` files based on a `language` field in `AppSettings`. A `Translations` singleton exposes typed property getters for all UI strings. Language change takes effect after restart.

**Tech Stack:** Kotlin 2.1.0, Compose Desktop 1.7.3, java.util.ResourceBundle

## Global Constraints

- All new files must use UTF-8 encoding without BOM
- SettingsManager uses `kotlinx.serialization` with `ignoreUnknownKeys = true`
- ResourceBundle base name is `strings`, loaded from classpath root (`src/main/resources/`)
- Language values: `"en"` (default) or `"zh"`
- All Translation property accessors must be typed (`val foo get() = ...`), no raw `bundle.getString()` in UI code

---

### Task 1: Add `language` field to AppSettings + fix tests

**Files:**
- Modify: `app/src/main/kotlin/javscraper/settings/SettingsManager.kt`
- Modify: `app/src/test/kotlin/javscraper/settings/SettingsManagerTest.kt`

**Interfaces:**
- Consumes: existing `AppSettings` data class
- Produces: `AppSettings.language: String` with default `"en"`

- [ ] **Step 1: Add language field to AppSettings**

```kotlin
// In SettingsManager.kt — AppSettings data class, add after autoScrape:
val language: String = "en",
```

- [ ] **Step 2: Update SettingsManagerTest default-values test**

In `AppSettings default values`, add after the `assertEquals(false, settings.autoScrape)` line:

```kotlin
assertEquals("en", settings.language)
```

- [ ] **Step 3: Update SettingsManagerTest round-trip test**

In `AppSettings JSON serialization round-trip`, add `language = "zh"` to the `AppSettings(...)` constructor call, and add after the `assertEquals(true, decoded.autoScrape)` line:

```kotlin
assertEquals("zh", decoded.language)
```

- [ ] **Step 4: Verify tests pass**

Run: `cd app && .\gradlew test --tests "javscraper.settings.SettingsManagerTest" -Dfile.encoding=UTF-8`
Expected: BUILD SUCCESSFUL

---

### Task 2: Create translation resource files

**Files:**
- Create: `app/src/main/resources/strings.properties`
- Create: `app/src/main/resources/strings_zh.properties`

**Interfaces:**
- Consumes: (none)
- Produces: classpath resources loaded by `ResourceBundle.getBundle("strings", locale)`

- [ ] **Step 1: Create strings.properties (English)**

File content — UTF-8 without BOM:

```properties
# App
app.title=JavScraper - JAV Video Metadata Scraper
app.status.initializing=Initializing...
app.status.starting=Starting...
app.status.ready=Ready ({0} sites)
app.status.failed=Failed
app.status.error=Error: {0}
app.status.dir_error=Directory selection error: {0}
app.status.scan_error=Scan error: {0}
app.status.scrape_error=Scrape error: {0}
app.status.file_not_found=File not found

# Navigation
nav.scan=Scan
nav.scrape=Scrape
nav.gallery=Gallery
nav.settings=Settings

# File Scan Screen
scan.title=File Scanner
scan.directory_label=Scan Directory
scan.button.browse=Browse
scan.button.scanning=Scanning...
scan.button.scan=Scan for Videos
scan.button.scrape=Start Scraping
scan.found=Found {0} videos with JAV numbers
scan.no_number=No number
scan.empty_hint=Select a directory and scan

# Scrape Progress Screen
progress.title=Scraping Progress
progress.cancel=Cancel
progress.start_all=Start All
progress.completed={0} / {1} completed

# Result Gallery Screen
gallery.title=Scrape Results
gallery.count={0} movies scraped
gallery.open_output=Open Output
gallery.clear=Clear
gallery.empty_hint=No results yet

# Settings Screen
settings.title=Settings
settings.worker_path=Worker Path
settings.scan_dir=Scan Directory
settings.scan_recursive=Scan subdirectories
settings.output_dir=Output Directory
settings.movie_folders=Movie folders
settings.hardlinks=Hardlinks
settings.download_images=Download images
settings.scraper_sites=Scraper Sites
settings.reset=Reset to Defaults
settings.language=Language
settings.language.en=English
settings.language.zh=中文
settings.language.restart_hint=Restart to apply language change

# Status Bar
statusbar.title=Scraper Sites

# Common
common.browse=Browse
```

- [ ] **Step 2: Create strings_zh.properties (Chinese)**

File content — UTF-8 without BOM:

```properties
# App
app.title=JavScraper - JAV 影片刮削器
app.status.initializing=初始化中...
app.status.starting=启动中...
app.status.ready=就绪（{0} 个站点）
app.status.failed=启动失败
app.status.error=错误：{0}
app.status.dir_error=目录选择错误：{0}
app.status.scan_error=扫描错误：{0}
app.status.scrape_error=刮削错误：{0}
app.status.file_not_found=文件未找到

# Navigation
nav.scan=扫描
nav.scrape=刮削
nav.gallery=图库
nav.settings=设置

# File Scan Screen
scan.title=文件扫描
scan.directory_label=扫描目录
scan.button.browse=浏览
scan.button.scanning=扫描中...
scan.button.scan=扫描视频
scan.button.scrape=开始刮削
scan.found=发现 {0} 个含番号的视频
scan.no_number=无番号
scan.empty_hint=请选择目录后扫描

# Scrape Progress Screen
progress.title=刮削进度
progress.cancel=取消
progress.start_all=全部开始
progress.completed={0} / {1} 已完成

# Result Gallery Screen
gallery.title=刮削结果
gallery.count={0} 部影片已刮削
gallery.open_output=打开输出目录
gallery.clear=清空
gallery.empty_hint=暂无结果

# Settings Screen
settings.title=设置
settings.worker_path=工作进程路径
settings.scan_dir=扫描目录
settings.scan_recursive=递归扫描子目录
settings.output_dir=输出目录
settings.movie_folders=影片文件夹
settings.hardlinks=硬链接
settings.download_images=下载图片
settings.scraper_sites=刮削站点
settings.reset=恢复默认
settings.language=语言
settings.language.en=English
settings.language.zh=中文
settings.language.restart_hint=重启后生效

# Status Bar
statusbar.title=刮削站点

# Common
common.browse=浏览
```

---

### Task 3: Create Translations singleton

**Files:**
- Create: `app/src/main/kotlin/javscraper/i18n/Translations.kt`

**Interfaces:**
- Consumes: `ResourceBundle` loaded from classpath — keys defined in Task 2
- Produces: `Translations.init(language: String)`, typed `val`/`fun` accessors for every key

- [ ] **Step 1: Create Translations.kt**

File content — UTF-8 without BOM:

```kotlin
package javscraper.i18n

import java.text.MessageFormat
import java.util.Locale
import java.util.ResourceBundle

object Translations {
    private var bundle: ResourceBundle = ResourceBundle.getBundle("strings", Locale("en"))

    fun init(language: String) {
        val locale = if (language == "zh") Locale("zh") else Locale("en")
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
```

---

### Task 4: Wire i18n into App.kt + Main.kt

**Files:**
- Modify: `app/src/main/kotlin/javscraper/Main.kt`
- Modify: `app/src/main/kotlin/javscraper/App.kt`

**Interfaces:**
- Consumes: `Translations.init()`, all `Translations.*` accessors
- Produces: app window with translated title, status bar, and navigation

- [ ] **Step 1: Update Main.kt — translate window title**

Replace:
```kotlin
title = "JavScraper - JAV Video Metadata Scraper"
```
With:
```kotlin
title = Translations.appTitle
```
Add import at top:
```kotlin
import javscraper.i18n.Translations
```

- [ ] **Step 2: Update App.kt — init Translations on startup**

In `LaunchedEffect(Unit)`, add `Translations.init(settings.language)` as the first line inside the try block, before `status.value = "Starting..."`:

```kotlin
Translations.init(settings.language)
```

Add import:
```kotlin
import javscraper.i18n.Translations
```

- [ ] **Step 3: App.kt — replace all hardcoded strings**

Replace these strings in App.kt:

| Location | Old | New |
|----------|-----|-----|
| `val status` initial value | `"Initializing..."` | `Translations.statusInitializing` |
| `status.value = "Starting..."` | `"Starting..."` | `Translations.statusStarting` |
| `status.value = "Ready (${sites.value.size} sites)"` | `"Ready (${sites.value.size} sites)"` | `"${Translations.statusReady(sites.value.size)}"` |
| `status.value = "Failed"` | `"Failed"` | `Translations.statusFailed` |
| `status.value = "Error: ${e.message}"` | `"Error: ${e.message}"` | `Translations.statusError(e.message ?: "")` |
| `status.value = "Directory selection error: ${e.message}"` | (both occurrences) | `Translations.statusDirError(...)` |
| `status.value = "Scan error: ${e.message}"` | same | `Translations.statusScanError(...)` |
| `status.value = "Scrape error: ${e.message}"` | same | `Translations.statusScrapeError(...)` |
| `error = "File not found"` | `"File not found"` | `Translations.statusFileNotFound` |
| `Text("Scan")` in NavigationBar | `"Scan"` | `Translations.navScan` |
| `Text("Scrape")` | `"Scrape"` | `Translations.navScrape` |
| `Text("Gallery")` | `"Gallery"` | `Translations.navGallery` |
| `Text("Settings")` | `"Settings"` | `Translations.navSettings` |

---

### Task 5: Update FileScanScreen.kt

**Files:**
- Modify: `app/src/main/kotlin/javscraper/ui/screens/FileScanScreen.kt`

**Interfaces:**
- Consumes: `Translations.scan*`, `Translations.commonBrowse`

- [ ] **Step 1: Replace all hardcoded strings**

Add import:
```kotlin
import javscraper.i18n.Translations
```

Replacements:
| Old | New |
|-----|-----|
| `Text("File Scanner"` | `Text(Translations.scanTitle` |
| `label = { Text("Scan Directory") }` | `label = { Text(Translations.scanDirectoryLabel) }` |
| `Icon(Icons.Default.FolderOpen, "Browse")` | `Icon(Icons.Default.FolderOpen, Translations.commonBrowse)` |
| `Text(if (isScanning) "Scanning..." else "Scan for Videos")` | `Text(if (isScanning) Translations.scanButtonScanning else Translations.scanButtonScan)` |
| `Text("Start Scraping")` | `Text(Translations.scanButtonScrape)` |
| `"Found ${scannedFiles.count { it.number.isNotBlank() }} videos with JAV numbers"` | `Translations.scanFound(scannedFiles.count { it.number.isNotBlank() })` |
| `Text("No number"` | `Text(Translations.scanNoNumber` |
| `Text("Select a directory and scan"` | `Text(Translations.scanEmptyHint` |

---

### Task 6: Update ScrapeProgressScreen.kt

**Files:**
- Modify: `app/src/main/kotlin/javscraper/ui/screens/ScrapeProgressScreen.kt`

**Interfaces:**
- Consumes: `Translations.progress*`

- [ ] **Step 1: Replace all hardcoded strings**

Add import:
```kotlin
import javscraper.i18n.Translations
```

Replacements:
| Old | New |
|-----|-----|
| `Text("Scraping Progress"` | `Text(Translations.progressTitle` |
| `Text("Cancel"` | `Text(Translations.progressCancel` |
| `Text("Start All"` | `Text(Translations.progressStartAll` |
| `"$ok / ${tasks.size} completed"` | `Translations.progressCompleted(ok, tasks.size)` |

---

### Task 7: Update ResultGalleryScreen.kt

**Files:**
- Modify: `app/src/main/kotlin/javscraper/ui/screens/ResultGalleryScreen.kt`

**Interfaces:**
- Consumes: `Translations.gallery*`

- [ ] **Step 1: Replace all hardcoded strings**

Add import:
```kotlin
import javscraper.i18n.Translations
```

Replacements:
| Old | New |
|-----|-----|
| `Text("Scrape Results"` | `Text(Translations.galleryTitle` |
| `"${results.size} movies scraped"` | `Translations.galleryCount(results.size)` |
| `Text("Open Output"` | `Text(Translations.galleryOpenOutput` |
| `Text("Clear"` | `Text(Translations.galleryClear` |
| `Text("No results yet"` | `Text(Translations.galleryEmptyHint` |

---

### Task 8: Update ScraperStatusBar.kt

**Files:**
- Modify: `app/src/main/kotlin/javscraper/ui/components/ScraperStatusBar.kt`

**Interfaces:**
- Consumes: `Translations.statusbarTitle`

- [ ] **Step 1: Replace header string**

Add import:
```kotlin
import javscraper.i18n.Translations
```

Replace:
```kotlin
Text("Scraper Sites", style = ...)
```
With:
```kotlin
Text(Translations.statusbarTitle, style = ...)
```

---

### Task 9: Update SettingsScreen.kt — add language selector + replace strings

**Files:**
- Modify: `app/src/main/kotlin/javscraper/ui/screens/SettingsScreen.kt`
- Modify: `app/src/main/kotlin/javscraper/App.kt` (pass new props)

**Interfaces:**
- Consumes: `Translations.settings*`, `Translations.commonBrowse`
- Produces: `SettingsScreen` gains `language`, `onLanguageChange`, `showRestartHint` params

- [ ] **Step 1: Replace all existing hardcoded strings in SettingsScreen.kt**

Add import:
```kotlin
import javscraper.i18n.Translations
```

Replacements:
| Old | New |
|-----|-----|
| `Text("Settings"` | `Text(Translations.settingsTitle` |
| `Text("Worker Path"` | `Text(Translations.settingsWorkerPath` |
| `Icon(Icons.Default.FileOpen, "Browse")` | `Icon(Icons.Default.FileOpen, Translations.commonBrowse)` |
| `Text("Scan Directory"` | `Text(Translations.settingsScanDir` |
| `Text("Scan subdirectories")` | `Text(Translations.settingsScanRecursive` |
| `Text("Output Directory"` | `Text(Translations.settingsOutputDir` |
| `Text("Movie folders")` | `Text(Translations.settingsMovieFolders` |
| `Text("Hardlinks")` | `Text(Translations.settingsHardlinks` |
| `Text("Download images")` | `Text(Translations.settingsDownloadImages` |
| `Text("Scraper Sites"` | `Text(Translations.settingsScraperSites` |
| `Text("Reset to Defaults")` | `Text(Translations.settingsReset` |

- [ ] **Step 2: Add language params to SettingsScreen function signature**

Add after `onToggleSite` param:
```kotlin
language: String,
onLanguageChange: (String) -> Unit,
showRestartHint: Boolean,
```

- [ ] **Step 3: Add language UI section in SettingsScreen body**

Insert between the Scraper Sites section (after `SiteSelector(...)`) and the Reset button:

```kotlin
Spacer(Modifier.height(20.dp))
Text(Translations.settingsLanguage, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
Spacer(Modifier.height(8.dp))
Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
    Text(Translations.settingsLanguageEn)
    RadioButton(selected = language == "en", onClick = { onLanguageChange("en") })
}
Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
    Text(Translations.settingsLanguageZh)
    RadioButton(selected = language == "zh", onClick = { onLanguageChange("zh") })
}
if (showRestartHint) {
    Spacer(Modifier.height(4.dp))
    Text(
        Translations.settingsLanguageRestartHint,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.tertiary
    )
}
```

- [ ] **Step 4: Wire new params in App.kt SettingsScreen call**

In the `Screen.SETTINGS` branch, add after `onToggleSite`:
```kotlin
language = settings.language,
onLanguageChange = { lang ->
    SettingsManager.update { it.copy(language = lang) }
},
showRestartHint = true,
```

Add import at top of App.kt if not already there:
```kotlin
import javscraper.settings.SettingsManager
```
(SettingsManager is already imported in App.kt.)

- [ ] **Step 5: Add RadioButton import to SettingsScreen.kt**

Ensure the import exists:
```kotlin
import androidx.compose.material3.RadioButton
```

---

### Task 10: Verify full build

- [ ] **Step 1: Run all tests**

Run: `cd app && .\gradlew test -Dfile.encoding=UTF-8`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Verify the app compiles**

Run: `cd app && .\gradlew compileKotlin -Dfile.encoding=UTF-8`
Expected: BUILD SUCCESSFUL