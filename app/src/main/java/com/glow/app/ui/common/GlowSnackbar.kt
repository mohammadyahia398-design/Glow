package com.glow.app.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glow.app.liquid.backdrop.Backdrop
import com.glow.app.liquid.backdrop.drawBackdrop
import com.glow.app.liquid.backdrop.effects.blur
import com.glow.app.liquid.backdrop.effects.lens
import com.glow.app.liquid.backdrop.effects.vibrancy
import com.glow.app.ui.glass.core.rememberGlassTier
import com.glow.app.ui.theme.LocalGlowSkin
import kotlinx.coroutines.delay

/** رسالة تأكيد واحدة. [id] يتغيّر مع كل رسالة جديدة فيُعاد ضبط مؤقّت الإخفاء. */
class GlowSnackbarMessage(
    val id: Long,
    val text: String,
    val actionLabel: String?,
    val onAction: (() -> Unit)?,
)

/** حالة الرسائل: رسالة واحدة ظاهرة في كل مرة، والجديدة تحلّ محل القديمة. */
class GlowSnackbarState {
    var current by mutableStateOf<GlowSnackbarMessage?>(null)
        private set
    private var counter = 0L

    fun show(text: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        counter += 1
        current = GlowSnackbarMessage(counter, text, actionLabel, onAction)
    }

    fun dismiss() {
        current = null
    }
}

/** يتيح لأي شاشة إظهار رسالة دون تمرير الحالة عبر المعاملات. */
val LocalGlowSnackbar = compositionLocalOf<GlowSnackbarState?> { null }

/**
 * مضيف الرسائل: كبسولة زجاجية تنكسر خلفها الشاشة على أندرويد 13+، وكبسولة مسطحة قبل ذلك.
 * ضعه في GlowApp خارج مصدر الخلفية، فوق الشريط السفلي والمشغّل المصغّر.
 */
@Composable
fun GlowSnackbarHost(
    state: GlowSnackbarState,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
) {
    val message = state.current
    // نحتفظ بآخر رسالة ليبقى نصها أثناء حركة الاختفاء.
    var lastShown by remember { mutableStateOf<GlowSnackbarMessage?>(null) }
    if (message != null) lastShown = message

    LaunchedEffect(message?.id) {
        if (message != null) {
            delay(if (message.actionLabel != null) 5000L else 3000L)
            state.dismiss()
        }
    }

    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = slideInVertically(tween(260)) { it / 2 } + fadeIn(tween(220)),
        exit = slideOutVertically(tween(200)) { it / 2 } + fadeOut(tween(180)),
    ) {
        lastShown?.let { m -> SnackbarPill(m, backdrop) { state.dismiss() } }
    }
}

@Composable
private fun SnackbarPill(message: GlowSnackbarMessage, backdrop: Backdrop?, onDone: () -> Unit) {
    val skin = LocalGlowSkin.current
    val refraction = rememberGlassTier().supportsRefraction
    val shape = RoundedCornerShape(50)
    val glass = refraction && backdrop != null
    val container =
        if (skin.isDark) Color(0xFF000002).copy(alpha = 0.40f)
        else Color(0xFFFEFDFF).copy(alpha = 0.40f)

    val surfaceModifier =
        if (glass && backdrop != null) {
            Modifier.drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(8f.dp.toPx())
                    lens(20f.dp.toPx(), 20f.dp.toPx())
                },
                onDrawSurface = { drawRect(container) },
            )
        } else {
            Modifier.background(skin.panel.copy(alpha = 0.97f), shape)
        }

    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .then(surfaceModifier)
            .clip(shape)
            .then(if (glass) Modifier else Modifier.border(1.dp, skin.glassBorder.copy(alpha = 0.30f), shape))
            .padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            message.text,
            color = skin.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 10.dp),
        )
        val label = message.actionLabel
        val action = message.onAction
        if (label != null && action != null) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(role = Role.Button) {
                        action()
                        onDone()
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = skin.accentText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
    }
}
