/*
 * Based on "Liquid Glass (Backdrop)" by Kyant (https://github.com/Kyant0/AndroidLiquidGlass),
 * Apache License 2.0. Modified for Glow: Android-only, package renamed. See THIRD_PARTY/liquid-glass/.
 */
package com.glow.app.liquid.backdrop.effects

import androidx.compose.ui.graphics.RenderEffect
import com.glow.app.liquid.backdrop.BackdropEffectScope
import com.glow.app.liquid.backdrop.RuntimeShader
import com.glow.app.liquid.backdrop.internal.RuntimeShaderEffect
import com.glow.app.liquid.backdrop.internal.chain
import com.glow.app.liquid.backdrop.isRenderEffectSupported
import com.glow.app.liquid.backdrop.isRuntimeShaderSupported
import kotlin.contracts.ExperimentalContracts

fun BackdropEffectScope.effect(effect: RenderEffect) {
    if (!isRenderEffectSupported()) return

    renderEffect = renderEffect.chain(effect)
}

@OptIn(ExperimentalContracts::class)
fun BackdropEffectScope.runtimeShaderEffect(
    key: String,
    shaderString: String,
    uniformShaderName: String,
    block: RuntimeShader.() -> Unit
) {
    if (!isRuntimeShaderSupported()) return

    val effect =
        RuntimeShaderEffect(
            runtimeShader = obtainRuntimeShader(key, shaderString).apply(block),
            uniformShaderName = uniformShaderName
        )
    renderEffect = renderEffect.chain(effect)
}
