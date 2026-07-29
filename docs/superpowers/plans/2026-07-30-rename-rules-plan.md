# Output Directory & File Rename Rules — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the hardcoded output folder and filename logic in ScrapeOrchestrator with a user-configurable template system supporting multi-layer folders and customizable filenames.

**Architecture:** Add a pure-Kotlin RenameFormatter object that resolves `{var}` templates against the `Video` data model, with sanitize/truncate/suffix-detection helpers. Extend AppSettings, AppViewModel, SettingsScreen, and i18n to expose and persist the new configuration. Wire the formatter into ScrapeOrchestrator for output path generation.

**Tech Stack:** Kotlin 2.4+, Compose Desktop 1.9+, kotlinx.serialization

## Global Constraints

- All new Kotlin files go under `app/src/main/kotlin/javscraper/io/`
- AppSettings uses `@Serializable` with `ignoreUnknownKeys = true` Json config
- Template variables match `Video` data class fields exactly
- Backward compatibility: old configs load with default rename values via `ignoreUnknownKeys`
- Windows path safety: sanitize `< > : " / \ | ? *` and strip trailing `. ` on Windows

---

### Task 1: SettingsManager — Add Rename Config Fields

**Files:**
- Modify: `app/src/main/kotlin/javscraper/settings/SettingsManager.kt`

**Interfaces:**
- Consumes: Existing `@Serializable data class AppSettings`
- Produces: Extended `AppSettings` with 5 new fields (with defaults)

- [ ] **Step 1: Add new fields to AppSettings**

```kotlin
@Serializable
data class AppSettings(
    // ... existing fields unchanged ...
    val folderLayers: List<String> = listOf("{num} {title}"),
    val filenameFormat: String = "{num} {title}",
    val maxTitleLength: Int = 50,
    val maxFilenameLength: Int = 60,
    val suffixKeywords: List<String> = listOf("-cd1", "-cd2", "-4k", "-uc")
)
```

- [ ] **Step 2: Compile to verify**

```powershell
cd app; ./gradlew compileKotlin --no-daemon
```
Expected: BUILD SUCCESSFUL

---

### Task 2: RenameFormatter — Core Template Engine

**Files:**
- Create: `app/src/main/kotlin/javscraper/io/RenameFormatter.kt`

**Interfaces:**
- Consumes: `javscraper.models.Video`
- Produces: `object RenameFormatter` with public methods:
  - `fun formatFolder(video: Video, layers: List<String>, suffix: String): List<String>`
  - `fun formatFilename(video: Video, template: String, suffix: String, maxFileLen: Int, maxTitleLen: Int): String`
  - `fun detectSuffix(originalFileName: String, keywords: List<String>): String`

- [ ] **Step 1: Create RenameFormatter.kt with full implementation**

