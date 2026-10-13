package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF66D4AB),
    onPrimary = Color(0xFF003824),
    primaryContainer = Color(0xFF1B4D3E),
    onPrimaryContainer = Color(0xFFE4F3ED),
    secondary = Color(0xFFE5C058),
    onSecondary = Color(0xFF3F2E00),
    secondaryContainer = Color(0xFF5B4300),
    onSecondaryContainer = Color(0xFFFBF4DF),
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = Color(0xFFE2E3DF),
    onSurface = Color(0xFFE2E3DF)
)

private val LightColorScheme = lightColorScheme(
    primary = HasadEmerald,
    onPrimary = Color.White,
    primaryContainer = HasadMintContainer,
    onPrimaryContainer = HasadOnMintContainer,
    secondary = HasadGold,
    onSecondary = Color.White,
    secondaryContainer = HasadGoldContainer,
    onSecondaryContainer = HasadOnGoldContainer,
    background = Color(0xFFF7F9F7),
    surface = Color.White,
    surfaceVariant = Color(0xFFEDF2EE),
    onBackground = Color(0xFF191C1A),
    onSurface = Color(0xFF191C1A),
    onSurfaceVariant = Color(0xFF424943)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
