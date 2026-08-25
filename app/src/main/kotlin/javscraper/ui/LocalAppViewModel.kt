package javscraper.ui

import androidx.compose.runtime.staticCompositionLocalOf
import javscraper.AppViewModel

/**
 * Provides the app-wide [AppViewModel] to screen composables,
 * mirroring the [javscraper.i18n.LocalTranslations] pattern.
 */
val LocalAppViewModel = staticCompositionLocalOf<AppViewModel> {
    error("LocalAppViewModel not provided")
}