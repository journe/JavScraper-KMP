# Single Scrape Dialog — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a per-item scrape button on the scan page that opens a 3-phase dialog (input → progress → result) and syncs results to the progress/gallery pages.

**Architecture:** A sealed interface `SingleScrapeDialogState` controls dialog phase transitions in `AppViewModel`. `FileScanScreen` renders the dialog inline and passes user actions back to the ViewModel. Confirm syncs the `ScrapeTask` to `tasks` and `Video` to `results`, enabling the PROGRESS and GALLERY navigation tabs.

**Tech Stack:** Kotlin + Compose Desktop, Material3 AlertDialog, existing ScrapeOrchestrator/SidecarManager.

---

## Task 1: Add i18n Strings

**Files:**
- Modify: `app/src/main/kotlin/javscraper/i18n/TranslationEn.kt`
- Modify: `app/src/main/kotlin/javscraper/i18n/TranslationZh.kt`

**Interfaces:**
- Consumes: existing `TranslationEn` open class pattern
- Produces: new `open val` strings used by Task 4 dialog composables

- [ ] **Step 1: Add English strings to TranslationEn.kt**

Insert after the `commonBrowse` line at the end of the class:

```kotlin
    // --- Single Scrape Dialog ---
    open val singleScrapeTitle: String = "Single Scrape"
    open val singleScrapeNumberLabel: String = "Number"
    open val singleScrapeSiteLabel: String = "Scraper Site"
    open val singleScrapeSiteAuto: String = "Auto"
    open val singleScrapeStart: String = "Start Scraping"
    open val singleScrapeInProgress: String = "Scraping in progress..."
    open val singleScrapeResultTitle: String = "Scrape Result"
    open val singleScrapeResultError: String = "Error"
    open val commonConfirm: String = "Confirm"
```

- [ ] **Step 2: Add Chinese strings to TranslationZh.kt**

Insert after the `override val commonBrowse` line (save with correct UTF-8 encoding):

```kotlin
    // --- Single Scrape Dialog ---
    override val singleScrapeTitle: String = "单个刮削"
    override val singleScrapeNumberLabel: String = "番号"
    override val singleScrapeSiteLabel: String = "刮削站点"
    override val singleScrapeSiteAuto: String = "自动（由服务端决定）"
    override val singleScrapeStart: String = "开始刮削"
    override val singleScrapeInProgress: String = "正在刮削..."
    override val singleScrapeResultTitle: String = "刮削结果"
    override val singleScrapeResultError: String = "错误"
    override val commonConfirm: String = "确认"
```

> **Encoding note:** The file must be saved with UTF-8 encoding (no BOM). Chinese characters must be actual Unicode characters, not escaped sequences.

---

## Task 2: Add SingleScrapeDialogState Sealed Interface

**Files:**
- Modify: `app/src/main/kotlin/javscraper/models/Models.kt`

**Interfaces:**
- Consumes: `Video` data class (already in Models.kt)
- Produces: `SingleScrapeDialogState` sealed interface used by Task 3 and Task 4

- [ ] **Step 1: Add the sealed interface to Models.kt**

Append at the end of the file (after `SiteInfo`):

```kotlin
sealed interface SingleScrapeDialogState {
    data object Closed : SingleScrapeDialogState
    data object Input : SingleScrapeDialogState
    data object Scraping : SingleScrapeDialogState
    data class Result(val video: Video?, val error: String?) : SingleScrapeDialogState
}
```

---

## Task 3: Add ViewModel State and Methods

**Files:**
- Modify: `app/src/main/kotlin/javscraper/AppViewModel.kt`

**Interfaces:**
- Consumes: `SingleScrapeDialogState`, existing `ScannedFile`, `ScrapeTask`, `ScrapeTaskStatus`, `Video`, `SiteInfo`, `orch?.process(sf, site)`
- Produces: state fields + openSingleScrape, closeSingleScrape, startSingleScrape, confirmSingleScrape, updateSingleScrapeNumber, updateSingleScrapeSite

- [ ] **Step 1: Add dialog state fields**

Insert after the `// --- Sites ---` section:

```kotlin
    // --- Single scrape dialog state ---
    var singleScrapeDialogState by mutableStateOf<SingleScrapeDialogState>(SingleScrapeDialogState.Closed)
        private set
    var singleScrapeFile by mutableStateOf<ScannedFile?>(null)
        private set
    var singleScrapeNumber by mutableStateOf("")
        private set
    var singleScrapeSite by mutableStateOf<String?>(null)
        private set
    var singleScrapeTask by mutableStateOf<ScrapeTask?>(null)
        private set
```

