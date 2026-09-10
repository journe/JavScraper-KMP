# 迁移 JavBus / D2Pass / FC2 刮削器计划

## 目标

将 `mdcxOpenAver/core/scrapers` 中 JavBus、D2Pass、FC2 三个成熟实现迁移到 `scraper-worker/scrapers/openaver`，保留当前 worker 的注册协议、`BaseScraper.search()` 归一化接口和网页归档能力。

## 结构

- JavBus：`censored/javbus_parsing.py` 承载多语言字段、详情页、搜索结果解析；`censored/javbus.py` 只负责请求与注册。
- D2Pass：`uncensored/d2pass_parsing.py` 承载 JSON、Caribbean HTML fallback、图库解析；`uncensored/d2pass.py` 负责三站点请求顺序与注册。
- FC2：`uncensored/fc2.py` 迁移 javten 镜像实现，保留 lxml 解析和当前 worker 归档接口。
- 测试：重写 `test_scraper_javbus.py`、`test_scraper_fc2.py`，新增 `test_scraper_d2pass.py`。

## 关键差异

- JavBus 源实现具备 zh-tw/ja/en 字段映射、番号标题剥离、搜索 ID 提取和完整浏览器指纹；目标实现只有 URL 分类猜测。
- D2Pass 源实现直接访问 1Pondo / Caribbeancom / 10musume JSON API，并按番号形态排序、带 Caribbean HTML fallback；目标实现仍访问已不可用的 d2pass 搜索页。
- FC2 源实现使用 javten 镜像站，能提取卖家、标签、简介、评分与图库；目标实现直连官方页面且字段覆盖较弱。

## TDD 与验证

1. 先写入三个刮削器的迁移行为测试并运行，确认当前目标实现失败。
2. 迁移解析与网络实现，保持每个 Python 文件低于 300 行。
3. 运行 `scraper-worker/venv/Scripts/python -m pytest tests/test_scraper_javbus.py tests/test_scraper_d2pass.py tests/test_scraper_fc2.py -q`。
4. 运行 worker 全量测试。
5. 重新打包 Python worker，并复制 `dist/scraper-worker.exe` 到 `app/worker/scraper-worker.exe`。

## 后续修订

FC2 官方直连与 `javten.com` 镜像已在 `2026-09-10-split-fc2-source-and-mirror.md` 中拆分为 `fc2` 与 `fc2mirror` 两个独立刮削器；本文中“FC2 使用 javten 镜像实现”的表述仅代表拆分前的中间方案。
