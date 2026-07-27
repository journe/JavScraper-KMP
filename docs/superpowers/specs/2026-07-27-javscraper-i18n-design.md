# JavScraper i18n Design Specification

## Overview

Add internationalization (i18n) support to the Kotlin Compose Desktop UI, supporting **English** and **Chinese** (Simplified) with a language switching option in Settings. Language change takes effect **after application restart**.

## Motivation

All UI strings are currently hardcoded in English across 4 screens, 3 components, and the main entry point. This makes the app inaccessible to Chinese-speaking users — the primary target audience for JAV content tools.

## Architecture

```
┌─────────────────────────────────────────────┐
│              AppSettings                     │
│  config.json → { language: "en" | "zh" }    │
└──────────────────────┬──────────────────────┘
                       │ read on startup
                       ▼
┌─────────────────────────────────────────────┐
│              Translations (singleton)        │
│  ResourceBundle ← strings.properties        │
│                 ← strings_zh.properties      │
│  Exposes typed getter properties:           │
│    Translations.scanTitle                   │
│    Translations.settingsLanguage            │
│    Translations.galleryCount(n)             │
└─────────┬───────────────────┬───────────────┘
          │                   │
          ▼                   ▼
    UI Code (Compose)    Navigation/Status
```

**Key Principles:**
- `Translations.init(language)` called once in `LaunchedEffect(Unit)` in `App.kt`
- `ResourceBundle` loaded with `Locale(language)` — `"en"` and `"zh"`
- All string lookups go through typed property/function accessors — no raw `bundle.getString()` in UI code
- Language setting persisted to `config.json` via `SettingsManager`

## Data Model Changes

`AppSettings` in `SettingsManager.kt`:

```kotlin
@Serializable
data class AppSettings(
    // ... existing fields unchanged ...
    val language: String = "en"  // NEW — "en" or "zh"
)
```

## Files

### New Files

| File | Purpose |
|------|---------|
| `src/main/resources/strings.properties` | English translations (default bundle) |
| `src/main/resources/strings_zh.properties` | Chinese (Simplified) translations |
| `src/main/kotlin/javscraper/i18n/Translations.kt` | Singleton wrapping `ResourceBundle` |

### Modified Files

| File | Changes |
|------|---------|
| `src/main/kotlin/javscraper/Main.kt` | Window title → `Translations.appTitle` |
| `src/main/kotlin/javscraper/App.kt` | `LaunchedEffect` calls `Translations.init(language)`; all hardcoded strings replaced |
| `src/main/kotlin/javscraper/settings/SettingsManager.kt` | `AppSettings.language` field |
| `src/main/kotlin/javscraper/ui/screens/SettingsScreen.kt` | Add language radio buttons + restart hint; replace all strings |
| `src/main/kotlin/javscraper/ui/screens/FileScanScreen.kt` | Replace all strings |
| `src/main/kotlin/javscraper/ui/screens/ScrapeProgressScreen.kt` | Replace all strings |
| `src/main/kotlin/javscraper/ui/screens/ResultGalleryScreen.kt` | Replace all strings |
| `src/main/kotlin/javscraper/ui/components/ScraperStatusBar.kt` | Replace header string |

### Files Unchanged

- `PosterCard.kt` — no user-facing text strings
- `SiteSelector.kt` — pure data-driven, no hardcoded labels

## Translation Keys (Complete)

See Section 2 in the brainstorming conversation for the full key/value table. All 48 keys are documented.

Key categories:
- `app.*` — Window title, status bar messages (8 keys)
- `nav.*` — Bottom navigation labels (4 keys)
- `scan.*` — File scan screen (9 keys)
- `progress.*` — Scrape progress screen (4 keys)
- `gallery.*` — Result gallery screen (5 keys)
- `settings.*` — Settings screen (13 keys, including 3 for language)
- `statusbar.*` — Scraper status bar (1 key)
- `common.*` — Shared strings (1 key)

## Settings UI Change

A new "Language" section is added to the Settings screen, between "Scraper Sites" and "Reset to Defaults":

- Section title: "Language"
- Two radio buttons: "English" / "中文"
- On selection change: immediately writes to `SettingsManager` and shows a small hint: "Restart to apply language change"
- No restart dialog — just the inline hint text

## Edge Cases

- **Missing key:** `ResourceBundle` throws `MissingResourceException` at startup. All keys will be double-checked during implementation to ensure both bundles have matching keys.
- **Invalid language value:** `Translations.init` falls back to `Locale("en")` if the language is not `"en"` or `"zh"`.
- **First run after upgrade:** Default `language = "en"` — existing users get English until they switch.
- **AppSettings language field is missing in old config:** `ignoreUnknownKeys = true` already in `SettingsManager`, new field gets default value.

## What This Does NOT Cover

- Runtime live-switching of language (explicitly: restart required)
- Translation of scraper-site names (these come from the Python worker)
- RTL layout support
- Pluralization rules beyond simple `{0}` substitution
- Translation of system dialogs (`JFileChooser`, etc.)