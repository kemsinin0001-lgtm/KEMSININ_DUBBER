package com.kemsinin.dubber.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF311B63),
    onPrimaryContainer = Color(0xFFE9DFFF),
    secondary = Emerald,
    onSecondary = Color(0xFF00351F),
    secondaryContainer = Color(0xFF064A2F),
    onSecondaryContainer = Color(0xFFB0F2D4),
    tertiary = CyanAccent,
    onTertiary = Color(0xFF00363D),
    background = DarkBackground,
    onBackground = Color(0xFFEDE9F6),
    surface = DarkSurface,
    onSurface = Color(0xFFEDE9F6),
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = Color(0xFFC9C0DE),
    error = Rose,
    onError = Color(0xFF5C0A1E),
)

private val LightColors = lightColorScheme(
    primary = VioletDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9DFFF),
    onPrimaryContainer = Color(0xFF230B54),
    secondary = Color(0xFF007A4D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB0F2D4),
    onSecondaryContainer = Color(0xFF00351F),
    tertiary = Color(0xFF00727D),
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = Color(0xFF1C1A23),
    surface = LightSurface,
    onSurface = Color(0xFF1C1A23),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF4A4560),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

@Composable
fun KemsininTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
