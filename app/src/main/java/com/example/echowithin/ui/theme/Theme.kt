package com.example.echowithin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EchoWithinColorScheme = darkColorScheme(
    primary = BrandOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3D2200),
    onPrimaryContainer = BrandAmber,
    secondary = BrandAmber,
    onSecondary = BrandInk,
    secondaryContainer = Color(0xFF3D2E00),
    onSecondaryContainer = BrandAmber,
    tertiary = InfoBlue,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRedContainer,
    onErrorContainer = Color(0xFFFCA5A5),
    outline = DarkBorder,
    outlineVariant = Color(0xFF2A3142),
    inverseSurface = TextPrimary,
    inverseOnSurface = DarkBackground,
    surfaceTint = BrandOrange
)

@Composable
fun EchoWithinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Always use brand dark theme — matches the website
    MaterialTheme(
        colorScheme = EchoWithinColorScheme,
        typography = Typography,
        content = content
    )
}