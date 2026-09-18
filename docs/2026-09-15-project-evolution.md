# 2026-09-15 以来 JavScraper 功能演进

## 文档范围

本文梳理 2026-09-15 至 2026-09-19 期间已经提交的 7 个功能变更，覆盖站点配置、扫描写盘、更新模式、详情页编辑和封面水印链路。内容以提交说明和相关设计文档为准，不包含当前工作区尚未提交的修改。

## 提交总览

| 日期 | 提交 | 主题 | 结果 |
| --- | --- | --- | --- |
| 2026-09-15 | `17c756c` | 支持站点分类并默认移动原视频 | 站点按有码、无码、混合分组展示；写盘默认剪切源视频 |
| 2026-09-15 | `2099127` | 支持刮削器镜像网址配置 | 每个站点可配置独立镜像，并作用于刮削、搜索和连通性检查 |
| 2026-09-16 | `1aafc62` | 完善多文件刮削与版本标识 | 同番号多文件统一处理，支持 FC2 变体与 `-C` / `-U` 属性后缀 |
| 2026-09-18 | `3fbbcdb` | 支持单文件更新模式 | 在扫描目录内整理旧文件夹，复用图片并字段级补丁 NFO |
| 2026-09-18 | `15f4c3f` | 支持详情页编辑 NFO 元数据 | 新增详情页编辑弹窗，保存后定向更新并回读 NFO |
| 2026-09-18 | `6f964d8` | 优化详情页查看器与元数据编辑 | 拆分查看器状态，评分改为滑杆，编辑弹窗可直接进入封面裁剪 |
| 2026-09-18 | `eecf5f0` | 裁剪封面支持添加水印 | 裁剪预览与 `poster.jpg` 输出均支持水印，布局与大小规则对齐 mdcx |

## 站点分类与镜像配置

- `ScraperRegistry.list_sites()` 返回站点分类，Kotlin `SiteInfo` 保存分类信息，设置页和站点选择器按有码、无码、混合站点分组展示。
- `AppSettings.siteMirrorUrls` 保存每个站点的独立镜像地址；`SidecarManager` 在刮削、搜索和站点检查 RPC 中传递该映射。
- Python `BaseScraper` 可通过 `MIRROR_URLS` 声明候选镜像。镜像仅在当前请求实例上覆盖 `BASE_URL`，不修改类的默认值。
- 内置 JavBus 镜像候选已经接入设置页弹窗；镜像配置异常或为空时回退默认站点地址。

## 扫描、多文件与写盘规则

- `FileScanner` 会将常见 FC2 写法归一到同一番号，例如 `FC2-4694056.mp4` 与 `FC2-PPV 4694056-2.mp4` 属于同一组。
- 同番号多个源文件会统一搜索并生成同一套元数据。输出视频使用纯番号作为基础名，再按 Jellyfin 多分片或多版本规则命名：无后缀 / 数字后缀生成 `part1`、`part2`，原有 `-cd1` 保留，`1080p`、`4K` 等识别为版本标签。
- `-C` / `-c` 表示中文字幕，`-U` / `-u` 表示无码流出；这些后缀不参与番号识别，会保留在输出文件名末尾并写入 `Video.version`，规范化为 `C`、`U` 或 `CU`。
- 标准写盘默认移动源视频；关闭“移动原视频”后改为复制并保留源文件。旧硬链接配置不再等价于禁用移动。
- 单文件刮削仍保持“元数据抓取 → 结果预览 → 用户确认 → 磁盘写入”的顺序；确认前不创建目录、不写 NFO、不下载图片、不移动或复制视频。

## 单文件更新模式

- 设置页“刮削”标签新增更新模式开关，配置持久化到 `AppSettings`；该模式只影响单文件刮削写盘路径，批量刮削继续使用标准模式。
- 更新模式要求同番号分组的源视频位于同一旧文件夹，并能定位旧 NFO；否则返回聚合错误，不改写 NFO。
- 目标文件夹按扫描目录和现有目录层级模板计算。目标路径变化时移动旧文件夹内全部条目并删除空目录；目标路径不变时不重复移动。
- 旧文件夹同时存在 `fanart.jpg` 与 `poster.jpg` 时复用封面；`extrafanart` 中已有 JPG 时复用预览图，只按需补齐缺失图片。
- `NfoUpdater` 对既有 NFO 做字段级补丁，只更新受管字段，保留未知字段、原格式、自定义内容和空白行结构。

