package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.luminance

@Composable
fun MyApplicationTheme(appTheme: AppTheme = AppTheme.SYSTEM, dynamicColor: Boolean = false, content: @Composable () -> Unit) {
    val scheme = appColorScheme(appTheme, isSystemInDarkTheme())
    CompositionLocalProvider(LocalAppColors provides semanticColors(scheme.background.luminance() < .179f)) {
        MaterialTheme(colorScheme = scheme, typography = Typography, shapes = AppShapes, content = content)
    }
}
