package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PrimaryAccent,
    onPrimary = Color.White,
    primaryContainer = PrimaryAccentLight,
    onPrimaryContainer = PrimaryAccentDark,
    secondary = TextSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = TextPrimary,
    background = BackgroundNeutral,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCardVariant,
    onSurfaceVariant = TextSecondary,
    outline = CardBorderColor,
    error = DestructiveRed,
    onError = Color.White,
    errorContainer = DestructiveRedLight,
    onErrorContainer = DestructiveRed
)

@Composable
fun AssuredCaptureTheme(
    content: @Composable () -> Unit
) {
    // For outdoor inspection tools, reliable high-contrast light neutral theme is crucial
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
