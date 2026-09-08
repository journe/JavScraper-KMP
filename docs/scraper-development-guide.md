# 刮削器开发指南（新增 / 从 mdcx 迁移）

本指南规范 scraper-worker 中刮削器的**新增**与**迁移**流程。以 2026-08 迁移 7mmtv（mdcx → scraper-worker）为实例，覆盖从编码、注册、测试到打包部署的全过程。

## 一、架构概览

每个刮削器是一个 `BaseScraper` 子类，按内容类型放在 `scraper-worker/scrapers/openaver/censored/`、`uncensored/` 或 `mixed/` 下，通过模块末尾的 `ScraperRegistry.register(...)` 自动注册。相关文件职责：

| 文件 | 职责 |
| --- | --- |
| `scrapers/base.py` | `BaseScraper` 抽象基类：`site_id` / `site_name` / `_search_one(number)`；公开 `search(number)` 包装为 `list[Video]` |
| `scrapers/models.py` | `Video` / `Actress` 数据模型与 `scrape_success` / `scrape_error` |
| `scrapers/registry.py` | 站点注册表：`register` / `get` / `list_sites` |
| `scrapers/openaver/censored/` | 有码站点刮削器实现 |
| `scrapers/openaver/uncensored/` | 无码站点刮削器实现 |
| `scrapers/openaver/mixed/` | 同时服务有码与无码搜索的综合性站点刮削器实现 |
| `core/smart_search.py` | 自动搜索优先级链（`CENSORED` / `UNCENSORED`） |
| `ipc_handler.py` | JSON-RPC 路由；**末尾 import 各刮削器以触发注册** |
| `scraper-worker.spec` | PyInstaller 打包配置（`hiddenimports`） |

## 二、新增刮削器标准步骤

### 1. 创建刮削器文件

新建 `scraper-worker/scrapers/openaver/<censored|uncensored|mixed>/<site>.py`，实现 `BaseScraper`；综合站点放入 `mixed/`：

```python
import requests
from typing import Optional

from scrapers.base import BaseScraper
from scrapers.models import Actress, Video
from scrapers.registry import ScraperRegistry


class XxxScraper(BaseScraper):
    BASE_URL = "https://example.com"

    @property
    def site_id(self) -> str:
        return "xxx"          # 小写短 id，与 AppSettings.enabledSites 一致

    @property
    def site_name(self) -> str:
        return "XxxSite"

    def __init__(self):
        self._session = requests.Session()
        self._session.headers.update({
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                          "Chrome/120.0.0.0 Safari/537.36",
            "Accept-Language": "zh-CN,zh;q=0.9,ja;q=0.8",
        })

    def _search_one(self, number: str) -> Optional[Video]:
        number = self.normalize_number(number)
        try:
            # ... 搜索并解析详情页；_search_one 返回 Video 或 None
            return None
        except (requests.Timeout, requests.ConnectionError):
            return None


ScraperRegistry.register(XxxScraper)
```

要求：
- 文件不超过 300 行；网络请求统一用 `requests.Session`（超时 15s），`_search_one` 失败返回 `None`；基类 `search()` 会把结果包装为空列表或单元素列表；
- 字段通过 `Video(...)` 填充，`source` 填 `site_id`，`detail_url` 填详情页地址；
- 文件末尾必须 `ScraperRegistry.register(XxxScraper)`。

### 2. 触发注册

在 `ipc_handler.py` 末尾的 import 列表中加入新模块：

```python
from scrapers.openaver.censored import xxx  # noqa: F401
```

### 3. 配置自动搜索优先级

在 `core/smart_search.py` 的 `CENSORED` / `UNCENSORED` 列表中加入新站点 id。自动搜索会在该优先级链基础上继续应用应用设置传入的启用站点列表；新增站点若要参与自动搜索，也必须加入应用设置默认启用列表。

原则：追加到**列表末尾**作为兜底，不改变现有站点优先级；无码/素人站点加 `UNCENSORED`，有码站点加 `CENSORED`（综合站点可两边都加）。

### 4. 更新 PyInstaller 配置

在 `scraper-worker.spec` 的 `hiddenimports` 中加入 `'scrapers.openaver.<category>.xxx'`。

> 注意：`hiddenimports` 里漏掉模块会导致**打包后**运行时 `Unknown: xxx` 或 ImportError，源码运行时却正常。

### 5. 加入默认站点列表

在 `app/src/main/kotlin/javscraper/settings/SettingsManager.kt` 的 `AppSettings.enabledSites` 默认列表末尾追加新站点 id，并同步更新 `app/src/test/kotlin/javscraper/settings/SettingsManagerTest.kt` 的默认数量断言。

### 6. 编写单元测试

新建 `scraper-worker/tests/test_scraper_<site>.py`，参考 `tests/test_scraper_javbus.py` / `tests/test_scraper_mmtv.py`：

