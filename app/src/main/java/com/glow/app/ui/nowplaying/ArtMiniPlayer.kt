package com.glow.app.ui.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glow.app.liquid.backdrop.Backdrop
import com.glow.app.liquid.backdrop.backdrops.rememberCanvasBackdrop
import com.glow.app.liquid.backdrop.drawBackdrop
import com.glow.app.liquid.backdrop.effects.blur
import com.glow.app.liquid.backdrop.effects.lens
import com.glow.app.liquid.backdrop.effects.vibrancy
import com.glow.app.liquid.ui.LiquidSlider
import com.glow.app.media.Track
import com.glow.app.ui.common.AlbumArt
import com.glow.app.ui.common.GlowIcon
import com.glow.app.ui.common.GlowIconTap
import com.glow.app.ui.common.IconKind
import com.glow.app.ui.glass.core.rememberGlassTier
import com.glow.app.ui.theme.LocalGlowSkin

/** ارتفاع المشغّل المصغّر؛ يعتمد عليه `GlowApp` لحساب طبقة التلاشي السفلية. */
val ArtMiniPlayerHeight = 92.dp

/** منطقة المنزلق: تتّسع لمقبض الضغط (24dp × 1.5 = 36dp) كاملاً فلا يخرج عن الكبسولة. */
private val SliderZone = 36.dp
private val SliderBottomGap = 4.dp

/**
 * المشغّل المصغّر بهوية Glow: كبسولة زجاجية سائلة بألوان الهوية فقط (سطح محايد + لون التمييز)،
 * أزرار موحّدة مع بقية التطبيق، ومنزلق سائل يبقى كله داخل الكبسولة.
 * لا يتسرّب إليه لون الغلاف: الألوان الزائدة أُزيلت.
 */
@Composable
fun ArtMiniPlayer(
    track: Track,
    isPlaying: Boolean,
    progress: () -> Float,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSeek: ((Float) -> Unit)? = null,
    backdrop: Backdrop? = null,
) {
    val skin = LocalGlowSkin.current
    val refraction = rememberGlassTier().supportsRefraction
    val shape = RoundedCornerShape(36.dp)
    val surface = skin.panel

    val surfaceModifier =
        if (refraction && backdrop != null) {
            Modifier.drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(8f.dp.toPx())
                    lens(16f.dp.toPx(), 16f.dp.toPx())
                },
                onDrawSurface = { drawRect(surface.copy(alpha = 0.62f)) },
            )
        } else {
            Modifier.background(surface.copy(alpha = 0.97f), shape)
        }

    Box(
        modifier
            .fillMaxWidth()
            .height(ArtMiniPlayerHeight)
            .then(surfaceModifier)
            .clip(shape)
            .border(1.dp, skin.glassBorder.copy(alpha = 0.30f), shape)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(ArtMiniPlayerHeight - SliderZone - SliderBottomGap)
                .padding(start = 14.dp, end = 10.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumArt(track, Modifier.size(40.dp), radius = 14.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = skin.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track.artist,
                    color = skin.textSecondary,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            GlowIconTap("السابق", onPrevious, size = 44.dp) {
                GlowIcon(IconKind.Prev, skin.text.copy(alpha = 0.9f), 22.dp)
            }
            // زر التشغيل: نفس تدرّج وحافة GlowFilledButton، بلا ألوان إضافية.
            GlowIconTap(if (isPlaying) "إيقاف مؤقت" else "تشغيل", onTogglePlay, size = 48.dp) {
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(lerp(skin.accent, Color.White, 0.14f), skin.accent),
                            ),
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    GlowIcon(if (isPlaying) IconKind.Pause else IconKind.Play, skin.onAccent, 22.dp)
                }
            }
            GlowIconTap("التالي", onNext, size = 44.dp) {
                GlowIcon(IconKind.Next, skin.text.copy(alpha = 0.9f), 22.dp)
            }
        }

        val trackColor = skin.text.copy(alpha = 0.16f)
        if (refraction && onSeek != null) {
            // منزلق المكتبة نفسه؛ يبقى من اليسار لليمين، ومنطقته تتّسع للمقبض المضغوط كاملاً.
            val sliderBackdrop = rememberCanvasBackdrop { drawRect(surface) }
            var drag by remember { mutableStateOf<Float?>(null) }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                LiquidSlider(
                    value = { drag ?: progress().let { if (it.isNaN()) 0f else it.coerceIn(0f, 1f) } },
                    onValueChange = { drag = it },
                    valueRange = 0f..1f,
                    visibilityThreshold = 0.001f,
                    backdrop = sliderBackdrop,
                    accentColor = skin.accent,
                    trackColor = trackColor,
                    onValueChangeFinished = {
                        drag?.let(onSeek)
                        drag = null
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 36.dp)
                        .padding(bottom = SliderBottomGap)
                        .height(SliderZone),
                )
            }
        } else {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp)
                    .padding(bottom = SliderBottomGap + (SliderZone - 12.dp) / 2)
                    .height(12.dp)
                    .drawBehind {
                        val trackH = 4.dp.toPx()
                        val top = (size.height - trackH) / 2f
                        val radius = CornerRadius(trackH / 2f)
                        drawRoundRect(
                            trackColor,
                            topLeft = Offset(0f, top),
                            size = Size(size.width, trackH),
                            cornerRadius = radius,
                        )
                        val fill = size.width * progress().coerceIn(0f, 1f)
                        if (fill > 0f) {
                            drawRoundRect(
                                skin.accent,
                                topLeft = Offset(0f, top),
                                size = Size(fill, trackH),
                                cornerRadius = radius,
                            )
                        }
                    },
            )
        }
    }
}
