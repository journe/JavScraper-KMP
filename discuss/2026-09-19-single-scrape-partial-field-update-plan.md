# 更新模式单文件刮削字段级更新方案

## 1. 背景与目标

当前更新模式下，单文件刮削在候选预览后直接把新刮削结果传入写盘链路。写盘时会移动旧文件夹、复用已有图片，并通过 `NfoUpdater` 对旧 NFO 做字段级补丁。

本次需要在用户选择候选结果后，增加“字段更新”入口：用户可以只把部分新字段合并进旧信息，先查看合并结果，确认后再写盘。

已确认的产品决策：

1. 新增字段合并确认流程；真正写入后仍进入现有结果页。
2. 列表字段使用“旧值 + 新值”去重合并策略。
3. 图片、封面、海报、预览图、网页归档不进入字段选择。
4. 本功能不拆分当前 `NfoUpdater.kt`，只做必要扩展。

## 2. 交互流程

更新模式开启时，候选预览页显示两个主动作：

- “确认写入”：保持现有行为，使用整条新刮削结果更新。
- “字段更新”：进入字段级更新流程。

字段级更新流程：

1. 用户在 `PreviewDialog` 选择候选结果，点击“字段更新”。
2. 应用只读定位同组视频所在旧文件夹和旧 NFO，并解析旧信息。
3. 打开字段选择对话框：
   - 旧信息没有、新信息有的字段默认勾选并标记“新增”，不允许取消。
   - 旧信息和新信息都有且值不同的字段默认不勾选，由用户决定是否更新。
   - 新信息为空的字段禁用勾选，保留旧值。
   - 新旧值相同的字段显示“无变化”。
4. 用户点击“下一步”后进入合并确认对话框，展示合并后的完整视频信息。
5. 用户点击“确认写入”后才执行目录移动、NFO 更新、网页归档、图片处理和视频文件处理。
6. 写盘成功后进入现有 `ResultDialog`；用户点击“确定”后按现有逻辑回填任务和结果。

取消规则：

- 字段选择对话框取消，返回候选预览。
- 合并确认对话框返回，回到字段选择。
- 进入最终写盘前不移动文件、不创建目录、不写 NFO、不下载资源。

## 3. 字段范围

### 3.1 纳入字段选择

文本、数值和日期字段：

- 番号
- 标题
- 日期
- 简介
- 制作商
- 发行商
- 系列
- 导演
- 时长
- 评分
- 想看人数
- 看过人数
- 评分人数
- 来源
- 详情链接

列表字段：

- 演员
- 标签
- 排行榜
- 评论

列表字段在用户勾选时采用去重合并：

- 演员：旧演员 + 新演员，按文本去重，保留旧值顺序，新值追加在后。
- 标签：旧标签 + 新标签，按文本去重。
- 排行榜：优先使用旧排行中已有的榜名；新增榜名追加在后。可通过“榜名 + 名次”判重，避免同榜重复。
- 评论：优先使用旧评论 ID；新评论 ID 为空时按作者、日期、内容判重。

### 3.2 不纳入字段选择

以下内容不出现在字段选择器：

- 封面 URL、海报 URL、预览图 URL
- 本地 `poster.jpg`、`fanart.jpg`、`extrafanart`
- MHTML 网页归档
- `mpaa`、`country`、`language`、`lockdata` 等系统生成字段
- 应用不解析、不展示的自定义 NFO 节点

字段更新时，图片和网页仍完全沿用当前更新模式规则：已有本地图片继续复用，缺失图片是否下载由现有设置决定。自定义 NFO 节点继续原样保留。

## 4. 字段映射

用户选择的是业务字段，写盘时映射为一组 NFO 元素：

| 业务字段 | NFO 元素 |
| --- | --- |
| 番号 | `num`、`originaltitle`、`sorttitle`、`uniqueid` |
| 标题 | `title` |
| 日期 | `year`、`premiered`、`release` |
| 简介 | `plot`、`outline` |
| 制作商 | `studio`、`maker` |
| 发行商 | `label` |
| 系列 | `set` |
| 导演 | `director` |
| 时长 | `runtime` |
| 评分 | `rating` |
| 想看人数 | `javdb_extra/want_count` |
| 看过人数 | `javdb_extra/watched_count` |
| 评分人数 | `javdb_extra/rating_count` |
| 排行榜 | `javdb_extra/ranking` |
| 评论 | `javdb_extra/review` |
| 标签 | `genre`、`tag` |
| 演员 | `actor` |
| 来源 | `source` |
| 详情链接 | `website` |

