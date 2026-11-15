package com.glow.app.ui.glass.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.glow.app.ui.glass.core.LocalGlassConfig
import com.glow.app.ui.glass.motion.glassTween
import com.glow.app.ui.theme.LocalGlowSkin

/**
 * حلقة حالة فوق السطح الزجاجي:
 * - [selected]: حلقة رفيعة بلون التمييز.
 * - [focused]: حلقة أسمك وأوضح للتنقل بلوحة المفاتيح، لإمكانية الوصول.
 *
 * الانتقال ناعم ويصير فورياً عند تقليل الحركة، والقراءة أثناء الرسم فلا تُعاد التركيبة.
 */
@Composable
fun Modifier.glassStateRing(
    selected: Boolean,
    focused: Boolean,
    shape: Shape,
): Modifier {
    val skin = LocalGlowSkin.current
    val reduce = LocalGlassConfig.current.reduceMotion
    val selectedAmount = animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = glassTween(reduce),
        label = "glassSelectedRing",
    )
    val focusAmount = animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = glassTween(reduce, 120),
        label = "glassFocusRing",
    )
    val selectedColor: Color = skin.accent
    val focusColor: Color = skin.accentText
    return drawWithContent {
        drawContent()
        val outline = shape.createOutline(size, layoutDirection, this)
        val s = selectedAmount.value
        if (s > 0.01f) {
            drawOutline(outline, selectedColor.copy(alpha = 0.60f * s), style = Stroke(1.2.dp.toPx()))
        }
        val f = focusAmount.value
        if (f > 0.01f) {
            drawOutline(outline, focusColor.copy(alpha = 0.95f * f), style = Stroke(2.dp.toPx()))
        }
    }
}