```kotlin
package javscraper.io

import javscraper.models.Video

object RenameFormatter {

    fun formatFolder(video: Video, layers: List<String>, suffix: String): List<String> {
        return layers.map { sanitize(formatTemplate(it, video, suffix, useFallback = true)) }
            .filter { it.isNotBlank() }
    }

    fun formatFilename(video: Video, template: String, suffix: String, maxFileLen: Int, maxTitleLen: Int): String {
        val result = formatTemplate(template, video, suffix, useFallback = false, maxTitleLen = maxTitleLen)
        return truncate(sanitize(result), maxFileLen)
    }

    fun detectSuffix(originalFileName: String, keywords: List<String>): String {
        val lower = originalFileName.lowercase()
        return keywords.filter { kw ->
            val kwl = kw.lowercase().trim()
            kwl.isNotBlank() && Regex(Regex.escape(kwl) + "(?=[-_.\\s]|\$)").containsMatchIn(lower)
        }.joinToString("")
    }

    private fun formatTemplate(
        template: String, video: Video, suffix: String, useFallback: Boolean, maxTitleLen: Int = Int.MAX_VALUE
    ): String {
        var result = template
        result = result.replace("{num}", video.number)
        val cleanedTitle = truncate(cleanSourceSuffix(video.title), maxTitleLen)
        result = result.replace("{title}", cleanedTitle)
        result = result.replace("{actor}", video.actresses.firstOrNull() ?: "")
        result = result.replace("{actors}", video.actresses.joinToString(" "))
        result = result.replace("{maker}", video.maker)
        result = result.replace("{label}", video.label)
        result = result.replace("{series}", video.series)
        result = result.replace("{director}", video.director)
        result = result.replace("{date}", video.date)
        result = result.replace("{year}", if (video.date.length >= 4) video.date.substring(0, 4) else "")
        result = result.replace("{month}", if (video.date.length >= 7) video.date.substring(5, 7) else "")
        result = result.replace("{day}", if (video.date.length >= 10) video.date.substring(8, 10) else "")
        result = result.replace("{suffix}", suffix)
        return result.trim()
    }

    fun sanitize(name: String): String {
        return name.replace(illegalCharsRegex, " ")
            .replace(multiSpaceRegex, " ")
            .trim()
            .let { stripWindowsTrailing(it) }
    }

    fun truncate(text: String, maxLen: Int): String {
        if (text.length <= maxLen) return text
        if (maxLen <= 3) return text.take(maxLen)
        return text.take(maxLen - 3).trimEnd() + "..."
    }

    fun stripWindowsTrailing(name: String): String {
        return if (System.getProperty("os.name").lowercase().contains("win")) {
            name.trimEnd('.', ' ')
        } else name
    }

    fun cleanSourceSuffix(text: String): String {
        val patterns = listOf(
            Regex("""\s*-\s*Jable\s*TV.*$""", RegexOption.IGNORE_CASE),
            Regex("""\s*-\s*Jable.*$""", RegexOption.IGNORE_CASE),
            Regex("""\s*-\s*Hayav\s*AV.*$""", RegexOption.IGNORE_CASE),
            Regex("""\s*-\s*Hayav.*$""", RegexOption.IGNORE_CASE),
            Regex("""\s*-\s*MissAV.*$""", RegexOption.IGNORE_CASE),
            Regex("""\s*-\s*Jable.*$""", RegexOption.IGNORE_CASE),
            Regex("""\s*-\s*$"""),
            Regex("""\s+-\d+$"""),
        )
        var result = text
        for (p in patterns) result = p.replaceFirst(result, "")
        return result.trim()
    }
}

private val illegalCharsRegex = """[<>:"/\\|?*]""".toRegex()
private val multiSpaceRegex = """\s+""".toRegex()
```

- [ ] **Step 2: Compile to verify**

```powershell
cd app; ./gradlew compileKotlin --no-daemon
```
Expected: BUILD SUCCESSFUL

---

### Task 3: i18n — Add Translation Strings

**Files:**
- Modify: `app/src/main/kotlin/javscraper/i18n/TranslationEn.kt`
- Modify: `app/src/main/kotlin/javscraper/i18n/TranslationZh.kt`