字段选择必须以“旧 NFO 中是否实际存在元素”为准，不能只以解析后的空字符串判断。例如旧 NFO 中没有 `<plot/>` 才属于“旧信息没有”；`<plot></plot>` 仍属于旧信息已有字段。

## 5. 领域模型与合并规则

新增字段更新领域模型，建议放在 `scrape/update/` 子包：

- `VideoUpdateField`：枚举业务字段，提供显示 key、读取新值、读取旧值、写入合并值等能力。
- `ExistingVideoMetadata`：旧 NFO 解析出的 `Video` 与字段存在性集合。
- `VideoFieldUpdateChoice`：字段选择状态，包含字段、是否强制新增、是否选中、是否禁用、变更状态。
- `VideoFieldMerger`：纯 Kotlin 合并器，输入旧元数据、新视频和选择集合，输出合并后的视频。

合并原则：

1. 旧 NFO 没有且新值非空的字段，强制加入合并结果。
2. 旧 NFO 已有且用户勾选的标量字段，用新值覆盖旧值。
3. 旧 NFO 已有且用户勾选的列表字段，使用旧值与新值去重合并。
4. 用户未勾选的字段保留旧值。
5. 新值为空时不覆盖旧值。
6. 未由 `NfoReader` 建模的自定义 XML 节点不在合并器处理，由定点 NFO 补丁保留。

合并器保持无 Compose 依赖，方便用单元测试覆盖所有边界。

## 6. NFO 读取与存在性

新增只读读取能力，用于字段选择前的准备：

- 校验同组源视频必须位于同一个文件夹。
- 校验源视频文件存在。
- 使用与写盘一致的 NFO 定位规则查找旧 NFO。
- 读取旧 NFO 的字段值和字段存在性。

该阶段不复用“移动后”的路径，也不提前移动文件夹。建议在 `UpdateModeWriter` 中拆出只读的定位结果，或新增只读准备方法；现有写盘 `prepare` 继续负责移动并返回写盘路径。

读取失败时返回明确错误并停留在候选预览页，候选结果不丢失。

## 7. 状态机调整

当前预览等待值为 `Int?`，只能表达候选索引或取消。为避免布尔参数和空值含义不清，建议改成明确决策：

- `Cancel`
- `FullWrite(index)`
- `ConfigureFields(index)`

新增对话框状态：

- `FieldUpdateSelection(old, new, choices, selectedIndex)`
- `FieldUpdateConfirm(old, new, merged, selectedFields, selectedIndex)`

状态流转：

```text
Preview
  -> FieldUpdateSelection -> FieldUpdateConfirm -> Scraping -> Result
  |        |                    |
  |        -> Preview           -> FieldUpdateSelection
  -> Scraping -> Result
```

字段选择状态需要保留候选索引。用户返回预览并切换候选结果后，再次点击字段更新时重新读取旧 NFO 并生成选择项。

## 8. NFO 更新扩展

`NfoUpdater` 增加字段掩码入参，例如 `enabledFields: Set<VideoUpdateField>`，默认值为全部字段，以保持普通刮削和详情页编辑的现有行为。

写字段时遵循：

- 掩码包含该字段才写入、删除或插入对应元素。
- 掩码不包含该字段时跳过该字段对应的所有 XML 元素。
- `insertMissingFields` 仍只对掩码内字段生效。
- 普通完整更新传入全部业务字段和系统字段，保持当前完整更新行为。
- 字段更新传入“强制新增字段 + 用户勾选字段”；`lockdata` 可作为字段更新的系统伴随字段继续写入，`mpaa`、`country`、`language` 不因字段更新而改写。
- 图片相关 XML 字段不进入业务字段掩码，字段更新时继续沿用当前更新模式逻辑，不改变现有图片行为。

标签合并沿用 `NfoTagMerger`。演员当前 `NfoUpdater` 是覆盖式写入，需要在字段更新且启用演员字段时按合并后的演员列表写入；由于合并器已经输出去重合并结果，`NfoUpdater` 不需要重复理解选择规则。

`javdb_extra` 是一个整块，字段更新时需要支持块内字段级写入：

- 三个统计字段按字段掩码独立更新。
- 排行榜、评论按合并后的完整列表写入。
- 未选择的扩展字段保留旧值。

