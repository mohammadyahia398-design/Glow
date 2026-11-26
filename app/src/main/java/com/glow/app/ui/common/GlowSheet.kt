package com.glow.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.glow.app.liquid.backdrop.backdrops.layerBackdrop
import com.glow.app.liquid.backdrop.backdrops.rememberLayerBackdrop
import com.glow.app.ui.glass.core.rememberGlassTier
import com.glow.app.ui.theme.LocalGlowSkin

/**
 * لوحة سفلية بهوية Glow: زوايا علوية 32dp، سطح داكن موحَّد بلمعة وصبغة، ومقبض كبسولة.
 * تحتفظ بسلوك ModalBottomSheet (السحب والحجب) وتستبدل شكله فقط.
 * على أندرويد 13+ يصبح سطحها مصدر الزجاج السائل لما بداخلها (LocalDialogBackdrop)، كما في [GlowDialog].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlowBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val skin = LocalGlowSkin.current
    val useLiquid = rememberGlassTier().supportsRefraction
    val backdrop = rememberLayerBackdrop()
    val shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = Color.Transparent,
        contentColor = skin.text,
        shape = shape,
        tonalElevation = 0.dp,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        dragHandle = null,
        // السطح الزجاجي يمتد خلف شريط النظام، والمحتوى يتجنبه بنفسه.
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.40f), Color.White.copy(alpha = 0.05f))),
                    shape,
                ),
        ) {
            // طبقة المصدر: تُلتقط بعد layerBackdrop فقط، ولا تحمل أي محتوى فوقها.
            Box(
                Modifier
                    .matchParentSize()
                    .then(if (useLiquid) Modifier.layerBackdrop(backdrop) else Modifier)
                    .background(skin.sheet.copy(alpha = 0.94f))
                    .drawBehind {
                        drawRect(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
                                endY = size.height * 0.30f,
                            ),
                        )
                        drawRect(
                            Brush.verticalGradient(
                                listOf(skin.accent.copy(alpha = 0f), skin.accent.copy(alpha = 0.14f)),
                                startY = size.height * 0.5f,
                                endY = size.height,
                            ),
                        )
                        if (useLiquid) {
                            // كرتان ضوئيتان ناعمتان: هما ما ينكسر خلف الأزرار والمنزلقات الزجاجية.
                            val r1 = size.width * 0.8f
                            val c1 = Offset(size.width * 0.10f, size.height * 0.70f)
                            drawCircle(
                                Brush.radialGradient(
                                    listOf(skin.accent.copy(alpha = 0.30f), Color.Transparent),
                                    center = c1,
                                    radius = r1,
                                ),
                                radius = r1,
                                center = c1,
                            )
                            val r2 = size.width * 0.7f
                            val c2 = Offset(size.width * 0.95f, size.height * 0.30f)
                            drawCircle(
                                Brush.radialGradient(
                                    listOf(Color(0xFF7226FF).copy(alpha = 0.28f), Color.Transparent),
                                    center = c2,
                                    radius = r2,
                                ),
                                radius = r2,
                                center = c2,
                            )
                        }
                    },
            )
            CompositionLocalProvider(LocalDialogBackdrop provides (if (useLiquid) backdrop else null)) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
                    Box(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 10.dp, bottom = 6.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(skin.textSecondary.copy(alpha = 0.55f)),
                    )
                    content()
                }
            }
        }
    }
}
