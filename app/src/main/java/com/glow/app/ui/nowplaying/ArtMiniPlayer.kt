package com.glow.app.ui.nowplaying

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.glow.app.liquid.backdrop.backdrops.rememberCanvasBackdrop
import com.glow.app.liquid.ui.LiquidSlider
import com.glow.app.ui.glass.core.rememberGlassTier
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glow.app.media.Track
import com.glow.app.ui.common.AlbumArt
import com.glow.app.ui.common.GlowIcon
import com.glow.app.ui.common.GlowIconTap
import com.glow.app.ui.common.IconKind
import com.glow.app.ui.common.rememberArtPalette
import com.glow.app.ui.theme.LocalGlowSkin

/** ارتفاع المشغّل المصغّر؛ يعتمد عليه `GlowApp` لحساب طبقة التلاشي السفلية. */
val ArtMiniPlayerHeight = 76.dp

private val GlowInk = Color(0xFF080B10)

/**
 * المشغّل المصغّر بهوية التوهّج: كبسولة داكنة يتسرّب إليها لون غلاف الأغنية من الأطراف،
 * بحافة لامعة، وكرة تشغيل متوهّجة، وشريط تقدّم رفيع برأس مضيء.
 * النص دائماً فاتح فوق خلفية داكنة، فالتباين ثابت مهما كان الغلاف.
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
) {
    val palette = rememberArtPalette(track)
    val primary by animateColorAsState(palette.primary, tween(450), label = "miniPrimary")
    val secondary by animateColorAsState(palette.secondary, tween(450), label = "miniSecondary")
    val base = lerp(GlowInk, primary, 0.16f)
    val shape = RoundedCornerShape(32.dp)
    val orbIcon = if (lerp(primary, secondary, 0.5f).luminance() > 0.55f) GlowInk else Color.White

    Box(
        modifier
            .fillMaxWidth()
            .height(ArtMiniPlayerHeight)
            .shadow(16.dp, shape, ambientColor = primary.copy(alpha = 0.6f), spotColor = primary)
            .clip(shape)
            .drawBehind {
                drawRect(base)
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(primary.copy(alpha = 0.60f), primary.copy(alpha = 0f)),
                        center = Offset(size.width * 0.10f, size.height * 1.15f),
                        radius = size.width * 0.55f,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(secondary.copy(alpha = 0.34f), secondary.copy(alpha = 0f)),
                        center = Offset(size.width * 0.92f, -size.height * 0.20f),
                        radius = size.width * 0.45f,
                    ),
                )
                drawRect(
                    Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0f)),
                        endY = size.height * 0.5f,
                    ),
                )
                // صبغة زجاجية متدرجة من الأسفل بلون الغلاف
                drawRect(
                    Brush.verticalGradient(
                        colors = listOf(primary.copy(alpha = 0f), primary.copy(alpha = 0.32f)),
                        startY = size.height * 0.35f,
                        endY = size.height,
                    ),
                )
                // حافة علوية زجاجية رفيعة تتلاشى عند الطرفين
                drawRect(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.85f), Color.Transparent),
                    ),
                    topLeft = Offset(size.width * 0.06f, 0f),
                    size = Size(size.width * 0.88f, 1.5.dp.toPx()),
                )
            }
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.06f),
                        secondary.copy(alpha = 0.35f),
                    ),
                ),
                shape,
            )
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(
            Modifier.fillMaxWidth().height(ArtMiniPlayerHeight).padding(start = 10.dp, end = 8.dp, bottom = 26.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumArt(track, Modifier.size(42.dp), radius = 13.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track.artist,
                    color = Color.White.copy(alpha = 0.70f),
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            GlowIconTap("السابق", onPrevious, size = 40.dp) {
                GlowIcon(IconKind.Prev, Color.White.copy(alpha = 0.88f), 22.dp)
            }
            GlowIconTap(if (isPlaying) "إيقاف مؤقت" else "تشغيل", onTogglePlay, size = 46.dp) {
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .drawBehind {
                            drawCircle(
                                Brush.radialGradient(
                                    colors = listOf(secondary, primary),
                                    center = Offset(size.width * 0.35f, size.height * 0.30f),
                                    radius = size.width * 0.85f,
                                ),
                            )
                            drawCircle(
                                Brush.radialGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0f)),
                                    center = Offset(size.width * 0.32f, size.height * 0.26f),
                                    radius = size.width * 0.38f,
                                ),
                            )
                        }
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.70f), Color.White.copy(alpha = 0.10f)),
                            ),
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    GlowIcon(if (isPlaying) IconKind.Pause else IconKind.Play, orbIcon, 24.dp)
                }
            }
            GlowIconTap("التالي", onNext, size = 40.dp) {
                GlowIcon(IconKind.Next, Color.White.copy(alpha = 0.88f), 22.dp)
            }
        }
        val accent = LocalGlowSkin.current.accent
        val glass = rememberGlassTier().supportsRefraction
        if (glass && onSeek != null) {
            // منزلق المكتبة نفسه (المقبض الكبسولي الزجاجي): قابل للسحب والنقر، ويبقى من اليسار لليمين.
            val sliderBackdrop = rememberCanvasBackdrop { drawRect(base) }
            var drag by remember { mutableStateOf<Float?>(null) }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                LiquidSlider(
                    value = { drag ?: progress().let { if (it.isNaN()) 0f else it.coerceIn(0f, 1f) } },
                    onValueChange = { drag = it },
                    valueRange = 0f..1f,
                    visibilityThreshold = 0.001f,
                    backdrop = sliderBackdrop,
                    accentColor = accent,
                    trackColor = Color.White.copy(alpha = 0.22f),
                    onValueChangeFinished = {
                        drag?.let(onSeek)
                        drag = null
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 30.dp)
                        .padding(bottom = 2.dp)
                        .height(26.dp),
                )
            }
        } else {
            val accent = LocalGlowSkin.current.accent
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp, vertical = 4.dp)
                    .height(12.dp)
                    .drawBehind {
                        val trackH = 4.dp.toPx()
                        val top = (size.height - trackH) / 2f
                        val radius = CornerRadius(trackH / 2f)
                        drawRoundRect(
                            Color.White.copy(alpha = 0.20f),
                            topLeft = Offset(0f, top),
                            size = Size(size.width, trackH),
                            cornerRadius = radius,
                        )
                        val fill = size.width * progress().coerceIn(0f, 1f)
                        if (fill > 0f) {
                            drawRoundRect(
                                accent,
                                topLeft = Offset(0f, top),
                                size = Size(fill, trackH),
                                cornerRadius = radius,
                            )
                        }
                        // مقبض كبسولي أبيض كمنزلق المكتبة وهو في حالة الراحة.
                        val thumbW = 18.dp.toPx()
                        val thumbH = 11.dp.toPx()
                        val cx = fill.coerceIn(thumbW / 2f, size.width - thumbW / 2f)
                        val thumbTop = Offset(cx - thumbW / 2f, (size.height - thumbH) / 2f)
                        val thumbRadius = CornerRadius(thumbH / 2f)
                        drawRoundRect(
                            Color.Black.copy(alpha = 0.25f),
                            topLeft = Offset(thumbTop.x, thumbTop.y + 1.dp.toPx()),
                            size = Size(thumbW, thumbH),
                            cornerRadius = thumbRadius,
                        )
                        drawRoundRect(
                            Color.White,
                            topLeft = thumbTop,
                            size = Size(thumbW, thumbH),
                            cornerRadius = thumbRadius,
                        )
                    },
            )
        }
    }
}
