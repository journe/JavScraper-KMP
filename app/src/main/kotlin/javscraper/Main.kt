package javscraper

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import javscraper.i18n.LocalTranslations
import java.awt.Dimension
import javax.imageio.ImageIO

fun main() = application {
    val windowState = remember { WindowState(size = DpSize(1280.dp, 800.dp)) }
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = LocalTranslations.current.appTitle
    ) {
        window.minimumSize = Dimension(800, 600)
        javaClass.getResourceAsStream("/icon/app_icon.png")?.let {
            window.iconImage = ImageIO.read(it)
        }
        App()
    }
}
