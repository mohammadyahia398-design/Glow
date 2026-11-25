package com.glow.app.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glow.app.ui.glass.core.LocalGlassConfig
import com.glow.app.ui.theme.LocalGlowSkin
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val TAU = (2.0 * PI).toFloat()
private val Violet = Color(0xFF7226FF)
private val SparkTint = Color(0xFFF3EAFF)

/** وميض قصير: يبقى صفراً معظم الدورة ثم يلمع لحظة. */
private fun twinkle(t: Float): Float {
    val s = sin(t * TAU)
    return if (s > 0f) s * s * s else 0f
}

/** شرارة خفيفة جداً: نجمة رباعية رفيعة بنقطة مضيئة في مركزها. */
private fun DrawScope.sparkle(center: Offset, radius: Float, alpha: Float) {
    if (alpha <= 0.02f) return
    val c = SparkTint.copy(alpha = alpha)
    val w = radius * 0.22f
    drawLine(c, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), w, StrokeCap.Round)
    drawLine(c, Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), w, StrokeCap.Round)
    drawCircle(c, radius * 0.26f, center)
}

/** دورة 0..1 متكررة؛ تتجمّد إذا أوقف النظام الرسوم المتحركة. */
@Composable
private fun rememberLoopPhase(periodMs: Int): State<Float> {
    val reduceMotion = LocalGlassConfig.current.reduceMotion
    return if (reduceMotion) {
        remember { mutableStateOf(0.25f) }
    } else {
        rememberInfiniteTransition(label = "glowLoading").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(periodMs, easing = LinearEasing), RepeatMode.Restart),
            label = "glowLoadingPhase",
        )
    }
}

/** حلقة صغيرة للأزرار والصفوف: قوس لامع بحافة دائرية يطول ويقصر وهو يدور، وخلفه شرارات خفيفة جداً. */
@Composable
fun GlowRingLoader(
    modifier: Modifier = Modifier,
    diameter: Dp = 24.dp,
    color: Color = LocalGlowSkin.current.accent,
    strokeWidth: Dp = 2.5.dp,
) {
    val phase = rememberLoopPhase(1200)
    Canvas(modifier.size(diameter)) {
        val stroke = strokeWidth.toPx()
        val inset = stroke * 1.4f
        val arcSize = Size(this.size.width - inset * 2f, this.size.height - inset * 2f)
        val topLeft = Offset(inset, inset)
        val t = phase.value
        val start = t * 360f - 90f
        val sweep = 100f + 60f * sin(t * TAU)
        drawArc(
            color = color.copy(alpha = 0.16f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(stroke),
        )
        drawArc(
            color = color.copy(alpha = 0.22f),
            startAngle = start,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(stroke * 2.2f, cap = StrokeCap.Round),
        )
        drawArc(
            color = color,
            startAngle = start,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
        // شرارتان خفيفتان خلف رأس القوس.
        val rr = arcSize.width / 2f
        for (k in 0..1) {
            val ang = (start + sweep - 40f - k * 95f) * (PI.toFloat() / 180f)
            val pos = Offset(center.x + rr * cos(ang), center.y + rr * sin(ang))
            sparkle(pos, stroke * 1.5f, 0.55f * twinkle(t * 2f + k * 0.37f))
        }
    }
}

/** أعمدة موسيقية: موجة هادئة متتابعة، للحالات التي لها علاقة بالصوت. */
@Composable
fun GlowBarsLoader(
    modifier: Modifier = Modifier,
    height: Dp = 28.dp,
    color: Color = LocalGlowSkin.current.accent,
    bars: Int = 4,
) {
    val phase = rememberLoopPhase(1100)
    val barW = 4.dp
    val gap = 4.dp
    Canvas(modifier.size(barW * bars + gap * (bars - 1), height)) {
        val bw = barW.toPx()
        val g = gap.toPx()
        val h = this.size.height
        for (i in 0 until bars) {
            val wave = 0.5f + 0.5f * sin((phase.value - i * 0.16f) * TAU)
            val bh = h * (0.32f + 0.68f * wave)
            drawRoundRect(
                color = color.copy(alpha = 0.55f + 0.45f * wave),
                topLeft = Offset(i * (bw + g), (h - bh) / 2f),
                size = Size(bw, bh),
                cornerRadius = CornerRadius(bw / 2f),
            )
        }
    }
}

/** ثلاث كرات ضوء ضبابية تتنفّس وتدور ببطء: نفس لغة خلفية شاشة التشغيل، للتحميل الكبير. */
@Composable
fun GlowOrbsLoader(
    modifier: Modifier = Modifier,
    diameter: Dp = 96.dp,
    color: Color = LocalGlowSkin.current.accent,
) {
    val phase = rememberLoopPhase(6000)
    val colors = listOf(color, Violet, lerp(color, Color.White, 0.30f))
    val sparks = listOf(Offset(0.30f, 0.28f), Offset(0.72f, 0.34f), Offset(0.58f, 0.74f), Offset(0.26f, 0.66f))
    Canvas(modifier.size(diameter)) {
        val r = this.size.minDimension
        val t = phase.value * TAU
        colors.forEachIndexed { i, c ->
            val a = t + i * (TAU / 3f)
            val ctr = Offset(center.x + r * 0.14f * cos(a), center.y + r * 0.14f * sin(a))
            val rad = r * (0.34f + 0.05f * sin(t * 2f + i))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(c.copy(alpha = 0.55f), Color.Transparent),
                    center = ctr,
                    radius = rad,
                ),
                radius = rad,
                center = ctr,
            )
        }
        sparks.forEachIndexed { i, f ->
            sparkle(Offset(this.size.width * f.x, this.size.height * f.y), r * 0.045f, 0.5f * twinkle(phase.value * 3f + i * 0.25f))
        }
    }
}

