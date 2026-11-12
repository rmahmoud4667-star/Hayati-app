package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = TurquoiseVibrant,
    onPrimary = NavyDarkBackground,
    primaryContainer = NavySurfaceVariant,
    onPrimaryContainer = TurquoiseVibrant,
    secondary = GoldAccent,
    onSecondary = NavyDarkBackground,
    secondaryContainer = NavyCard,
    onSecondaryContainer = GoldLight,
    tertiary = TurquoiseLight,
    onTertiary = Color.White,
    background = NavyDarkBackground,
    onBackground = TextPrimaryDark,
    surface = NavySurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = NavyBorder,
    error = ErrorRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = TurquoisePrimary,
    onPrimary = Color.White,
    primaryContainer = TurquoiseContainer,
    onPrimaryContainer = Color(0xFF0F766E),
    secondary = GoldPrimary,
    onSecondary = Color.White,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = Color(0xFF92400E),
    tertiary = TurquoiseLight,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightBorder,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun HayatiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