- [ ] **Step 2: Add setter methods**

```kotlin
    fun updateSingleScrapeNumber(value: String) {
        singleScrapeNumber = value
    }

    fun updateSingleScrapeSite(value: String?) {
        singleScrapeSite = value
    }
```

- [ ] **Step 3: Add core methods**

```kotlin
    fun openSingleScrape(file: ScannedFile) {
        singleScrapeFile = file
        singleScrapeNumber = file.number
        singleScrapeSite = null
        singleScrapeTask = ScrapeTask(file.number, file.fileName, status = ScrapeTaskStatus.PENDING)
        singleScrapeDialogState = SingleScrapeDialogState.Input
    }

    fun closeSingleScrape() {
        singleScrapeDialogState = SingleScrapeDialogState.Closed
        singleScrapeFile = null
        singleScrapeTask = null
    }

    fun startSingleScrape() {
        val file = singleScrapeFile ?: return
        val number = singleScrapeNumber
        val site = singleScrapeSite
        if (number.isBlank()) return
        singleScrapeDialogState = SingleScrapeDialogState.Scraping
        scope.launch {
            val sf = file.copy(number = number)
            singleScrapeTask = singleScrapeTask?.copy(status = ScrapeTaskStatus.SCRAPING)
            try {
                val result = orch?.process(sf, site) ?: return@launch
                if (result.success && result.data != null) {
                    singleScrapeDialogState = SingleScrapeDialogState.Result(result.data, null)
                    singleScrapeTask = singleScrapeTask?.copy(status = ScrapeTaskStatus.SUCCESS, video = result.data)
                } else {
                    val errMsg = result.error?.message ?: "Unknown error"
                    singleScrapeDialogState = SingleScrapeDialogState.Result(null, errMsg)
                    singleScrapeTask = singleScrapeTask?.copy(status = ScrapeTaskStatus.FAILED, error = errMsg)
                }
            } catch (e: Exception) {
                singleScrapeDialogState = SingleScrapeDialogState.Result(null, e.message ?: "Unknown error")
                singleScrapeTask = singleScrapeTask?.copy(status = ScrapeTaskStatus.FAILED, error = e.message ?: "")
            }
        }
    }

    fun confirmSingleScrape() {
        val task = singleScrapeTask
        val state = singleScrapeDialogState
        if (task != null) {
            tasks = tasks + task
        }
        if (state is SingleScrapeDialogState.Result && state.video != null) {
            results = results + state.video
        }
        closeSingleScrape()
    }
```

---

## Task 4: Update FileScanScreen with Button and Dialogs

**Files:**
- Modify: `app/src/main/kotlin/javscraper/ui/screens/FileScanScreen.kt`

- [ ] **Step 1: Add imports**

```kotlin
import javscraper.models.SingleScrapeDialogState
import javscraper.models.SiteInfo
import javscraper.ui.components.SiteItem
```

- [ ] **Step 2: Add new parameters to FileScanScreen**

```kotlin
@Composable
fun FileScanScreen(
    scannedFiles: List<ScannedFile>,
    scanDir: String,
    isScanning: Boolean,
    onSelectDirectory: () -> Unit,
    onStartScan: () -> Unit,
    onStartScrape: () -> Unit,
    // New:
    onSingleScrapeClick: (ScannedFile) -> Unit,
    singleScrapeDialogState: SingleScrapeDialogState,
    singleScrapeNumber: String,
    singleScrapeTask: ScrapeTask?,
    sites: List<SiteInfo>,
    onSingleScrapeNumberChange: (String) -> Unit,
    onSingleScrapeSiteChange: (String?) -> Unit,
    onStartSingleScrape: () -> Unit,
    onCloseSingleScrape: () -> Unit,
    onConfirmScrapeResult: () -> Unit,
    modifier: Modifier = Modifier
)
```

- [ ] **Step 3: Add per-item icon button**

In each Card Row, after the number badge Surface, add:

```kotlin
                            if (file.number.isNotBlank()) {
                                IconButton(
                                    onClick = { onSingleScrapeClick(file) },
                                    enabled = !isScanning
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = t.singleScrapeTitle,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
```

- [ ] **Step 4: Add dialog composables at the end of Column**

