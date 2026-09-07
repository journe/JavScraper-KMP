package javscraper.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import javscraper.i18n.LocalTranslations
import javscraper.i18n.TranslationEn
import javscraper.models.SiteCheckResult
import javscraper.ui.components.SiteItem
import javscraper.ui.components.SiteSelector

/** Scraper sites and image download behavior. */
@Composable
fun ScrapingSettingsTab(state: SettingsState, actions: SettingsActions) {
    val t = LocalTranslations.current
    var checkDialogVisible by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(t.commonScraperSites, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        SiteSelector(
            sites = state.sites.map { SiteItem(it.id, it.name, it.id in state.enabledSiteIds) },
            onToggle = actions.onToggleSite
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { checkDialogVisible = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Link, null)
            Spacer(Modifier.width(8.dp))
            Text(t.settingsTestSites)
        }
        Spacer(Modifier.height(16.dp))
        SettingsSwitchRow(t.settingsDownloadImages, state.downloadImages, actions.onDownloadImagesChange)
        SettingsSwitchRow(t.settingsDownloadWebPages, state.downloadWebPages, actions.onDownloadWebPagesChange)
        Spacer(Modifier.height(32.dp))
    }
    if (checkDialogVisible) {
        SiteCheckDialog(
            running = state.siteCheckRunning,
            results = state.siteCheckResults,
            siteNames = state.sites.associate { it.id to it.name },
            onRetry = actions.onCheckSites,
            onDismiss = { checkDialogVisible = false }
        )
    }
}

@Composable
private fun SiteCheckDialog(
    running: Boolean,
    results: List<SiteCheckResult>?,
    siteNames: Map<String, String>,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val t = LocalTranslations.current
    LaunchedEffect(Unit) { onRetry() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t.settingsTestSitesTitle) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (running) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(t.settingsTestSitesRunning)
                    }
                } else if (results == null || results.isEmpty()) {
                    Text(t.settingsTestSitesEmpty)
                } else {
                    results.forEach { result ->
                        SiteCheckRow(result, siteNames[result.id] ?: result.id, t)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onRetry, enabled = !running) { Text(t.settingsTestSitesRetry) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(t.settingsTestSitesClose) }
        }
    )
}

@Composable
private fun SiteCheckRow(result: SiteCheckResult, displayName: String, t: TranslationEn) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (result.ok) {
            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
        } else {
            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(displayName, fontWeight = FontWeight.Medium)
            Text(
                if (result.ok) t.settingsTestSitesOk else (result.error ?: t.settingsTestSitesFail),
                style = MaterialTheme.typography.bodySmall,
                color = if (result.ok) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.error
            )
        }
        result.latencyMs?.let { ms ->
            Text(
                t.settingsTestSitesLatency(ms),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
fun ScrapingSettingsTabPreview() {
    SettingsPreview {
        ScrapingSettingsTab(previewSettingsState(), previewSettingsActions())
    }
}