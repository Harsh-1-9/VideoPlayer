package com.example.videoplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryColor,
    onPrimary = TextPrimary,
    primaryContainer = PrimarySubtle,
    onPrimaryContainer = PrimaryLight,

    secondary = PrimaryLight,
    onSecondary = AppBackground,

    background = AppBackground,
    onBackground = TextPrimary,

    surface = AppSurface,
    onSurface = TextPrimary,
    surfaceVariant = AppSurfaceCard,
    onSurfaceVariant = TextSecondary,

    outline = BorderColor,
    error = ErrorRed,
    onError = TextPrimary
)

@Composable
fun VideoPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content
    )
}