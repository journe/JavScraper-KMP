# 更新模式字段级更新实施计划

**Goal:** 在单文件更新模式中支持选择部分刮削字段，与旧 NFO 信息合并后再写盘。

**Architecture:** 字段定义、旧元数据和选择状态放在模型层；字段合并为纯 Kotlin 逻辑；`NfoUpdater` 使用字段掩码定点补丁；单刮削控制器新增字段选择与合并确认两个状态；Compose 只展示状态并转发动作。

**Tech Stack:** Kotlin/JVM、Compose Desktop、JUnit/kotlin.test、Gradle、现有 NFO XML 定点补丁器。

## 全局约束

- 对话、文档、提交信息使用简体中文。
- 不修改 Python worker，不重新打包 `scraper-worker.exe`。
- 图片、封面、海报、预览图、MHTML 不进入字段选择。
- 列表字段勾选时使用旧值加新值去重合并。
- 不拆分 `NfoUpdater.kt`。
- 保持预览确认前不创建目录、不移动文件、不写 NFO、不下载资源。
- Kotlin 文件尽量不超过 400 行，每层文件夹尽量不超过 8 个文件。

## Task 1: 字段模型与纯合并逻辑

**Files:**
- Create: `app/src/main/kotlin/javscraper/models/VideoUpdateModels.kt`
- Create: `app/src/main/kotlin/javscraper/scrape/update/VideoFieldMerger.kt`
- Test: `app/src/test/kotlin/javscraper/scrape/update/VideoFieldMergerTest.kt`

**Interfaces:**
- Produces: `VideoUpdateField`、`ExistingVideoMetadata`、`VideoFieldUpdateChoice`、`VideoFieldUpdateChoiceStatus`。
- Produces: `VideoFieldUpdatePlanner.choices(existing, incoming): List<VideoFieldUpdateChoice>`。
- Produces: `VideoFieldMerger.merge(existing, incoming, selectedFields): Video`。

- [ ] 先写测试覆盖：旧无新有强制新增；旧有字段勾选覆盖；未勾选保留；空新值不覆盖；演员、标签、排行、评论去重合并。
- [ ] 运行单测确认因类型不存在而失败。
- [ ] 实现模型和纯合并器。
- [ ] 重新运行单测确认通过。

## Task 2: 旧 NFO 只读定位与字段存在性

**Files:**
- Create: `app/src/main/kotlin/javscraper/io/NfoFieldPresenceReader.kt`
- Create: `app/src/main/kotlin/javscraper/scrape/update/UpdateModeExistingMetadataReader.kt`
- Modify: `app/src/main/kotlin/javscraper/scrape/UpdateModeWriter.kt`
- Test: `app/src/test/kotlin/javscraper/scrape/update/UpdateModeExistingMetadataReaderTest.kt`

**Interfaces:**
- Produces: `NfoFieldPresenceReader.read(path): Set<VideoUpdateField>`。
- Produces: reader 返回 `ExistingMetadataResult.Ready(ExistingVideoMetadata)` 或 `ExistingMetadataResult.Failed(errors)`。
- 读取阶段只校验同文件夹、源文件存在、NFO 存在且 XML 可解析，不移动任何文件。

- [ ] 先写测试覆盖正常读取、字段缺失与空元素区别、多文件夹失败、NFO 缺失失败、非法 XML 失败。
- [ ] 运行单测确认失败。
- [ ] 实现存在性读取和只读 reader，并让写盘移动逻辑复用同一套源目录/NFO 定位规则。
- [ ] 重新运行单测和现有更新模式测试。

## Task 3: NfoUpdater 字段掩码

**Files:**
- Modify: `app/src/main/kotlin/javscraper/io/NfoUpdater.kt`
- Modify: `app/src/main/kotlin/javscraper/scrape/ScrapeOrchestrator.kt`
- Test: `app/src/test/kotlin/javscraper/io/NfoUpdaterFieldMaskTest.kt`
- Test: `app/src/test/kotlin/javscraper/scrape/ScrapeOrchestratorUpdateModeTest.kt`

**Interfaces:**
- `NfoUpdater.update(..., enabledFields: Set<VideoUpdateField>? = null)`。
- `ScrapeOrchestrator.writeSingleScrapeToDisk(files, video, updateFields: Set<VideoUpdateField>? = null)`。
- `null` 表示完整更新；非空集合只更新对应业务字段。

- [ ] 先写测试：只选番号和简介时其他字段不动；未选标签、演员保留；选择后合并；扩展统计字段按掩码更新；图片 XML 字段在字段更新中不改变。
- [ ] 运行测试确认失败。
- [ ] 为每类字段加掩码判断，系统字段 `mpaa/country/language` 在字段更新中跳过。
- [ ] `lockdata` 继续按设置写入；未纳入选择器。
- [ ] 通过合并后的 `Video` 更新 `javdb_extra`，保证未选统计、排行、评论仍保留旧值。
- [ ] 运行 NFO 与更新模式测试。

## Task 4: 单刮削状态机

