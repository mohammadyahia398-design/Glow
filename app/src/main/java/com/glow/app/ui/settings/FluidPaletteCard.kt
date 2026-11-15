package com.glow.app.ui.settings

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glow.app.ui.common.GlowIcon
import com.glow.app.ui.common.IconKind
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * محاكاة أربع بقع سائلة داخل بطاقة. لكل بقعة كتلة مختلفة، فتتبع الإصبع بتفاوت وتنفصل وتلتقي.
 *
 * - اللمس: دفعة رذاذ فورية بعيداً عن نقطة اللمس، وتموّج دائري يتّسع.
 * - الإمساك: البقع تنجذب إلى الإصبع كمغناطيس، وتُدفع عنه إن اقتربت جداً، وتدور حوله مع سرعة حركته.
 * - الإفلات: ارتداد هلامي نحو الإصبع ثم تعود إلى مواضعها.
 * - بلا لمس: انجراف بطيء، ولمعة تتجوّل وحدها.
 */
private class FluidSim {
    val x = FloatArray(4)
    val y = FloatArray(4)
    private val vx = FloatArray(4)
    private val vy = FloatArray(4)
    var w = 0f
        private set
    var h = 0f
        private set
    var time = 0f
        private set
    var touchAmt = 0f
        private set
    var hx = 0f
        private set
    var hy = 0f
        private set
    var pressX = 0f
        private set
    var pressY = 0f
        private set
    var ripple = 1f
        private set
    private var tx = 0f
    private var ty = 0f
    private var touching = false
    private var fvx = 0f
    private var fvy = 0f

    private val homeX = floatArrayOf(0.26f, 0.76f, 0.30f, 0.78f)
    private val homeY = floatArrayOf(0.28f, 0.30f, 0.76f, 0.74f)
    private val mass = floatArrayOf(1.0f, 0.8f, 1.25f, 0.9f)

    fun resize(width: Float, height: Float) {
        val first = w <= 0f
        w = width
        h = height
        if (first && w > 0f) {
            hx = 0.40f * w
            hy = 0.30f * h
            pressX = hx
            pressY = hy
            for (i in 0..3) {
                x[i] = homeX[i] * w
                y[i] = homeY[i] * h
            }
        }
    }

    fun press(px: Float, py: Float) {
        touching = true
        tx = px
        ty = py
        pressX = px
        pressY = py
        ripple = 0f
        fvx = 0f
        fvy = 0f
        if (w <= 0f) return
        val m = min(w, h)
        val reach = m * 0.95f
        for (i in 0..3) {
            val dx = x[i] - px
            val dy = y[i] - py
            val dist = sqrt(dx * dx + dy * dy) + 0.001f
            val f = max(0f, 1f - dist / reach)
            vx[i] += dx / dist * f * m * 2.4f
            vy[i] += dy / dist * f * m * 2.4f
        }
    }

    fun move(px: Float, py: Float, dx: Float, dy: Float) {
        tx = px
        ty = py
        fvx = dx * 60f
        fvy = dy * 60f
    }

    fun release() {
        touching = false
        if (w <= 0f) return
        val m = min(w, h)
        val reach = m * 1.1f
        for (i in 0..3) {
            val dx = tx - x[i]
            val dy = ty - y[i]
            val dist = sqrt(dx * dx + dy * dy) + 0.001f
            val f = max(0f, 1f - dist / reach)
            vx[i] += dx / dist * f * m * 1.6f
            vy[i] += dy / dist * f * m * 1.6f
        }
    }

