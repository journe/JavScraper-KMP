package javscraper.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class SiteItem(val id: String, val name: String, val enabled: Boolean = true)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SiteSelector(
    sites: List<SiteItem>,
    onToggle: (String, Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sites.forEach { site ->
            FilterChip(
                selected = site.enabled,
                onClick = { onToggle(site.id, !site.enabled) },
                label = { Text(site.name) },
                leadingIcon = if (site.enabled) {
                    {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else {
                    null
                }
            )
        }
    }
}