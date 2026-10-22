package com.example.echowithin.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * The app renders in a brand dark theme regardless of the system setting
 * (Notesnook-inspired aesthetic). Any code that previously consulted
 * `isSystemInDarkTheme()` to decide dark vs light styling (WebView CSS,
 * note color tints) must use this instead, otherwise the note area renders
 * "light" inside an always-dark scaffold on light-mode devices.
 */
const val AppIsDarkTheme = true

private val EchoWithinColorScheme = darkColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF003D19),
    onPrimaryContainer = BrandGreenLight,
    secondary = BrandGreenLight,
    onSecondary = BrandInk,
    secondaryContainer = Color(0xFF1B3D1F),
    onSecondaryContainer = BrandGreenLight,
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
    outlineVariant = Color(0xFF2A2A2A),
    inverseSurface = TextPrimary,
    inverseOnSurface = DarkBackground,
    surfaceTint = BrandGreen
)

@Composable
fun EchoWithinTheme(
    darkTheme: Boolean = AppIsDarkTheme,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Always use brand dark theme — matches the Notesnook-inspired aesthetic
    MaterialTheme(
        colorScheme = EchoWithinColorScheme,
        typography = Typography,
        content = content
    )
}