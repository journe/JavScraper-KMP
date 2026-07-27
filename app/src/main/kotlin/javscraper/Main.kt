package javscraper

import androidx.compose.runtime.remember
import javscraper.i18n.Translations
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import java.awt.Dimension

fun main() = application {
    val windowState = remember { WindowState(size = DpSize(1200.dp, 800.dp)) }
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = Translations.appTitle
    ) {
        window.minimumSize = Dimension(800, 600)
        App()
    }
}