    fun step(dt: Float, seconds: Float) {
        time = seconds
        if (w <= 0f || h <= 0f) return
        val m = min(w, h)
        val reachPull = m * 1.15f
        val reachPush = m * 0.50f
        touchAmt += ((if (touching) 1f else 0f) - touchAmt) * min(1f, dt * 9f)
        ripple = min(1f, ripple + dt / 1.1f)
        fvx *= 1f - min(1f, dt * 8f)
        fvy *= 1f - min(1f, dt * 8f)

        val gx = if (touching) tx else w * (0.40f + 0.14f * sin(seconds * 0.55f))
        val gy = if (touching) ty else h * (0.30f + 0.10f * cos(seconds * 0.43f))
        val hk = min(1f, dt * (if (touching) 14f else 3f))
        hx += (gx - hx) * hk
        hy += (gy - hy) * hk

        val speed = sqrt(fvx * fvx + fvy * fvy)
        for (i in 0..3) {
            val homeXi = homeX[i] * w + sin(seconds * 0.7f + i * 1.9f) * 0.09f * w
            val homeYi = homeY[i] * h + cos(seconds * 0.6f + i * 2.4f) * 0.09f * h
            val k = 34f / mass[i]
            var ax = (homeXi - x[i]) * k
            var ay = (homeYi - y[i]) * k
            if (touching) {
                val dx = x[i] - tx
                val dy = y[i] - ty
                val dist = sqrt(dx * dx + dy * dy) + 0.001f
                val fp = max(0f, 1f - dist / reachPull)
                // مغناطيس: انجذاب نحو الإصبع
                ax -= dx / dist * fp * fp * 11f * m / mass[i]
                ay -= dy / dist * fp * fp * 11f * m / mass[i]
                // تنافر قريب كي لا تنطبق البقعة على الإصبع
                if (dist < reachPush) {
                    val f = 1f - dist / reachPush
                    ax += dx / dist * f * f * 16f * m
                    ay += dy / dist * f * f * 16f * m
                }
                // دوران حول الإصبع يزداد مع سرعته، وسحب مع اتجاهه
                val sw = fp * min(speed, 2500f) * 0.9f
                ax += -dy / dist * sw + fvx * fp * 4f
                ay += dx / dist * sw + fvy * fp * 4f
            }
            val damp = 1f - min(0.9f, 3.6f * dt)
            vx[i] = (vx[i] + ax * dt) * damp
            vy[i] = (vy[i] + ay * dt) * damp
            x[i] = (x[i] + vx[i] * dt).coerceIn(-0.25f * w, 1.25f * w)
            y[i] = (y[i] + vy[i] * dt).coerceIn(-0.25f * h, 1.25f * h)
        }
    }
}

/**
 * شيدر AGSL: كل بقعة لها «مجال» غاوسي، واللون عند كل بكسل مزج ناعم متصل بين الألوان
 * بأوزان أُسّية، فتنساب الألوان في بعضها كتدرّج زجاجي سائل بلا حدود حادّة.
 * حافة البطاقة تميل إلى بياض حليبي، وتحت الإصبع لمعة تتبعه، وعند اللمس تموّج دائري يتّسع،
 * وحبيبات دقيقة كالصور المرجعية.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class FluidShader {
    private val shader = RuntimeShader(SOURCE)
    val brush = ShaderBrush(shader)

    fun update(sim: FluidSim, colors: List<Color>, base: Color) {
        val m = min(sim.w, sim.h)
        shader.setFloatUniform("uSize", sim.w, sim.h)
        shader.setFloatUniform("uTime", sim.time)
        shader.setFloatUniform("uP0", sim.x[0], sim.y[0])
        shader.setFloatUniform("uP1", sim.x[1], sim.y[1])
        shader.setFloatUniform("uP2", sim.x[2], sim.y[2])
        shader.setFloatUniform("uP3", sim.x[3], sim.y[3])
        shader.setFloatUniform("uRad", m * 0.70f, m * 0.66f, m * 0.64f, m * 0.68f)
        shader.setFloatUniform("uC0", colors[0].red, colors[0].green, colors[0].blue)
        shader.setFloatUniform("uC1", colors[1].red, colors[1].green, colors[1].blue)
        shader.setFloatUniform("uC2", colors[2].red, colors[2].green, colors[2].blue)
        shader.setFloatUniform("uC3", colors[3].red, colors[3].green, colors[3].blue)
        shader.setFloatUniform("uBase", base.red, base.green, base.blue)
        shader.setFloatUniform("uHi", sim.hx, sim.hy)
        shader.setFloatUniform("uTouchAmt", sim.touchAmt)
        shader.setFloatUniform("uPress", sim.pressX, sim.pressY)
        shader.setFloatUniform("uRipple", sim.ripple)
    }

    companion object {
        const val SOURCE: String = """
uniform float2 uSize;
uniform float uTime;
uniform float2 uP0;
uniform float2 uP1;
uniform float2 uP2;
uniform float2 uP3;
uniform float4 uRad;
uniform float3 uC0;
uniform float3 uC1;
uniform float3 uC2;
uniform float3 uC3;
uniform float3 uBase;
uniform float2 uHi;
uniform float uTouchAmt;
uniform float2 uPress;
uniform float uRipple;

half4 main(float2 fc) {
    float m = min(uSize.x, uSize.y);
    float2 warp = float2(sin(fc.y * 0.016 + uTime * 1.1), cos(fc.x * 0.014 - uTime * 0.9)) * (m * 0.035);
    float2 q = fc + warp;

    float2 d0 = q - uP0;
    float2 d1 = q - uP1;
    float2 d2 = q - uP2;
    float2 d3 = q - uP3;
    float s0 = exp(-dot(d0, d0) / (uRad.x * uRad.x));
    float s1 = exp(-dot(d1, d1) / (uRad.y * uRad.y));
    float s2 = exp(-dot(d2, d2) / (uRad.z * uRad.z));
    float s3 = exp(-dot(d3, d3) / (uRad.w * uRad.w));

    float k = 4.6;
    float w0 = exp(k * s0);
    float w1 = exp(k * s1);
    float w2 = exp(k * s2);
    float w3 = exp(k * s3);
    float wb = exp(k * 0.08);
    float sum = w0 + w1 + w2 + w3 + wb;

    float3 col = (uC0 * w0 + uC1 * w1 + uC2 * w2 + uC3 * w3 + uBase * wb) / sum;

    float2 p = fc - uSize * 0.5;
    float r = m * 0.12;
    float2 qq = abs(p) - uSize * 0.5 + float2(r);
    float sd = min(max(qq.x, qq.y), 0.0) + length(max(qq, float2(0.0))) - r;
    float rim = 1.0 - smoothstep(0.0, m * 0.18, -sd);
    col = mix(col, float3(0.96, 0.94, 0.98), rim * 0.20);

    float2 dh = fc - uHi;
    float hi = exp(-dot(dh, dh) / (m * m * 0.07));
    col = col + float3(hi * (0.12 + 0.40 * uTouchAmt));

    float rd = length(fc - uPress);
    float rr = m * (0.10 + 1.15 * uRipple);
    float rt = (rd - rr) / (m * 0.10);
    float ring = exp(-rt * rt) * (1.0 - uRipple) * (1.0 - uRipple);
    col = col + float3(ring * 0.30);

    float n = fract(sin(dot(fc, float2(12.9898, 78.233))) * 43758.5453);
    col = col + float3((n - 0.5) * 0.035);

    return half4(half3(clamp(col, float3(0.0), float3(1.0))), 1.0);
}
"""
    }
}

private fun nowSeconds(nanos: Long): Float = (nanos % 1_000_000_000_000L) / 1_000_000_000f

/**
 * بطاقة لوحة ألوان سائلة: أربع بقع ملوّنة تنساب في بعضها، وتتفاعل مع اللمس والإمساك والسحب
 * (انجذاب وتنافر ودوران وتموّج ولمعة تتبع الإصبع وارتداد هلامي). الضغطة القصيرة تختار اللوحة.
 * على Android 13+ تُرسم بشيدر، وفي الأقدم بتدرجات شعاعية.
 */
