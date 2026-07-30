package javscraper.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import javscraper.Screen
import javscraper.i18n.LocalTranslations

@Composable
fun CollapsibleNavRail(
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
