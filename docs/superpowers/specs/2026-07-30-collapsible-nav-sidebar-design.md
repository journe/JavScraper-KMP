# Collapsible Navigation Sidebar — Design Spec

## Overview

Replace the existing bottom navigation bar (`NavigationBar` with three tabs) with a
left-side collapsible navigation sidebar. The sidebar toggles between a narrow
icon-only state (~72dp) and an expanded icon+label state (~200dp) via the
hamburger icon in the TopAppBar.

## Motivation

- Better use of horizontal space on wide screens
- More room for content area (FileScan, ScrapeProgress, Gallery)
- Industry-standard navigation pattern for desktop applications
- Frees up vertical space currently occupied by the bottom bar

## States & Interactions

### Sidebar Width

| State | Width | Content |
|-------|-------|---------|
| Collapsed | 72dp | Three icon-only NavigationRailItems |
| Expanded | 200dp | Three items with icon + label text |

### Transition

- `animateDpAsState` with `tween(250ms, easing = FastOutSlowInEasing)`
- Label text fades in/out via `AnimatedVisibility`

### Toggle

- TopAppBar left side gets a hamburger icon button
- Collapsed state: `Icons.Default.Menu`
- Expanded state: `Icons.Default.MenuOpen`
- Click toggles `navExpanded` boolean

## Layout Structure

```
+--------------------------------------------------+
|  hamburger  JavScraper    [status text]  gear    |  <- TopAppBar
+----------+---------------------------------------+
|          |                                       |
|  Scan    |      Content Area                     |
|          |  (FileScanScreen / ScrapeProgress     |
|  Scrape  |   / ResultGalleryScreen)              |
|          |                                       |
|  Gallery |                                       |
+----------+---------------------------------------+
```

## Navigation Items (in sidebar)

| Item | Icon | Selected Logic | Enabled Logic |
|------|------|----------------|---------------|
| Scan | Icons.Default.Search | currentScreen == SCAN | Always enabled |
| Scrape | Icons.Default.CloudDownload / CloudOff | currentScreen == PROGRESS | tasks.isNotEmpty() |
| Gallery | Icons.Default.PhotoLibrary | currentScreen == GALLERY | results.isNotEmpty() |

Settings (Icons.Default.Settings) remains in the TopAppBar actions slot.

## Implementation Plan

### New code in App.kt

1. Add `var navExpanded by remember { mutableStateOf(false) }`
2. Add `CollapsibleNavRail` composable (private function):
   - Parameters: expanded, currentScreen, onNavigate, scrapeEnabled, galleryEnabled
   - Uses NavigationRail with animated width
   - Each NavigationRailItem has animated label visibility
3. Modify TopAppBar:
   - Add navigationIcon slot with Menu/MenuOpen button
4. Remove bottomBar block entirely
5. Wrap content in Row { CollapsibleNavRail + Box(weight=1f) { ... } }

### No changes needed

- AppViewModel.kt — navigation logic unchanged
- All screen files — layout unaffected
- Theme.kt — no color changes needed
- i18n/ — existing navScan/navScrape/navGallery strings reused

## Scope Assessment

- **Single implementation unit**: Yes. All changes are concentrated in App.kt.
- **Decomposition needed**: No. One focused file change.
- **Dependencies**: None. Pure UI restructuring; no data layer, API, or business logic changes.

## Affected Files

| File | Impact |
|------|--------|
| app/src/main/kotlin/javscraper/App.kt | Add CollapsibleNavRail + toggle state, modify Scaffold — ~+55 lines, ~-30 lines |