/**
 * شريط تقدّم: بلا قيمة = شريحة ضوء تنزلق (تحميل مفتوح)، وبقيمة 0..1 = تعبئة ناعمة.
 * مسار دقيق بحواف دائرية وارتفاع 6dp مثل مسار المنزلق في المشغّل.
 */
@Composable
fun GlowProgressBar(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    color: Color = LocalGlowSkin.current.accent,
) {
    val phase = rememberLoopPhase(1500)
    val shown by animateFloatAsState(progress?.coerceIn(0f, 1f) ?: 0f, label = "glowProgress")
    Canvas(modifier.fillMaxWidth().height(6.dp)) {
        val w = this.size.width
        val h = this.size.height
        val shape = Path().apply { addRoundRect(RoundRect(0f, 0f, w, h, CornerRadius(h / 2f))) }
        var headX = -1f
        clipPath(shape) {
            drawRect(color.copy(alpha = 0.16f))
            if (progress != null) {
                drawRoundRect(
                    color = color,
                    size = Size(w * shown, h),
                    cornerRadius = CornerRadius(h / 2f),
                )
                if (shown > 0.02f) headX = w * shown
            } else {
                val seg = w * 0.4f
                val x = -seg + (w + seg) * phase.value
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, color, Color.Transparent),
                        startX = x,
                        endX = x + seg,
                    ),
                    topLeft = Offset(x, 0f),
                    size = Size(seg, h),
                )
                headX = x + seg * 0.62f
            }
        }
        if (headX in 0f..w) {
            sparkle(Offset(headX, h / 2f), 5.dp.toPx(), 0.5f * twinkle(phase.value * 3f))
        }
    }
}

enum class GlowLoadingStyle { Ring, Bars, Orbs }

/** حالة تحميل كاملة (مؤشر + نص اختياري) للشاشات والقوائم الفارغة أثناء التحميل. */
@Composable
fun GlowLoading(
    modifier: Modifier = Modifier,
    style: GlowLoadingStyle = GlowLoadingStyle.Orbs,
    label: String? = null,
) {
    val skin = LocalGlowSkin.current
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (style) {
            GlowLoadingStyle.Ring -> GlowRingLoader(diameter = 36.dp, strokeWidth = 3.dp)
            GlowLoadingStyle.Bars -> GlowBarsLoader()
            GlowLoadingStyle.Orbs -> GlowOrbsLoader()
        }
        if (label != null) {
            Text(text = label, color = skin.textSecondary, fontSize = 14.sp)
        }
    }
}
