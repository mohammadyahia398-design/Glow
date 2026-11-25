/*
 * Based on "Liquid Glass (Backdrop)" by Kyant (https://github.com/Kyant0/AndroidLiquidGlass),
 * Apache License 2.0. Modified for Glow. See THIRD_PARTY/liquid-glass/.
 */
package com.glow.app.liquid.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.glow.app.liquid.backdrop.Backdrop
import com.glow.app.liquid.backdrop.drawBackdrop
import com.glow.app.liquid.backdrop.effects.blur
import com.glow.app.liquid.backdrop.effects.lens
import com.glow.app.liquid.backdrop.effects.vibrancy

/** بطاقة زجاجية سائلة: تنكسر الخلفية عند حوافها، ومحتواها فوقها بلا تأثير. للأجهزة التي تدعم الانكسار فقط. */
@Composable
fun LiquidGlassCard(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    surfaceColor: Color = Color.Unspecified,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(10f.dp.toPx())
                    lens(14f.dp.toPx(), 28f.dp.toPx())
                },
                onDrawSurface = {
                    if (surfaceColor.isSpecified) drawRect(surfaceColor)
                },
            )
            .clip(shape),
        content = content,
    )
}
