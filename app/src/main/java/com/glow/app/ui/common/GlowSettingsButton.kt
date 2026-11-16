package com.glow.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** النيلي: لون زر الإعدادات، ومنه هالة انكسار العدسة. */
val GlowIndigo = Color(0xFF3730A3)

/**
 * زر الإعدادات: مربع مدوّر بثلاثة أشرطة (≡).
 * الافتراضي: حاوية نيلية وأشرطة بيضاء، وتبقى واضحة في الوضعين.
 * لتغيير الشكل عدّل [container] و[bars] أو [corner] من مكان الاستدعاء.
 */
@Composable
fun GlowSettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    corner: Dp = 14.dp,
    container: Color = GlowIndigo,
    bars: Color = Color.White,
    description: String = "الإعدادات",
) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(container)
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        GlowIcon(IconKind.Menu, bars, size)
    }
}
