package com.glow.app.ui.common

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import com.glow.app.liquid.backdrop.Backdrop
import com.glow.app.liquid.backdrop.backdrops.layerBackdrop
import com.glow.app.liquid.backdrop.backdrops.rememberLayerBackdrop
import com.glow.app.liquid.ui.LiquidButton
import com.glow.app.ui.glass.core.rememberGlassTier
import com.glow.app.ui.theme.LocalGlowSkin
import com.glow.app.ui.theme.accentBrush

/** خلفية النافذة الزجاجية التي تنكسر خلف أزرارها وصفوفها (أندرويد 13+)، وnull حيث لا انكسار. */
val LocalDialogBackdrop = compositionLocalOf<Backdrop?> { null }

/**
 * نافذة Glow: بطاقة مدوّرة (32dp) بسطح داكن موحَّد، عنوان، محتوى، وأزرار سفلية متساوية العرض.
 * على أندرويد 13+ تصبح خلفية البطاقة مصدر الزجاج السائل لأزرارها وصفوفها، وما دون ذلك يبقى الشكل المسطّح.
 */
@Composable
fun GlowDialog(
    title: String,
    onDismiss: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val skin = LocalGlowSkin.current
    val useLiquid = rememberGlassTier().supportsRefraction
    val backdrop = rememberLayerBackdrop()
    Dialog(onDismissRequest = onDismiss) {
        DialogBackdropBlur()
        val glassShape = RoundedCornerShape(32.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .clip(glassShape)
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.40f), Color.White.copy(alpha = 0.05f))),
                    glassShape,
                ),
        ) {
            // طبقة المصدر: تُلتقط بعد layerBackdrop فقط، ولا تحمل أي محتوى فوقها.
            Box(
                Modifier
                    .matchParentSize()
                    .then(if (useLiquid) Modifier.layerBackdrop(backdrop) else Modifier)
                    .background(skin.sheet.copy(alpha = 0.90f))
                    .drawBehind {
                        // لمعة علوية وصبغة خفيفة من الأسفل
                        drawRect(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
                                endY = size.height * 0.35f,
                            ),
                        )
                        drawRect(
                            Brush.verticalGradient(
                                listOf(skin.accent.copy(alpha = 0f), skin.accent.copy(alpha = 0.16f)),
                                startY = size.height * 0.5f,
                                endY = size.height,
                            ),
                        )
                        if (useLiquid) {
                            // كرتان ضوئيتان ناعمتان: هما ما ينكسر خلف الأزرار الزجاجية.
                            val r1 = size.width * 0.75f
                            val c1 = Offset(size.width * 0.12f, size.height * 0.62f)
                            drawCircle(
                                Brush.radialGradient(
                                    listOf(skin.accent.copy(alpha = 0.34f), Color.Transparent),
                                    center = c1,
                                    radius = r1,
                                ),
                                radius = r1,
                                center = c1,
                            )
                            val r2 = size.width * 0.65f
                            val c2 = Offset(size.width * 0.92f, size.height * 0.30f)
                            drawCircle(
                                Brush.radialGradient(
                                    listOf(Color(0xFF7226FF).copy(alpha = 0.30f), Color.Transparent),
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
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 12.dp),
                ) {
                    Text(
                        title,
                        color = skin.text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Column(
                        Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp),
                        content = content,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions,
                    )
                }
            }
        }
    }
}

/** زر سفلي في [GlowDialog]؛ [primary] يلوّنه بلون التمييز. زجاجي سائل حين تتوفر خلفية النافذة. */
@Composable
fun RowScope.GlowDialogAction(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    primary: Boolean = false,
) {
    val skin = LocalGlowSkin.current
    val backdrop = LocalDialogBackdrop.current
    if (backdrop != null) {
        LiquidButton(
            onClick = { if (enabled) onClick() },
            backdrop = backdrop,
            modifier = Modifier
                .weight(1f)
                .alpha(if (enabled) 1f else 0.4f),
            tint = if (primary) skin.accent else Color.Unspecified,
            surfaceColor = if (primary) Color.Unspecified else Color.White.copy(alpha = 0.10f),
        ) {
            Text(
                text,
                color = if (primary) skin.onAccent else skin.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    } else {
        Box(
            Modifier
                .weight(1f)
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .then(if (primary) Modifier.background(skin.accentBrush()) else Modifier.background(skin.chip))
                .alpha(if (enabled) 1f else 0.4f)
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text,
                color = if (primary) skin.onAccent else skin.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** صف خيار داخل [GlowDialog]: كبسولة زجاجية سائلة، أو كبسولة مسطّحة حيث لا انكسار. */
@Composable
fun GlowDialogRow(text: String, onClick: () -> Unit) {
    val skin = LocalGlowSkin.current
    val backdrop = LocalDialogBackdrop.current
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        if (backdrop != null) {
            LiquidButton(
                onClick = onClick,
                backdrop = backdrop,
                modifier = Modifier.fillMaxWidth(),
                surfaceColor = Color.White.copy(alpha = 0.08f),
            ) {
                Text(
                    text,
                    color = skin.text,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(skin.chip)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Text(text, color = skin.text, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** نافذة اختيار واحد من قائمة بأزرار راديو بلون الهوية. */
@Composable
fun <T> GlowChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val skin = LocalGlowSkin.current
    GlowDialog(
        title = title,
        onDismiss = onDismiss,
        actions = { GlowDialogAction("إلغاء", onDismiss) },
    ) {
        options.forEach { (value, label) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelect(value); onDismiss() }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlowRadio(value == selected)
                Spacer(Modifier.width(16.dp))
                Text(
                    label,
                    color = if (value == selected) skin.accentText else skin.text,
                    fontSize = 17.sp,
                )
            }
        }
    }
}

@Composable
private fun GlowRadio(selected: Boolean) {
    val skin = LocalGlowSkin.current
    val ring = if (selected) skin.accentText else skin.textSecondary
    Box(
        Modifier
            .size(22.dp)
            .border(2.dp, ring, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(skin.accentText, CircleShape),
            )
        }
    }
}

/** يطلب من النظام تمويه ما خلف النافذة (أندرويد 12+). يُتجاهل بصمت إن لم يتوفر. */
@Composable
private fun DialogBackdropBlur() {
    val view = LocalView.current
    SideEffect {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val window = (view.parent as? DialogWindowProvider)?.window
                if (window != null) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    val attrs = window.attributes
                    attrs.blurBehindRadius = 28
                    window.attributes = attrs
                }
            }
        }
    }
}
