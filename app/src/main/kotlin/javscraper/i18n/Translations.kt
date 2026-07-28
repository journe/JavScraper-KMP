package javscraper.i18n

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal that provides the current [TranslationStrings] instance
 * down the composable tree. Replace this value via [CompositionLocalProvider]
 * when the user changes language.
 *
 * Usage inside any @Composable:
 *   val t = LocalTranslations.current
 *   Text(t.scanTitle)
 */
val LocalTranslations = staticCompositionLocalOf { TranslationStrings() }
