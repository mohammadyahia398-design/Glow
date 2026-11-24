/*
 * Based on "Liquid Glass (Backdrop)" by Kyant (https://github.com/Kyant0/AndroidLiquidGlass),
 * Apache License 2.0. Modified for Glow: Android-only, package renamed. See THIRD_PARTY/liquid-glass/.
 */
package com.glow.app.liquid.backdrop.internal

import android.graphics.BlurMaskFilter
import androidx.compose.ui.graphics.Paint
import com.glow.app.liquid.backdrop.RuntimeShader
import com.glow.app.liquid.backdrop.asAndroidRuntimeShader

internal fun Paint.blur(radius: Float) {
    this.asFrameworkPaint().maskFilter =
        if (radius > 0f) BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
        else null
}

internal fun Paint.setRuntimeShader(runtimeShader: RuntimeShader?) {
    this.asFrameworkPaint().shader = runtimeShader?.asAndroidRuntimeShader()
}
