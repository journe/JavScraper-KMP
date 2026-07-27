# JavScraper Phase 3 — Testing & Integration

**Status:** ⬜ PENDING

**Goal:** Add Kotlin unit tests for all core components, write end-to-end integration tests, and produce a manual smoke-test checklist.

## Task 1: Kotlin Unit Tests

### 1.1 SidecarManager 测试
- **File:** `app/src/test/kotlin/javscraper/sidecar/SidecarManagerTest.kt`
- Mock the Python worker process with a fake stdin/stdout script
- Test `start()` / `stop()` lifecycle
- Test `listSites()` returns parsed `SiteInfo` list
- Test `scrape()` returns success result
- Test timeout / error handling
- Test process crash recovery

### 1.2 NfoWriter 测试
- **File:** `app/src/test/kotlin/javscraper/io/NfoWriterTest.kt`
- Test basic NFO generation with full `Video` object
- Test XML structure: `<movie>` root, `<title>`, `<actor>`, `<genre>` etc.
- Test edge cases: empty actresses list, null duration, blank title
- Test special characters in strings (Japanese, Unicode)
- Verify output matches Jellyfin expected format

### 1.3 FileScanner 测试
- **File:** `app/src/test/kotlin/javscraper/io/FileScannerTest.kt`
- Test `extractNumber()` with various filename patterns:
  - Standard: `SONE-205.mp4`, `ABP-123.avi`
  - FC2: `FC2-1234567.mp4`
  - HEYZO: `HEYZO-1234.mkv`
  - Six-digit: `123456-789.mp4`
  - Bracketed: `[FC2]1234567.mp4`
  - Lowercase: `sone-205.mkv` → `SONE-205`
- Test `isVideo()` with valid/invalid extensions
- Test `isSample()` sample filename detection
- Test `scanDirectory()` with mock directory tree

### 1.4 ImageSaver 测试
- **File:** `app/src/test/kotlin/javscraper/io/ImageSaverTest.kt`
- Test URL normalization (`//host/path` → `https://host/path`)
- Test data URI skip
- Test parallel download behavior

### 1.5 Models 测试
- **File:** `app/src/test/kotlin/javscraper/models/ModelsTest.kt`
- Test `Video` data class serialization to/from JSON
- Test `ScrapeResult` success/error variants
- Test `ScannedFile` creation

### 1.6 SettingsManager 测试
- **File:** `app/src/test/kotlin/javscraper/settings/SettingsManagerTest.kt`
- Test config load/save cycle
- Test defaults when no config file exists
- Test update and reset

## Task 2: End-to-End Integration Test

### 2.1 Python Worker IPC 联通测试
- **File:** `app/src/test/kotlin/javscraper/sidecar/SidecarE2ETest.kt`
- Start actual Python worker process
- Call `list_sites` and verify 9 sites returned
- Call `get_capabilities` and verify features
- Call `probe_file` with sample filename
- Call `shutdown` and verify process exits

### 2.2 ScrapeOrchestrator 集成测试
- **File:** `app/src/test/kotlin/javscraper/scrape/ScrapeOrchestratorTest.kt`
- Mock SidecarManager to return controlled responses
- Test full pipeline: scrape → NFO write → image download → file link
- Verify NFO file content on disk
- Verify file linking/copying

## Task 3: Manual Smoke Test Checklist

### 3.1 Worker 测试
```powershell
# 1. 启动 Worker 并列出站点
echo {"jsonrpc":"2.0","id":"1","method":"list_sites","params":{}} | venv\Scripts\python main.py

# 2. 测试 scrape
echo {"jsonrpc":"2.0","id":"2","method":"scrape","params":{"number":"SONE-205"}} | venv\Scripts\python main.py

# 3. 测试 probe_file
echo {"jsonrpc":"2.0","id":"3","method":"probe_file","params":{"file_path":"C:\\test\\SONE-205.mp4"}} | venv\Scripts\python main.py
```

### 3.2 Kotlin 桌面端测试
```powershell
cd app
gradle run
```
- [ ] UI 窗口正常启动 (1200x800)
- [ ] Bottom navigation 4 个 tab 可点击
- [ ] Worker 状态栏显示 "Ready (9 sites)"
- [ ] "Select Directory" 弹出文件选择器
- [ ] 扫描视频文件并显示番号
- [ ] 点击 "Start Scraping" 进入进度页

## Task 4: Test Configuration

### 4.1 Gradle 测试配置
Add to `app/build.gradle.kts`:
```kotlin
dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
```

### 4.2 测试目录结构
```
app/src/test/kotlin/javscraper/
├── sidecar/SidecarManagerTest.kt
├── io/NfoWriterTest.kt
├── io/FileScannerTest.kt
├── io/ImageSaverTest.kt
├── models/ModelsTest.kt
└── settings/SettingsManagerTest.kt
```

## Dependencies
- kotlin.test (built into Kotlin Gradle plugin)
- kotlinx-coroutines-test for async testing
- Python Worker must be locatable for E2E tests