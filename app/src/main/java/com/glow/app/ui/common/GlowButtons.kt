package com.glow.app.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glow.app.ui.icons.GlowGlyph
import com.glow.app.ui.icons.Glyph
import com.glow.app.ui.theme.LocalGlowSkin

/*
 * أزرار Glow الموحّدة: كبسولات بنفس الاستدارة والسماكة والضغطة النابضة،
 * بدل أزرار Material الافتراضية. الاستعمال يطابق Button/OutlinedButton/TextButton.
 */

private val PillShape = RoundedCornerShape(50)

@Composable
private fun GlowButtonBase(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    background: Brush,
    border: BorderStroke?,
    contentColor: Color,
    fontSize: TextUnit,
    horizontalPadding: Dp,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "glowButtonPress",
    )
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.4f
            }
            .clip(PillShape)
            .background(background)
            .then(if (border != null) Modifier.border(border, PillShape) else Modifier)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            ProvideTextStyle(TextStyle(fontSize = fontSize, fontWeight = FontWeight.SemiBold)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    content = content,
                )
            }
        }
    }
}

/** الزر الرئيسي: تدرّج بلون التمييز وحافة لامعة. */
@Composable
fun GlowFilledButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val skin = LocalGlowSkin.current
    GlowButtonBase(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        background = Brush.verticalGradient(listOf(lerp(skin.accent, Color.White, 0.14f), skin.accent)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
        contentColor = skin.onAccent,
        fontSize = 16.sp,
        horizontalPadding = 22.dp,
        content = content,
    )
}

/** الزر الثانوي: سطح شفاف بلون المحتوى وحافة رفيعة. */
@Composable
fun GlowOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = Color.Unspecified,
    content: @Composable RowScope.() -> Unit,
) {
    val skin = LocalGlowSkin.current
    val color = if (contentColor.isSpecified) contentColor else skin.accentText
    GlowButtonBase(
        onClick = onClick,
        modifier = modifier.heightIn(min = 44.dp),
        enabled = enabled,
        background = SolidColor(color.copy(alpha = 0.10f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.38f)),
        contentColor = color,
        fontSize = 15.sp,
        horizontalPadding = 20.dp,
        content = content,
    )
}

/** الزر النصي الصغير: كبسولة خفيفة بلون التمييز. */
@Composable
fun GlowTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val skin = LocalGlowSkin.current
    GlowButtonBase(
        onClick = onClick,
        modifier = modifier.heightIn(min = 36.dp),
        enabled = enabled,
        background = SolidColor(skin.accentText.copy(alpha = 0.12f)),
        border = null,
        contentColor = skin.accentText,
        fontSize = 14.sp,
        horizontalPadding = 14.dp,
        content = content,
    )
}

/** زر دائري صغير بأيقونة من محرّك الأيقونات (بدل الرموز النصية ↑ ↓ ×). */
@Composable
fun GlowRoundButton(
    glyph: Glyph,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 36.dp,
    tint: Color = Color.Unspecified,
) {
    val skin = LocalGlowSkin.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "glowRoundPress",
    )
    Box(
        modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.35f
            }
            .clip(CircleShape)
            .background(skin.chip)
            .semantics { contentDescription = description }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        GlowGlyph(glyph, tint = if (tint.isSpecified) tint else skin.text, size = size * 0.5f)
    }
}
