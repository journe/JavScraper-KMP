package javscraper.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import javscraper.Screen
import javscraper.i18n.LocalTranslations
import javscraper.i18n.TranslationEn
import javscraper.ui.theme.JavScraperTheme

fun main() = application {
    val windowState = remember { WindowState(size = DpSize(400.dp, 500.dp)) }
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "CollapsibleNavRail Preview"
    ) {
        JavScraperTheme {
            Surface(modifier = Modifier.padding(0.dp)) {
                PreviewContent()
            }
        }
    }
}

@Composable
private fun PreviewContent() {
    val t = TranslationEn()
    var expanded by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf(Screen.SCAN) }
    var scrapeEnabled by remember { mutableStateOf(false) }
    var galleryEnabled by remember { mutableStateOf(false) }

    androidx.compose.runtime.CompositionLocalProvider(LocalTranslations provides t) {
        Row {
            CollapsibleNavRail(
                expanded = expanded,
                currentScreen = currentScreen,
                onNavigate = { currentScreen = it },
                scrapeEnabled = scrapeEnabled,
                galleryEnabled = galleryEnabled
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
            ) {
                HorizontalDivider()
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Screen: ${currentScreen.name}",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    }
}
