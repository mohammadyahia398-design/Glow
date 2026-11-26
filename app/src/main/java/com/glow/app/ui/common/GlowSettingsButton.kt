package com.glow.app.ui.common

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.glow.app.ui.theme.LocalGlowSkin

/** Compatibility color retained for callers that referenced the old token. */
val GlowIndigo = Color(0xFF7226FF)

/** Settings action styled with the same quiet luminous surface as the rest of Glow. */
@Composable
fun GlowSettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    corner: Dp = 50.dp,
    container: Color? = null,
    bars: Color? = null,
    description: String = "الإعدادات",
) {
    val skin = LocalGlowSkin.current
    val shape = RoundedCornerShape(corner)
    val surface = container ?: skin.panel
    val iconColor = bars ?: skin.accentText
    Box(
        modifier
            .size(size)
            .glowLiquid(
                shape,
                Brush.verticalGradient(listOf(surface.copy(alpha = 0.98f), surface.copy(alpha = 0.78f))),
                skin.isDark,
                skin.glassBorder,
                fillAlpha = 0.5f,
            )
            .semantics { contentDescription = description }
            .clickable(
                interactionSource = null,
                indication = if (LocalLiquidBackdrop.current != null) null else LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        GlowIcon(IconKind.Menu, iconColor, 22.dp)
    }
}
