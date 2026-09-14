# 扫描目录渐进式加载优化计划

## 根因结论

`AppViewModel.startScan()` 在 Compose 主协程中直接调用 `FileScanner.scanDirectory()`。扫描过程包含目录枚举、每个视频匹配 NFO 的文件系统检查，以及已刮削 NFO 的读取和 XML 解析；350 个文件本地温缓存约需 314ms，冷缓存/机械盘/网络盘会放大到数秒。由于所有工作都在 UI 主线程执行，界面会冻结。

## 方案

- `FileScanner` 新增 `scanDirectoryFlow(dir, recursive, batchSize)`，按 50 条一批发射扫描结果；同步 `scanDirectory()` 保持原行为。
- `AppViewModel.startScan()` 改为收集 Flow：上游通过 `flowOn(Dispatchers.IO)` 执行 IO，Main 收集器分批更新 `scannedFiles` 与 `scrapedFiles`。
- 扫描开始先进入 `scanning` 状态并清空上轮列表，避免旧结果与分批结果混合；完成后再生成刮削任务。
- 使用可注入扫描 Flow 写线程与渐进状态测试，不把 UI 框架引入单元测试。

## 验证

- `FileScannerTest` 覆盖 55 个文件按 20/20/15 三批发射。
- `AppViewModelScanTest` 覆盖扫描在后台线程执行、第一批发射后状态已可见、最终列表完整。
- 全量 `:app:test` 与 `:app:compileKotlin`。