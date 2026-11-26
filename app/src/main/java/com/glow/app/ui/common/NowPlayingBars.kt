package com.glow.app.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** مؤشر «قيد التشغيل»: ثلاثة أعمدة تتحرك أثناء التشغيل وتثبت عند الإيقاف المؤقت. */
@Composable
fun NowPlayingBars(
    color: Color,
    playing: Boolean,
    modifier: Modifier = Modifier.size(18.dp),
) {
    val transition = rememberInfiniteTransition(label = "nowPlayingBars")
    val a by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(520, easing = LinearEasing), RepeatMode.Reverse),
        label = "barA",
    )
    val b by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(430, easing = LinearEasing), RepeatMode.Reverse),
        label = "barB",
    )
    val c by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(610, easing = LinearEasing), RepeatMode.Reverse),
        label = "barC",
    )
    Canvas(modifier) {
        val barW = size.width * 0.22f
        val gap = (size.width - barW * 3f) / 2f
        val heights = if (playing) listOf(a, b, c) else listOf(0.4f, 0.7f, 0.5f)
        heights.forEachIndexed { i, f ->
            val h = size.height * f
            drawRoundRect(
                color = color,
                topLeft = Offset(i * (barW + gap), size.height - h),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f),
            )
        }
    }
}
