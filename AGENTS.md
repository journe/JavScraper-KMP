# Repository Guidelines

## 项目结构与模块组织

本仓库包含两个主要组件：

- **`app/`** — Compose Desktop（Kotlin/JVM）桌面应用，使用 Gradle 构建。源码位于 `app/src/main/kotlin/javscraper/`，按包组织：
  - `i18n/` — 类型安全的多语言字符串（数据类 + CompositionLocal）
  - `io/` — 文件读写与持久化
  - `models/` — 数据类与领域模型
  - `scrape/` — 刮削编排（ScrapeOrchestrator.kt）
  - `settings/` — 应用设置管理（SettingsManager、AppSettings）
  - `sidecar/` — Python worker 进程生命周期管理
  - `ui/` — Compose UI 屏幕与组件（`ui/screens/settings/` 为设置页子包）
  - 根包下另有 `SettingsController.kt`（设置状态与持久化）、`WorkerController.kt`（worker 生命周期与站点检查）、`AppViewModel.kt`（聚合门面）
- **`scraper-worker/`** — Python JSON-RPC worker（Python 3.12+），刮削器按有码/无码/混合位于 `scrapers/openaver/` 子包，核心工具位于 `core/`，测试位于 `tests/`；站点连通性检查位于 `scrapers/site_check.py`
- **`installer/`** — WiX 工具集文件（bundle.wxs、build.bat），用于 Windows 打包
- **`docs/`** — 设计规格与实施计划（`docs/superpowers/`）及操作文档（如 `docs/worker-build-guide.md` 打包指南）

测试源码与源码目录结构镜像：`app/src/test/kotlin/javscraper/` 与 `scraper-worker/tests/`。

## 构建、测试与开发命令

Gradle wrapper 统一位于**项目根目录**（不在 app 下），请从根目录执行。

### Kotlin（Compose Desktop）

```powershell
# 编译（不运行测试）
.\gradlew :app:compileKotlin --no-daemon

# 运行全部单元测试
.\gradlew :app:test --no-daemon

# 运行桌面应用
.\gradlew :app:run --no-daemon
```

### Python Worker

```powershell
# 安装依赖（推荐使用 scraper-worker/venv）
cd scraper-worker; venv\Scripts\python -m pip install -r requirements.txt

# 运行全部测试
cd scraper-worker; venv\Scripts\python -m pytest tests/

# 运行指定测试文件
cd scraper-worker; venv\Scripts\python -m pytest tests/test_registry.py

# 打包 worker（修改 Python 源码后必须重新打包，详见 docs/worker-build-guide.md）
cd scraper-worker; venv\Scripts\python -m PyInstaller scraper-worker.spec --noconfirm
```
> 打包完成并生成新的 ``dist/scraper-worker.exe`` 后，必须由 Codex 自动将该文件部署（复制）到应用运行目录：默认目标为 ``app/worker/scraper-worker.exe``。
## 编码风格与命名约定

### Kotlin

- 全文件使用 4 空格缩进。
- 遵循 Kotlin 编码规范。
- 包名小写：javscraper.models、javscraper.ui。
- 类名使用 PascalCase；函数与属性使用 camelCase。
- 优先 val 而非 var；模型使用不可变 data class。
- Composable 函数使用 PascalCase（如 SettingsScreen）。

### Python

- 遵循 PEP 8，4 空格缩进。
- 模块与包使用 snake_case。
- 类名使用 PascalCase；函数与变量使用 snake_case。

## 测试指南

代码改动后默认只运行与本次修改直接相关的增量测试；全量测试仅在用户明确要求时执行。

### Kotlin 测试

- 使用 kotlin.test 框架 + JUnit 运行器。
- 测试文件命名为 ClassTest.kt，放在 app/src/test/kotlin/ 对应包下。
- 测试函数使用描述性名称。

### Python 测试

- 使用 pytest 与普通 assert。
- 测试文件命名为 test_module.py，位于 scraper-worker/tests/。
- 在 scraper-worker/ 目录下运行 pytest tests/。

## 提交与 PR 指南

- 使用 Conventional Commits 前缀（feat:、fix:、refactor:、docs:、build:）。
- 提交信息使用中文，标题一行 + 空行 + 以 "- " 开头的要点正文（参照历史提交格式）。
- PR 应包含变更描述并关联相关 issue 或设计文档。

## 安全与配置提示

- 绝不提交用户特定配置（已通过 .gitignore 忽略）。
- Python worker 通过 stdin/stdout 与 Kotlin 应用进行 JSON-RPC 通信。
- 新增或迁移刮削站点时，遵循完整流程（详见 docs/scraper-development-guide.md）：在 scrapers/openaver/<censored|uncensored|mixed>/ 新建文件、文件末尾注册 ScraperRegistry、在 ipc_handler.py 末尾导入、更新 smart_search.py 优先级链、更新 scraper-worker.spec 的 hiddenimports 与 AppSettings 默认站点列表，并编写测试。
- 修改 Python 源码后必须重新打包 worker exe（详见 docs/worker-build-guide.md）。

## 单文件刮削

- 修改单文件刮削链路前，先阅读 `docs/single-file-scrape-flow.md`。
- 保持核心顺序：元数据抓取 → 结果预览 → 用户确认 → 磁盘写入；进入预览确认前不得创建目录、写 NFO、下载图片或复制/链接视频。
- 站点选择必须遵守设置中的 `enabledSites`；显式选择和自动搜索都不能访问未启用站点。
- 输出目录为空时不启动任务；抓取或写入失败时保留输入态并显示错误。
- 修改番号时同步任务展示与回填；任务回填按同源文件替换，结果回填按番号替换。
- `writeToDisk` 必须聚合目录创建、NFO、图片和文件复制/链接错误；任一关键 IO 失败时返回失败。
- 涉及状态机、站点过滤、IO 结果或回填逻辑的修改必须补充对应 Kotlin/Python 测试；修改 Python worker 后重新打包并部署 exe。

## 国际化（i18n）

翻译定义为 app/src/main/kotlin/javscraper/i18n/ 下的 Kotlin 类：

- TranslationEn.kt：基类（open class），包含所有 UI 字符串的 open val 属性与 open fun 方法（用于带参数字符串），默认值为英文。
- TranslationZh.kt：中文（简体）本地化，覆盖全部属性。
- Translations.kt：暴露 LocalTranslations（staticCompositionLocalOf）供 Compose 响应式访问。

### 在 Composable 中使用

val t = LocalTranslations.current
Text(t.scanTitle)
Text(t.scanFound(matchedFiles.size))

### 新增语言

1. 新建继承 TranslationEn 的类。
2. 覆盖所有 open val / open fun 成员为翻译文本。
3. 在 App.kt 的 localeStrings 解析中加入新语言。

### 新增 UI 字符串

1. 在 TranslationEn 中添加 open val（带参数用 open fun）。
2. 在所有语言子类中覆盖。
3. 在 Composable 中通过 LocalTranslations.current.xxx 引用。
