/*
 * Based on "Liquid Glass (Backdrop)" by Kyant (https://github.com/Kyant0/AndroidLiquidGlass),
 * Apache License 2.0. Modified for Glow: Android-only, packages renamed, colors/params added.
 * See THIRD_PARTY/liquid-glass/.
 */
package com.glow.app.liquid.ui.utils

fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction
