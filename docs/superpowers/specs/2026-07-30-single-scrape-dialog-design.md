# Single Scrape Dialog — Design Spec

**Date:** 2026-07-30
**Status:** Approved for design
**Component:** FileScanScreen / AppViewModel / i18n

---

## 1. Objective

Currently the scan page has only a single "Start Scraping" button that scrapes all scanned files in bulk. Add the ability to scrape individual files: a per-item button opens a dialog where the user can edit the number, select a scraper site, view progress, and see results — then confirm to sync results back to the progress and gallery pages.

## 2. Current State

- `FileScanScreen` shows a list of `ScannedFile` cards with filename and number badge
- Only one bulk action button "Start Scraping" at the top
- No per-item scrape capability
- `ScrapeOrchestrator.process(sf, site)` already supports site-specific scraping
- `SidecarManager.listSites()` provides available scraper sites

## 3. Dialog State Machine

A sealed interface controls the three phases of the dialog:

```kotlin
sealed interface SingleScrapeDialogState {
    data object Closed : SingleScrapeDialogState
    data object Input : SingleScrapeDialogState
    data object Scraping : SingleScrapeDialogState
    data class Result(val video: Video?, val error: String?) : SingleScrapeDialogState
}
```

- **Input** — User edits the JAV number and selects a scraper site
- **Scraping** — Input hidden, progress card with spinning indicator
- **Result** — Success card (video summary) or failure card (error message) with Confirm button

## 4. ViewModel Changes

### New State Fields (in AppViewModel)

```kotlin
var singleScrapeDialogState by mutableStateOf<SingleScrapeDialogState>(SingleScrapeDialogState.Closed)
var singleScrapeFile by mutableStateOf<ScannedFile?>(null)
var singleScrapeNumber by mutableStateOf("")
var singleScrapeSite by mutableStateOf<String?>(null)
var singleScrapeTask by mutableStateOf<ScrapeTask?>(null)
```

### New Methods

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

fun startSingleScrape(number: String, site: String?) {
    val file = singleScrapeFile ?: return
    singleScrapeNumber = number
    singleScrapeSite = site
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

## 5. FileScanScreen Changes

### New Callback Parameters

```kotlin
@Composable
fun FileScanScreen(
    // ... existing ...
    onSingleScrapeClick: (ScannedFile) -> Unit,
    singleScrapeDialogState: SingleScrapeDialogState,
    singleScrapeNumber: String,
    singleScrapeSite: String?,
    singleScrapeTask: ScrapeTask?,
    sites: List<SiteInfo>,
    onSingleScrapeNumberChange: (String) -> Unit,
    onSingleScrapeSiteChange: (String?) -> Unit,
    onStartSingleScrape: (number: String, site: String?) -> Unit,
    onCloseSingleScrape: () -> Unit,
    modifier: Modifier = Modifier
)
```

### List Item Button

In each item Card Row, next to the number badge:

```kotlin
IconButton(onClick = { onSingleScrapeClick(file) }, enabled = file.number.isNotBlank()) {
    Icon(Icons.Default.PlayArrow, "Scrape single")
}
```

### Dialog Rendering

```kotlin
when (val state = singleScrapeDialogState) {
    SingleScrapeDialogState.Input -> SingleScrapeInputDialog(...)
    SingleScrapeDialogState.Scraping -> SingleScrapeProgressDialog(...)
    is SingleScrapeDialogState.Result -> SingleScrapeResultDialog(...)
    SingleScrapeDialogState.Closed -> { }
}
```

## 6. Dialog Component Layouts

### 6a. SingleScrapeInputDialog

Standard AlertDialog containing:
- OutlinedTextField for number (pre-filled, editable)
- DropdownMenu for site selection ("Auto" + each SiteInfo)
- Cancel and "Start Scraping" buttons

### 6b. SingleScrapeProgressDialog

AlertDialog containing a ScrapeTask card with:
- ScrapeTaskStatus.SCRAPING display + CircularProgressIndicator
- "Scraping in progress..." text
- Cancel button

### 6c. SingleScrapeResultDialog

AlertDialog containing:
- Success: number, title, maker, actresses, date with green check icon
- Failure: number + error message with red error icon
- Confirm button to close and sync results

## 7. i18n Strings

### TranslationEn.kt

```kotlin
open val singleScrapeTitle: String = "Single Scrape"
open val singleScrapeNumberLabel: String = "Number"
open val singleScrapeSiteLabel: String = "Scraper Site"
open val singleScrapeSiteAuto: String = "Auto"
open val singleScrapeStart: String = "Start Scraping"
open val singleScrapeInProgress: String = "Scraping in progress..."
open val singleScrapeResultTitle: String = "Scrape Result"
open val singleScrapeResultError: String = "Error"
```

### TranslationZh.kt

```kotlin
override val singleScrapeTitle: String = "单个刮削"
override val singleScrapeNumberLabel: String = "番号"
override val singleScrapeSiteLabel: String = "刮削站点"
override val singleScrapeSiteAuto: String = "自动（由服务端决定）"
override val singleScrapeStart: String = "开始刮削"
override val singleScrapeInProgress: String = "正在刮削..."
override val singleScrapeResultTitle: String = "刮削结果"
override val singleScrapeResultError: String = "错误"
```

## 8. App.kt Changes

Wire new ViewModel state to FileScanScreen:

```kotlin
Screen.SCAN -> FileScanScreen(
    // ... existing ...
    onSingleScrapeClick = viewModel::openSingleScrape,
    singleScrapeDialogState = viewModel.singleScrapeDialogState,
    singleScrapeNumber = viewModel.singleScrapeNumber,
    singleScrapeSite = viewModel.singleScrapeSite,
    singleScrapeTask = viewModel.singleScrapeTask,
    sites = viewModel.sites,
    onSingleScrapeNumberChange = { viewModel.singleScrapeNumber = it },
    onSingleScrapeSiteChange = { viewModel.singleScrapeSite = it },
    onStartSingleScrape = viewModel::startSingleScrape,
    onCloseSingleScrape = viewModel::closeSingleScrape
)
```

## 9. Files Changed

| File | Change |
|------|--------|
| `AppViewModel.kt` | Add dialog state + single scrape methods |
| `FileScanScreen.kt` | Add per-item button + dialog composables |
| `App.kt` | Wire new state to FileScanScreen |
| `TranslationEn.kt` | Add i18n strings |
| `TranslationZh.kt` | Add Chinese translations |

## 10. Out of Scope

- Multi-select single scrape (checkboxes for batch)
- Retry from dialog after failure
- Real-time log streaming within dialog
- Keyboard shortcuts
