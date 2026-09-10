# FC2 官方源与镜像源拆分说明

## 需求

FC2 官方直连实现与 `javten.com` 镜像实现必须作为两个独立刮削器存在，避免一个站点 ID 混用两套数据源。

## 设计

- `fc2`：保持官方站 `https://adult.contents.fc2.com`，沿用原有解析、网页归档与测试行为。
- `fc2mirror`：新增镜像站 `https://javten.com`，保留镜像实现中的卖家、标签、简介、评分与图库解析。
- 两个类分别注册到 `ScraperRegistry`，`Video.source` 分别填写 `fc2` 与 `fc2mirror`。
- 自动无码搜索顺序为 `fc2 -> fc2mirror -> heyzo -> avsox -> d2pass -> mmtv`；显式选择与自动搜索都继续受 `enabledSites` 过滤。
- 应用默认启用列表新增 `fc2mirror`；既有配置保持用户原有选择，可在设置页手动启用新站点。

## 验证

- 官方 FC2 与镜像 FC2 分别拥有独立测试文件。
- 智能搜索测试覆盖 `fc2mirror` 的启用过滤。
- 应用设置测试覆盖默认站点数量与 `fc2mirror`。
- 修改 Python worker 后重新打包并部署 exe。