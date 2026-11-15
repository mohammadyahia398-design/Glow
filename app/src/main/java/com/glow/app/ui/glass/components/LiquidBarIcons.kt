package com.glow.app.ui.glass.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.glow.app.ui.common.GlowIcon
import com.glow.app.ui.common.IconKind

/**
 * أيقونات الشريط الزجاجي: خمس أيقونات بنمط واحد (خط 1.9 من شبكة 24، أطراف مدوّرة،
 * بلا تعبئة) وبنفس الحجم البصري، فتتناسق معاً. ما عداها يعود إلى [GlowIcon].
 */
@Composable
fun LiquidBarIcon(kind: IconKind, tint: Color, size: Dp = 26.dp) {
    if (kind !in BarKinds) {
        GlowIcon(kind, tint, size)
        return
    }
    Canvas(Modifier.size(size)) {
        val k = this.size.width / 24f
        val st = Stroke(width = 1.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        scale(k, k, pivot = Offset.Zero) {
            when (kind) {
                IconKind.Note -> {
                    drawCircle(tint, radius = 2.9f, center = Offset(8.2f, 17.2f), style = st)
                    val stem = Path().apply {
                        moveTo(11.1f, 17.2f); lineTo(11.1f, 5.2f)
                        cubicTo(13.6f, 5.4f, 17.4f, 6.4f, 17.6f, 10.4f)
                    }
                    drawPath(stem, tint, style = st)
                }
                IconKind.Album -> {
                    drawCircle(tint, radius = 8.6f, center = Offset(12f, 12f), style = st)
                    drawCircle(tint, radius = 2.6f, center = Offset(12f, 12f), style = st)
                }
                IconKind.Artist -> {
                    drawCircle(tint, radius = 3.4f, center = Offset(12f, 8.2f), style = st)
                    val body = Path().apply {
                        moveTo(5f, 19.5f)
                        cubicTo(5f, 16.2f, 8f, 14.6f, 12f, 14.6f)
                        cubicTo(16f, 14.6f, 19f, 16.2f, 19f, 19.5f)
                    }
                    drawPath(body, tint, style = st)
                }
                IconKind.Heart -> {
                    val heart = Path().apply {
                        moveTo(12f, 19.8f)
                        cubicTo(5.2f, 15.2f, 3.6f, 11.4f, 5.2f, 8.2f)
                        cubicTo(6.6f, 5.6f, 10.2f, 5.6f, 12f, 8.6f)
                        cubicTo(13.8f, 5.6f, 17.4f, 5.6f, 18.8f, 8.2f)
                        cubicTo(20.4f, 11.4f, 18.8f, 15.2f, 12f, 19.8f)
                        close()
                    }
                    drawPath(heart, tint, style = st)
                }
                else -> {
                    val lines = Path().apply {
                        moveTo(4f, 7f); lineTo(15f, 7f)
                        moveTo(4f, 12f); lineTo(15f, 12f)
                        moveTo(4f, 17f); lineTo(9.5f, 17f)
                    }
                    drawPath(lines, tint, style = st)
                    drawCircle(tint, radius = 2.1f, center = Offset(16.4f, 17.6f), style = st)
                    val stem = Path().apply {
                        moveTo(18.5f, 17.6f); lineTo(18.5f, 10.6f)
                        cubicTo(19.6f, 10.8f, 20.6f, 11.4f, 20.8f, 13f)
                    }
                    drawPath(stem, tint, style = st)
                }
            }
        }
    }
}

private val BarKinds = setOf(
    IconKind.Note, IconKind.Album, IconKind.Artist, IconKind.Heart, IconKind.Playlist,
)
