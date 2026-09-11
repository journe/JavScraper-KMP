package javscraper.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * App 层 SharedTransitionLayout 的 scope,通过 CompositionLocal 下发。
 *
 * 官方建议:嵌套层级较深时用 CompositionLocal 而非参数逐层透传,
 * 避免中间组件仅为转发而携带参数。未提供时为 null,消费方退化为无转场。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }
