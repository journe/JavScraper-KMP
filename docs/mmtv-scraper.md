# 7mmtv 刮削器说明

## 文档范围

本文梳理 `scraper-worker/scrapers/openaver/mmtv.py` 的职责、多候选搜索流程、字段解析规则、注册调用链、测试覆盖与维护注意事项。该实现是从 MDCx 项目迁移而来的同步版本，当前属于 `scraper-worker` 的 `openaver` 站点适配层。

## 模块定位

`MmtvScraper` 继承 `BaseScraper`，负责将 7mmtv 页面解析为统一的 `Video` 模型：

| 成员 | 值 | 说明 |
|---|---|---|
| `site_id` | `mmtv` | 注册 ID，也是应用设置和 IPC 调用使用的站点标识 |
| `site_name` | `7mmtv` | 展示名称 |
| `BASE_URL` | `https://www.7mmtv.sx` | 站点根地址 |
| `search()` 返回值 | `list[Video]` | 全部可解析的匹配候选 |

模块末尾执行 `ScraperRegistry.register(MmtvScraper)`，因此在 `ipc_handler.py` 导入该模块时自动注册。打包配置 `scraper-worker.spec` 已包含该模块。

## 多候选搜索流程

1. `search(number)` 先调用 `normalize_number(number)` 规范输入，例如 `abc123` / `ABC123` 会变为 `ABC-123`。
2. `_find_detail_urls(number)` 请求中文搜索页：
   - 普通番号直接作为关键词；
   - `FC2-*` 番号提取连续数字作为关键词，例如 `FC2-424646` 搜索 `424646`。
3. `_match_detail_urls(html, number)` 遍历搜索结果，收集全部匹配链接并去重。
4. 对每个详情链接依次请求：
   - HTTP 状态码非 `200` 时跳过该候选；
   - 连接错误或超时时跳过该候选，继续尝试后续候选；
   - `_parse()` 返回 `None`（例如标题为空）时跳过该候选；
   - 解析成功的 `Video` 追加到结果列表。
5. 返回全部成功解析的候选；没有任何候选时返回空列表。

因此，7mmtv 不再只取页面中的第一个匹配项；若第一个详情页失败，也会继续尝试第二个及后续匹配项。

## 搜索结果匹配规则

搜索页解析依赖 `figure.video-preview` 下的链接和图片 `alt` 标题。每个候选链接必须同时具备 `href` 和 `img/@alt`，否则跳过。

### FC2 番号

输入会转换为站点标题格式：

```text
FC2-424646 -> FC2-PPV 424646
```

候选标题转大写后以该前缀开头即匹配。

### 普通番号

取图片 `alt` 的第一个空格分隔字段作为站点番号。满足以下任一条件即匹配：

- 站点番号以输入番号开头；
- 站点番号以输入番号结尾，且移除输入番号后的剩余部分全为数字。

## 详情页字段解析

| 输出字段 | 解析来源与规则 |
|---|---|
| `number` | 取 `div.d-flex.mb-4` 第 1 个 `span`；将 `FC2-PPV 424646` 归一为 `FC2-424646` |
| `title` | 取 `h1.fullvideo-title.h5.mb-2` 文本；删除站点番号并去除空白；为空时该候选被跳过 |
| `actresses` | 取 `div.fullvideo-idol` 下 `span/a`；删除全角括号补充说明并取第一个空格分隔字段 |
| `date` | 从第 2 个基础信息 `span` 提取 `YYYY-MM-DD` |
| `duration` | 从第 3 个基础信息 `span` 解析分钟数；支持 `HH:MM[:SS]`、`60分`、`60min` |
| `maker` | 信息区中链接包含 `makersr` 的文本；`N/A`、`----` 归为空 |
| `label` | 信息区中链接包含 `issuer` 的文本；占位值归为空 |
| `director` | 信息区中链接包含 `director` 的文本；占位值归为空 |
| `tags` | `div.d-flex.flex-wrap.categories` 下 `a` 文本 |
| `cover_url` | 正则提取 `div.player-cover` 内 `img@src`；相对地址拼接 `BASE_URL` |
| `sample_images` | 收集 `span/img.lazyload` 的 `data-src`，并追加详情区 JavaScript 中的 JPEG 链接 |
| `summary` | 取 `div.video-introduction-images-text` 下最后一个 `p` 文本 |
| `source` | 固定 `mmtv` |
| `detail_url` | 详情响应 `resp.url`，为空时回退搜索链接 |

马赛克标签先读取面包屑：`無碼AV` / `國產影片` 追加 `无码`，`有碼AV` / `素人AV` 追加 `有码`；面包屑异常时按番号兜底。该标签追加在站点分类标签之后。

## 注册与调用链

- **IPC `search`**：`ipc_handler.py` 将 `search(number, sites)` 路由到候选搜索，返回所有成功候选的字典数组；指定 `sites` 时按该列表搜索，未指定时走智能搜索启用链。
- **智能搜索**：`smart_search()` 取候选列表第一项，保持批量刮削单结果契约；`search_candidates()` 返回全部候选。
- **桌面单文件刮削**：Kotlin 调用 IPC `search`，预览弹窗展示候选列表，用户选择后才会写盘。
- **站点过滤**：应用会传入设置中的 `enabledSites`，未启用站点不会被访问。

`mmtv` 在智能搜索有码链路和无码链路均位于末位。

## 测试覆盖

`scraper-worker/tests/test_scraper_mmtv.py` 覆盖：

- 自动注册与站点属性；
- 单候选完整字段解析；
- 多候选全部返回；
- 失败详情候选跳过；
- 无结果、连接错误、超时、标题为空返回空列表；
- FC2 标题匹配、番号归一化和无码标签；
- 番号规范化。

运行测试：

```powershell
cd F:\codeprojects\JavScraper\scraper-worker
venv\Scripts\python -m pytest tests/test_scraper_mmtv.py
```

## 维护注意事项

- 实现强依赖 7mmtv 当前页面结构；站点改版时优先核对 XPath、图片 `alt`、面包屑和时长格式。
- 搜索页请求发生连接错误或超时时整体返回空列表；详情页单候选网络失败不会阻断后续候选。
- 封面使用正则解析原始 HTML，其余字段主要使用 XPath，修改模板时需同时关注两种方式。
- 修改本 Python 文件后，必须按 `docs/worker-build-guide.md` 重新打包并部署 `scraper-worker.exe`。