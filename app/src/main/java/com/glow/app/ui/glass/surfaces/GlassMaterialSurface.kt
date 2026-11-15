package com.glow.app.ui.glass.surfaces

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.glow.app.ui.glass.core.GlassMaterial
import com.glow.app.ui.glass.motion.GlassInteractionState

/**
 * [GlassSurface] بحسب [GlassMaterial]: النمط والعمق يأتيان من المادة.
 * للتحكم الدقيق استعمل الدالة الأصلية التي تأخذ `style` و`depth`.
 */
@Composable
fun GlassSurface(
    material: GlassMaterial,
    modifier: Modifier = Modifier,
    backdrop: GlassBackdrop? = null,
    shape: Shape = GlassSurfaceDefaults.CardShape,
    interaction: GlassInteractionState? = null,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    GlassSurface(
        modifier = modifier,
        backdrop = backdrop,
        shape = shape,
        style = material.style,
        depth = material.depth,
        interaction = interaction,
        contentAlignment = contentAlignment,
        content = content,
    )
}
