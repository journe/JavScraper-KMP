# JavDB 浏览器登录 Implementation Plan

> 执行时使用 superpowers:executing-plans。

**Goal:** 新增“登录获取 JavDB Cookie”能力，用应用管理的 Edge/Chrome 临时 Profile 完成登录，并通过 CDP 自动保存 `_jdb_session`。
**Architecture:** 新增 `javscraper/auth` 子模块负责浏览器进程、CDP 通信和登录流程；设置页只呈现状态并调用控制器。Cookie 仍复用现有 `javdbSessionCookie` 设置和 worker 重启链路。
**Tech Stack:** Kotlin/Compose、Java HttpClient WebSocket、Chrome DevTools Protocol、kotlinx.serialization。

## Global Constraints

- 不读取用户日常浏览器 Profile，只使用本次登录创建的临时 Profile。
- CDP 报文与 Cookie 值不得写入应用日志。
- 只保存目标域名下的 `_jdb_session`，不保存整段 Cookie。
- 登录取消、失败或完成后关闭浏览器进程并清理临时 Profile。
- 保留现有手动输入 Cookie 作为兜底。
- 本次不自动提交，因为工作区已有其他未提交改动。
- 新增 Kotlin 文件不超过 400 行；`auth/` 与设置页子包文件数不超过 8 个。

### Task 1: 登录 URL 与状态模型

**Files:**
- Create: `app/src/main/kotlin/javscraper/auth/JavdbLoginModels.kt`
- Test: `app/src/test/kotlin/javscraper/auth/JavdbLoginModelsTest.kt`

**Interfaces:**
- Produces: `JavdbLoginState`、`JavdbLoginStatus`、`resolveJavdbLoginUrl(baseUrl: String): String`

- [x] 测试镜像地址转登录 URL、非法输入与状态默认值。
- [x] 实现最小模型和 URL 解析。
- [x] 运行 `:app:test --tests javscraper.auth.JavdbLoginModelsTest`。

### Task 2: CDP 结果解析

**Files:**
- Create: `app/src/main/kotlin/javscraper/auth/JavdbCdpParsers.kt`
- Test: `app/src/test/kotlin/javscraper/auth/JavdbCdpParsersTest.kt`

**Interfaces:**
- Produces: `parseJavdbLoginPage(payload: String): JavdbLoginPage`
- Produces: `extractJavdbSessionCookie(payload: String, host: String): String?`

- [x] 测试登录页标志：匿名 `/users/new`、登录后 `/users/profile` 或 logout。
- [x] 测试只提取目标 host 的 `_jdb_session`，忽略其他 Cookie。
- [x] 实现解析器并运行聚焦测试。

### Task 3: 浏览器进程与 CDP 客户端

**Files:**
- Create: `app/src/main/kotlin/javscraper/auth/JavdbBrowserLauncher.kt`
- Create: `app/src/main/kotlin/javscraper/auth/JavdbCdpClient.kt`

**Interfaces:**
- Produces: `JavdbBrowserSession`
- Produces: `JavdbCdpClient.evaluate(expression: String): String`
- Produces: `JavdbCdpClient.cookies(url: String): String`

- [x] 测试浏览器路径选择优先级与启动参数。
- [x] 测试 DevToolsActivePort 解析。
- [x] 实现 Java HttpClient/WebSocket CDP 客户端。
- [x] 确认客户端发送 `Runtime.evaluate` 与 `Network.getCookies` 的 JSON 结构。

### Task 4: 登录流程控制器

**Files:**
- Create: `app/src/main/kotlin/javscraper/auth/JavdbLoginController.kt`
- Test: `app/src/test/kotlin/javscraper/auth/JavdbLoginControllerTest.kt`

**Interfaces:**
- Consumes: `JavdbBrowserLoginClient`
- Produces: `start(baseUrl: String, onCookie: (String) -> Unit)`、`cancel()`

- [x] 用假客户端测试等待登录、保存成功、用户取消、浏览器异常和 Cookie 缺失。
- [x] 实现协程控制器与状态转换。
- [x] 运行聚焦测试。

### Task 5: 设置页接线与端到端验证

**Files:**
- Create: `app/src/main/kotlin/javscraper/ui/screens/settings/JavdbCookieSettings.kt`
- Modify: `app/src/main/kotlin/javscraper/ui/screens/settings/ScrapingSettingsTab.kt`
- Modify: `app/src/main/kotlin/javscraper/ui/screens/settings/SettingsScreen.kt`
- Modify: `app/src/main/kotlin/javscraper/AppViewModel.kt`
- Modify: `app/src/main/kotlin/javscraper/i18n/TranslationEn.kt`
- Modify: `app/src/main/kotlin/javscraper/i18n/TranslationZh.kt`
- Test: `app/src/test/kotlin/javscraper/auth/JavdbCdpSmokeTest.kt`

**Interfaces:**
- Consumes: `JavdbLoginController`
- Produces: 设置页“登录获取”按钮、等待/取消/成功/失败状态

- [x] 补 i18n 字符串。
- [x] 状态和动作穿透 `SettingsState`/`SettingsActions`。
- [x] 将 Cookie 保存接到现有 `updateJavdbSessionCookie`。
- [x] 运行设置页相关测试和编译。
- [x] 用本机 Edge headless 对匿名页面做真实 CDP 冒烟，确认可读取匿名 `_jdb_session` 且登录状态为 false；完整登录流程需用户输入验证码后人工确认。

## Self Review

- 覆盖方案中的镜像选择、自动登录态识别、Cookie 过滤、清理和兜底手动输入。
- 不引入 Playwright/JCEF/JavaFX。
- 不修改 Python worker，因为 Cookie 仍由现有环境变量链路注入。
