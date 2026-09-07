# 应用生命周期日志实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 为全应用提供生命周期日志缓存、可选本地文件输出、日志查看弹窗，并补齐 SidecarManager 全方法日志。

**Architecture:** 通过 Logback root appender 捕获现有 `KotlinLogging` 输出；`AppLogController` 维护生命周期内存缓存并按需把缓存与后续日志追加到本地文件。设置项由 `AppSettings` 持久化并经 `SettingsController/AppViewModel` 流入无状态 UI；TopAppBar 与日志弹窗只消费状态，不直接依赖日志实现。

**Tech Stack:** Kotlin、Compose Desktop、Material 3、Logback classic、kotlinx.coroutines、kotlin.test。

## Global Constraints

- 所有新增 UI 文案必须同步 `TranslationEn` 与 `TranslationZh`。
- Kotlin 单文件不超过 400 行；每层目录文件尽量不超过 8 个。
- 日志文件默认路径：`%USERPROFILE%/.javscraper/logs/javscraper.log`。
- 文件日志开启时先写入当前生命周期已缓存日志，再实时追加后续日志。
- `SidecarManager` 所有公开与私有方法都必须有日志。`sendRequest` 与 `readResponses` 统一记录完整 JSON-RPC 请求体和响应体 JSON。
- Gradle wrapper 已确认使用腾讯云镜像，不需调整。

---

### Task 1: 日志核心

**Files:**

- Create: `app/src/main/kotlin/javscraper/io/logging/LogEntry.kt`
- Create: `app/src/main/kotlin/javscraper/io/logging/LifecycleLogStore.kt`
- Create: `app/src/main/kotlin/javscraper/io/logging/LifecycleLogbackAppender.kt`
- Create: `app/src/main/kotlin/javscraper/io/logging/AppLogController.kt`
- Test: `app/src/test/kotlin/javscraper/io/logging/AppLogControllerTest.kt`

**Interfaces:**

- `data class LogEntry(timestampMillis: Long, level: String, loggerName: String, message: String, throwableStack: String? = null)`
- `AppLogController(logFilePath: Path = Path.of(System.getProperty("user.home"), ".javscraper", "logs", "javscraper.log"))`
  - `val entries: List<LogEntry>`
  - `val logFilePath: Path`
  - `fun setFileLoggingEnabled(enabled: Boolean)`
  - `fun dispose()`

- [x] 编写失败测试：Logback 日志进入生命周期缓存；开启文件日志后缓存与后续日志均写入文件。
- [x] 运行 `.\gradlew.bat :app:test --tests javscraper.io.logging.AppLogControllerTest --no-daemon`，确认编译失败。
- [x] 实现日志模型、线程安全缓存、Logback appender 与文件开关。
- [x] 重跑测试确认通过。

### Task 2: 设置持久化与 UI 入口

**Files:**

- Modify: `app/src/main/kotlin/javscraper/settings/SettingsManager.kt`
- Modify: `app/src/main/kotlin/javscraper/SettingsController.kt`
- Modify: `app/src/main/kotlin/javscraper/ui/screens/settings/SettingsScreen.kt`
- Modify: `app/src/main/kotlin/javscraper/ui/screens/settings/OtherSettingsTab.kt`
- Modify: `app/src/main/kotlin/javscraper/ui/screens/settings/SettingsPreview.kt`
- Modify: `app/src/main/kotlin/javscraper/AppViewModel.kt`
- Modify: `app/src/main/kotlin/javscraper/App.kt`
- Create: `app/src/main/kotlin/javscraper/ui/components/LogsDialog.kt`
- Modify: `app/src/main/kotlin/javscraper/i18n/TranslationEn.kt`
- Modify: `app/src/main/kotlin/javscraper/i18n/TranslationZh.kt`
- Test: `app/src/test/kotlin/javscraper/settings/SettingsManagerTest.kt`

**Interfaces:**

- `AppSettings.fileLoggingEnabled: Boolean = false`
- `SettingsState.fileLoggingEnabled: Boolean`
- `SettingsActions.onFileLoggingChange: (Boolean) -> Unit`
- `AppLogController.entries/logFilePath` 暴露到 `AppViewModel`
- `LogsDialog(entries: List<LogEntry>, filePath: String, onDismiss: () -> Unit)`

- [x] 先扩展设置序列化测试，确认 `fileLoggingEnabled` 缺失时默认 false、序列化可往返。
- [x] 实现设置模型、控制器和 State/Actions 字段。
- [x] 在其他设置页新增日志文件开关。
- [x] TopAppBar 设置按钮左侧新增日志按钮，并接入生命周期日志弹窗。
- [x] 补齐中英文文案与预览数据。
- [x] 运行设置测试和 Kotlin 编译。

### Task 3: SidecarManager 全方法日志

**Files:**

- Modify: `app/src/main/kotlin/javscraper/sidecar/SidecarManager.kt`
- Test: `app/src/test/kotlin/javscraper/sidecar/SidecarManagerLoggingTest.kt`

**Methods:**

- `start/stop/close/listSites/checkSites/scrape`
- `sendRequest/readResponses/ensureRunning/cleanup`

- [x] 编写失败测试：使用无效 worker 路径依次调用公开方法，断言生命周期缓存中包含核心方法与内部链路日志标记。
- [x] 为每个方法补充入口、结果、失败或状态日志，记录耗时与关键参数。
- [x] 运行新增测试确认通过。
- [x] 编写失败测试：模拟 worker 返回 JSON，断言请求体与响应体完整进入日志。
- [x] `sendRequest` 与 `readResponses` 统一记录完整 JSON，不逐个业务方法重复拼接。
- [x] 执行全量 Kotlin 测试、`git diff --check` 和行数/目录约束检查。
