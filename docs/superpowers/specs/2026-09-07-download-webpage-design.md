# 下载网页功能设计

## 背景

用户需要在“设置 - 刮削”中新增“下载网页”开关。开启后，刮削视频文件时保存最终命中的详情页为单文件 MHTML，文件名为 `番号-站点.mhtml`。例如使用 FC2 站点刮削 `FC2-PPV-1723984.mp4` 时，应生成 `FC2-PPV-1723984-FC2.mhtml`。

现有链路由 Kotlin Compose Desktop 发起 JSON-RPC，Python worker 抓取站点详情并返回 `Video` 元数据；Kotlin 在用户确认预览或批量刮削后写 NFO、下载图片并整理视频。现有图片下载会再次访问 worker 已解析出的图片 URL，造成重复网络请求。

## 目标

1. 新增持久化设置 `downloadWebPages`，默认关闭，并在刮削设置页显示为“下载网页”。
2. 开关开启时，worker 捕获最终命中的详情页 HTML 及其图片、样式等展示资源，生成完整单文件 MHTML。
3. MHTML 与元数据一起返回 Kotlin，但保持单文件刮削“预览确认前不写盘”的约束；用户确认后才写入目标目录。
4. 图片保存改为从 MHTML 提取封面、海报和剧照，不再按原始图片 URL 发起第二次网络请求。
5. 覆盖现有全部站点，避免在每个站点刮削器中复制归档逻辑。
6. 保留原刮削行为与兼容性：开关关闭时不捕获页面，不影响已有配置解析。

## 非目标

1. 不归档搜索结果页、未命中站点页面或整站资源。
2. 不引入浏览器内核或 Playwright 等重型依赖。
3. 不改变站点选择、自动优先级、预览状态机和输出目录校验逻辑。
4. 不把视频文件、NFO 或图片保存逻辑迁移到 Python worker。

## 架构设计

### Python Worker

新增通用 MHTML 生成模块，负责：

- 记录一次刮削过程中命中的详情页响应；
- 解析详情页中的图片、样式、脚本等静态资源 URL；
- 使用同一个网络会话抓取这些资源一次；
- 按标准 MIME MHTML 结构写入根 HTML、子 HTML、图片和样式资源；
- 提供 MHTML 读取接口，按 Content-Location 提取图片字节。

`BaseScraper.search()` 增加可选 `save_webpage` 参数。各站点 `_search_one()` 仍只负责现有解析；成功返回 `Video` 后，基类捕获本次详情页响应并调用 MHTML 生成器。对于 FC2 这类通过 iframe 补充简介的站点，iframe HTML 作为子资源加入同一个 MHTML。

`smart_search`、`search_candidates`、`search_multi` 与 IPC 的 `scrape`、`search` 方法透传 `save_webpage`。返回结构在 `Video` 字典中新增可选 `webpage` 字段，内容为 base64 编码的 MHTML 字符串；开关关闭时该字段为空字符串，避免破坏旧客户端。

### Kotlin 应用

`AppSettings` 新增 `downloadWebPages`，`SettingsController` 负责状态与持久化，`WorkerController` 重建编排器时透传该设置。设置状态、操作回调和刮削设置页新增“下载网页”开关，英文与简体中文翻译同步补齐。

`Video` 模型新增可选 `webpage` 字段，用于承载 worker 返回的 MHTML base64 内容。`ScrapeOrchestrator.writeToDisk()` 在开关开启且 `webpage` 非空时：

1. 解码并写入 `番号-站点.mhtml`；
2. 若图片下载开启，调用 worker 的图片提取接口，从刚写入的 MHTML 提取封面、海报和剧照；
3. 不再调用按 URL 下载的 `ImageSaver.download()`。

批量刮削在抓取后立即进入写盘，因此 MHTML 会随本次写入保存；单文件刮削在预览确认后写入，取消预览不会留下 MHTML、NFO 或图片。

### 文件命名与失败语义

MHTML 文件与 NFO 保存到同一输出目录或影片子目录。文件名使用 `Video.number` 与 `Video.source`，转换为大写并替换 Windows 非法字符，形成 `番号-站点.mhtml`。

MHTML 写入失败计入 `writeToDisk` 聚合 IO 错误；图片提取失败与现有图片下载失败语义一致，记录警告并聚合错误。若 MHTML 缺失个别非关键资源，只要根 HTML 与可用图片存在，仍保存已捕获内容，不因装饰性资源失败中断刮削。

## 测试策略

Python 侧新增或扩展测试：

- 详情页与图片资源只请求一次，MHTML 包含根 HTML 和图片；
- FC2 主页面与简介 iframe 合并进同一个 MHTML；
- MHTML 文件名中的站点标识与番号格式正确；
- IPC `scrape` / `search` 透传开关并在返回中包含或省略 `webpage`；
- MHTML 图片提取器能按封面、海报、剧照 URL 提取字节并写入目标文件。

Kotlin 侧新增或扩展测试：

- 设置默认值、持久化与 UI 状态传递；
- 开启开关后写入 `FC2-PPV-1723984-FC2.mhtml`；
- 开关关闭时不写 MHTML；
- 图片保存不再访问原始 URL，而使用 MHTML 提取结果；
- MHTML 或图片提取失败时 `writeToDisk` 返回失败并聚合错误。

## 部署要求

修改 Python worker 后必须重新打包 `scraper-worker.exe`，并自动复制到 `app/worker/scraper-worker.exe`。Kotlin 编译与单元测试、Python 全部测试均需通过。