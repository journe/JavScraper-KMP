# Output Directory & File Rename Rules — Design Spec

**Date:** 2026-07-30
**Status:** Approved for implementation
**Component:** Settings Screen / ScrapeOrchestrator / RenameFormatter

---

## 1. Objective

Replace the current hardcoded output path format (`{number} [{maker}] {actresses}`) in `ScrapeOrchestrator.getOut()` with a user-configurable template system that supports multi-layer folder structures and customizable filename patterns, inspired by mdcxOpenAver's organizer.py.

## 2. Current State

- `ScrapeOrchestrator.getOut()` generates: `{outputDir}/{number} [{maker}] {actresses}` when `createMovieFolders=true`
- Output filenames keep the original filename unchanged
- No user-facing rename configuration exists
- `AppSettings` has no rename-related fields

## 3. Template Variables

All variables are resolved from the `Video` data class at scrape time:

| Variable | Source | Example |
|----------|--------|---------|
| `{num}` | `video.number` | `SONE-001` |
| `{title}` | `video.title` | `Beautiful Girl` |
| `{actor}` | `video.actresses.first()` | `Aoi Yuki` |
| `{actors}` | `video.actresses.joinToString(" ")` | `Aoi Yuki Ema Kato` |
| `{maker}` | `video.maker` | `S1` |
| `{label}` | `video.label` | `S1 NO.1 STYLE` |
| `{series}` | `video.series` | `Beautiful系列` |
| `{director}` | `video.director` | `Takeshi` |
| `{date}` | `video.date` | `2024-01-15` |
| `{year}` | `video.date.take(4)` | `2024` |
| `{month}` | `video.date[5..7]` | `01` |
| `{day}` | `video.date[8..10]` | `15` |
| `{suffix}` | detected from original filename | `-cd1`, `-4k`, `-uc` |

Empty variables resolve to empty string (no `[Unknown]` fallback in filename context — avoids noise like `[Unknown Maker]`).

## 4. Configuration Model

### AppSettings (SettingsManager.kt)

New fields added to existing `@Serializable data class AppSettings`:

```kotlin
val folderLayers: List<String> = listOf("{num} {title}")
val filenameFormat: String = "{num} {title}"
val maxTitleLength: Int = 50
val maxFilenameLength: Int = 60
val suffixKeywords: List<String> = listOf("-cd1", "-cd2", "-4k", "-uc")
```

Defaults produce output identical in spirit to the current behavior but with the user-friendly template `{num} {title}`.

The existing `createMovieFolders: Boolean` toggle remains as the on/off switch: when off, files go directly into `outputDir` with no folder or rename applied (current behavior preserved).

## 5. New File: RenameFormatter.kt

**Package:** `javscraper.io`
**Path:** `app/src/main/kotlin/javscraper/io/RenameFormatter.kt`

### Public API

```kotlin
object RenameFormatter {

    /** Build folder path layers from template strings. Returns one name per layer. */
    fun formatFolder(video: Video, layers: List<String>, suffix: String): List<String>

    /** Build filename (without extension) from template string. */
    fun formatFilename(video: Video, template: String, suffix: String): String

    /** Detect version suffix from original filename against keyword list. */
    fun detectSuffix(originalFileName: String, keywords: List<String>): String

    /** Format a single template string by substituting variables. Internal use. */
    fun formatTemplate(template: String, video: Video, suffix: String, useFallback: Boolean = false): String
}
```

### Helper Functions (private)

- `sanitize(name: String): String` — Replace illegal Windows filename chars (`< > : " / \ | ? *`) with spaces; collapse multiple spaces; trim
- `truncate(text: String, maxLen: Int): String` — Truncate with `...` if length exceeds `maxLen` (minimum 3 chars reserved for `...`)
- `stripWindowsTrailing(name: String): String` — Remove trailing `.` and spaces for Windows compatibility
- `cleanSourceSuffix(text: String): String` — Remove known source suffixes (e.g., ` - Jable TV`, ` - MissAV`) from title
- `firstOf(vararg values: String): String` — Returns the first non-blank value

