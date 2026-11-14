package com.example.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/** Choose readable text even for very light or very dark custom accents. */
fun readableOn(color: Color): Color = if (color.luminance() > 0.179f) Color.Black else Color.White

private fun accessibleAccent(color: Color, dark: Boolean, background: Color = if (dark) Color(0xFF121212) else Color.White): Color {
    var result = color
    repeat(20) {
        val a = result.luminance()
        val b = background.luminance()
        if ((maxOf(a, b) + .05f) / (minOf(a, b) + .05f) >= 4.5f) return result
        result = lerp(result, if (dark) Color.White else Color.Black, .1f)
    }
    return result
}

/** Every scheme starts with all M3 roles, then derives the accent and surface families. */
fun appColorScheme(theme: AppTheme, systemDark: Boolean = false): ColorScheme {
    val dark = when (theme) {
        AppTheme.SYSTEM -> systemDark
        AppTheme.LIGHT, AppTheme.NATURE, AppTheme.SUNSET -> false
        is AppTheme.Custom -> theme.data.isDark
        else -> true
    }
    val seed = when (theme) {
        AppTheme.NATURE -> Color(0xFF2E7D32)
        AppTheme.DRACULA -> Color(0xFFBD93F9)
        AppTheme.SUNSET -> Color(0xFFFF7043)
        AppTheme.CYBERPUNK -> Color(0xFFF000FF)
        is AppTheme.Custom -> Color(theme.data.primaryColor.hex)
        else -> Color(0xFF0061A4)
    }
    val background = when (theme) {
        AppTheme.AMOLED -> Color.Black
        AppTheme.DEEP_BLUE -> Color(0xFF0A192F)
        AppTheme.DRACULA -> Color(0xFF282A36)
        AppTheme.CYBERPUNK -> Color(0xFF000B18)
        AppTheme.NATURE -> Color(0xFFF1F8E9)
        AppTheme.SUNSET -> Color(0xFFFFF3E0)
        else -> if (dark) Color(0xFF121212) else Color(0xFFF8F9FA)
    }
    val surface = if (dark) lerp(background, Color.White, .04f) else Color.White
    val accentBackground = if (dark) surface else background
    val primary = accessibleAccent(seed, dark, accentBackground)
    val secondary = accessibleAccent(lerp(seed, if (dark) Color(0xFFFFCA80) else Color(0xFF705D42), .55f), dark, accentBackground)
    val tertiary = accessibleAccent(lerp(seed, Color(0xFF8F65B5), .65f), dark, accentBackground)
    val containerSeed = (theme as? AppTheme.Custom)?.data?.profileColor?.let { Color(it.hex) } ?: seed
    fun container(color: Color) = lerp(surface, color, if (dark) .22f else .12f)
    val pc = container(containerSeed)
    val sc = container(secondary)
    val tc = container(tertiary)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val onSurface = readableOn(surface)
    return base.copy(
        primary = primary, onPrimary = readableOn(primary), primaryContainer = pc, onPrimaryContainer = readableOn(pc),
        inversePrimary = accessibleAccent(seed, !dark),
        secondary = secondary, onSecondary = readableOn(secondary), secondaryContainer = sc, onSecondaryContainer = readableOn(sc),
        tertiary = tertiary, onTertiary = readableOn(tertiary), tertiaryContainer = tc, onTertiaryContainer = readableOn(tc),
        background = background, onBackground = readableOn(background), surface = surface, onSurface = onSurface,
        surfaceVariant = container(onSurface), onSurfaceVariant = readableOn(container(onSurface)),
        surfaceTint = primary, inverseSurface = onSurface, inverseOnSurface = surface,
        outline = if (dark) Color(0xFF99939F) else Color(0xFF79747E),
        outlineVariant = if (dark) Color(0xFF49454F) else Color(0xFFCAC4D0),
        error = if (dark) Color(0xFFFFB4AB) else Color(0xFFBA1A1A),
        onError = if (dark) Color(0xFF690005) else Color.White,
        errorContainer = if (dark) Color(0xFF93000A) else Color(0xFFFFDAD6),
        onErrorContainer = if (dark) Color(0xFFFFDAD6) else Color(0xFF410002), scrim = Color.Black,
        surfaceDim = lerp(surface, Color.Black, .08f), surfaceBright = lerp(surface, Color.White, .08f),
        surfaceContainerLowest = background, surfaceContainerLow = surface,
        surfaceContainer = lerp(surface, onSurface, .04f), surfaceContainerHigh = lerp(surface, onSurface, .07f),
        surfaceContainerHighest = lerp(surface, onSurface, .1f)
    )
}

data class SemanticTone(val color: Color, val container: Color, val onContainer: Color)
data class AppColors(val success: SemanticTone, val warning: SemanticTone, val danger: SemanticTone, val info: SemanticTone) {
    val gradeDistribution: List<Color> get() = listOf(success.color, info.color, warning.color, danger.color)
}
fun semanticColors(dark: Boolean): AppColors {
    fun tone(light: Long, night: Long): SemanticTone {
        val color = Color(if (dark) night else light)
        val container = lerp(if (dark) Color(0xFF121212) else Color.White, color, .15f)
        return SemanticTone(color, container, readableOn(container))
    }
    return AppColors(tone(0xFF216E39, 0xFF81D995), tone(0xFF805600, 0xFFFFD166),
        tone(0xFFBA1A1A, 0xFFFFB4AB), tone(0xFF005EA8, 0xFF9ECAFF))
}
val LocalAppColors = staticCompositionLocalOf { semanticColors(false) }
val MaterialTheme.appColors: AppColors
    @androidx.compose.runtime.Composable get() = LocalAppColors.current
