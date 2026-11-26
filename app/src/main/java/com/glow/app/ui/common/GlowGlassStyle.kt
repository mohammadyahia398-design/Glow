package com.glow.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * سطح زجاجي خفيف للأزرار والشرائح داخل الشاشات: تعبئة متدرّجة، لمعة علوية، وحافة ضوء.
 * لا يعتمد على التقاط الخلفية، فيعمل على كل إصدارات أندرويد وداخل أي شاشة.
 */
fun Modifier.glowGlass(
    shape: Shape,
    fill: Brush,
    isDark: Boolean,
    rimColor: Color = Color.White,
    glow: Color? = null,
): Modifier {
    val highlight = if (isDark) 0.22f else 0.55f
    val rim = if (isDark) 0.40f else 0.70f
    return this
        .then(
            if (glow != null) {
                Modifier.shadow(
                    elevation = 8.dp,
                    shape = shape,
                    ambientColor = glow.copy(alpha = 0.35f),
                    spotColor = glow.copy(alpha = 0.50f),
                )
            } else Modifier,
        )
        .clip(shape)
        .background(fill)
        .drawBehind {
            drawRect(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = highlight), Color.Transparent),
                    endY = this.size.height * 0.55f,
                ),
            )
        }
        .border(
            1.dp,
            Brush.linearGradient(
                listOf(
                    rimColor.copy(alpha = rim),
                    rimColor.copy(alpha = rim * 0.25f),
                    rimColor.copy(alpha = rim * 0.55f),
                ),
            ),
            shape,
        )
}
