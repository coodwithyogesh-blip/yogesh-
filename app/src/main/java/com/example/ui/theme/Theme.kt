package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ArushiColorScheme = darkColorScheme(
    primary = ArushiNeonViolet,
    onPrimary = Color.White,
    primaryContainer = ArushiCardBg,
    onPrimaryContainer = ArushiNeonPink,
    secondary = ArushiNeonCyan,
    onSecondary = Color.Black,
    secondaryContainer = ArushiSurfaceVariant,
    onSecondaryContainer = Color.White,
    tertiary = ArushiNeonPink,
    onTertiary = Color.White,
    background = ArushiDeepBg,
    onBackground = ArushiTextPrimary,
    surface = ArushiCardBg,
    onSurface = ArushiTextPrimary,
    surfaceVariant = ArushiSurfaceVariant,
    onSurfaceVariant = ArushiTextSecondary,
    outline = ArushiCardBorder
)

@Composable
fun ArushiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ArushiColorScheme,
        typography = Typography,
        content = content
    )
}
