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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glow.app.ui.common.GlowIcon
import com.glow.app.ui.common.IconKind
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * محاكاة أربع بقع سائلة داخل بطاقة. كل بقعة نابض يشدّها إلى موضعها الأصلي (مع انجراف بطيء)،
 * والإصبع يدفعها بعيداً ويسحبها مع حركته، فترتدّ بعد الإفلات كالسائل.
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
    var tx = 0f
        private set
    var ty = 0f
        private set
    private var touching = false
    private var fvx = 0f
    private var fvy = 0f

    private val homeX = floatArrayOf(0.26f, 0.76f, 0.30f, 0.78f)
    private val homeY = floatArrayOf(0.28f, 0.30f, 0.76f, 0.74f)

    fun resize(width: Float, height: Float) {
        val first = w <= 0f
        w = width
        h = height
        if (first && w > 0f) {
            tx = 0.40f * w
            ty = 0.30f * h
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
        fvx = 0f
        fvy = 0f
    }

    fun move(px: Float, py: Float, dx: Float, dy: Float) {
        tx = px
        ty = py
        fvx = dx * 60f
        fvy = dy * 60f
    }

    fun release() {
        touching = false
    }

    fun step(dt: Float, seconds: Float) {
        time = seconds
        if (w <= 0f || h <= 0f) return
        val m = min(w, h)
        val reach = m * 0.62f
        touchAmt += ((if (touching) 1f else 0f) - touchAmt) * min(1f, dt * 8f)
        fvx *= 1f - min(1f, dt * 10f)
        fvy *= 1f - min(1f, dt * 10f)
        for (i in 0..3) {
            val hx = homeX[i] * w + sin(seconds * 0.7f + i * 1.9f) * 0.07f * w
            val hy = homeY[i] * h + cos(seconds * 0.6f + i * 2.4f) * 0.07f * h
            var ax = (hx - x[i]) * 34f
            var ay = (hy - y[i]) * 34f
            if (touching) {
                val dx = x[i] - tx
                val dy = y[i] - ty
                val dist = sqrt(dx * dx + dy * dy) + 0.001f
                if (dist < reach) {
                    val f = 1f - dist / reach
                    val push = f * f * 14f * m
                    ax += dx / dist * push + fvx * f * 5f
                    ay += dy / dist * push + fvy * f * 5f
                }
            }
            val damp = 1f - min(0.9f, 4.5f * dt)
            vx[i] = (vx[i] + ax * dt) * damp
            vy[i] = (vy[i] + ay * dt) * damp
            x[i] = (x[i] + vx[i] * dt).coerceIn(-0.2f * w, 1.2f * w)
            y[i] = (y[i] + vy[i] * dt).coerceIn(-0.2f * h, 1.2f * h)
        }
    }
}

/**
 * شيدر AGSL: كل بقعة لها «مجال» غاوسي واسع، واللون عند كل بكسل مزج ناعم متصل بين الألوان
 * بأوزان أُسّية خفيفة، فتنساب الألوان في بعضها كتدرّج زجاجي سائل بلا حدود حادّة.
 * حافة البطاقة تميل إلى بياض حليبي خفيف، وتحت الإصبع لمعة ناعمة، وحبيبات دقيقة كالصور المرجعية.
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
        shader.setFloatUniform("uRad", m * 0.82f, m * 0.78f, m * 0.76f, m * 0.80f)
        shader.setFloatUniform("uC0", colors[0].red, colors[0].green, colors[0].blue)
        shader.setFloatUniform("uC1", colors[1].red, colors[1].green, colors[1].blue)
        shader.setFloatUniform("uC2", colors[2].red, colors[2].green, colors[2].blue)
        shader.setFloatUniform("uC3", colors[3].red, colors[3].green, colors[3].blue)
        shader.setFloatUniform("uBase", base.red, base.green, base.blue)
        shader.setFloatUniform("uTouch", sim.tx, sim.ty)
        shader.setFloatUniform("uTouchAmt", sim.touchAmt)
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
uniform float2 uTouch;
uniform float uTouchAmt;

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

    float k = 3.6;
    float w0 = exp(k * s0);
    float w1 = exp(k * s1);
    float w2 = exp(k * s2);
    float w3 = exp(k * s3);
    float wb = exp(k * 0.10);
    float sum = w0 + w1 + w2 + w3 + wb;

    float3 col = (uC0 * w0 + uC1 * w1 + uC2 * w2 + uC3 * w3 + uBase * wb) / sum;

    float2 p = fc - uSize * 0.5;
    float r = m * 0.12;
    float2 qq = abs(p) - uSize * 0.5 + float2(r);
    float sd = min(max(qq.x, qq.y), 0.0) + length(max(qq, float2(0.0))) - r;
    float rim = 1.0 - smoothstep(0.0, m * 0.18, -sd);
    col = mix(col, float3(0.96, 0.94, 0.98), rim * 0.20);

    float2 dh = fc - uTouch;
    float hi = exp(-dot(dh, dh) / (m * m * 0.12));
    col = col + float3(hi * (0.10 + 0.30 * uTouchAmt));

    float n = fract(sin(dot(fc, float2(12.9898, 78.233))) * 43758.5453);
    col = col + float3((n - 0.5) * 0.035);

    return half4(half3(clamp(col, float3(0.0), float3(1.0))), 1.0);
}
"""
    }
}

private fun nowSeconds(nanos: Long): Float = (nanos % 1_000_000_000_000L) / 1_000_000_000f

/**
 * بطاقة لوحة ألوان سائلة: أربع بقع ملوّنة تتحرك ببطء، وتتفاعل مع اللمس وسحب الإصبع كالسوائل
 * التي لا تمتزج. الضغطة القصيرة تختار اللوحة. على Android 13+ تُرسم بشيدر، وفي الأقدم بتدرجات شعاعية.
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
                    val down = awaitFirstDown(requireUnconsumed = false)
                    sim.press(down.position.x, down.position.y)
                    var moved = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val ch = event.changes.firstOrNull() ?: break
                        if (!ch.pressed) break
                        if (ch.isConsumed) {
                            moved = true
                            break
                        }
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
