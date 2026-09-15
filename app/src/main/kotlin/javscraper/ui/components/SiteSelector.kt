package javscraper.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import javscraper.i18n.LocalTranslations
import javscraper.i18n.TranslationEn
import javscraper.models.SiteCategory

data class SiteItem(
    val id: String,
    val name: String,
    val category: SiteCategory = SiteCategory.UNKNOWN,
    val enabled: Boolean = true
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SiteSelector(
    sites: List<SiteItem>,
    onToggle: (String, Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val t = LocalTranslations.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        groupSitesByCategory(sites).forEach { (category, categorySites) ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = siteCategoryLabel(category, t),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categorySites.forEach { site ->
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
        }
    }
}

internal fun groupSitesByCategory(
    sites: List<SiteItem>
): List<Pair<SiteCategory, List<SiteItem>>> =
    SiteCategory.entries.mapNotNull { category ->
        sites.filter { it.category == category }
            .takeIf { it.isNotEmpty() }
            ?.let { category to it }
    }

private fun siteCategoryLabel(category: SiteCategory, t: TranslationEn): String =
    when (category) {
        SiteCategory.CENSORED -> t.settingsSiteCategoryCensored
        SiteCategory.UNCENSORED -> t.settingsSiteCategoryUncensored
        SiteCategory.MIXED -> t.settingsSiteCategoryMixed
        SiteCategory.UNKNOWN -> t.settingsSiteCategoryUnknown
    }
