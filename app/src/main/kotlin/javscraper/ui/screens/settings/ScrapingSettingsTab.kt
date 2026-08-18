package javscraper.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import javscraper.i18n.LocalTranslations
import javscraper.ui.components.SiteItem
import javscraper.ui.components.SiteSelector

/** Scraper sites and image download behavior. */
@Composable
fun ScrapingSettingsTab(state: SettingsState, actions: SettingsActions) {
    val t = LocalTranslations.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(t.settingsScraperSites, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        SiteSelector(
            sites = state.sites.map { SiteItem(it.id, it.name, it.id in state.enabledSiteIds) },
            onToggle = actions.onToggleSite
        )
        Spacer(Modifier.height(16.dp))
        SettingsSwitchRow(t.settingsDownloadImages, state.downloadImages, actions.onDownloadImagesChange)
        Spacer(Modifier.height(32.dp))
    }
}

@Preview
@Composable
fun ScrapingSettingsTabPreview() {
    SettingsPreview {
        ScrapingSettingsTab(previewSettingsState(), previewSettingsActions())
    }
}