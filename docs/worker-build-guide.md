# scraper-worker 打包指南

## 概述

`scraper-worker` 是 Python 编写的 JSON-RPC 后台进程，Kotlin 应用通过 stdin/stdout 与它通信。发布时必须使用 PyInstaller 将源码打包为单文件可执行程序 `scraper-worker.exe`。

## 环境要求

- Python 3.12+（本项目使用 `scraper-worker/venv`，Python 3.12 + PyInstaller 6.21）
- 依赖安装：`pip install -r requirements.txt`

## 打包命令

在 `scraper-worker/` 目录下执行：

```powershell
venv\Scripts\python -m PyInstaller scraper-worker.spec --noconfirm
```

产物位于 `scraper-worker/dist/scraper-worker.exe`。

## 重要：修改源码后必须重新打包

`scraper-worker.exe` 是打包产物，不随源码自动更新。**修改 `scraper-worker/` 下任何 `.py` 文件后，必须重新执行打包命令**，否则应用运行时加载的仍是旧代码（例如新加的 JSON-RPC 方法会返回 `Unknown: xxx`）。

## spec 注意事项

- `hiddenimports`：函数体内延迟导入（`from xxx import yyy` 写在函数内部）的模块，PyInstaller 静态分析无法发现，必须手动加入 `scraper-worker.spec` 的 `hiddenimports`。
  - 示例：`scrapers.site_check` 在 `ipc_handler.py` 的 `check_sites` handler 内延迟导入，已显式加入。
- 新增刮削站点时，需要同步更新三处：
  1. 在 `scrapers/openaver/<censored|uncensored|mixed>/` 新建文件并在模块末尾注册；
  2. 在 `ipc_handler.py` 末尾加入对应 import（触发注册）；
  3. 在 `scraper-worker.spec` 的 `hiddenimports` 中加入新模块。

## 部署位置

- 应用默认 worker 路径为运行时目录下的 `worker/scraper-worker.exe`（相对于 `user.dir`）；
- 也可以通过应用设置中的「工作进程路径」手动选择 exe 位置；
- `scraper-worker/dist/` 与 `scraper-worker/build/` 已被 `.gitignore` 忽略，exe 产物不入库。

## 验证方法

打包后可用 JSON-RPC 请求验证功能是否完整：

```powershell
# 启动 exe 后发送以下请求
# {"jsonrpc":"2.0","id":"1","method":"get_capabilities","params":{}}
# {"jsonrpc":"2.0","id":"2","method":"check_sites","params":{}}
```

- `get_capabilities` 的 `features` 应包含 `check_sites`；
- `check_sites` 应返回全部注册站点的连通状态、状态码、延迟与错误信息。

单元测试：`pytest tests/`（在 `scraper-worker/` 目录下执行）。