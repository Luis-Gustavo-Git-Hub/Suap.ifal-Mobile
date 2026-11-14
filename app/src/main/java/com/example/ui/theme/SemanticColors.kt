package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class SemanticColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val danger: Color,
    val onDanger: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color
)

val LocalSemanticColors = staticCompositionLocalOf {
    LightSemanticColors
}

val LightSemanticColors = SemanticColors(
    success = Color(0xFF1B873F),
    onSuccess = Color.White,
    successContainer = Color(0xFFD4F5DC),
    onSuccessContainer = Color(0xFF094D20),
    warning = Color(0xFFB57400),
    onWarning = Color.White,
    warningContainer = Color(0xFFFFF0D0),
    onWarningContainer = Color(0xFF5B3900),
    danger = Color(0xFFBA1A1A),
    onDanger = Color.White,
    dangerContainer = Color(0xFFFFDAD6),
    onDangerContainer = Color(0xFF410002),
    info = Color(0xFF0061A4),
    onInfo = Color.White,
    infoContainer = Color(0xFFD1E4FF),
    onInfoContainer = Color(0xFF001D36)
)

val DarkSemanticColors = SemanticColors(
    success = Color(0xFF66DB83),
    onSuccess = Color(0xFF003915),
    successContainer = Color(0xFF095324),
    onSuccessContainer = Color(0xFFB8F5C8),
    warning = Color(0xFFFFBA38),
    onWarning = Color(0xFF422B00),
    warningContainer = Color(0xFF634100),
    onWarningContainer = Color(0xFFFFDDB0),
    danger = Color(0xFFFFB4AB),
    onDanger = Color(0xFF690005),
    dangerContainer = Color(0xFF93000A),
    onDangerContainer = Color(0xFFFFDAD6),
    info = Color(0xFF9ECAFF),
    onInfo = Color(0xFF003258),
    infoContainer = Color(0xFF00497D),
    onInfoContainer = Color(0xFFD1E4FF)
)

val AmoledSemanticColors = SemanticColors(
    success = Color(0xFF00E676),
    onSuccess = Color.Black,
    successContainer = Color(0xFF003314),
    onSuccessContainer = Color(0xFFB9F6CA),
    warning = Color(0xFFFFD600),
    onWarning = Color.Black,
    warningContainer = Color(0xFF332B00),
    onWarningContainer = Color(0xFFFFF9C4),
    danger = Color(0xFFFF5252),
    onDanger = Color.Black,
    dangerContainer = Color(0xFF3B0003),
    onDangerContainer = Color(0xFFFFCDD2),
    info = Color(0xFF40C4FF),
    onInfo = Color.Black,
    infoContainer = Color(0xFF002238),
    onInfoContainer = Color(0xFFB3E5FC)
)

val AppSemanticColors: SemanticColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSemanticColors.current