- [ ] **Step 1: Add English strings to TranslationEn** (after `settingsLanguageRestartHint`)

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
open val settingsRenameVariables: String = "Available: {num} {title} {actor} {actors} {maker} {label} {series} {director} {date} {year} {month} {day} {suffix}"
open val settingsAdvanced: String = "Advanced"
open val settingsFolderLayerPlaceholder: String = "e.g. {num} {title}"
open val settingsFilenamePlaceholder: String = "e.g. {num} {title}"
```

- [ ] **Step 2: Add Chinese strings to TranslationZh**

```kotlin
override val settingsRenameTitle: String = "重命名规则"
override val settingsFolderLayers: String = "文件夹层级"
override val settingsAddLayer: String = "+ 添加层级"
override val settingsRemoveLayer: String = "删除"
override val settingsFilenameFormat: String = "文件名格式"
override val settingsMaxTitleLength: String = "标题最大长度"
override val settingsMaxFilenameLength: String = "文件名最大长度"
override val settingsSuffixKeywords: String = "版本后缀关键词"
override val settingsSuffixKeywordsHint: String = "逗号分隔，如 -cd1,-cd2,-4k"
override val settingsRenameVariables: String = "可用变量：{num} {title} {actor} {actors} {maker} {label} {series} {director} {date} {year} {month} {day} {suffix}"
override val settingsAdvanced: String = "高级选项"
override val settingsFolderLayerPlaceholder: String = "例如 {num} {title}"
override val settingsFilenamePlaceholder: String = "例如 {num} {title}"
```

- [ ] **Step 3: Compile to verify**

```powershell
cd app; ./gradlew compileKotlin --no-daemon
```
Expected: BUILD SUCCESSFUL

---

### Task 4: AppViewModel — Add Rename State and Actions

**Files:**
- Modify: `app/src/main/kotlin/javscraper/AppViewModel.kt`

**Interfaces:**
- Consumes: `SettingsManager.get().folderLayers` etc.
- Produces: State vars + action methods + `updateRenameOrchestrator()` for ScrapeOrchestrator re-creation

- [ ] **Step 1: Add state variables after existing settings state block**

```kotlin
var folderLayers by mutableStateOf(SettingsManager.get().folderLayers)
    private set
var filenameFormat by mutableStateOf(SettingsManager.get().filenameFormat)
    private set
var maxTitleLength by mutableStateOf(SettingsManager.get().maxTitleLength)
    private set
var maxFilenameLength by mutableStateOf(SettingsManager.get().maxFilenameLength)
    private set
var suffixKeywords by mutableStateOf(SettingsManager.get().suffixKeywords)
    private set
```

- [ ] **Step 2: Add rename action methods after existing settings methods**

```kotlin
fun updateFolderLayer(index: Int, value: String) {
    folderLayers = folderLayers.toMutableList().also { it[index] = value }
    SettingsManager.update { it.copy(folderLayers = folderLayers) }
    updateRenameOrchestrator()
}

fun addLayer() {
    folderLayers = folderLayers + ""
    SettingsManager.update { it.copy(folderLayers = folderLayers) }
}

fun removeLayer(index: Int) {
    folderLayers = folderLayers.toMutableList().also { it.removeAt(index) }
    SettingsManager.update { it.copy(folderLayers = folderLayers) }
    updateRenameOrchestrator()
}

fun updateFilenameFormat(v: String) {
    filenameFormat = v
    SettingsManager.update { it.copy(filenameFormat = v) }
    updateRenameOrchestrator()
}

fun updateMaxTitleLength(v: Int) {
    maxTitleLength = v
    SettingsManager.update { it.copy(maxTitleLength = v) }
}

fun updateMaxFilenameLength(v: Int) {
    maxFilenameLength = v
    SettingsManager.update { it.copy(maxFilenameLength = v) }
    updateRenameOrchestrator()
}

fun updateSuffixKeywords(v: List<String>) {
    suffixKeywords = v
    SettingsManager.update { it.copy(suffixKeywords = v) }
    updateRenameOrchestrator()
}