本次不拆分 `NfoUpdater.kt`。由于文件当前约 400 行，扩展时应只增加小的私有方法或参数透传，不把 UI、合并规则或 XML 存在性扫描塞进该文件。

## 9. UI 组织

为遵守动态语言及 Compose 文件可维护性约束，虽然 Kotlin 指标为 400 行，也不继续把所有新 UI 堆进 `SingleScrapeDialog.kt`。建议新增：

- `ui/screens/single/FieldUpdateSelectionDialog.kt`
- `ui/screens/single/FieldUpdateConfirmDialog.kt`
- 如需要，再增加 `FieldUpdateFieldRows.kt`

字段选择行建议使用复选框加紧凑状态文本：

- 新增：默认勾选、禁用。
- 覆盖：默认不勾选。
- 相同：禁用并显示无变化。
- 新值为空：禁用并显示保留旧值。

合并确认页继续复用 `VideoInfoCard(mergedVideo)`，上方用简短摘要显示将新增和覆盖的字段数量。

所有新增文案同步加入：

- `TranslationEn.kt`
- `TranslationZh.kt`

## 10. 写盘数据

用户最终确认后，控制器调用现有：

```kotlin
writeSingleScrapeToDisk(writeFiles, mergedVideo)
```

同时传入字段更新选择结果。为避免参数继续膨胀，可以定义：

```kotlin
data class SingleScrapeWriteOptions(
    val video: Video,
    val updateFields: Set<VideoUpdateField>? = null
)
```

- `updateFields = null`：完整更新，保持现有行为。
- `updateFields != null`：字段级更新，`NfoUpdater` 只处理对应字段。

目录和文件名仍根据合并后的 `Video` 计算。例如用户勾选更新番号和标题，目标文件夹、NFO 名、视频文件名按合并后的番号与标题计算；未勾选时仍使用旧番号与旧标题。

## 11. 错误处理

- 输出目录为空：标准模式沿用现有输入页提醒。更新模式允许输出目录为空，字段准备阶段只校验同组源文件和旧 NFO。
- 源文件不在同一文件夹、源文件不存在、旧 NFO 缺失或无法解析：显示错误并停留预览页。
- 合并结果为空选择但存在强制新增字段时，仍可写入；没有任何新增或勾选字段时，“下一步”禁用。
- NFO 写入、网页保存、图片处理、视频移动或复制失败：沿用 `writeToDisk` 聚合错误，回到输入态。
- 失败前已完成的 IO 不自动回滚，保持当前语义。

## 12. 测试计划

新增或扩展以下测试：

1. `VideoFieldMergerTest`
   - 旧无新有强制新增。
   - 旧有新有，勾选标量字段后覆盖。
   - 未勾选标量字段保留旧值。
   - 新值为空不覆盖。
   - 演员、标签、排行榜、评论去重合并。

2. `UpdateModeMetadataReaderTest`
   - 能读取字段值。
   - 能区分字段缺失与空元素。
   - 多个源文件必须在同一文件夹。
   - NFO 缺失和 XML 非法返回失败。

3. `NfoUpdaterTest`
   - 只更新番号和简介时，其他旧字段不变。
   - 未选择标签时保留旧 `genre`、`tag`。
   - 选择标签时执行新旧合并。
   - 未选择演员时保留旧演员。
   - 扩展统计、排行榜、评论按字段掩码更新。
   - 自定义节点继续保留。

4. `ScrapeOrchestratorUpdateModeTest`
   - 字段级更新调用后只写入选定字段。
   - 合并后的番号和标题仍能驱动文件夹整理。
   - 图片复用策略不受字段选择影响。
   - 批量 `writeToDisk` 行为不变。

5. `SingleScrapeControllerTest`
   - 完整写入与字段更新决策分流。
   - 字段选择取消返回预览。
   - 合并确认返回字段选择。
   - 最终写入使用合并后的视频和字段掩码。

## 13. 实施边界

本功能只修改 Kotlin 应用侧，不修改 Python worker，不需要重新打包或部署 `scraper-worker.exe`。

不包含：

- 在字段选择器中选择是否更新图片。
- 在字段选择器中选择 MHTML 网页归档。
- 对单个标签、演员、评论做逐项选择。
- 写盘失败后的自动事务回滚。
- 批量刮削字段级更新。