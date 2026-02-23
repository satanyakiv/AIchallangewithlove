package com.portfolio.ai_challange_with_love.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFB4415A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9DE),
    onPrimaryContainer = Color(0xFF3F0018),
    secondary = Color(0xFF75565C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD9DE),
    onSecondaryContainer = Color(0xFF2B151A),
    tertiary = Color(0xFF7B5733),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCBE),
    onTertiaryContainer = Color(0xFF2C1600),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF201A1B),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF201A1B),
    surfaceVariant = Color(0xFFF3DDE0),
    onSurfaceVariant = Color(0xFF524345),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB1C0),
    onPrimary = Color(0xFF67002D),
    primaryContainer = Color(0xFF912943),
    onPrimaryContainer = Color(0xFFFFD9DE),
    secondary = Color(0xFFE4BDC3),
    onSecondary = Color(0xFF43292E),
    secondaryContainer = Color(0xFF5C3F44),
    onSecondaryContainer = Color(0xFFFFD9DE),
    tertiary = Color(0xFFEFBD94),
    onTertiary = Color(0xFF462A0C),
    tertiaryContainer = Color(0xFF60401E),
    onTertiaryContainer = Color(0xFFFFDCBE),
    background = Color(0xFF201A1B),
    onBackground = Color(0xFFECE0E1),
    surface = Color(0xFF201A1B),
    onSurface = Color(0xFFECE0E1),
    surfaceVariant = Color(0xFF524345),
    onSurfaceVariant = Color(0xFFD6C2C4),
)

@Composable
fun AiChallengeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
