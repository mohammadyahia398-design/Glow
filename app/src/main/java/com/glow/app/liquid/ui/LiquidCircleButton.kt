/*
 * Based on "Liquid Glass (Backdrop)" by Kyant (https://github.com/Kyant0/AndroidLiquidGlass),
 * Apache License 2.0. Same glass recipe as LiquidButton, as a round icon button of a given size.
 * See THIRD_PARTY/liquid-glass/.
 */
package com.glow.app.liquid.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.glow.app.liquid.backdrop.Backdrop
import com.glow.app.liquid.backdrop.drawBackdrop
import com.glow.app.liquid.backdrop.effects.blur
import com.glow.app.liquid.backdrop.effects.lens
import com.glow.app.liquid.backdrop.effects.vibrancy
import com.glow.app.liquid.ui.utils.InteractiveHighlight
import com.glow.app.liquid.ui.utils.lerp
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/** زر دائري زجاجي: ينتفخ عند اللمس وتنكسر خلفه الألوان. [description] لقارئ الشاشة. */
@Composable
fun LiquidCircleButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    buttonSize: Dp,
    description: String,
    modifier: Modifier = Modifier,
    surfaceColor: Color = Color.Unspecified,
    content: @Composable BoxScope.() -> Unit,
) {
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    Box(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(50) },
                effects = {
                    vibrancy()
                    blur(2f.dp.toPx())
                    lens(buttonSize.toPx() * 0.2f, buttonSize.toPx() * 0.4f)
                },
                layerBlock = {
                    val width = size.width
                    val height = size.height

                    val progress = interactiveHighlight.pressProgress
                    val scale = lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)

                    val maxOffset = size.minDimension
                    val initialDerivative = 0.05f
                    val offset = interactiveHighlight.offset
                    translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                    translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                    val maxDragScale = 4f.dp.toPx() / size.height
                    val offsetAngle = atan2(offset.y, offset.x)
                    scaleX =
                        scale +
                            maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                            (width / height).coerceAtMost(1f)
                    scaleY =
                        scale +
                            maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                            (height / width).coerceAtMost(1f)
                },
                onDrawSurface = {
                    if (surfaceColor.isSpecified) drawRect(surfaceColor)
                },
            )
            .semantics { contentDescription = description }
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .then(interactiveHighlight.modifier)
            .then(interactiveHighlight.gestureModifier)
            .size(buttonSize),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
