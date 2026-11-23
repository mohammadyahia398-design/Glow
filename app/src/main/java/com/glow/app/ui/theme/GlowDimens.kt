package com.glow.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Central spacing and component metrics. Keep screen-level values on this scale. */
@Immutable
data class GlowDimens(
    val spaceXxs: Dp = 2.dp,
    val spaceXs: Dp = 4.dp,
    val spaceSm: Dp = 8.dp,
    val spaceMd: Dp = 12.dp,
    val spaceLg: Dp = 16.dp,
    val spaceXl: Dp = 24.dp,
    val spaceXxl: Dp = 32.dp,
    val spaceXxxl: Dp = 48.dp,
    val screenPadding: Dp = 20.dp,
    val miniPlayerHeight: Dp = 68.dp,
    val navBarHeight: Dp = 76.dp,
    val minTouchTarget: Dp = 48.dp,
    val hairline: Dp = 1.dp,
    val artworkSmall: Dp = 48.dp,
    val artworkMedium: Dp = 64.dp,
    val artworkLarge: Dp = 320.dp,
    val glowBlur: Dp = 28.dp,
    val haloRadius: Dp = 144.dp,
)

internal val LocalGlowDimens = staticCompositionLocalOf { GlowDimens() }
