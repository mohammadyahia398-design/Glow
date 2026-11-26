package com.glow.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.glow.app.liquid.backdrop.Backdrop
import com.glow.app.liquid.backdrop.drawBackdrop
import com.glow.app.liquid.backdrop.effects.blur
import com.glow.app.liquid.backdrop.effects.lens
import com.glow.app.liquid.backdrop.effects.vibrancy
import com.glow.app.liquid.ui.utils.InteractiveHighlight
import com.glow.app.liquid.ui.utils.lerp

/**
 * الخلفية التي تنكسر خلف عناصر Glow القابلة للضغط داخل الشاشة الحالية.
 * null = لا انكسار (أندرويد أقدم من 13 أو خارج أي شاشة توفّرها) فيُرسم الزجاج البديل glowGlass.
 */
val LocalLiquidBackdrop = compositionLocalOf<Backdrop?> { null }

/**
 * سطح ليكويد جلاس موحّد لكل الأزرار والشرائح: عدسة تكسر ما خلفها، حافة لامعة،
 * صبغة بلون العنصر، وانتفاخ ولمعة تتبع الإصبع عند الضغط.
 * يُوضع قبل clickable في سلسلة Modifier.
 */
@Composable
fun Modifier.glowLiquid(
    shape: Shape,
    fill: Brush,
    isDark: Boolean,
    rimColor: Color = Color.White,
    glow: Color? = null,
    fillAlpha: Float = 1f,
    interactive: Boolean = true,
): Modifier {
    val backdrop = LocalLiquidBackdrop.current
    val scope = rememberCoroutineScope()
    val highlight = remember(scope) { InteractiveHighlight(animationScope = scope) }
    if (backdrop == null) return this.glowGlass(shape, fill, isDark, rimColor, glow)
    val sheen = if (isDark) 0.14f else 0.38f
    return this
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(2f.dp.toPx())
                val d = size.minDimension
                lens(d * 0.25f, d * 0.5f)
            },
            layerBlock = if (interactive) {
                {
                    val s = lerp(1f, 1f + 4f.dp.toPx() / size.height, highlight.pressProgress)
                    scaleX = s
                    scaleY = s
                }
            } else {
                null
            },
            onDrawSurface = {
                drawRect(fill, alpha = fillAlpha)
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = sheen), Color.Transparent),
                        endY = size.height * 0.5f,
                    ),
                )
            },
        )
        .then(if (interactive) highlight.modifier.then(highlight.gestureModifier) else Modifier)
}
