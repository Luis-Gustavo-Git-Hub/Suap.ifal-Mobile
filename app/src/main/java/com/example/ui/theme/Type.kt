package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// Bundled Google Fonts from res/font/
val OutfitFontFamily = FontFamily(Font(R.font.outfit, FontWeight.Normal))
val PlusJakartaFontFamily = FontFamily(Font(R.font.plus_jakarta_sans, FontWeight.Normal))
val SpaceGroteskFontFamily = FontFamily(Font(R.font.space_grotesk, FontWeight.Normal))
val FiraCodeFontFamily = FontFamily(Font(R.font.fira_code, FontWeight.Normal))
val NunitoFontFamily = FontFamily(Font(R.font.nunito, FontWeight.Normal))
val MontserratFontFamily = FontFamily(Font(R.font.montserrat, FontWeight.Normal))
val FredokaFontFamily = FontFamily(Font(R.font.fredoka, FontWeight.Normal))
val CinzelFontFamily = FontFamily(Font(R.font.cinzel, FontWeight.Normal))

fun getFontFamily(font: AppFontFamily): FontFamily {
    return when (font) {
        AppFontFamily.DEFAULT -> FontFamily.Default
        AppFontFamily.OUTFIT -> OutfitFontFamily
        AppFontFamily.PLUS_JAKARTA -> PlusJakartaFontFamily
        AppFontFamily.SPACE_GROTESK -> SpaceGroteskFontFamily
        AppFontFamily.FIRA_CODE -> FiraCodeFontFamily
        AppFontFamily.NUNITO -> NunitoFontFamily
        AppFontFamily.MONTSERRAT -> MontserratFontFamily
        AppFontFamily.FREDOKA -> FredokaFontFamily
        AppFontFamily.CINZEL -> CinzelFontFamily
    }
}

fun getTypographyForTheme(appTheme: AppTheme, fontOverride: AppFontFamily? = null): Typography {
    val targetFont = fontOverride ?: when (appTheme) {
        is AppTheme.SYSTEM -> AppFontFamily.DEFAULT
        is AppTheme.LIGHT -> AppFontFamily.PLUS_JAKARTA
        is AppTheme.DARK -> AppFontFamily.OUTFIT
        is AppTheme.DEEP_BLUE -> AppFontFamily.PLUS_JAKARTA
        is AppTheme.NATURE -> AppFontFamily.NUNITO
        is AppTheme.AMOLED -> AppFontFamily.CINZEL
        is AppTheme.DRACULA -> AppFontFamily.FIRA_CODE
        is AppTheme.SUNSET -> AppFontFamily.MONTSERRAT
        is AppTheme.CYBERPUNK -> AppFontFamily.SPACE_GROTESK
        is AppTheme.ROBIN_KAWA -> AppFontFamily.FREDOKA
        is AppTheme.Custom -> appTheme.data.fontFamily
    }
    return createAppTypography(getFontFamily(targetFont), targetFont)
}

