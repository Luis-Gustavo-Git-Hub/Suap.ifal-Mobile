package com.example.ui.components

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal providing current WindowSizeClass for adaptive layouts.
 */
val LocalWindowSizeClass = compositionLocalOf<WindowSizeClass?> { null }

/**
 * Returns true if the screen is Medium or Expanded width (tablets, foldables unfolded, landscape).
 */
@Composable
fun isWideScreen(): Boolean {
    val windowSize = LocalWindowSizeClass.current
    return windowSize?.widthSizeClass == WindowWidthSizeClass.Medium ||
        windowSize?.widthSizeClass == WindowWidthSizeClass.Expanded
}

/**
 * Returns true only if the screen is Expanded width (>= 840dp, e.g. large tablets / desktop).
 */
@Composable
fun isExpandedScreen(): Boolean {
    val windowSize = LocalWindowSizeClass.current
    return windowSize?.widthSizeClass == WindowWidthSizeClass.Expanded
}
