package javscraper.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import javscraper.i18n.LocalTranslations
import javscraper.models.SiteInfo
import javscraper.settings.isValidSiteMirrorUrl

internal data class SiteMirrorDisplayItem(
    val id: String,
    val name: String,
    val baseUrl: String,
    val effectiveUrl: String
)

internal data class SiteMirrorChoice(
    val value: String,
    val isDefault: Boolean
)

internal fun mirrorSettingsAvailable(sites: List<SiteInfo>): Boolean =
    sites.any { it.baseUrl.isNotBlank() }

internal fun siteMirrorDisplayItems(
    sites: List<SiteInfo>,
    siteMirrorUrls: Map<String, String>
): List<SiteMirrorDisplayItem> = sites
    .filter { it.baseUrl.isNotBlank() }
    .map { site ->
        SiteMirrorDisplayItem(
            id = site.id,
            name = site.name,
            baseUrl = site.baseUrl,
            effectiveUrl = siteMirrorUrls[site.id]?.takeIf { it.isNotBlank() } ?: site.baseUrl
        )
    }

internal fun siteMirrorChoices(site: SiteInfo, configuredMirror: String?): List<SiteMirrorChoice> =
    sequenceOf(site.baseUrl)
        .plus(site.mirrorUrls)
        .plus(listOfNotNull(configuredMirror?.takeIf { it.isNotBlank() && it != site.baseUrl }))
        .filter { it.isNotBlank() }
        .distinct()
        .map { SiteMirrorChoice(it, it == site.baseUrl) }
        .toList()

@Composable
fun SiteMirrorSettings(
    sites: List<SiteInfo>,
    siteMirrorUrls: Map<String, String>,
    onSiteMirrorChange: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
) {
    val t = LocalTranslations.current
    var editingSiteId by remember { mutableStateOf<String?>(null) }
    val items = siteMirrorDisplayItems(sites, siteMirrorUrls)

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (showTitle) {
            Text(
                t.settingsMirrorUrls,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        items.forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(item.name, fontWeight = FontWeight.Medium)
                    Text(
                        item.effectiveUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { editingSiteId = item.id }) {
                    Icon(Icons.Default.Edit, contentDescription = t.settingsMirrorUrls)
                }
            }
        }
    }

    editingSiteId?.let { siteId ->
        sites.firstOrNull { it.id == siteId }?.let { site ->
            SiteMirrorDialog(
                site = site,
                configuredMirror = siteMirrorUrls[site.id],
                onSave = { url ->
                    onSiteMirrorChange(site.id, url)
                    editingSiteId = null
                },
                onDismiss = { editingSiteId = null }
            )
        }
    }
}

@Composable
private fun SiteMirrorDialog(
    site: SiteInfo,
    configuredMirror: String?,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val t = LocalTranslations.current
    val choices = remember(site, configuredMirror) { siteMirrorChoices(site, configuredMirror) }
    var selectedUrl by remember(site.id) {
        mutableStateOf(configuredMirror?.takeIf { it.isNotBlank() } ?: site.baseUrl)
    }
    var customUrl by remember(site.id) {
        mutableStateOf(configuredMirror?.takeIf { mirror -> mirror.isNotBlank() && choices.none { it.value == mirror } } ?: "")
    }
    val selectedIsKnown = selectedUrl == site.baseUrl || selectedUrl in site.mirrorUrls
    val customIsValid = customUrl.isBlank() || isValidSiteMirrorUrl(customUrl)
    val canSave = selectedIsKnown || (customUrl.isNotBlank() && customIsValid)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${site.name} - ${t.settingsMirrorUrls}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                choices.forEach { choice ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = selectedUrl == choice.value,
                            onClick = {
                                selectedUrl = choice.value
                                customUrl = ""
                            }
                        )
                        Column {
                            if (choice.isDefault) {
                                Text(t.settingsMirrorDefault, style = MaterialTheme.typography.labelSmall)
                            }
                            Text(choice.value, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = customUrl,
                    onValueChange = {
                        customUrl = it
                        selectedUrl = it
                    },
                    label = { Text(t.settingsMirrorCustom) },
                    isError = customUrl.isNotBlank() && !customIsValid,
                    supportingText = if (customUrl.isNotBlank() && !customIsValid) {
                        { Text(t.settingsMirrorInvalid) }
                    } else {
                        null
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(if (selectedUrl == site.baseUrl) "" else selectedUrl) },
                enabled = canSave
            ) { Text(t.commonConfirm) }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        selectedUrl = site.baseUrl
                        customUrl = ""
                    }
                ) { Text(t.commonResetToDefaults) }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = onDismiss) { Text(t.commonCancel) }
            }
        }
    )
}
