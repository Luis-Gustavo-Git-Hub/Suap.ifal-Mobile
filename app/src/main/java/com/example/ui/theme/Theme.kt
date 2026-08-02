package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import androidx.compose.ui.graphics.Color

private val LightColorScheme =  lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueContainer,
    onPrimaryContainer = OnPrimaryBlueContainer,
    secondary = SecondaryBrown,
    secondaryContainer = SecondaryYellowContainer,
    onSecondaryContainer = OnSecondaryYellowContainer,
    tertiary = TertiaryPurple,
    tertiaryContainer = TertiaryPurpleContainer,
    onTertiaryContainer = OnTertiaryPurpleContainer,
    background = Background,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    onBackground = OnBackground,
    onSurface = OnBackground,
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFFFB951),
    secondaryContainer = Color(0xFF653E00),
    onSecondaryContainer = Color(0xFFFFDDB3),
    tertiary = Color(0xFFD0BCFF),
    tertiaryContainer = Color(0xFF4F378B),
    onTertiaryContainer = Color(0xFFEADDFF),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFCAC4D0),
    onBackground = Color(0xFFE2E2E6),
    onSurface = Color(0xFFE2E2E6),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val DeepBlueColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFF64B5F6),
    background = Color(0xFF0A192F),
    surface = Color(0xFF112240),
    surfaceVariant = Color(0xFF233554),
    onBackground = Color(0xFFCCD6F6),
    onSurface = Color(0xFFCCD6F6),
    onSurfaceVariant = Color(0xFF8892B0)
)

private val NatureColorScheme = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA5D6A7),
    onPrimaryContainer = Color(0xFF1B5E20),
    secondary = Color(0xFF8D6E63),
    background = Color(0xFFF1F8E9),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFDCEDC8),
    onBackground = Color(0xFF1B1B1B),
    onSurface = Color(0xFF1B1B1B),
    onSurfaceVariant = Color(0xFF33691E)
)

private val AmoledColorScheme = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFFFB951),
    background = Color.Black,
    surface = Color(0xFF050505),
    surfaceVariant = Color(0xFF121212),
    onBackground = Color(0xFFE2E2E6),
    onSurface = Color(0xFFE2E2E6),
    onSurfaceVariant = Color(0xFFCAC4D0)
)

private val DraculaColorScheme = darkColorScheme(
    primary = Color(0xFFBD93F9),
    onPrimary = Color(0xFF282A36),
    primaryContainer = Color(0xFF6272A4),
    onPrimaryContainer = Color(0xFFF8F8F2),
    secondary = Color(0xFF50FA7B),
    background = Color(0xFF282A36),
    surface = Color(0xFF44475A),
    surfaceVariant = Color(0xFF6272A4),
    onBackground = Color(0xFFF8F8F2),
    onSurface = Color(0xFFF8F8F2),
    onSurfaceVariant = Color(0xFFF8F8F2),
    error = Color(0xFFFF5555)
)

private val SunsetColorScheme = lightColorScheme(
    primary = Color(0xFFFF7043),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFCCBC),
    onPrimaryContainer = Color(0xFFBF360C),
    secondary = Color(0xFFFFCA28),
    background = Color(0xFFFFF3E0),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFFFE0B2),
    onBackground = Color(0xFF3E2723),
    onSurface = Color(0xFF3E2723),
    onSurfaceVariant = Color(0xFF5D4037)
)

private val CyberpunkColorScheme = darkColorScheme(
    primary = Color(0xFFF000FF),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF7A0080),
    onPrimaryContainer = Color(0xFFFFD1FF),
    secondary = Color(0xFF00F0FF),
    background = Color(0xFF000B18),
    surface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFF1E293B),
    onBackground = Color(0xFFE0E7FF),
    onSurface = Color(0xFFE0E7FF),
    onSurfaceVariant = Color(0xFF94A3B8)
)

@Composable
fun MyApplicationTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    dynamicColor: Boolean = false, // disabled for strict theme matching
    content: @Composable () -> Unit,
) {
    val isSystemDark = isSystemInDarkTheme()
    val colorScheme = when (appTheme) {
        is AppTheme.SYSTEM -> if (isSystemDark) DarkColorScheme else LightColorScheme
        is AppTheme.LIGHT -> LightColorScheme
        is AppTheme.DARK -> DarkColorScheme
        is AppTheme.DEEP_BLUE -> DeepBlueColorScheme
        is AppTheme.NATURE -> NatureColorScheme
        is AppTheme.AMOLED -> AmoledColorScheme
        is AppTheme.DRACULA -> DraculaColorScheme
        is AppTheme.SUNSET -> SunsetColorScheme
        is AppTheme.CYBERPUNK -> CyberpunkColorScheme
        is AppTheme.Custom -> {
            val primary = Color(appTheme.data.primaryColor.hex)
            val profile = appTheme.data.profileColor?.let { Color(it.hex) }
            if (appTheme.data.isDark) {
                darkColorScheme(
                    primary = primary,
                    onPrimary = Color.Black,
                    primaryContainer = profile ?: primary.copy(alpha = 0.5f),
                    onPrimaryContainer = profile?.copy(alpha = 0.8f) ?: primary.copy(alpha = 0.8f),
                    secondary = primary,
                    background = Color(0xFF121212),
                    surface = Color(0xFF1E1E1E),
                    surfaceVariant = Color(0xFF2C2C2C),
                    onBackground = Color(0xFFE2E2E6),
                    onSurface = Color(0xFFE2E2E6),
                    onSurfaceVariant = Color(0xFFCAC4D0)
                )
            } else {
                lightColorScheme(
                    primary = primary,
                    onPrimary = Color.White,
                    primaryContainer = profile ?: primary.copy(alpha = 0.2f),
                    onPrimaryContainer = profile ?: primary,
                    secondary = primary,
                    background = Color(0xFFF8F9FA),
                    surface = Color(0xFFFFFFFF),
                    surfaceVariant = Color(0xFFE7E0EC),
                    onBackground = Color(0xFF1B1B1B),
                    onSurface = Color(0xFF1B1B1B),
                    onSurfaceVariant = Color(0xFF49454F)
                )
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
