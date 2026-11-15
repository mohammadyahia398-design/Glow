package com.glow.app.ui.glass.components

import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.glow.app.ui.glass.core.GlassDepth
import com.glow.app.ui.glass.core.GlassStyle
import com.glow.app.ui.glass.motion.glassClickable
import com.glow.app.ui.glass.motion.rememberGlassInteraction
import com.glow.app.ui.glass.surfaces.GlassBackdrop
import com.glow.app.ui.glass.surfaces.GlassSurface

private val PillShape = RoundedCornerShape(percent = 50)

/** الحد الأدنى لمنطقة اللمس الموصى بها (48dp). */
private val MinTouchTarget = 48.dp

/**
 * زر زجاجي على شكل كبسولة بمحتوى أفقي (نص وأيقونة).
 *
 * عند [enabled] = false يخفت الزر ولا يتفاعل مع اللمس.
 */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: GlassBackdrop? = null,
    enabled: Boolean = true,
    selected: Boolean = false,
    style: GlassStyle = GlassStyle.ClearInteractive,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = rememberGlassInteraction()
    val focused by interaction.interactionSource.collectIsFocusedAsState()

    GlassSurface(
        modifier = modifier
            .heightIn(min = MinTouchTarget)
            .alpha(if (enabled) 1f else 0.4f)
            .glassStateRing(selected, focused && enabled, PillShape)
            .glassClickable(
                state = interaction,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        backdrop = backdrop,
        shape = PillShape,
        style = if (selected) GlassStyle.Interactive else style,
        depth = GlassDepth.Raised,
        interaction = if (enabled) interaction else null,
        contentAlignment = Alignment.Center,
        pressedScale = 1.05f,
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/**
 * زر دائري زجاجي لأيقونة واحدة.
 *
 * @param contentDescription وصف الزر لقارئات الشاشة؛ إلزامي لأن الزر بلا نص.
 * @param size قطر الزر؛ لا يُنصح بأقل من 48dp.
 */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    backdrop: GlassBackdrop? = null,
    enabled: Boolean = true,
    selected: Boolean = false,
    size: Dp = MinTouchTarget,
    style: GlassStyle = GlassStyle.ClearInteractive,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = rememberGlassInteraction()
    val focused by interaction.interactionSource.collectIsFocusedAsState()
    val description = contentDescription

    GlassSurface(
        modifier = modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.4f)
            .semantics { this.contentDescription = description }
            .glassStateRing(selected, focused && enabled, CircleShape)
            .glassClickable(
                state = interaction,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        backdrop = backdrop,
        shape = CircleShape,
        style = if (selected) GlassStyle.Interactive else style,
        depth = GlassDepth.Raised,
        interaction = if (enabled) interaction else null,
        contentAlignment = Alignment.Center,
        pressedScale = 1.10f,
        content = content,
    )
}
