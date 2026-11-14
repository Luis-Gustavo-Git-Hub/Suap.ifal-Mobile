package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContrastTest {
    private fun contrast(a: Color, b: Color): Float {
        val x = a.luminance(); val y = b.luminance()
        return (maxOf(x, y) + .05f) / (minOf(x, y) + .05f)
    }
    @Test fun allThemesAndCustomAccentsHaveReadableRoles() {
        val themes = AppTheme.values() + ThemeColor.values().flatMap { primary ->
            ThemeColor.values().flatMap { profile -> listOf(false, true).map { dark ->
                AppTheme.Custom(CustomThemeData("test", "Test", primary, primary, profile, dark))
            } }
        }
        for (theme in themes) for (systemDark in listOf(false, true)) {
            val c = appColorScheme(theme, systemDark)
            val pairs = listOf(c.primary to c.onPrimary, c.primaryContainer to c.onPrimaryContainer,
                c.secondary to c.onSecondary, c.secondaryContainer to c.onSecondaryContainer,
                c.tertiary to c.onTertiary, c.tertiaryContainer to c.onTertiaryContainer,
                c.surface to c.onSurface, c.background to c.onBackground,
                c.error to c.onError, c.errorContainer to c.onErrorContainer,
                c.surfaceVariant to c.onSurfaceVariant, c.surfaceContainerHighest to c.onSurface,
                c.background to c.primary, c.surface to c.primary,
                c.background to c.secondary, c.surface to c.secondary,
                c.background to c.tertiary, c.surface to c.tertiary)
            pairs.forEach { (background, text) ->
                assertTrue("${theme.id}: contrast ${contrast(background, text)}", contrast(background, text) >= 4.5f)
            }
        }
    }
    @Test fun semanticContainersAndBodyTextMeetAA() {
        for (dark in listOf(false, true)) {
            val c = semanticColors(dark)
            for (tone in listOf(c.success, c.warning, c.danger, c.info)) {
                assertTrue(contrast(tone.container, tone.onContainer) >= 4.5f)
                assertTrue(contrast(tone.color, if (dark) Color(0xFF121212) else Color.White) >= 4.5f)
            }
        }
    }
}
