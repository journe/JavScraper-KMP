# JavScraper Design Specification

## Overview

JavScraper is a **local video metadata scraper (刮削器)** for Windows desktop. It scans video files, extracts numeric identifiers (番号), scrapes metadata from JAV websites via existing Python scraper code, and generates NFO files + cover images compatible with Jellyfin/Emby/Kodi media servers.

### Goals
- Provide a modern native desktop UI (Kotlin Compose Desktop)
- Reuse existing Python scraper assets from **OpenAver** (primary) and **mdcx** (secondary)
- Generate Jellyfin-compatible NFO + poster/fanart images
- Run fully offline/local, single-installer for Windows

### Non-Goals
- Not a media server — output targets Jellyfin/Emby/Kodi
- Not a video player
- Not a database-managed library (file system is the source of truth)

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│              Kotlin Compose Desktop                  │
│  ┌──────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │FileScanner│  │ScrapeOrch.  │  │SettingsManager│  │
│  └────┬─────┘  └──────┬───────┘  └───────┬───────┘  │
│       │               │                   │          │
│  ┌────▼───────────────▼───────────────────▼───────┐  │
│  │              SidecarManager                     │  │
│  │  - JSON-RPC 2.0 over stdin/stdout               │  │
│  │  - Process lifecycle (start/stop/restart)       │  │
│  │  - Health check & auto-recovery                 │  │
│  └──────────────────────┬──────────────────────────┘  │
└─────────────────────────┼─────────────────────────────┘
                          │ stdin/stdout (JSON-RPC)
┌─────────────────────────▼─────────────────────────────┐
│           Python Worker (scraper-worker.exe)           │
│  ┌─────────────────────────────────────────────────┐  │
│  │  IPC Handler (main.py)                          │  │
│  │  Methods: scrape, search, probe_file,           │  │
│  │  list_sites, get_capabilities, shutdown         │  │
│  ├──────────────────┬──────────────────────────────┤  │
│  │  OpenAver 9 Scrapers │                          │  │
│  │  · JavBus / JavDB / Jav321 / FC2 / HEYZO       │  │
│  │  · AVSox / D2Pass / DMM / JavLibrary            │  │
│  ├──────────────────┴──────────────────────────────┤  │
│  │  Core: file_prober, smart_search                │  │
│  └─────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────┐
│              Kotlin 文件 I/O (主进程侧)               │
│  ├─ NfoWriter      — 从 JSON 写 NFO XML              │
│  ├─ ImageSaver     — 从 URL 下载并保存图片             │
│  └─ Mover          — 文件链接/复制/整理               │
└─────────────────────────────────────────────────────┘
```

**Key principle:** Worker is stateless — returns only structured JSON + image URLs. Kotlin handles all file system I/O (NFO writing, image downloading & saving).

---

## Data Flow

```
用户选择文件夹 → FileScanner → 扫描视频文件 → 提取番号
                                    ↓
                           ScrapeOrchestrator
                                    ↓
                    SidecarManager.scrape(file_path, site?)
                                    ↓
                           Python Worker → smart_search
                                    ↓
                    ┌─── ImageSaver.download() → poster/fanart
                    ├─── NfoWriter.write() → .nfo 文件
                    └─── 硬链接/复制原始视频到输出目录
```

---

## Project Structure

```
JavScraper/
├── app/                              # Kotlin Compose Desktop
│   ├── build.gradle.kts              # Gradle build (Compose 1.7.3 + Kotlin 2.1.0)
│   ├── settings.gradle.kts
│   └── src/main/kotlin/javscraper/
│       ├── Main.kt                   # Entry point
│       ├── App.kt                    # Main composable + navigation
│       ├── sidecar/SidecarManager.kt # JSON-RPC process management
│       ├── models/Models.kt          # Video, Actress, ScrapeResult
│       ├── io/NfoWriter.kt           # NFO XML generation
│       ├── io/ImageSaver.kt          # Image downloader
│       ├── io/FileScanner.kt         # File scanning + number extraction
│       ├── scrape/ScrapeOrchestrator.kt
│       ├── settings/SettingsManager.kt
│       └── ui/                       # Compose UI
│           ├── theme/
│           ├── screens/              # 4 screens
│           └── components/
│
└── scraper-worker/                   # Python Worker
    ├── main.py                       # Entry: stdio JSON-RPC loop
    ├── ipc_handler.py                # Request routing
    ├── core/                         # file_prober, smart_search
    ├── scrapers/                     # 9 scrapers + registry
    └── tests/                        # 47 tests
```

---

## Phases

### Phase 1 ✅ — Worker IPC & Scrapers
- Python worker with JSON-RPC 2.0 over stdin/stdout
- 9 OpenAver scrapers (JavBus, JavDB, Jav321, FC2, HEYZO, AVSox, D2Pass, DMM, JavLibrary)
- File prober, smart search, scraper registry
- 47 unit/integration tests

### Phase 2 ✅ — Kotlin Compose Desktop
- Gradle project with Compose Desktop 1.7.3 + Kotlin 2.1.0
- SidecarManager: Python worker lifecycle & JSON-RPC
- NfoWriter, ImageSaver, FileScanner
- ScrapeOrchestrator: full scrape pipeline
- SettingsManager: %LOCALAPPDATA% config persistence
- Material3 UI: 4 screens (Scan, Progress, Gallery, Settings)

### Phase 3 ⬜ — Testing & Integration
- Kotlin unit tests (SidecarManager, NfoWriter, FileScanner)
- End-to-end integration test (IPC → scrape → NFO)
- Manual smoke test with sample files

### Phase 4 ⬜ — Packaging
- PyInstaller: scraper-worker.exe (onefile)
- WiX Toolset: JavScraper.msi
- Final installer merges Kotlin + Worker
- Installed layout: %LOCALAPPDATA%\\JavScraper\\

---

## NFO Output Format (Jellyfin/Emby/Kodi)

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<movie>
  <title>完全タイトル</title>
  <originaltitle>SONE-205</originaltitle>
  <sorttitle>SONE-205</sorttitle>
  <set>So甜美</set>
  <year>2024</year>
  <premiered>2024-06-11</premiered>
  <release>2024-06-11</release>
  <runtime>120</runtime>
  <mpaa>JP-18+</mpaa>
  <plot>影片简介</plot>
  <outline>影片简介</outline>
  <studio>SOD Create</studio>
  <maker>SOD Create</maker>
  <label>STAR</label>
  <director>監督名</director>
  <actor><name>女優A</name><role>女優A</role><order>1</order></actor>
  <genre>高清画质</genre>
  <tag>中文字幕</tag>
  <rating>7.5</rating>
</movie>
```

---

## File Output Structure

```
Output Dir/
└── SONE-205 [SOD] 女優A 女優B/
    ├── SONE-205.mp4         # Original file (hardlink)
    ├── SONE-205.nfo         # Jellyfin NFO
    ├── poster.jpg           # Vertical poster
    ├── fanart.jpg           # Horizontal background
    └── extrafanart/         # Extra images (optional)
        └── fanart1.jpg
```

---

## Development Workflow

```
# Terminal 1: Python Worker
cd scraper-worker
venv\Scripts\python main.py --dev

# Terminal 2: Kotlin Desktop
cd app
gradle run

# Test IPC manually:
echo {"jsonrpc":"2.0","id":"1","method":"list_sites","params":{}} | venv\Scripts\python main.py
```