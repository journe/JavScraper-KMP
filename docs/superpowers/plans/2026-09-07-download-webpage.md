# 下载网页功能实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增“下载网页”设置，在刮削成功时保存最终详情页 MHTML，并从该 MHTML 提取图片，避免重复网络请求。

**Architecture:** Python worker 在 `BaseScraper` 层捕获详情页响应，用通用 MHTML 生成器内嵌页面资源，并把 base64 内容附加到 `Video.webpage`。Kotlin 保存设置并透传开关；写盘阶段先保存 MHTML，再通过 worker RPC 从 MHTML 提取图片，替代 URL 下载。

**Tech Stack:** Python 3.12 标准库 `email`、`requests`、`BeautifulSoup`；Kotlin Compose、kotlinx.serialization、JSON-RPC sidecar。

## Global Constraints

- 单文件刮削保持“元数据抓取 → 预览 → 确认 → 写盘”，预览取消不得产生任何输出文件。
- MHTML 文件名为安全化后的 `番号-站点.mhtml`，例如 `FC2-PPV-1723984-FC2.mhtml`。
- 开关默认关闭，旧配置可解析。
- Python 源码修改后必须重新打包并部署 `app/worker/scraper-worker.exe`。
- 每个生产行为先写失败测试，再实现。
- 不自动提交 Git commit。

---

### Task 1: Python MHTML 生成与提取

**Files:**
- Create: `scraper-worker/core/webpage_archive.py`
- Test: `scraper-worker/tests/test_webpage_archive.py`

**Interfaces:**
- Produces: `CapturedResponse(url: str, content: bytes, content_type: str)`
- Produces: `build_mhtml(root: CapturedResponse, related: list[CapturedResponse], fetch: Callable[[str], CapturedResponse | None], extra_urls: Iterable[str] = ()) -> bytes`
- Produces: `extract_images(mhtml_path: str, output_dir: str, cover_url: str = "", poster_url: str = "", sample_images: Iterable[str] = ()) -> dict`

- [x] 编写失败测试：根 HTML、CSS、图片只抓取一次并写入 MHTML；CSS 内部资源会递归抓取；提取器写出 `poster.jpg`、`fanart.jpg`、`extrafanart/fanart1.jpg`。
- [x] 运行 `scraper-worker\venv\Scripts\python -m pytest tests/test_webpage_archive.py -q`，确认因模块不存在失败。
- [x] 实现标准 MIME Multipart/Related 归档、URL 规范化、资源去重、失败资源跳过和图片提取。
- [x] 重跑测试并确认通过。

### Task 2: Worker 捕获与 IPC 透传

**Files:**
- Modify: `scraper-worker/scrapers/base.py`
- Modify: `scraper-worker/scrapers/models.py`
- Modify: `scraper-worker/scrapers/openaver/*.py`
- Modify: `scraper-worker/core/smart_search.py`
- Modify: `scraper-worker/ipc_handler.py`
- Modify: `scraper-worker/scraper-worker.spec`
- Test: `scraper-worker/tests/test_smart_search.py`
- Test: `scraper-worker/tests/test_ipc_handler.py`
- Test: `scraper-worker/tests/test_scraper_fc2.py`

**Interfaces:**
- `BaseScraper.search(number: str, save_webpage: bool = False) -> list[Video]`
- `BaseScraper.create_session() -> requests.Session`
- `smart_search(..., save_webpage: bool = False)`
- `search_candidates(..., save_webpage: bool = False)`
- `search_multi(..., save_webpage: bool = False)`
- IPC 参数名：`save_webpage`
- `Video.to_dict()` 仅在 MHTML 非空时加入 `webpage`

- [x] 编写失败测试：开启开关后候选带 `webpage`，关闭不带；FC2 主页面与 iframe 均进入 MHTML；自动搜索只归档最终命中站点；`extract_webpage_images` RPC 可提取图片。
- [x] 运行上述测试，确认失败原因是接口缺失。
- [x] 在 `BaseScraper.search()` 中临时包装现有会话的 `get` 方法捕获响应，成功后按 `detail_url` 定位根页面，避免修改全部站点实现。
- [x] 透传 smart search 与 IPC 参数，新增 `extract_webpage_images` 方法，并把新模块加入 PyInstaller hiddenimports。
- [x] 运行 Python 相关测试，随后运行全部 `pytest tests/`。

### Task 3: Kotlin 设置与模型

