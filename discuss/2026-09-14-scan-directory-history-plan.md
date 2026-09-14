# 扫描目录历史记录实施计划

> **执行说明：** 本计划按任务内测试先行执行；本次为用户明确要求的当前会话内联实现，不派发新任务。

**目标：** 为扫描目录提供最多 10 条、最近优先且去重的持久化历史记录，并允许在扫描页和设置页通过输入框下拉快速选择。

**架构：** 历史规则放在 `javscraper.settings.ScanDirectoryHistory` 纯函数对象中；`SettingsController` 负责状态与 `SettingsManager` 持久化；两个界面复用 `ScanDirectoryField` 组件，避免重复 UI 逻辑。

**技术栈：** Kotlin、JetBrains Compose Material 3、kotlinx.serialization、kotlin.test。

## 全局约束

- 历史记录最多保留 10 条。
- 新选择目录插入最前；重复目录只保留一次。
- 点击输入框仅在历史非空时弹出下拉。
- 设置重置后历史为空。
- 不修改已有的 `NfoReader` 相关用户改动。

---

## 任务 1：历史规则与配置模型

**文件：**
- 创建：`app/src/main/kotlin/javscraper/settings/ScanDirectoryHistory.kt`
- 修改：`app/src/main/kotlin/javscraper/settings/SettingsManager.kt`
- 测试：`app/src/test/kotlin/javscraper/settings/ScanDirectoryHistoryTest.kt`
- 测试：`app/src/test/kotlin/javscraper/settings/SettingsManagerTest.kt`

**接口：**
- `ScanDirectoryHistory.MAX_ENTRIES`
- `ScanDirectoryHistory.remember(history: List<String>, directory: String): List<String>`
- `AppSettings.scanDirHistory: List<String>`

步骤：
- 先写失败测试：默认配置历史为空；旧 JSON 解码后历史为空；新路径置前；重复路径去重；超过 10 条截断。
- 运行 `.\gradlew :app:test --tests "javscraper.settings.ScanDirectoryHistoryTest" --no-daemon`，确认因类型不存在失败。
- 增加纯函数与配置字段，重跑定向测试通过。

## 任务 2：控制器与状态流

**文件：**
- 修改：`app/src/main/kotlin/javscraper/SettingsController.kt`
- 修改：`app/src/main/kotlin/javscraper/AppViewModel.kt`
- 修改：`app/src/main/kotlin/javscraper/ui/screens/FileScanScreen.kt`
- 修改：`app/src/main/kotlin/javscraper/ui/screens/settings/SettingsScreen.kt`
- 修改：`app/src/main/kotlin/javscraper/ui/screens/settings/SettingsPreview.kt`

**接口：**
- `SettingsController.scanDirHistory`
- `SettingsController.selectScanDirFromHistory(directory: String)`
- `FileScanState.scanDirHistory`
- `FileScanActions.onSelectDirectoryFromHistory`
- `SettingsState.scanDirHistory`
- `SettingsActions.onSelectScanDirFromHistory`

步骤：
- 文件选择成功后调用历史规则并保存。
- 历史选择成功后更新当前目录、重排历史并保存。
- 重置设置时同步清空历史。
- `AppViewModel` 转发状态与动作，两个界面状态包补字段。

## 任务 3：共享输入组件

**文件：**
- 创建：`app/src/main/kotlin/javscraper/ui/components/ScanDirectoryField.kt`
- 修改：`app/src/main/kotlin/javscraper/ui/screens/FileScanScreen.kt`
- 修改：`app/src/main/kotlin/javscraper/ui/screens/settings/DirectorySettingsTab.kt`

**接口：**
- `ScanDirectoryField(value, history, onHistorySelect, onBrowse, label, modifier)`

步骤：
- 使用 `ExposedDropdownMenuBox`；历史非空时输入框可展开。
- 菜单项单行省略显示路径，点击后回填并关闭。
- 保留右侧文件夹按钮，按钮行为仍是打开系统目录选择器。
- 扫描页与设置页替换为共享组件。

## 验证

- `.\gradlew :app:test --no-daemon`
- `.\gradlew :app:compileKotlin --no-daemon`
- 检查相关源码行数均低于 400 行。
- `git diff --check`