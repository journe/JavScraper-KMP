# 图片命名规则反转：封面→fanart / 海报→poster 实施与验证报告

> 日期：2026-09-20　|　分支：develop　|　含更新模式 fanart/poster 分离修复

## 规则（用户确认）

| 站点字段 | 含义 | 落盘文件 |
|---|---|---|
| `cover_url` | 封面（横版大图） | `fanart.jpg` |
| `poster_url` | 海报（竖版） | `poster.jpg` |
| 两者不同 | — | 分别下载，各存一份 |
| 两者相同 / poster 为空 | — | 只下载一次存为 `fanart.jpg`，再复制一份命名为 `poster.jpg` |

> 旧规则（已废弃）：cover→poster.jpg + 复制为 fanart.jpg，poster_url 覆盖 fanart。方向完全相反。

## 代码改动

### Kotlin 直连链路 — `app/src/main/kotlin/javscraper/io/ImageSaver.kt`
- `download()` 参数重命名 `fanartUrl` → `posterUrl`，语义与 `Video.posterUrl` 对齐。
- `coverUrl → fanart.jpg`（下载）；`posterUrl` 与 cover 不同且非空时独立下载为 `poster.jpg`；否则 `fanart.jpg` 复制为 `poster.jpg`（3 次重试，间隔 300ms，应对媒体服务器文件锁）。
- fanart 与 poster 各自独立协程、独立错误处理，互不拖垮。

### Python MHTML 链路 — `scraper-worker/core/webpage_archive.py` `extract_images()`
- `("fanart", cover_url, fanart.jpg)`、poster 独立时追加 `("poster", poster_url, poster.jpg)`。
- poster 非独立时，fanart 写盘后复制同一内容为 `poster.jpg`。
- 错误消息同步反转（`poster: not found` 表示海报缺失）。

### 调用方适配 — `ScrapeOrchestrator.writeCoverArt()`
- `ImageSaver.download(folder, video.coverUrl, video.posterUrl)` 命名参数调用，语义不变、自动匹配新参数名。

## 更新模式 fanart/poster 分离修复（后续补丁）

旧逻辑 `UpdateModeWriter.hasReusablePoster` 只检查 `poster.jpg` 存在就跳过整组封面下载，与命名反转后的语义不再对齐（poster 是 fanart 的副本）。已改为两个独立标记：

| 旧目录状态 | 旧行为 | 新行为 |
|---|---|---|
| fanart + poster 都在 | 全部复用 | 全部复用，不联网 |
| 只有 fanart | **错误地跳过下载**（poster 缺失被忽视） | 本地复制 fanart → poster 补副本，不联网 |
| 只有 poster（用户编辑版） | 跳过一切下载，fanart 永远缺失 | 只重新下载 fanart；poster 不被覆盖 |
| 两者都缺 | 下载 | 完整下载（fanart + poster） |

### 改动文件
- `UpdateModeWriter`：`posterReusable` → `fanartReusable` + `posterPresent` 两个标记。
- `ScrapeOrchestrator.writeCoverArt`：新增 `writePoster` 参数；更新模式下按上表分支处理。
- `ImageSaver.download`：新增 `copyPosterFromFanart` 参数，为 false 时禁止从 fanart 复制生成 poster。
- `WebpageArchiver` / `SidecarWebpageArchiver` / `SidecarManager.extractWebpageImages`：透传 `writePoster`。
- Python `extract_images()` 与 IPC `_extract_webpage_images`：新增 `write_poster` 参数（JSON-RPC `extract_webpage_images` 增加 `write_poster` 字段），为 False 时不写任何 poster.jpg。

## 测试

| 测试 | 结果 |
|---|---|
| Kotlin `ImageSaverTest`（2 例：cover 复制为 poster / poster 独立下载） | ✅ |
| Kotlin `javscraper.io.*` + `javscraper.scrape.*`（含更新模式 2 个新用例：fanart 在补 poster 副本 / poster 编辑版不被覆盖且重下 fanart） | ✅ 159 例全过 |
| 存量用例 `update mode preserves edited poster without fanart` 断言已按新语义更新（0 请求 → 1 请求下载 fanart） | ✅ |
| Python `test_webpage_archive.py`（含新增 `write_poster=False` 用例） | ✅ 7 例全过 |
| Python `test_ipc_handler.py::test_extract_webpage_images`（补 `write_poster=True` 断言） | ✅ |
| `test_search_returns_all_candidates` | 存量失败，与本次改动无关（stash 验证过） |

## mmtv 实测：https://7mmtv.sx/zh/reducing-mosaic_content/78328/476MLA-192.html

使用 `MmtvScraper` 实际抓取（HTTP 200）：

| 字段 | 值 |
|---|---|
| number | `476MLA-192` |
| title | `[無碼破解] 【从头到尾高潮不断的超级杂鱼M●KO】…高潮不断到无法停止！！` |
| actresses | `うゆ` |
| date | `2024-09-14` |
| duration | 89 分钟 |
| maker / label | `まんまんランド` |
| tags | 仅限送货、美尻、业余、苗条、乳房、第一人称摄影、中出、美少女、角色扮演、短发、有码 |
| cover_url | `https://n1.1025cdn.sx/amateur/b/246107_476MLA-192.jpg` |
| poster_url | `""`（空，站点无独立海报） |
| sample_images | 15 张（`246107_476MLA-192_1..15.jpg`） |
| summary | 完整中文简介（约 400 字） |
| source / detail_url | `mmtv` / 原 URL |

**按新规则此片的落盘结果**（poster_url 为空 → 走复制路径）：
- `fanart.jpg` = 下载 `cover_url` 的图片内容
- `poster.jpg` = `fanart.jpg` 的字节副本（不再单独下载）
- `extrafanart/fanart1..15.jpg` = 15 张预览图（仅在开启"下载预览图"时）

## 打包与端到端验证

- `PyInstaller scraper-worker.spec --noconfirm` → `dist/scraper-worker.exe` 构建成功，已部署至 `app/worker/scraper-worker.exe`。
- 首次部署：2026-09-20 18:28（命名反转）；
- 二次部署：2026-09-20 19:00（write_poster 参数）。
- 用**打包后的 exe** 通过 JSON-RPC 实测：
  1. `search {"number":"476MLA-192","site":"mmtv"}` → 返回该候选（cover 有值、poster 为空）✅
  2. `extract_webpage_images`（cover_url 有效、poster_url 空）→ `fanart.jpg` 与 `poster.jpg` 内容一致且均等于封面资源 ✅ **RULE CHECK: PASS**

## 遗留说明

- NFO 中的 `<poster>/<thumb>`（取 posterUrl.ifBlank{coverUrl}）与 `<cover>` 字段语义未动；旧刮削结果 NFO 反读时 `posterUrl` 可能回落到 cover 值，与新落盘规则不冲突（poster.jpg 本就是封面副本）。
- ~~更新模式 `hasReusablePoster` 只查 poster.jpg~~ 已于本次修复（见上方「更新模式 fanart/poster 分离修复」）。
- `test_search_returns_all_candidates` 为存量失败（git stash 验证与本次改动无关），建议另行排查。