**Files:**
- Modify: `app/src/main/kotlin/javscraper/settings/SettingsManager.kt`
- Modify: `app/src/main/kotlin/javscraper/SettingsController.kt`
- Modify: `app/src/main/kotlin/javscraper/AppViewModel.kt`
- Modify: `app/src/main/kotlin/javscraper/WorkerController.kt`
- Modify: `app/src/main/kotlin/javscraper/models/Models.kt`
- Modify: `app/src/main/kotlin/javscraper/ui/screens/settings/SettingsScreen.kt`
- Modify: `app/src/main/kotlin/javscraper/ui/screens/settings/ScrapingSettingsTab.kt`
- Modify: `app/src/main/kotlin/javscraper/ui/screens/settings/SettingsPreview.kt`
- Modify: `app/src/main/kotlin/javscraper/i18n/TranslationEn.kt`
- Modify: `app/src/main/kotlin/javscraper/i18n/TranslationZh.kt`
- Test: `app/src/test/kotlin/javscraper/settings/SettingsManagerTest.kt`
- Test: `app/src/test/kotlin/javscraper/models/ModelsTest.kt`
- Test: `app/src/test/kotlin/javscraper/i18n/TranslationsTest.kt`

**Interfaces:**
- `AppSettings.downloadWebPages: Boolean = false`
- `SettingsController.downloadWebPages` / `updateDownloadWebPages(Boolean)`
- `SettingsState.downloadWebPages` / `SettingsActions.onDownloadWebPagesChange`
- `Video.webpage: String = ""`
- `ScrapeOrchestrator.downloadWebPages: Boolean`
- 文案属性：`settingsDownloadWebPages`

- [x] 编写失败测试：默认关闭、JSON round-trip、`Video.webpage` 反序列化、双语言文案完整。
- [x] 运行指定 Kotlin 测试并确认失败。
- [x] 实现设置持久化、控制器状态、UI 开关、双语文案、模型字段和编排器参数。
- [x] 运行相关 Kotlin 测试并确认通过。

### Task 4: Kotlin 写盘与图片提取

**Files:**
- Create: `app/src/main/kotlin/javscraper/models/WebpageArchiver.kt`
- Create: `app/src/main/kotlin/javscraper/scrape/SidecarWebpageArchiver.kt`
- Modify: `app/src/main/kotlin/javscraper/scrape/ScrapeOrchestrator.kt`
- Modify: `app/src/main/kotlin/javscraper/sidecar/SidecarManager.kt`
- Test: `app/src/test/kotlin/javscraper/scrape/ScrapeOrchestratorTest.kt`

**Interfaces:**
- `interface WebpageArchiver { suspend fun extractImages(mhtmlPath: Path, outputDir: Path, video: Video): WebpageImageResult }`
- `data class WebpageImageResult(val success: Boolean, val message: String = "")`
- `SidecarManager.extractWebpageImages(...)`
- MHTML 输出函数：`ScrapeOrchestrator.writeWebpage(folder: Path, video: Video): Path?`

- [x] 编写失败测试：开启开关时写入 `FC2-PPV-1723984-FC2.mhtml` 并调用归档器；关闭时不写；MHTML 写入或图片提取失败会聚合到 `writeToDisk` 错误。
- [x] 运行 `.\gradlew :app:test --tests "javscraper.scrape.ScrapeOrchestratorTest" --no-daemon`，确认失败。
- [x] 实现安全文件名、Base64 解码写盘、Sidecar RPC 与可测试归档接口；开启 MHTML 时不再调用 `ImageSaver.download`。
- [x] 重跑指定测试和全部 Kotlin 测试。

### Task 5: 集成验证与部署

**Files:**
- Modify: `docs/single-file-scrape-flow.md`
- Build output: `scraper-worker/dist/scraper-worker.exe`
- Deploy target: `app/worker/scraper-worker.exe`

- [x] 更新单文件链路文档，说明 MHTML 生成、预览前不落盘、确认后写盘和图片提取。
- [x] 运行 `scraper-worker\venv\Scripts\python -m pytest tests/`。
- [x] 运行 `.\gradlew :app:test --no-daemon`。
- [x] 运行 `scraper-worker\venv\Scripts\python -m PyInstaller scraper-worker.spec --noconfirm`。
- [x] 将 `scraper-worker\dist\scraper-worker.exe` 复制为 `app\worker\scraper-worker.exe`，并校验文件存在且时间戳更新。
