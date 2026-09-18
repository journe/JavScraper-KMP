# 迁移 MDCx 国产刮削器实施清单

## 目标

将 MDCx 默认国产网站源 `madouqu`、`mdtv`、`hdouban`、`cnmdb`、`javday` 迁移到 `scraper-worker`，新增 `domestic` 站点分类，并完成注册、自动搜索、设置默认值、测试、打包与部署。

## 实施步骤

1. 测试先行扩展分类与自动搜索：`ScraperRegistry` 识别 `domestic`，`SiteCategory` 支持中文分组，国产番号进入独立优先级链。
2. 逐站迁移 5 个刮削器：保留 MDCx 的解析规则和站点评测口径，改为 `requests.Session + lxml`，统一返回 `Video`，结果标签包含 `国产`。
3. 补齐注册链路：`ipc_handler.py`、`smart_search.py`、`scraper-worker.spec`、`AppSettings.enabledSites`。
4. 为每个站点建立解析、未命中、连接失败、超时测试；注册和分类测试覆盖全部新站点。
5. 运行 Python 全量测试与 Kotlin 相关测试；真实站点可访问时做抽样验证。
6. 使用 PyInstaller 重新打包 worker，验证 `list_sites`，并部署到 `app/worker/scraper-worker.exe`。

## 边界

- 不迁移 `love6`、`guochan.py` 聚合工具及其他仅在 MDCx 中偶发返回国产数据的站点。
- 不引入文件路径反推国产的逻辑；自动搜索仅根据番号特征进入国产链，用户仍可显式选择任一新站点。
- 不调整现有有码/无码站点顺序。