**Files:**
- Modify: `app/src/main/kotlin/javscraper/models/Models.kt`
- Modify: `app/src/main/kotlin/javscraper/SingleScrapeController.kt`
- Modify: `app/src/main/kotlin/javscraper/scrape/ScrapeOrchestrator.kt`
- Test: `app/src/test/kotlin/javscraper/SingleScrapeControllerTest.kt`

**Interfaces:**
- New states: `SingleScrapeDialogState.FieldUpdateSelection`、`FieldUpdateConfirm`。
- New controller methods: `configureFieldUpdate()`、`toggleFieldUpdateField(field, selected)`、`confirmFieldUpdateSelection()`、`backToFieldUpdateSelection()`、`confirmFieldUpdateWrite()`、`backToPreviewFromFieldUpdate()`。
- Preview decision: `FullWrite(index)` 或 `ConfigureFields(index)`。

- [ ] 先写控制器状态流测试。
- [ ] 实现预览决策分流、旧元数据只读准备、字段勾选状态、合并确认状态和返回预览。
- [ ] 最终写入调用 `writeSingleScrapeToDisk(files, mergedVideo, selectedFields)`。
- [ ] 读取旧 NFO 失败时停留预览并显示错误，候选结果不丢失。
- [ ] 运行控制器测试。

## Task 5: Compose UI、i18n 与接线

**Files:**
- Create: `app/src/main/kotlin/javscraper/ui/screens/single/FieldUpdateTexts.kt`
- Create: `app/src/main/kotlin/javscraper/ui/screens/single/FieldUpdateSelectionDialog.kt`
- Create: `app/src/main/kotlin/javscraper/ui/screens/single/FieldUpdateConfirmDialog.kt`
- Modify: `app/src/main/kotlin/javscraper/ui/screens/SingleScrapeDialog.kt`
- Modify: `app/src/main/kotlin/javscraper/ui/screens/ScrapeProgressScreen.kt`
- Modify: `app/src/main/kotlin/javscraper/AppViewModel.kt`
- Modify: `app/src/main/kotlin/javscraper/i18n/TranslationEn.kt`
- Modify: `app/src/main/kotlin/javscraper/i18n/TranslationZh.kt`

**Interfaces:**
- 只有更新模式开启时预览页显示“字段更新”。
- 字段选择对话框支持勾选可覆盖字段，展示新增、无变化、新值为空状态。
- 合并确认对话框复用 `VideoInfoCard` 展示最终 `Video`。

- [ ] 先扩展状态和动作接线，再添加对话框。
- [ ] 为所有新增文案补齐英文基类和中文覆盖。
- [ ] 运行 Kotlin 编译。
- [ ] 运行全部本次相关测试。

## 最终验证

- `.\gradlew :app:compileKotlin --no-daemon`
- `.\gradlew :app:test --no-daemon --tests "javscraper.scrape.update.*" --tests "javscraper.io.NfoUpdater*" --tests "javscraper.SingleScrapeControllerTest" --tests "javscraper.scrape.ScrapeOrchestratorUpdateModeTest"`
- 检查 `git diff`，确认没有 Python worker、exe、无关配置变更。
## 实施结果（2026-09-19）

- 字段模型、纯合并器、旧 NFO 只读读取、NFO 字段掩码、单刮削状态机、Compose 字段选择/合并确认 UI 与中英文文案已实现。
- 旧信息缺失且新值可用的字段默认勾选且不可取消；新旧都存在的字段由用户勾选是否覆盖。
- 列表字段按已确认策略合并：演员、标签去重合并，排行榜按榜单名去重，评论优先按 ID 去重。
- 图片、封面、海报、预览图与 MHTML 不进入字段选择；网页内容仍沿用现有更新模式行为。
- `NfoUpdater.kt` 保持单文件，当前 398 行；批量刮削仍走完整更新。
- 仅修改 Kotlin 应用侧，未修改 Python worker，不需要重新打包或部署 `scraper-worker.exe`。

验证结果：

- `.\gradlew :app:compileKotlin --no-daemon`：通过。
- 相关测试（`javscraper.scrape.update.*`、`NfoUpdaterFieldMaskTest`、`NfoUpdaterTest`、`SingleScrapeControllerTest`、`ScrapeOrchestratorUpdateModeTest`）：通过。
- `.\gradlew :app:test --no-daemon`：310 个测试中 3 个失败，均为既有 `AdaptiveVideoInfoCardTest` 布局宽度断言；本次未修改该组件或其测试。
- `git diff --check`：通过。

## 结构收敛（2026-09-19）

- 新增 `BatchScrapeController`，将批量刮削任务状态与执行流程从 `AppViewModel` 中拆出；`AppViewModel` 保持门面角色并降至 400 行。
- 新增 `VideoCollectionMerger`，统一排行/评论去重合并逻辑，`VideoFieldMerger` 与 `JavdbExtraNfo` 复用同一实现。
- 将 `DirectoryPicker.kt`、`FilePicker.kt` 移入 `javscraper.io.pickers`，`io/` 根层文件数降至 8。
- 为 `BatchScrapeController` 补充无匹配源文件、worker 缺失和清空结果测试；worker 缺失时会标记任务失败并复位运行状态。