fun createAppTypography(fontFamily: FontFamily, font: AppFontFamily = AppFontFamily.DEFAULT): Typography {
    // Calibrate font size and letter spacing per font type to prevent line wrapping & text overflow on low DPI/narrow screens
    return when (font) {
        AppFontFamily.FIRA_CODE -> {
            // Monospace has wide fixed-width character glyphs -> tighter tracking and scaled font sizes
            Typography(
                displayLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 46.sp, lineHeight = 52.sp, letterSpacing = (-0.5).sp),
                displayMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 38.sp, lineHeight = 44.sp, letterSpacing = (-0.3).sp),
                displaySmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.2).sp),
                headlineLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.2).sp),
                headlineMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp),
                headlineSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 24.sp, letterSpacing = (-0.2).sp),
                titleLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = (-0.2).sp),
                titleMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 19.sp, letterSpacing = (-0.3).sp),
                titleSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = (-0.3).sp),
                bodyLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 13.5.sp, lineHeight = 19.sp, letterSpacing = (-0.3).sp),
                bodyMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = (-0.3).sp),
                bodySmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = (-0.3).sp),
                labelLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = (-0.3).sp),
                labelMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = (-0.3).sp),
                labelSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = (-0.3).sp)
            )
        }
        AppFontFamily.CINZEL -> {
            // Classical Roman Display font -> compact scale for body/labels with neutral tracking
            Typography(
                displayLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 48.sp, lineHeight = 54.sp, letterSpacing = (-0.2).sp),
                displayMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 40.sp, lineHeight = 46.sp, letterSpacing = (-0.2).sp),
                displaySmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = 0.sp),
                headlineLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 27.sp, lineHeight = 33.sp, letterSpacing = 0.sp),
                headlineMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 23.sp, lineHeight = 29.sp, letterSpacing = 0.sp),
                headlineSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 25.sp, letterSpacing = 0.sp),
                titleLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 25.sp, letterSpacing = 0.sp),
                titleMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
                titleSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, lineHeight = 17.sp, letterSpacing = 0.sp),
                bodyLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = (-0.1).sp),
                bodyMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 17.sp, letterSpacing = (-0.1).sp),
                bodySmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = (-0.1).sp),
                labelLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 12.5.sp, lineHeight = 17.sp, letterSpacing = (-0.1).sp),
                labelMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = (-0.1).sp),
                labelSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = (-0.1).sp)
            )
        }
        AppFontFamily.SPACE_GROTESK, AppFontFamily.MONTSERRAT, AppFontFamily.FREDOKA -> {
            // Extended geometric/rounded fonts -> slightly tighter letter-spacing and tailored sizes
            Typography(
                displayLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 52.sp, lineHeight = 58.sp, letterSpacing = (-0.3).sp),
                displayMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 42.sp, lineHeight = 48.sp, letterSpacing = (-0.2).sp),
                displaySmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = 0.sp),
                headlineLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 29.sp, lineHeight = 36.sp, letterSpacing = (-0.1).sp),
                headlineMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 25.sp, lineHeight = 32.sp, letterSpacing = (-0.1).sp),
                headlineSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 27.sp, letterSpacing = (-0.1).sp),
                titleLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.1).sp),
                titleMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 21.sp, letterSpacing = (-0.1).sp),
                titleSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = (-0.1).sp),
                bodyLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 14.5.sp, lineHeight = 21.sp, letterSpacing = (-0.1).sp),
                bodyMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = (-0.1).sp),
                bodySmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = (-0.1).sp),
                labelLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = (-0.1).sp),
                labelMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = (-0.1).sp),
                labelSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = (-0.1).sp)
            )
        }
        else -> {
            // Standard / Plus Jakarta / Outfit / Nunito
            Typography(
                displayLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 54.sp, lineHeight = 60.sp, letterSpacing = (-0.25).sp),
                displayMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 44.sp, lineHeight = 50.sp, letterSpacing = 0.sp),
                displaySmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 35.sp, lineHeight = 42.sp, letterSpacing = 0.sp),
                headlineLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 38.sp, letterSpacing = 0.sp),
                headlineMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 34.sp, letterSpacing = 0.sp),
                headlineSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
                titleLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 27.sp, letterSpacing = 0.sp),
                titleMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 15.5.sp, lineHeight = 22.sp, letterSpacing = 0.sp),
                titleSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, lineHeight = 18.sp, letterSpacing = 0.sp),
                bodyLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = 0.15.sp),
                bodyMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 13.5.sp, lineHeight = 19.sp, letterSpacing = 0.1.sp),
                bodySmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 11.5.sp, lineHeight = 15.sp, letterSpacing = 0.1.sp),
                labelLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 13.5.sp, lineHeight = 18.sp, letterSpacing = 0.1.sp),
                labelMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 11.5.sp, lineHeight = 15.sp, letterSpacing = 0.1.sp),
                labelSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.1.sp)
            )
        }
    }
}

val Typography = createAppTypography(FontFamily.Default)