private fun updateRenameOrchestrator() {
    mgr?.let { m ->
        orch = ScrapeOrchestrator(
            m, outputDir, createMovieFolders, hardlinkInsteadOfCopy,
            downloadImages, folderLayers, filenameFormat,
            maxTitleLength, maxFilenameLength, suffixKeywords
        )
    }
}
```

- [ ] **Step 3: Extend resetSettings() to reset rename fields**

Add after existing reset assignments:
```kotlin
folderLayers = fresh.folderLayers
filenameFormat = fresh.filenameFormat
maxTitleLength = fresh.maxTitleLength
maxFilenameLength = fresh.maxFilenameLength
suffixKeywords = fresh.suffixKeywords
```

- [ ] **Step 4: Update init() ScrapeOrchestrator construction**

Replace existing `orch = ScrapeOrchestrator(m, outputDir, createMovieFolders, hardlinkInsteadOfCopy, downloadImages)` with:
```kotlin
orch = ScrapeOrchestrator(
    m, outputDir, createMovieFolders, hardlinkInsteadOfCopy,
    downloadImages, folderLayers, filenameFormat,
    maxTitleLength, maxFilenameLength, suffixKeywords
)
```

- [ ] **Step 5: Compile to verify**

```powershell
cd app; ./gradlew compileKotlin --no-daemon
```
Expected: compilation errors expected (SettingsState/SettingsActions not yet updated). Next task resolves them.

---

### Task 5: SettingsScreen — Add Naming Rules UI Section

**Files:**
- Modify: `app/src/main/kotlin/javscraper/ui/screens/SettingsScreen.kt`

- [ ] **Step 1: Extend SettingsState and SettingsActions data classes**

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

- [ ] **Step 2: Add naming rules UI after the download images switch**

Insert after `SettingsSwitchRow(t.settingsDownloadImages, ...)` and its `Spacer`:

```kotlin
Spacer(Modifier.height(20.dp))
Text(t.settingsRenameTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
Spacer(Modifier.height(8.dp))

// Folder Layers
Text(t.settingsFolderLayers, style = MaterialTheme.typography.bodyMedium)
Spacer(Modifier.height(4.dp))
Column {
    state.folderLayers.forEachIndexed { index, layer ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = layer,
                onValueChange = { actions.onFolderLayerChange(index, it) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text(t.settingsFolderLayerPlaceholder) }
            )
            IconButton(onClick = { actions.onRemoveLayer(index) }) {
                Icon(Icons.Default.RemoveCircleOutline, t.settingsRemoveLayer)
            }
        }
        Spacer(Modifier.height(4.dp))
    }
    OutlinedButton(onClick = actions.onAddLayer, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text(t.settingsAddLayer)
    }
}
Spacer(Modifier.height(12.dp))

// Filename Format
Text(t.settingsFilenameFormat, style = MaterialTheme.typography.bodyMedium)
Spacer(Modifier.height(4.dp))
OutlinedTextField(
    value = state.filenameFormat,
    onValueChange = actions.onFilenameFormatChange,
    modifier = Modifier.fillMaxWidth(),
    singleLine = true,
    placeholder = { Text(t.settingsFilenamePlaceholder) }
)
Spacer(Modifier.height(4.dp))
Text(
    t.settingsRenameVariables,
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant
)
Spacer(Modifier.height(16.dp))

