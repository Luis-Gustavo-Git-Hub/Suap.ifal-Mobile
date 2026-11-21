package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance

// Light mode colors
val Background = Color(0xFFF3F4F6) // Light gray for background
val Surface = Color(0xFFFFFFFF) // Pure white for cards
val SurfaceVariant = Color(0xFFE5E7EB) // Slightly darker gray for variant cards
val OnBackground = Color(0xFF1B1B1F)
val OnSurfaceVariant = Color(0xFF4B5563)

val PrimaryBlue = Color(0xFF0061A4)
val PrimaryBlueContainer = Color(0xFFD1E4FF)
val OnPrimaryBlueContainer = Color(0xFF001D36)

val TertiaryPurple = Color(0xFF6750A4)
val TertiaryPurpleContainer = Color(0xFFF3F0FF)
val OnTertiaryPurpleContainer = Color(0xFF21005D)

val SecondaryAmber = Color(0xFFF59E0B)
val SecondaryAmberContainer = Color(0xFFFEF3C7)
val OnSecondaryAmberContainer = Color(0xFF78350F)

val SecondaryBrown = SecondaryAmber
val SecondaryYellowContainer = SecondaryAmberContainer
val OnSecondaryYellowContainer = OnSecondaryAmberContainer

val SecondaryTeal = Color(0xFF006A60)
val SecondaryTealContainer = Color(0xFFD7EFE9)
val OnSecondaryTealContainer = Color(0xFF00201C)

val SuccessGreen = Color(0xFF2E7D32)
val SuccessGreenContainer = Color(0xFFE8F5E9)
val OnSuccessGreenContainer = Color(0xFF1B5E20)

// Academic Rank Colors
val AcademicRankLevel1 = Color(0xFF81C784)
val AcademicRankLevel2 = Color(0xFF64B5F6)
val AcademicRankLevel3 = Color(0xFFBA68C8)
val AcademicRankLevel4 = Color(0xFFFFB74D)
val AcademicRankLevel5 = Color(0xFFFFD54F)

/** Preserve the chosen palette, correcting only unreadable foreground/background pairs. */
internal fun androidx.compose.material3.ColorScheme.withAccessibleContentColors(): androidx.compose.material3.ColorScheme {
    fun readable(foreground: Color, background: Color): Color {
        val opaque = background.compositeOver(surface)
        fun contrast(a: Color, b: Color): Float {
            val x = a.luminance()
            val y = b.luminance()
            return (maxOf(x, y) + 0.05f) / (minOf(x, y) + 0.05f)
        }
        if (contrast(foreground.compositeOver(opaque), opaque) >= 4.5f) return foreground
        return if (contrast(Color.Black, opaque) >= contrast(Color.White, opaque)) Color.Black else Color.White
    }
    return copy(
        onPrimary = readable(onPrimary, primary),
        onPrimaryContainer = readable(onPrimaryContainer, primaryContainer),
        onSecondary = readable(onSecondary, secondary),
        onSecondaryContainer = readable(onSecondaryContainer, secondaryContainer),
        onTertiary = readable(onTertiary, tertiary),
        onTertiaryContainer = readable(onTertiaryContainer, tertiaryContainer),
        onError = readable(onError, error),
        onErrorContainer = readable(onErrorContainer, errorContainer),
        onSurface = readable(onSurface, surface),
        onSurfaceVariant = readable(onSurfaceVariant, surfaceVariant),
        onBackground = readable(onBackground, background)
    )
}
