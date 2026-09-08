# OpenAver 有码/无码子包拆分计划

## 目标

将 `scrapers/openaver/` 从 11 个文件拆分为按内容类型组织的子包，使每个目录不超过 8 个文件，同时不改变站点 ID、注册顺序、搜索优先级和对外 JSON-RPC 行为。

## 结构

- `censored/`：`javbus`、`javdb`、`jav321`、`javlibrary`、`dmm`。
- `uncensored/`：`fc2`、`heyzo`、`avsox`、`d2pass`。
- `mixed/`：`mmtv`；它同时出现在现有有码与无码优先级链中，单独放置避免错误归类。
- 根包仅保留 `__init__.py`，子包各自拥有 `__init__.py`。

## 变更点

1. 移动 10 个站点模块到上述子包。
2. 更新 `ipc_handler.py`、`scraper-worker.spec`、测试 mock 路径与当前开发文档。
3. 不修改 `smart_search.py` 的 `CENSORED` / `UNCENSORED` 顺序。
4. 全量 Python 测试、打包和本地 JSON-RPC 冒烟验证。
