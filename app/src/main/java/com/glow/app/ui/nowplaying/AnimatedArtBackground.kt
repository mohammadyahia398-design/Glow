package com.glow.app.ui.nowplaying

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.glow.app.ui.common.ArtPalette
import com.glow.app.ui.glass.core.LocalGlassConfig
import com.glow.app.ui.theme.GlowSkin
import com.glow.app.ui.theme.LocalGlowSkin
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val ORB_PERIOD_MS = 36_000
private const val ORB_COUNT = 4

/** ألوان الهوية عند غياب الغلاف أو كونه رمادياً. */
private val IdentityOrbs = listOf(
    DominantColor(Color(0xFFF042FF), 1.00f),
    DominantColor(Color(0xFF7226FF), 0.80f),
    DominantColor(Color(0xFF87F5F5), 0.65f),
)
private val IdentityFill = listOf(Color(0xFFF042FF), Color(0xFF7226FF), Color(0xFF87F5F5), Color(0xFFFFE5F1))

/** موضع كل كرة وحركتها: مركز (x, y)، مدى الانجراف (ax, ay)، سرعة (1 أو 2)، وإزاحة زمنية. */
private class OrbSlot(
    val x: Float, val y: Float,
    val ax: Float, val ay: Float,
    val speedX: Float, val speedY: Float,
    val phase: Float,
)

private val OrbSlots = listOf(
    OrbSlot(0.28f, 0.30f, 0.12f, 0.07f, 1f, 1f, 0f),
    OrbSlot(0.78f, 0.52f, 0.10f, 0.06f, 1f, 2f, 1.7f),
    OrbSlot(0.42f, 0.80f, 0.14f, 0.05f, 2f, 1f, 3.1f),
    OrbSlot(0.86f, 0.14f, 0.08f, 0.06f, 1f, 1f, 4.6f),
)

/** لون النصوص والأزرار في أسفل المشغّل: الخلفية داكنة شبه سوداء أو فاتحة شبه بيضاء بحسب المظهر. */
fun playerContentColor(@Suppress("UNUSED_PARAMETER") palette: ArtPalette, skin: GlowSkin): Color =
    if (skin.isDark) Color.White else Color(0xFF010030)

/**
 * خلفية المشغّل: خلفية داكنة (أو فاتحة) شبه صرفة، وفوقها كرات ضوء ناعمة منفوشة باهتة
 * بألوان الغلاف الغالبة. حجم الكرة ووضوحها يتبعان مساحة اللون في الغلاف، وتطفو ببطء.
 * بلا غلاف أو بغلاف رمادي تظهر كرات بألوان الهوية. تتوقف الحركة إذا أوقف النظام الرسوم.
 */
@Composable
fun AnimatedArtBackground(
    @Suppress("UNUSED_PARAMETER") palette: ArtPalette,
    modifier: Modifier = Modifier,
    art: Bitmap? = null,
) {
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

    val target = remember(art) {
        art?.dominantColors(ORB_COUNT)?.takeIf { it.isNotEmpty() } ?: IdentityOrbs
    }
    val colorSpec: AnimationSpec<Color> = if (reduceMotion) snap() else tween(900)
    val floatSpec: AnimationSpec<Float> = if (reduceMotion) snap() else tween(900)
    // عند تغيّر الأغنية تنتقل الكرات بنعومة إلى ألوان الغلاف الجديد.
    val colors = List(ORB_COUNT) { i ->
        animateColorAsState(target.getOrNull(i)?.color ?: IdentityFill[i], colorSpec, label = "orbColor$i")
    }
    val weights = List(ORB_COUNT) { i ->
        animateFloatAsState(target.getOrNull(i)?.share ?: 0f, floatSpec, label = "orbWeight$i")
    }
    val maxAlpha = if (skin.isDark) 0.42f else 0.50f

    Canvas(modifier.fillMaxSize()) {
        // الحركة والألوان تُقرأ هنا فقط، فلا تُعاد التركيبة في كل إطار.
        val t = phase.value * 2f * PI.toFloat()
        val w = size.width
        val h = size.height

        drawRect(Brush.verticalGradient(listOf(skin.bgTop, skin.bgBottom)))

        for (i in 0 until ORB_COUNT) {
            val share = weights[i].value.coerceIn(0f, 1f)
            if (share < 0.02f) continue
            val slot = OrbSlots[i]
            val color = colors[i].value
            val center = Offset(
                w * (slot.x + slot.ax * sin(t * slot.speedX + slot.phase)),
                h * (slot.y + slot.ay * cos(t * slot.speedY + slot.phase)),
            )
            val radius = w * (0.40f + 0.34f * sqrt(share))
            val a = maxAlpha * (0.45f + 0.55f * share)
            // سقوط تدريجي ناعم: قلب باهت وحواف تذوب بلا حدّ واضح.
            drawCircle(
                brush = Brush.radialGradient(
                    0f to color.copy(alpha = a),
                    0.45f to color.copy(alpha = a * 0.55f),
                    0.75f to color.copy(alpha = a * 0.18f),
                    1f to Color.Transparent,
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
            // لمعة بلورية صغيرة في أعلى الكرة.
            val glint = Offset(center.x - radius * 0.18f, center.y - radius * 0.20f)
            drawCircle(
                brush = Brush.radialGradient(
                    0f to Color.White.copy(alpha = 0.10f * share),
                    1f to Color.Transparent,
                    center = glint,
                    radius = radius * 0.42f,
                ),
                radius = radius * 0.42f,
                center = glint,
            )
        }

        // تهدئة خفيفة أسفل الشاشة حتى تبقى الأزرار والنصوص واضحة.
        drawRect(
            Brush.verticalGradient(
                0.55f to Color.Transparent,
                1f to skin.bgBottom.copy(alpha = 0.60f),
            ),
        )
    }
}
