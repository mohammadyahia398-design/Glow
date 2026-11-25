package com.glow.app.ui.nowplaying

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.runtime.State
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.glow.app.ui.common.ArtPalette
import com.glow.app.ui.glass.core.LocalGlassConfig
import com.glow.app.ui.theme.GlowSkin
import com.glow.app.ui.theme.LocalGlowSkin

private const val ORB_PERIOD_MS = 36_000

private fun bottomTintAlpha(skin: GlowSkin) = if (skin.isDark) 0.55f else 0.75f

/** لون النصوص والأزرار في أسفل المشغّل: داكن أو فاتح بحسب سطوع الخلفية هناك. */
fun playerContentColor(palette: ArtPalette, skin: GlowSkin): Color {
    val bottom = palette.primary.copy(alpha = bottomTintAlpha(skin)).compositeOver(skin.bgBottom)
    return if (bottom.luminance() > 0.5f) Color(0xFF010030) else Color.White
}

/**
 * خلفية المشغّل: انعكاس الغلاف نفسه مموّهاً وخافتاً كأنه خلف زجاج، وفوقه ثلاث كرات ضوء
 * ضبابية بألوان قريبة من الغلاف تنجرف ببطء وبالكاد تُرى. تتوقف الحركة إذا أوقف النظام الرسوم.
 */
@Composable
fun AnimatedArtBackground(palette: ArtPalette, modifier: Modifier = Modifier, art: Bitmap? = null) {
    val skin = LocalGlowSkin.current
    val reduceMotion = LocalGlassConfig.current.reduceMotion

    val phase: State<Float> = if (reduceMotion) {
        remember { mutableStateOf(0.5f) }
    } else {
        rememberInfiniteTransition(label = "artBackdrop").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(ORB_PERIOD_MS, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "artBackdropPhase",
        )
    }
    val orbAlpha = if (skin.isDark) 0.16f else 0.22f
    val artAlpha = if (skin.isDark) 0.36f else 0.30f
    // صورة صغيرة جداً تُكبَّر بنعومة: تمويه بلا اعتماد على إصدار أندرويد.
    val soft = remember(art) {
        art?.let { runCatching { Bitmap.createScaledBitmap(it, 24, 24, true).asImageBitmap() }.getOrNull() }
    }
    val glowMix = lerp(palette.primary, palette.secondary, 0.5f)

    Canvas(modifier.fillMaxSize()) {
        // الحركة تُقرأ هنا فقط، فلا تُعاد التركيبة في كل إطار.
        val t = phase.value * 2f * PI.toFloat()
        val w = size.width
        val h = size.height

        drawRect(Brush.verticalGradient(listOf(skin.bgTop, skin.bgBottom)))

        soft?.let { img ->
            drawImage(
                image = img,
                srcSize = IntSize(img.width, img.height),
                dstSize = IntSize(w.roundToInt(), h.roundToInt()),
                alpha = artAlpha,
                filterQuality = FilterQuality.High,
            )
            // لمعة زجاجية خفيفة من الأعلى.
            drawRect(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.05f),
                    0.35f to Color.Transparent,
                ),
            )
        }

        fun orb(color: Color, center: Offset, radius: Float) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = orbAlpha), Color.Transparent),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }
        orb(palette.secondary, Offset(w * (0.25f + 0.12f * sin(t)), h * (0.30f + 0.07f * cos(t))), w * 0.70f)
        orb(palette.primary, Offset(w * (0.78f + 0.10f * cos(2f * t)), h * (0.55f + 0.06f * sin(2f * t))), w * 0.65f)
        orb(glowMix, Offset(w * (0.50f + 0.16f * sin(t + 2f)), h * (0.80f + 0.05f * cos(t + 2f))), w * 0.60f)

        drawRect(
            Brush.verticalGradient(
                0.45f to Color.Transparent,
                1f to palette.primary.copy(alpha = bottomTintAlpha(skin)),
            ),
        )
    }
}