## 详情页查看与 NFO 编辑

- 详情页新增 `VideoEditDialog` 与 `VideoEditForm`，复用 `VideoFields` 字段定义；保存后通过 `VideoMetadataEditor` 定向更新 NFO，再回读元数据并刷新图库状态。
- 空时长和空评分允许保存；评分输入改为滑杆，不再限制十分制。
- 查看器状态拆分到 `VideoDetailViewerState`，海报与图集转场使用隔离 key，避免共享动画状态串扰。
- 编辑弹窗可直接进入封面裁剪，裁剪完成后同步刷新海报显示。

## 封面裁剪与水印

- 水印类型为 `4K`、`8K`、字幕、有码、破解、流出、无码，模型定义在 `models/Watermark.kt`，渲染器位于 `io/image/WatermarkRenderer.kt`。
- 水印高度公式为“目标图高 × size / 40”，`size` 范围 1..10，默认 5；开关与大小持久化到应用设置。
- 布局对齐 mdcx 默认 `not_fixed` 行为：4K / 8K 固定右下角，其余水印从左上角起顺时针排布并跳过已占用角。
- 默认根据标签和文件名推断字幕、清晰度、无码、破解、流出水印；有码不自动勾选，但可手动选择。
- 裁剪预览与最终 `poster.jpg` 输出复用同一布局规则；素材读取失败时跳过该水印，不阻断裁剪保存。
- 相关媒体组件归组到 `ui/components/media/`，图片处理归组到 `io/image/`，避免原目录继续膨胀。

## 关键维护入口

| 领域 | 主要文件 |
| --- | --- |
| 站点分类与注册 | `scraper-worker/scrapers/registry.py`、`app/src/main/kotlin/javscraper/models/Models.kt` |
| 镜像配置与 RPC | `app/src/main/kotlin/javscraper/settings/SiteMirrorUrls.kt`、`app/src/main/kotlin/javscraper/sidecar/SidecarManager.kt` |
| 扫描与多文件命名 | `app/src/main/kotlin/javscraper/io/FileScanner.kt`、`app/src/main/kotlin/javscraper/scrape/ScrapeOrchestrator.kt` |
| 更新模式与 NFO 补丁 | `app/src/main/kotlin/javscraper/scrape/UpdateModeWriter.kt`、`app/src/main/kotlin/javscraper/io/NfoUpdater.kt` |
| 详情页编辑 | `app/src/main/kotlin/javscraper/ui/screens/detail/VideoEditDialog.kt`、`app/src/main/kotlin/javscraper/io/metadata/VideoMetadataEditor.kt` |
| 封面水印 | `app/src/main/kotlin/javscraper/models/Watermark.kt`、`app/src/main/kotlin/javscraper/io/image/WatermarkRenderer.kt` |

## 测试覆盖

- 站点分类、镜像配置和 RPC 参数：`test_registry.py`、`test_smart_search.py`、`SiteMirrorUrlsTest`、`SidecarWebpageRequestTest`。
- 扫描、多文件命名与版本标识：`FileScannerTest`、`ScrapeOrchestratorTest`、`ScrapeTaskOperationsTest`、`test_mmtv_fc2.py`。
- 更新模式与 NFO 处理：`NfoUpdaterTest`、`ScrapeOrchestratorUpdateModeTest`、`ScrapeOrchestratorWebpageTest`。
- 详情页编辑与查看器：`VideoEditFormTest`、`VideoMetadataEditorTest`、`VideoDetailScreenTest`、`PosterViewerTransitionTest`。
- 封面水印：`WatermarkTest`、`WatermarkRendererTest`、`PosterCropperTest`、`PosterWatermarkStateTest`。

修改上述链路时优先运行对应测试；若改动 Python worker，重新打包并部署 `scraper-worker.exe`。