- `test_auto_register`：注册表包含该类；
- `test_site_properties`：`site_id` / `site_name` 正确；
- `test_search_success`：mock `requests.Session`，先验证 `search()` 返回单元素列表，再验证首个 `Video` 各字段；
- `test_search_not_found` / `test_search_connection_error` / `test_search_timeout`；
- 特殊逻辑（如 FC2 匹配、多演员清洗、有码无码识别）单独用例。

运行测试：

```powershell
cd scraper-worker; .venv\Scripts\python -m pytest tests/
```

### 7. 真实站点验证

```powershell
cd scraper-worker; .venv\Scripts\python -c "from scrapers.openaver.censored.xxx import XxxScraper; print(XxxScraper().search('ABC-123'))"
```

确认能返回真实候选列表；若失败检查站点结构变化或反爬（UA / 地区限制）。站点如需返回多个候选，应覆盖 `search()` 并确保失败详情页不阻断后续候选。

### 8. 重新打包 worker

修改任何 Python 源码后必须重新打包（详见 `docs/worker-build-guide.md`）：

```powershell
cd scraper-worker; venv\Scripts\python -m PyInstaller scraper-worker.spec --noconfirm
```

打包后验证站点列表包含新站点：向 exe 发送 `list_sites` JSON-RPC 请求。

### 9. 部署

将 `scraper-worker/dist/scraper-worker.exe` 复制到应用运行目录（默认 `app/worker/scraper-worker.exe`）或通过设置「工作进程路径」指定。

### 10. 检查 .gitignore

新增目录后确认未被 `.gitignore` 误忽略：

```powershell
git check-ignore -v <新文件>
```

> 教训：`.gitignore` 中的 `OpenAver` 曾因未锚定根目录（应为 `/OpenAver`）而误忽略 `scrapers/openaver/` 下所有**新文件**（已跟踪文件不受影响，导致问题隐蔽）。

## 三、从 mdcx 迁移刮削器要点

参考代码位于 `mdcx/mdcx/crawlers/`（如 7mmtv 为 `mmtv.py`）。mdcx 是异步 + 站点配置 + LogBuffer 架构，迁移到 scraper-worker 需要改写：

1. **网络请求**：`await manager.computed.async_client.get_text(url)` → `self._session.get(url, timeout=15)`；
2. **解析库**：mdcx 用 `lxml.etree`，保留 lxml 即可（`requirements.txt` 已包含）；
3. **入口函数**：mdcx 的 `main(number, appoint_url, file_path, **kwargs)` → `search(number)`；
4. **依赖文件路径的逻辑**（如 `get_extra_info` 依赖本地文件补充演员）：无 `file_path` 时跳过，只使用网页数据；
5. **返回值**：mdcx 返回多语言嵌套 dict → 转换为 `Video` 对象。

### mdcx 字段 → Video 字段映射

| mdcx 字段 | Video 字段 | 说明 |
| --- | --- | --- |
| number | number | 网页番号；`FC2-PPV ` 前缀转回 `FC2-` |
| title | title | 通常从标题中移除番号后再使用 |
| actor | actresses | 逗号分隔 → `list[Actress]`，需清洗括号/多余描述 |
| outline | summary | 简介 |
| tag | tags | 逗号分隔 → `list[str]`，可附加有码/无码 |
| release | date | `YYYY-MM-DD` |
| runtime | duration | `时:分` 或 `N分/min` → 分钟 `int` |
| director | director | |
| studio | maker | 制作商 |
| publisher | label | 发行商 |
| thumb | cover_url | 相对路径需补全域名 |
| extrafanart | sample_images | 样图列表 |
| source | source | 填 `site_id` |
| website | detail_url | 详情页 URL |
| mosaic | tags 附加 | 有码/无码，无独立字段 |

### 7mmtv 迁移实例

完整示例见 `scraper-worker/scrapers/openaver/mixed/mmtv.py`（230 行）与 `tests/test_scraper_mmtv.py`。特殊点：

- 搜索时 FC2 番号需提取数字部分做关键词；匹配详情链接时用 `FC2-PPV ` 前缀比对标题；
- 覆盖 `search()` 返回全部匹配候选，单个详情页解析失败时跳过并继续；
- 有码/无码优先从面包屑判断，其次按 FC2 前缀兜底；
- 演员清洗：`re.sub(r"（.+）", "", each).split(" ")[0]`。

## 四、常见问题

| 问题 | 原因 | 解决 |
| --- | --- | --- |
| 源码运行正常、打包后 `Unknown: xxx` | `hiddenimports` 漏了模块 | 加入 `scraper-worker.spec` |
| 新文件 git 看不到 | `.gitignore` 规则未锚定根目录误匹配 | `git check-ignore -v` 定位并锚定 |
| 站点搜索全部超时/被重置 | 站点反爬或地区限制 | 检查 UA、代理、站点可达性（可用设置页「测试站点」） |
