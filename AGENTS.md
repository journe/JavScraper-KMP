# Repository Guidelines

## Project Structure & Module Organization

This repository contains two main components:

- **`app/`** — Compose Desktop (Kotlin/JVM) built with Gradle. Source lives under `app/src/main/kotlin/javscraper/` organized into packages:
  - `i18n/` — Type-safe translation strings via data classes + CompositionLocal
  - `io/` — File I/O and persistence
  - `models/` — Data classes and domain models
  - `scrape/` — Scraping orchestration (ScrapeOrchestrator.kt)
  - `settings/` — Application settings management
  - `sidecar/` — Lifecycle management for the Python worker process
  - `ui/` — Compose UI screens and components
- **`scraper-worker/`** — Python JSON-RPC worker (Python 3.12+) with scrapers under scrapers/openaver/, core utilities in core/, and tests in tests/.
- **`installer/`** — WiX toolset files (bundle.wxs, build.bat) for Windows packaging.
- **`docs/superpowers/`** — Design specs and implementation plans.

Test sources mirror the source layout under `app/src/test/kotlin/javscraper/` and scraper-worker/tests/.

## Build Test and Development Commands

### Kotlin (Compose Desktop)

```powershell
# Compile without running tests
cd app; ./gradlew compileKotlin --no-daemon

# Run all unit tests
cd app; ./gradlew test --no-daemon

# Run the desktop application
cd app; ./gradlew run --no-daemon
```

### Python Worker

```powershell
# Install dependencies
cd scraper-worker; pip install -r requirements.txt

# Run all tests
cd scraper-worker; pytest tests/

# Run a specific test file
cd scraper-worker; pytest tests/test_registry.py
```

## Coding Style amp; Naming Conventions

### Kotlin

- Use 4-space indentation throughout.
- Follow the Kotlin Coding Conventions.
- Package names are lowercase: javscraper.models, javscraper.ui.
- Classes use PascalCase; functions and properties use camelCase.
- Prefer val over var; use immutable data classes for models.
- Composable functions are PascalCase (e.g., SettingsScreen).

### Python

- Follow PEP 8 with 4-space indentation.
- Modules and packages use snake_case.
- Classes use PascalCase; functions and variables use snake_case.

## Testing Guidelines

### Kotlin Tests

- Use the built-in kotlin.test framework with JUnit runner.
- Test files are named ClassTest.kt and placed in the corresponding package under app/src/test/kotlin/.
- Test functions use descriptive names.

### Python Tests

- Use pytest with plain assert statements.
- Test files are named test_module.py inside scraper-worker/tests/.
- Run with pytest tests/ from the scraper-worker/ directory.

## Commit amp; Pull Request Guidelines

The project uses Conventional Commits (feat:, fix:, refactor:, docs:). Commit messages are short, imperative, and English-only. Pull requests should include a description of the change and reference related issues or design docs.

## Security amp; Configuration Tips

- Never commit user-specific configuration. These are gitignored.
- The Python worker communicates with the Kotlin app via JSON-RPC over stdin/stdout.
- When adding a new scraper source, register it in scrapers/registry.py and add a corresponding file under scrapers/openaver/.

## Internationalization (i18n)

Translations are defined as Kotlin classes in app/src/main/kotlin/javscraper/i18n/:

- TranslationStrings.kt: Base open class with all UI strings as open val properties and open fun methods (for parameterized strings). Default values are English.
- TranslationZh.kt: Chinese (Simplified) localization, overrides all properties.
- Translations.kt: Exposes LocalTranslations (staticCompositionLocalOf) for Compose-native reactive access.

### Usage in Composable functions

val t = LocalTranslations.current
Text(t.scanTitle)
Text(t.scanFound(matchedFiles.size))

### Adding a new locale

1. Create a new class extending TranslationStrings.
2. Override all open val / open fun members with the translated text.
3. In App.kt, add the locale to the localeStrings resolution.

### Adding a new UI string

1. Add an open val (or open fun if parameterized) to TranslationStrings.
2. Override it in all locale subclasses.
3. Reference it via LocalTranslations.current.xxx in composables.