@Composable
internal fun FluidPaletteCard(
    colors: List<Color>,
    base: Color,
    label: String,
    selected: Boolean,
    borderColor: Color,
    badgeBg: Color,
    badgeFg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sim = remember { FluidSim() }
    val currentClick by rememberUpdatedState(onClick)
    val haptic = LocalHapticFeedback.current
    var tick by remember { mutableIntStateOf(0) }
    val holder = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) FluidShader() else null
    }

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { t ->
                val dt = if (last == 0L) 0.016f else ((t - last) / 1_000_000_000f).coerceIn(0.001f, 0.033f)
                last = t
                sim.step(dt, nowSeconds(t))
                tick++
            }
        }
    }

    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier
            .graphicsLayer {
                val sc = if (tick >= 0) 1f - 0.035f * sim.touchAmt else 1f
                scaleX = sc
                scaleY = sc
            }
            .aspectRatio(1.12f)
            .clip(shape)
            .drawBehind {
                if (tick < 0) return@drawBehind
                sim.resize(size.width, size.height)
                if (holder != null) {
                    holder.update(sim, colors, base)
                    drawRect(brush = holder.brush)
                } else {
                    drawRect(base)
                    val r = min(size.width, size.height) * 0.6f
                    for (i in 0..3) {
                        val c = Offset(sim.x[i], sim.y[i])
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(colors[i], Color.Transparent),
                                center = c,
                                radius = r,
                            ),
                            radius = r,
                            center = c,
                        )
                    }
                }
            }
            .border(
                width = if (selected) 2.5.dp else 0.5.dp,
                color = if (selected) borderColor else borderColor.copy(alpha = 0.25f),
                shape = shape,
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    // نراقب الحدث في مرحلة Initial بلا استهلاك، فلا يقطعه تمرير الصفحة.
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    sim.press(down.position.x, down.position.y)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    var moved = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val ch = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!ch.pressed) break
                        val delta = ch.position - ch.previousPosition
                        sim.move(ch.position.x, ch.position.y, delta.x, delta.y)
                        if ((ch.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                            moved = true
                        }
                    }
                    sim.release()
                    if (!moved) currentClick()
                }
            },
    ) {
        Text(
            label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
                .background(Color.Black.copy(alpha = 0.28f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(24.dp)
                    .background(badgeBg, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                GlowIcon(IconKind.Check, badgeFg, 16.dp)
            }
        }
    }
}
