package com.example.videoplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LuxuryColorScheme = darkColorScheme(
    primary            = GoldPrimary,
    onPrimary          = LuxuryBlack,
    primaryContainer   = GoldSubtle,
    onPrimaryContainer = GoldLight,

    secondary          = PlatinumMid,
    onSecondary        = LuxuryBlack,
    secondaryContainer = PlatinumSubtle,
    onSecondaryContainer = PlatinumLight,

    tertiary           = GoldLight,
    onTertiary         = LuxuryBlack,

    background         = LuxuryBlack,
    onBackground       = PlatinumLight,

    surface            = LuxuryDarkSurface,
    onSurface          = PlatinumLight,
    surfaceVariant     = LuxuryCardSurface,
    onSurfaceVariant   = PlatinumMid,

    outline            = PlatinumDark,
    outlineVariant     = GlassBorder,

    error              = ErrorRed,
    onError            = LuxuryBlack,
)

@Composable
fun VideoPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LuxuryColorScheme,
        typography  = LuxuryTypography,
        content     = content
    )
}
