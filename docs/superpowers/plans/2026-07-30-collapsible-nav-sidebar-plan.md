# Collapsible Navigation Sidebar Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the bottom NavigationBar with a left-side collapsible NavigationRail sidebar.

**Architecture:** Single file change in App.kt. Add a CollapsibleNavRail composable using Material3 NavigationRail with animated width. Hamburger icon in TopAppBar toggles collapsed/expanded state. Content area wraps in Row alongside the sidebar.

**Tech Stack:** Kotlin, Compose Desktop, Material3

## Global Constraints

- Only modify `app/src/main/kotlin/javscraper/App.kt` — no other files changed
- Use existing imports already at top of App.kt; add only what is missing
- Navigation state (`currentScreen`, `navigate()`) is unchanged in AppViewModel
- Locale strings `navScan`/`navScrape`/`navGallery` already exist and are reused
- Settings gear icon stays in TopAppBar actions

---

### Task 1: Add CollapsibleNavRail to App.kt

**Files:**
- Modify: `app/src/main/kotlin/javscraper/App.kt`

**Interfaces:**
- Consumes: `Screen` enum, `viewModel.currentScreen`, `viewModel.navigate(Screen)`, `viewModel.tasks`, `viewModel.results`, `Icons.Default.*`, `LocalTranslations.current`
- Produces: Modified App.kt with collapsible sidebar replacing bottom bar

- [ ] **Step 1: Add missing imports**

Add these imports at the top of App.kt:

```kotlin
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.ui.unit.dp
```

- [ ] **Step 2: Write the CollapsibleNavRail composable**

Add this private composable function before the App() function (or at the bottom of the file):

```kotlin
@Composable
private fun CollapsibleNavRail(
    expanded: Boolean,
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    scrapeEnabled: Boolean,
    galleryEnabled: Boolean
) {
    val t = LocalTranslations.current
    val navWidth by animateDpAsState(
        targetValue = if (expanded) 200.dp else 72.dp,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
    )

    NavigationRail(
        modifier = Modifier.width(navWidth),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Spacer(Modifier.height(8.dp))
        NavigationRailItem(
            selected = currentScreen == Screen.SCAN,
            onClick = { onNavigate(Screen.SCAN) },
            icon = { Icon(Icons.Default.Search, contentDescription = t.navScan) },
            label = {
                AnimatedVisibility(visible = expanded) {
                    Text(t.navScan)
                }
            }
        )
        NavigationRailItem(
            selected = currentScreen == Screen.PROGRESS,
            onClick = { onNavigate(Screen.PROGRESS) },
            enabled = scrapeEnabled,
            icon = {
                Icon(
                    if (scrapeEnabled) Icons.Default.CloudDownload
                    else Icons.Default.CloudOff,
                    contentDescription = t.navScrape
                )
            },
            label = {
                AnimatedVisibility(visible = expanded) {
                    Text(t.navScrape)
                }
            }
        )
        NavigationRailItem(
            selected = currentScreen == Screen.GALLERY,
            onClick = { onNavigate(Screen.GALLERY) },
            enabled = galleryEnabled,
            icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = t.navGallery) },
            label = {
                AnimatedVisibility(visible = expanded) {
                    Text(t.navGallery)
                }
            }
        )
    }
}
```

- [ ] **Step 3: Modify TopAppBar — add hamburger navigation icon**

In the `TopAppBar` call inside App(), add the `navigationIcon` slot between `topBar = {` and `title = {...}`:

```kotlin
TopAppBar(
    navigationIcon = {
        IconButton(onClick = { navExpanded = !navExpanded }) {
            Icon(
                if (navExpanded) Icons.Default.MenuOpen
                else Icons.Default.Menu,
                contentDescription = "Toggle navigation"
            )
        }
    },
    title = { Text("JavScraper") },
    // ... rest unchanged
)
```

- [ ] **Step 4: Add navExpanded state variable**

Inside App(), before the Scaffold, add:

```kotlin
var navExpanded by remember { mutableStateOf(false) }
```

- [ ] **Step 5: Remove the bottomBar block**

Delete the entire `bottomBar = { ... }` section from the Scaffold — from `bottomBar = {` through the closing brace of the `NavigationBar`.

- [ ] **Step 6: Wrap content in a Row with the sidebar**

Replace:

```kotlin
) { padding ->
    Box(Modifier.padding(padding)) {
        when (viewModel.currentScreen) {
            ...
        }
    }
}
```

With:

```kotlin
) { padding ->
    Row(Modifier.padding(padding)) {
        CollapsibleNavRail(
            expanded = navExpanded,
            currentScreen = viewModel.currentScreen,
            onNavigate = viewModel::navigate,
            scrapeEnabled = viewModel.tasks.isNotEmpty(),
            galleryEnabled = viewModel.results.isNotEmpty()
        )
        Box(Modifier.weight(1f)) {
            when (viewModel.currentScreen) {
                Screen.SCAN -> FileScanScreen(...)
                Screen.PROGRESS -> ScrapeProgressScreen(...)
                Screen.GALLERY -> ResultGalleryScreen(...)
                Screen.SETTINGS -> SettingsScreen(...)
            }
        }
    }
}
```

- [ ] **Step 7: Compile to verify**

```bash
cd app; ./gradlew compileKotlin --no-daemon
```

Expected: BUILD SUCCESSFUL — no errors.
