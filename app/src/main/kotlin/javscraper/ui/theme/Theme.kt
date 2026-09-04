package javscraper.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Dark = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF0D47A1),
    primaryContainer = Color(0xFF1E3A5F),
    secondary = Color(0xFFCE93D8),
    tertiary = Color(0xFFA5D6A7),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2C2C2C),
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
    error = Color(0xFFEF5350)
)
private val Light = lightColorScheme(
    primary = Color(0xFF1565C0), onPrimary = Color.White, primaryContainer = Color(0xFFBBDEFB),
    secondary = Color(0xFF7B1FA2), tertiary = Color(0xFF388E3C), background = Color(0xFFF5F5F5),
    surface = Color.White, surfaceVariant = Color(0xFFE8E8E8), onBackground = Color(0xFF212121),
    onSurface = Color(0xFF212121), error = Color(0xFFD32F2F)
)

@Composable
fun JavScraperTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) Dark else Light, content = content)
}