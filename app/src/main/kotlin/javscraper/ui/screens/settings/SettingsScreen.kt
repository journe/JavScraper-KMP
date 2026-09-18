package javscraper.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import javscraper.i18n.LocalTranslations
import javscraper.i18n.TranslationEn
import javscraper.models.SiteCheckResult
import javscraper.models.SiteInfo

/** Bundled settings state to reduce parameter count on [SettingsScreen]. */
data class SettingsState(
    val workerPath: String,
    val outputDir: String,
    val scanDir: String,
    val scanDirHistory: List<String>,
    val scanRecursive: Boolean,
    val createMovieFolders: Boolean,
    val moveInsteadOfCopy: Boolean,
    val downloadImages: Boolean,
    val downloadPreviewImages: Boolean,
    val downloadWebPages: Boolean,
    val lockData: Boolean,
    val updateMode: Boolean,
    val autoScrape: Boolean,
    val fileLoggingEnabled: Boolean,
    val sites: List<SiteInfo>,
    val enabledSiteIds: List<String>,
    val siteMirrorUrls: Map<String, String>,
    val language: String,
    val showRestartHint: Boolean,
    val folderLayers: List<String>,
    val filenameFormat: String,
    val maxTitleLength: Int,
    val maxFilenameLength: Int,
    val suffixKeywords: List<String>,
    val requestTimeoutMs: Int,
    val siteCheckRunning: Boolean,
    val siteCheckResults: List<SiteCheckResult>?
)

data class SettingsActions(
    val onLanguageChange: (String) -> Unit,
    val onSelectOutputDir: () -> Unit,
    val onSelectScanDir: () -> Unit,
    val onSelectScanDirFromHistory: (String) -> Unit,
    val onSelectWorkerPath: () -> Unit,
    val onWorkerPathChange: (String) -> Unit,
    val onScanRecursiveChange: (Boolean) -> Unit,
    val onCreateMovieFoldersChange: (Boolean) -> Unit,
    val onMoveInsteadOfCopyChange: (Boolean) -> Unit,
    val onDownloadImagesChange: (Boolean) -> Unit,
    val onDownloadPreviewImagesChange: (Boolean) -> Unit,
    val onDownloadWebPagesChange: (Boolean) -> Unit,
    val onLockDataChange: (Boolean) -> Unit,
    val onUpdateModeChange: (Boolean) -> Unit,
    val onAutoScrapeChange: (Boolean) -> Unit,
    val onFileLoggingChange: (Boolean) -> Unit,
    val onToggleSite: (String, Boolean) -> Unit,
    val onSiteMirrorChange: (String, String) -> Unit,
    val onReset: () -> Unit,
    val onFolderLayerChange: (Int, String) -> Unit,
    val onAddLayer: () -> Unit,
    val onRemoveLayer: (Int) -> Unit,
    val onFilenameFormatChange: (String) -> Unit,
    val onMaxTitleLengthChange: (Int) -> Unit,
    val onMaxFilenameLengthChange: (Int) -> Unit,
    val onSuffixKeywordsChange: (List<String>) -> Unit,
    val onRequestTimeoutMsChange: (Int) -> Unit,
    val onCheckSites: () -> Unit
)

/** Settings categories rendered as floating tabs at the top of the screen. */
private enum class SettingsCategory(val icon: ImageVector) {
    DIRECTORIES(Icons.Filled.Folder),
    SCRAPING(Icons.Filled.Search),
    OTHER(Icons.Filled.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    actions: SettingsActions,
    modifier: Modifier = Modifier
) {
    val t = LocalTranslations.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text(t.commonSettings, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        FloatingSettingsTabs(selectedTab, t, onSelect = { selectedTab = it })
        Spacer(Modifier.height(16.dp))
        when (SettingsCategory.entries[selectedTab]) {
            SettingsCategory.DIRECTORIES -> DirectorySettingsTab(state, actions)
            SettingsCategory.SCRAPING -> ScrapingSettingsTab(state, actions)
            SettingsCategory.OTHER -> OtherSettingsTab(state, actions)
        }
    }
}

/** Floating tab bar shown above the settings content. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FloatingSettingsTabs(
    selectedTab: Int,
    t: TranslationEn,
    onSelect: (Int) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        SecondaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            divider = {}
        ) {
            SettingsCategory.entries.forEachIndexed { index, category ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { onSelect(index) },
                    text = { Text(categoryLabel(category, t)) },
                    icon = { Icon(category.icon, null) }
                )
            }
        }
    }
}

private fun categoryLabel(category: SettingsCategory, t: TranslationEn): String = when (category) {
    SettingsCategory.DIRECTORIES -> t.settingsTabDirectories
    SettingsCategory.SCRAPING -> t.settingsTabScraping
    SettingsCategory.OTHER -> t.settingsTabOther
}

@Composable
internal fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(label)
        Switch(checked, onCheckedChange = onCheckedChange)
    }
}

@Preview
@Composable
fun SettingsScreenPreview() {
    SettingsPreview {
        SettingsScreen(previewSettingsState(), previewSettingsActions())
    }
}
