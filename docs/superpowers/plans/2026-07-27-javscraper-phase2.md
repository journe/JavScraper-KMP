# JavScraper Phase 2 — Kotlin Compose Desktop

**Status:** ✅ COMPLETED

**Goal:** Build the Kotlin Compose Desktop app with SidecarManager, NFO generation, image downloading, file scanning, and Material3 UI.

## File Structure

```
app/
├── build.gradle.kts             # Compose Desktop 1.7.3 + Kotlin 2.1.0
├── settings.gradle.kts
├── gradle.properties            # JDK: IntelliJ IDEA JBR
├── gradle/wrapper/
└── src/main/
    ├── resources/logback.xml
    └── kotlin/javscraper/
        ├── Main.kt              # Entry: Window(1200x800)
        ├── App.kt               # Main composable + 4-tab navigation
        ├── models/Models.kt     # Video, Actress, ScrapeResult, ScannedFile
        ├── sidecar/SidecarManager.kt  # JSON-RPC process manager
        ├── io/NfoWriter.kt      # Jellyfin NFO XML generation
        ├── io/ImageSaver.kt     # Async image download
        ├── io/FileScanner.kt    # Video scanning + number extraction
        ├── scrape/ScrapeOrchestrator.kt
        ├── settings/SettingsManager.kt
        └── ui/
            ├── theme/Theme.kt   # Material3 dark/light theme
            ├── components/      # PosterCard, ScraperStatusBar, SiteSelector
            └── screens/         # FileScan, ScrapeProgress, ResultGallery, Settings
```

## Key Components

### SidecarManager
- ProcessBuilder + Coroutine-based process management
- JSON-RPC 2.0 over stdin/stdout
- ConcurrentHashMap for request-response correlation
- 60s timeout, auto-recovery

### NfoWriter
- XMLStreamWriter-based NFO generation
- Full Jellyfin/Emby/Kodi compatibility: title, plot, studio, actor, genre, rating

### FileScanner
- 3 regex patterns for JAV number extraction
- Auto-excludes sample/trailer files

### ScrapeOrchestrator
- Full pipeline: scrape → NFO → images → hardlink/copy

### SettingsManager
- %LOCALAPPDATA%/JavScraper/config.json persistence
- kotlinx.serialization for JSON config

## Build

```powershell
gradle compileKotlin  # BUILD SUCCESSFUL
gradle jar            # BUILD SUCCESSFUL
```

## Environment

- JDK: IntelliJ IDEA JBR (C:\Users\journ\AppData\Local\Programs\IntelliJ IDEA\jbr)
- Gradle: 8.13 (cached in ~/.gradle/wrapper/dists)
- Kotlin: 2.1.0, Compose: 1.7.3