### Template Resolution Flow

1. Replace `{num}` with `video.number` (guaranteed non-empty)
2. Replace `{title}` with cleaned/truncated title
3. Replace `{actor}` / `{actors}` with actress names
4. Replace `{maker}`, `{label}`, `{series}`, `{director}` with corresponding fields
5. Replace `{date}`, `{year}`, `{month}`, `{day}` from date field
6. Replace `{suffix}` with detected suffix string
7. Apply `sanitize()` to the result
8. Apply `truncate()` if result exceeds `maxFilenameLength`
9. Apply `stripWindowsTrailing()` for safety

## 6. ScrapeOrchestrator Changes

### Constructor Parameters (expanded)

```kotlin
class ScrapeOrchestrator(
    private val sidecar: SidecarManager,
    private val outputDir: String,
    private val createMovieFolders: Boolean = true,
    private val hardlinkInsteadOfCopy: Boolean = true,
    private val downloadImages: Boolean = true,
    private val folderLayers: List<String> = listOf("{num} {title}"),
    private val filenameFormat: String = "{num} {title}",
    private val maxTitleLength: Int = 50,
    private val maxFilenameLength: Int = 60,
    private val suffixKeywords: List<String> = listOf("-cd1", "-cd2", "-4k", "-uc")
)
```

### Method Changes

- Replace `getOut()` with `resolveOutput(sf: ScannedFile, video: Video): OutputPaths`
- New `OutputPaths` data class: `data class OutputPaths(val folder: Path, val filename: String, val fullPath: Path)`
- `process()` calls `resolveOutput()` instead of `getOut()`
- The title in `NfoWriter.generate()` should also use the (potentially truncated) formatted title

### OutputPaths Resolution Flow

```
if outputDir blank → folder = current dir
if not createMovieFolders → folder = outputDir, filename = original filename

suffix = detectSuffix(originalFileName, suffixKeywords)
layers = formatFolder(video, folderLayers, suffix)
folder = layers.fold(outputDir) { acc, layer → acc.resolve(layer) }
filename = formatFilename(video, filenameFormat, suffix) + extension
```

## 7. SettingsScreen UI Additions

New section inserted after the output directory row and the boolean toggles, before "Scraper Sites".

### Layout (approximate)

```
--- Output & Naming ---

[Output Directory field ...]           (existing)

☑ Movie folders                        (existing, affects folderLayers toggle below)
  Folders enabled → folderLayers active
  Folders disabled → no folders, original filename kept

--- Naming Rules ---

Folder Layers:
  Layer 1: [ {num} {title}                ] [×]
  + Add Layer

  Preview: {outputDir}/{Layer1}/{Layer2}/...

Filename Format:
  [ {num} {title}                        ]

Available variables: {num} {title} {actor} {actors} {maker} {label} {series} {director}
{date} {year} {month} {day} {suffix}

▼ Advanced:
  Max Title Length: [ 50  ]
  Max Filename Length: [ 60  ]
  Suffix Keywords: -cd1, -cd2, -4k, -uc

--- Scraper Sites --- (existing)
```

### Interaction Notes

- **Folder Layers** uses a dynamic list: each row has an `OutlinedTextField` + delete button
- **+ Add Layer** appends a new empty row
- Validation: at least one non-blank layer required when `createMovieFolders` is on
- Template variable help is shown as compact hint text below the filename field
- **Advanced** section is in a collapsed `AnimatedVisibility` block to keep the default view clean

## 8. AppViewModel Changes

### New State Variables

```kotlin
var folderLayers by mutableStateOf(SettingsManager.get().folderLayers)
var filenameFormat by mutableStateOf(SettingsManager.get().filenameFormat)
var maxTitleLength by mutableStateOf(SettingsManager.get().maxTitleLength)
var maxFilenameLength by mutableStateOf(SettingsManager.get().maxFilenameLength)
var suffixKeywords by mutableStateOf(SettingsManager.get().suffixKeywords)
```

### New Actions (added to SettingsActions)