// Advanced section
var advancedExpanded by remember { mutableStateOf(false) }
OutlinedButton(
    onClick = { advancedExpanded = !advancedExpanded },
    modifier = Modifier.fillMaxWidth()
) {
    Icon(if (advancedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
    Spacer(Modifier.width(8.dp))
    Text(t.settingsAdvanced)
}
AnimatedVisibility(visible = advancedExpanded) {
    Column(Modifier.padding(top = 8.dp)) {
        OutlinedTextField(
            value = state.maxTitleLength.toString(),
            onValueChange = { it.toIntOrNull()?.let(actions.onMaxTitleLengthChange) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(t.settingsMaxTitleLength) }
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.maxFilenameLength.toString(),
            onValueChange = { it.toIntOrNull()?.let(actions.onMaxFilenameLengthChange) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(t.settingsMaxFilenameLength) }
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.suffixKeywords.joinToString(", "),
            onValueChange = {
                actions.onSuffixKeywordsChange(
                    it.split(",").map { s -> s.trim() }.filter { s -> s.isNotBlank() }
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(t.settingsSuffixKeywords) },
            placeholder = { Text(t.settingsSuffixKeywordsHint) }
        )
    }
}
```

- [ ] **Step 3: Add missing imports at top of file**

```kotlin
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RemoveCircleOutline
```

- [ ] **Step 4: Compile to verify**

```powershell
cd app; ./gradlew compileKotlin --no-daemon
```
Expected: BUILD SUCCESSFUL

---

### Task 6: ScrapeOrchestrator — Integrate RenameFormatter

**Files:**
- Modify: `app/src/main/kotlin/javscraper/scrape/ScrapeOrchestrator.kt`

- [ ] **Step 1: Expand constructor with rename params and add import**

```kotlin
import javscraper.io.RenameFormatter

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

- [ ] **Step 2: Replace getOut() with resolveOutputPaths()**

Remove the existing getOut() method and add:

```kotlin
private fun resolveOutputPaths(sf: ScannedFile, video: Video): OutputPaths {
    val ext = sf.fileName.substringAfterLast('.')
    if (outputDir.isBlank()) return OutputPaths(Path.of(""), sf.fileName, Path.of(sf.fileName))

    val base = Path.of(outputDir)
    if (!createMovieFolders) return OutputPaths(base, sf.fileName, base.resolve(sf.fileName))

    val suffix = RenameFormatter.detectSuffix(sf.fileName, suffixKeywords)
    val layers = RenameFormatter.formatFolder(video, folderLayers, suffix)
    val folder = layers.fold(base) { acc, layer -> acc.resolve(layer) }
    val rawFilename = RenameFormatter.formatFilename(
        video, filenameFormat, suffix, maxFilenameLength, maxTitleLength
    )
    return OutputPaths(folder, "$rawFilename.$ext", folder.resolve("$rawFilename.$ext"))
}

private data class OutputPaths(val folder: Path, val filename: String, val fullPath: Path)
```

- [ ] **Step 3: Update process() to use resolveOutputPaths**

Replace `val out = getOut(sf.number, video)` and everything after it up to `return result`:

```kotlin
val paths = resolveOutputPaths(sf, video)
Files.createDirectories(paths.folder)
try {
    Files.writeString(paths.folder.resolve("${video.number}.nfo"), NfoWriter.generate(video))
} catch (e: Exception) { log.error(e) { "NFO failed" } }
if (downloadImages) try {
    ImageSaver.download(paths.folder, video.coverUrl, video.posterUrl, video.sampleImages)
} catch (e: Exception) { log.warn(e) { "Images failed" } }
try {
    val src = Path.of(sf.path)
    val tgt = paths.fullPath
    if (!Files.exists(tgt)) {
        if (hardlinkInsteadOfCopy) {
            try { Files.createLink(tgt, src) }
            catch (_: Exception) { Files.copy(src, tgt, StandardCopyOption.REPLACE_EXISTING) }
        } else Files.copy(src, tgt, StandardCopyOption.REPLACE_EXISTING)
    }
} catch (e: Exception) { log.warn(e) { "File move failed" } }
```

- [ ] **Step 4: Compile to verify**

```powershell
cd app; ./gradlew compileKotlin --no-daemon
```
Expected: BUILD SUCCESSFUL

---

### Task 7: Wire App.kt — Pass new state and actions to SettingsScreen

**Files:**
- Modify: `app/src/main/kotlin/javscraper/App.kt`

- [ ] **Step 1: Add new fields to SettingsState construction**

```kotlin
folderLayers = vm.folderLayers,
filenameFormat = vm.filenameFormat,
maxTitleLength = vm.maxTitleLength,
maxFilenameLength = vm.maxFilenameLength,
suffixKeywords = vm.suffixKeywords,
```

- [ ] **Step 2: Add new fields to SettingsActions construction**

```kotlin
onFolderLayerChange = vm::updateFolderLayer,
onAddLayer = vm::addLayer,
onRemoveLayer = vm::removeLayer,
onFilenameFormatChange = vm::updateFilenameFormat,
onMaxTitleLengthChange = vm::updateMaxTitleLength,
onMaxFilenameLengthChange = vm::updateMaxFilenameLength,
onSuffixKeywordsChange = vm::updateSuffixKeywords,
```

- [ ] **Step 3: Compile to verify**

```powershell
cd app; ./gradlew compileKotlin --no-daemon
```
Expected: BUILD SUCCESSFUL

---

### Task 8: Verify full build

- [ ] **Step 1: Run full compile**

```powershell
cd app; ./gradlew compileKotlin --no-daemon
```
Expected: BUILD SUCCESSFUL IN ...

- [ ] **Step 2: Run tests**

```powershell
cd app; ./gradlew test --no-daemon
```
Expected: All tests pass
