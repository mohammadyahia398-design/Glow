package com.glow.app.ui.settings

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glow.app.ui.theme.LocalGlowSkin

/**
 * منزلق زجاجي بكبسولة سميكة: حافة مضيئة بلون التمييز، تعبئة متدرجة لامعة،
 * وهج عند طرف التعبئة، وثلاث نقاط مقبض. يتسمّك قليلاً عند اللمس.
 *
 * التعبئة دائماً من اليسار (كالمرجع) بغضّ النظر عن اتجاه الشاشة.
 *
 * @param fillColors ألوان تدرّج التعبئة على كامل المسار. فارغة = أبيض حليبي.
 */
@Composable
fun GlowGlassSlider(
    label: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    fillColors: List<Color> = emptyList(),
    rimColor: Color = LocalGlowSkin.current.accent,
) {
    val skin = LocalGlowSkin.current
    val currentChange by rememberUpdatedState(onValueChange)
    var pressed by remember { mutableStateOf(false) }
    val press by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "glowSliderPress",
    )
    val f = value.coerceIn(0f, 1f)

    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, color = skin.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(valueText, color = skin.textSecondary, fontSize = 13.sp)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .graphicsLayer {
                    val sc = 1f + 0.07f * press
                    scaleY = sc
                    scaleX = 1f + 0.012f * press
                }
                .semantics {
                    contentDescription = label
                    progressBarRangeInfo = ProgressBarRangeInfo(f, 0f..1f)
                    setProgress { target -> currentChange(target.coerceIn(0f, 1f)); true }
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        pressed = true
                        val minFill = size.height / 2f
                        fun map(x: Float): Float =
                            ((x - minFill) / (size.width - minFill)).coerceIn(0f, 1f)
                        currentChange(map(down.position.x))
                        down.consume()
                        while (true) {
                            val event = awaitPointerEvent()
                            val ch = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!ch.pressed) break
                            currentChange(map(ch.position.x))
                            ch.consume()
                        }
                        pressed = false
                    }
                }
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    val r = h / 2f
                    val minFill = r
                    val fillW = minFill + f * (w - minFill)
                    val corner = CornerRadius(r, r)
                    val track = Path().apply { addRoundRect(RoundRect(0f, 0f, w, h, corner)) }
                    val dark = skin.isDark

                    // 1) الجسم الزجاجي الداكن
                    clipPath(track) {
                        drawRect(Color(0xFF000002).copy(alpha = if (dark) 0.90f else 0.78f))
                        if (fillColors.size >= 2) {
                            drawRect(Brush.horizontalGradient(fillColors, 0f, w), alpha = 0.22f)
                        }

                        // 2) التعبئة اللامعة
                        clipPath(Path().apply { addRect(androidx.compose.ui.geometry.Rect(0f, 0f, fillW, h)) }) {
                            if (fillColors.size >= 2) {
                                drawRect(Brush.horizontalGradient(fillColors, 0f, w))
                                drawRect(Color.White.copy(alpha = 0.16f))
                            } else {
                                drawRect(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFD8CCF2), Color(0xFFFFFFFF)),
                                        0f, fillW,
                                    ),
                                )
                            }
                            // لمعة علوية وظل سفلي يعطيان حجم الزجاج
                            drawRect(
                                Brush.verticalGradient(
                                    0f to Color.White.copy(alpha = 0.55f),
                                    0.5f to Color.White.copy(alpha = 0.06f),
                                    1f to Color.Black.copy(alpha = 0.16f),
                                ),
                            )
                        }

                        // 3) وهج طرف التعبئة
                        val glowW = 34.dp.toPx()
                        drawRect(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, rimColor.copy(alpha = 0.55f + 0.25f * press), Color.White.copy(alpha = 0.95f)),
                                fillW - glowW, fillW,
                            ),
                            topLeft = Offset(fillW - glowW, 0f),
                            size = Size(glowW, h),
                        )
                        drawRect(
                            Brush.horizontalGradient(
                                listOf(rimColor.copy(alpha = 0.50f), Color.Transparent),
                                fillW, fillW + 14.dp.toPx(),
                            ),
                            topLeft = Offset(fillW, 0f),
                            size = Size(14.dp.toPx(), h),
                        )

                        // 4) انعكاس زجاجي علوي على كامل الكبسولة
                        drawRect(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.20f), Color.Transparent),
                                0f, h * 0.5f,
                            ),
                            size = Size(w, h * 0.5f),
                        )

                        // 5) نقاط المقبض
                        val dotX = w - r
                        val dotColor = if (dotX < fillW) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.55f)
                        val gap = 9.dp.toPx()
                        for (i in -1..1) {
                            drawCircle(dotColor, 2.6.dp.toPx(), Offset(dotX, h / 2f + i * gap))
                        }
                    }

                    // 6) الحافة المضيئة: هالة خارجية ثم خط واضح ثم خيط أبيض داخلي
                    val halo = 0.20f + 0.25f * press
                    drawRoundRect(rimColor.copy(alpha = halo * 0.6f), Offset(-3.dp.toPx(), -3.dp.toPx()),
                        Size(w + 6.dp.toPx(), h + 6.dp.toPx()), CornerRadius(r + 3.dp.toPx()), style = Stroke(6.dp.toPx()))
                    drawRoundRect(rimColor.copy(alpha = halo), Offset.Zero, Size(w, h), corner, style = Stroke(4.dp.toPx()))
                    drawRoundRect(rimColor.copy(alpha = 0.95f), Offset(1.25.dp.toPx(), 1.25.dp.toPx()),
                        Size(w - 2.5.dp.toPx(), h - 2.5.dp.toPx()), CornerRadius(r - 1.25.dp.toPx()), style = Stroke(2.5.dp.toPx()))
                    drawRoundRect(
                        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent), 0f, h * 0.6f),
                        Offset(3.5.dp.toPx(), 3.5.dp.toPx()),
                        Size(w - 7.dp.toPx(), h - 7.dp.toPx()), CornerRadius(r - 3.5.dp.toPx()), style = Stroke(1.dp.toPx()),
                    )
                },
        ) {}
    }
}
