package javscraper.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import javscraper.Screen
import javscraper.i18n.LocalTranslations
import javscraper.i18n.TranslationEn
import javscraper.ui.theme.JavScraperTheme

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
        targetValue = if (expanded) 160.dp else 72.dp,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
    )

    NavigationRail(
        modifier = Modifier.width(navWidth),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Spacer(Modifier.height(8.dp))
        NavRailItem(
            icon = Icons.Default.Search,
            label = t.navScan,
            selected = currentScreen == Screen.SCAN,
            expanded = expanded,
            onClick = { onNavigate(Screen.SCAN) }
        )
        NavRailItem(
            icon = if (scrapeEnabled) Icons.Default.CloudDownload else Icons.Default.CloudOff,
            label = t.navScrape,
            selected = currentScreen == Screen.PROGRESS,
            expanded = expanded,
            enabled = scrapeEnabled,
            onClick = { onNavigate(Screen.PROGRESS) }
        )
        NavRailItem(
            icon = Icons.Default.PhotoLibrary,
            label = t.navGallery,
            selected = currentScreen == Screen.GALLERY,
            expanded = expanded,
            enabled = galleryEnabled,
            onClick = { onNavigate(Screen.GALLERY) }
        )
    }
}

@Composable
private fun NavRailItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    expanded: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val backgroundColor = if (selected) colors.secondaryContainer else Color.Transparent
    val contentColor = when {
        !enabled -> colors.onSurfaceVariant.copy(alpha = 0.38f)
        selected -> colors.onSecondaryContainer
        else -> colors.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .selectable(selected = selected, enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = contentColor)
        AnimatedVisibility(
            visible = expanded,
            enter = expandHorizontally() + fadeIn(),
            exit = shrinkHorizontally() + fadeOut(),
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(start = 12.dp),
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview
@Composable
fun CollapsibleNavRailPreviewCollapsed() {
    val t = TranslationEn()
    CompositionLocalProvider(LocalTranslations provides t) {
        JavScraperTheme {
            CollapsibleNavRail(
                expanded = false,
                currentScreen = Screen.SCAN,
                onNavigate = {},
                scrapeEnabled = true,
                galleryEnabled = false
            )
        }
    }
}

@Preview
@Composable
fun CollapsibleNavRailPreviewExpanded() {
    val t = TranslationEn()
    CompositionLocalProvider(LocalTranslations provides t) {
        JavScraperTheme {
            CollapsibleNavRail(
                expanded = true,
                currentScreen = Screen.GALLERY,
                onNavigate = {},
                scrapeEnabled = true,
                galleryEnabled = true
            )
        }
    }
}

