package com.glow.app.ui.glass.effects

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.glow.app.ui.glass.core.GlassStyle
import com.glow.app.ui.glass.core.GlassTier
import com.glow.app.ui.glass.lighting.LightSource
import kotlin.math.min

/** أقصى عرض لشريط الانكسار عند الحافة. */
private val MaxEdgeWidth = 26.dp

/** أقصى إزاحة (عند الحافة) في أقصى قوة انكسار. */
private val MaxShift = 30.dp

/** أقصى تشتّت لوني نسبي. */
private const val MAX_DISPERSION = 0.16f

/**
 * نصف قطر الزوايا الموحّد الذي يستخدمه الشيدر، أو null إذا كان الشكل غير مدعوم.
 *
 * - مستطيل: 0.
 * - مستطيل مستدير (ومنه الكبسولة والدائرة): أكبر نصف قطر بين الزوايا الأربع.
 * - شكل عام ([Outline.Generic]): غير مدعوم، فلا يُطبَّق الانكسار.
 */
fun Shape.refractionCornerRadius(
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density,
): Float? = when (val outline = createOutline(size, layoutDirection, density)) {
    is Outline.Rectangle -> 0f
    is Outline.Rounded -> {
        val rr = outline.roundRect
        maxOf(
            rr.topLeftCornerRadius.x,
            rr.topRightCornerRadius.x,
            rr.bottomRightCornerRadius.x,
            rr.bottomLeftCornerRadius.x,
        )
    }
    is Outline.Generic -> null
}

/**
 * يبني التأثير النهائي لطبقة الخلفية: التمويه ثم الانكسار فوقه.
 *
 * يعيد [blur] كما هو (قد يكون null) في أي من الحالات التالية، فلا يتعطل الرسم:
 * - المستوى لا يدعم الانكسار أو قوته صفر.
 * - الحجم غير معروف بعد.
 * - الجهاز أقدم من API 33.
 * - الشكل غير مدعوم من الشيدر.
 *
 * يُحتفظ بالنتيجة عبر إعادة التركيب، ولا يُعاد بناؤها إلا عند تغيّر المدخلات.
 *
 * @param size حجم السطح بالبكسل، يُقاس بـ onSizeChanged.
 * @param blur التمويه المبني مسبقاً (من rememberGlassBlurEffect).
 */
@Composable
fun rememberGlassRefractionEffect(
    style: GlassStyle,
    tier: GlassTier,
    shape: Shape,
    size: IntSize,
    blur: RenderEffect?,
    light: LightSource = LightSource.Default,
): RenderEffect? {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    return remember(style, tier, shape, size, blur, density, layoutDirection, light) {
        createGlassRefractionEffect(style, tier, shape, size, blur, density, layoutDirection, light)
    }
}

/** النسخة غير القابلة للتركيب من [rememberGlassRefractionEffect] (مفيدة للاختبار). */
fun createGlassRefractionEffect(
    style: GlassStyle,
    tier: GlassTier,
    shape: Shape,
    size: IntSize,
    blur: RenderEffect?,
    density: Density,
    layoutDirection: LayoutDirection,
    light: LightSource = LightSource.Default,
): RenderEffect? {
    val refraction = style.effectiveRefraction(tier)
    if (refraction <= 0f || size.width <= 0 || size.height <= 0) return blur
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return blur

    val widthPx = size.width.toFloat()
    val heightPx = size.height.toFloat()
    val minDimension = min(widthPx, heightPx)

    val cornerRadius = shape.refractionCornerRadius(
        size = Size(widthPx, heightPx),
        layoutDirection = layoutDirection,
        density = density,
    ) ?: return blur

    val maxEdgePx = with(density) { MaxEdgeWidth.toPx() }
    val maxShiftPx = with(density) { MaxShift.toPx() }

    return applyRefraction(
        blur = blur,
        widthPx = widthPx,
        heightPx = heightPx,
        cornerRadiusPx = cornerRadius.coerceIn(0f, minDimension / 2f),
        edgeWidthPx = min(minDimension * 0.5f, maxEdgePx),
        strengthPx = refraction * maxShiftPx,
        dispersion = refraction * MAX_DISPERSION,
        blurRadiusPx = with(density) { style.effectiveBlur(tier).toPx() },
        light = light,
    )
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun applyRefraction(
    blur: RenderEffect?,
    widthPx: Float,
    heightPx: Float,
    cornerRadiusPx: Float,
    edgeWidthPx: Float,
    strengthPx: Float,
    dispersion: Float,
    blurRadiusPx: Float,
    light: LightSource,
): RenderEffect {
    val shader = RuntimeShader(RefractionShader.SOURCE)
    shader.setFloatUniform(RefractionShader.UNIFORM_SIZE, widthPx, heightPx)
    shader.setFloatUniform(RefractionShader.UNIFORM_CORNER_RADIUS, cornerRadiusPx)
    shader.setFloatUniform(RefractionShader.UNIFORM_EDGE_WIDTH, edgeWidthPx)
    shader.setFloatUniform(RefractionShader.UNIFORM_STRENGTH, strengthPx)
    shader.setFloatUniform(RefractionShader.UNIFORM_DISPERSION, dispersion)
    val direction = light.towardLight
    shader.setFloatUniform(
        RefractionShader.UNIFORM_LIGHT_DIRECTION,
        direction.x,
        direction.y,
    )
    shader.setFloatUniform(
        RefractionShader.UNIFORM_LIGHT_INTENSITY,
        light.intensity.coerceIn(0f, 2f),
    )

    val shaderEffect = AndroidRenderEffect.createRuntimeShaderEffect(
        shader,
        RefractionShader.UNIFORM_CONTENT,
    )

    // نبني Blur على Android مباشرة حتى تكون سلسلة التأثيرات على منصة واحدة.
    // createChainEffect(outer, inner): يُطبَّق inner أولاً ثم outer.
    val nativeBlur = if (blur != null && blurRadiusPx > 0f) {
        AndroidRenderEffect.createBlurEffect(
            blurRadiusPx,
            blurRadiusPx,
            android.graphics.Shader.TileMode.MIRROR,
        )
    } else {
        null
    }
    val chained = if (nativeBlur != null) {
        AndroidRenderEffect.createChainEffect(shaderEffect, nativeBlur)
    } else {
        shaderEffect
    }
    return chained.asComposeRenderEffect()
}