```kotlin
        // --- Single scrape dialog ---
        when (singleScrapeDialogState) {
            SingleScrapeDialogState.Closed -> { }
            SingleScrapeDialogState.Input -> {
                AlertDialog(
                    onDismissRequest = onCloseSingleScrape,
                    title = { Text(t.singleScrapeTitle) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = singleScrapeNumber,
                                onValueChange = onSingleScrapeNumberChange,
                                label = { Text(t.singleScrapeNumberLabel) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            SiteSelector(
                                sites = listOf(SiteItem("", t.singleScrapeSiteAuto, true)) +
                                    sites.map { SiteItem(it.id, it.name, true) },
                                onToggle = { id, _ -> onSingleScrapeSiteChange(id.ifEmpty { null }) }
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = onStartSingleScrape,
                            enabled = singleScrapeNumber.isNotBlank()
                        ) { Text(t.singleScrapeStart) }
                    },
                    dismissButton = {
                        TextButton(onClick = onCloseSingleScrape) { Text(t.progressCancel) }
                    }
                )
            }
            SingleScrapeDialogState.Scraping -> {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text(t.singleScrapeTitle) },
                    text = {
                        val task = singleScrapeTask
                        if (task != null) {
                            Card(Modifier.fillMaxWidth()) {
                                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(task.number, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(t.singleScrapeInProgress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = onCloseSingleScrape) { Text(t.progressCancel) }
                    }
                )
            }
            is SingleScrapeDialogState.Result -> {
                val resultState = singleScrapeDialogState as SingleScrapeDialogState.Result
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text(t.singleScrapeResultTitle) },
                    text = {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                if (resultState.video != null) {
                                    val v = resultState.video
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.tertiary)
                                        Spacer(Modifier.width(8.dp))
                                        Text(v.number, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text("Title: " + v.title, style = MaterialTheme.typography.bodySmall)
                                    Text("Maker: " + v.maker, style = MaterialTheme.typography.bodySmall)
                                    Text("Actresses: " + v.actresses.joinToString(", "), style = MaterialTheme.typography.bodySmall)
                                    if (v.date.isNotBlank()) Text("Date: " + v.date, style = MaterialTheme.typography.bodySmall)
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                                        Spacer(Modifier.width(8.dp))
                                        Text(singleScrapeNumber, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        resultState.error ?: t.singleScrapeResultError,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = onConfirmScrapeResult) { Text(t.commonConfirm) }
                    },
                    dismissButton = {}
                )
            }
        }
```

---

## Task 5: Wire App.kt

**Files:**
- Modify: `app/src/main/kotlin/javscraper/App.kt`

- [ ] **Step 1: Wire new params in SCAN screen block**

Replace the SCAN block:

```kotlin
                        Screen.SCAN -> FileScanScreen(
                            scannedFiles = viewModel.scannedFiles,
                            scanDir = viewModel.scanDir,
                            isScanning = viewModel.scanning,
                            onSelectDirectory = viewModel::selectScanDir,
                            onStartScan = viewModel::startScan,
                            onStartScrape = { viewModel.navigate(Screen.PROGRESS) },
                            onSingleScrapeClick = viewModel::openSingleScrape,
                            singleScrapeDialogState = viewModel.singleScrapeDialogState,
                            singleScrapeNumber = viewModel.singleScrapeNumber,
                            singleScrapeTask = viewModel.singleScrapeTask,
                            sites = viewModel.sites,
                            onSingleScrapeNumberChange = viewModel::updateSingleScrapeNumber,
                            onSingleScrapeSiteChange = viewModel::updateSingleScrapeSite,
                            onStartSingleScrape = viewModel::startSingleScrape,
                            onCloseSingleScrape = viewModel::closeSingleScrape,
                            onConfirmScrapeResult = viewModel::confirmSingleScrape
                        )
```

---

## Task 6: Verify Compilation

- [ ] **Step 1: Compile**

```powershell
cd app; ./gradlew compileKotlin --no-daemon
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run tests**

```powershell
cd app; ./gradlew test --no-daemon
```

Expected: All tests pass.

---

## Self-Review Checklist

- [x] Spec coverage: All requirements mapped to tasks (i18n -> sealed interface -> ViewModel -> UI -> wiring -> verify)
- [x] No placeholders: Every step has complete code, exact paths, exact commands
- [x] Type consistency: SingleScrapeDialogState used consistently across all tasks
- [x] Correct UTF-8 handling noted for TranslationZh.kt