```kotlin
val onFolderLayerChange: (Int, String) -> Unit,  // index, new value
val onAddLayer: () -> Unit,
val onRemoveLayer: (Int) -> Unit,
val onFilenameFormatChange: (String) -> Unit,
val onMaxTitleLengthChange: (Int) -> Unit,
val onMaxFilenameLengthChange: (Int) -> Unit,
val onSuffixKeywordsChange: (List<String>) -> Unit,
```

### ScrapeOrchestrator Re-creation

When rename settings change, the orchestrator must be re-created (same pattern as existing `updateCreateMovieFolders` / `updateHardlink` / `updateDownloadImages`):

```kotlin
fun updateRenameSettings() {
    orch = ScrapeOrchestrator(
        mgr, outputDir, createMovieFolders, hardlinkInsteadOfCopy,
        downloadImages, folderLayers, filenameFormat,
        maxTitleLength, maxFilenameLength, suffixKeywords
    )
}
```

Called from each rename-related setting change handler.

### SettingsState / SettingsActions Updates

```kotlin
data class SettingsState(
    // ... existing ...
    val folderLayers: List<String>,
    val filenameFormat: String,
    val maxTitleLength: Int,
    val maxFilenameLength: Int,
    val suffixKeywords: List<String>,
)

data class SettingsActions(
    // ... existing ...
    val onFolderLayerChange: (Int, String) -> Unit,
    val onAddLayer: () -> Unit,
    val onRemoveLayer: (Int) -> Unit,
    val onFilenameFormatChange: (String) -> Unit,
    val onMaxTitleLengthChange: (Int) -> Unit,
    val onMaxFilenameLengthChange: (Int) -> Unit,
    val onSuffixKeywordsChange: (List<String>) -> Unit,
)
```

## 9. i18n Strings

### TranslationEn additions

```kotlin
open val settingsRenameTitle: String = "Naming Rules"
open val settingsFolderLayers: String = "Folder Layers"
open val settingsAddLayer: String = "+ Add Layer"
open val settingsRemoveLayer: String = "Remove"
open val settingsFilenameFormat: String = "Filename Format"
open val settingsMaxTitleLength: String = "Max Title Length"
open val settingsMaxFilenameLength: String = "Max Filename Length"
open val settingsSuffixKeywords: String = "Suffix Keywords"
open val settingsSuffixKeywordsHint: String = "Comma-separated, e.g. -cd1,-cd2,-4k"
open val settingsRenamePreview: String = "Preview:"
open val settingsRenameVariables: String = "Available variables: "
open val settingsAdvanced: String = "Advanced"
open val settingsFolderLayerPlaceholder: String = "e.g. {num} {title}"
open val settingsFilenamePlaceholder: String = "e.g. {num} {title}"
```

### TranslationZh additions

Same keys with Chinese translations.

## 10. Backward Compatibility

- Existing config files without the new fields: `ignoreUnknownKeys = true` in the `Json` instance (already set) means old configs load fine with default values
- `createMovieFolders = false` continues to skip folder creation and filename rename entirely
- Default templates (`{num} {title}`) produce reasonable output for new users

## 11. Files Changed

| File | Change |
|------|--------|
| `app/src/main/kotlin/javscraper/settings/SettingsManager.kt` | Add new AppSettings fields |
| `app/src/main/kotlin/javscraper/io/RenameFormatter.kt` | **New file** — template engine |
| `app/src/main/kotlin/javscraper/scrape/ScrapeOrchestrator.kt` | Use RenameFormatter for output paths |
| `app/src/main/kotlin/javscraper/ui/screens/SettingsScreen.kt` | Add naming rules UI section |
| `app/src/main/kotlin/javscraper/AppViewModel.kt` | Add rename state + actions |
| `app/src/main/kotlin/javscraper/i18n/TranslationEn.kt` | Add i18n strings |
| `app/src/main/kotlin/javscraper/i18n/TranslationZh.kt` | Add Chinese i18n strings |

## 12. Out of Scope

- No Python worker changes (rename logic stays on Kotlin side)
- No preview/test-rename button in settings (can be added later)
- No batch rename of already-scraped files
- No per-site rename rule overrides
