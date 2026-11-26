package com.glow.app.ui.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glow.app.media.Track
import com.glow.app.ui.theme.LocalGlowSkin

/**
 * خلية شبكة: للألبوم غلاف بزوايا مدوّرة وتحته العنوان والسطر الثانوي بمحاذاة البداية.
 * للفنان (حين يُمرَّر [overlay]) صورة دائرية واسمه تحتها بالوسط.
 */
@Composable
fun CoverCell(
    art: Track,
    title: String?,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    overlay: String? = null,
) {
    val skin = LocalGlowSkin.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "cellScale",
    )
    val isArtist = overlay != null
    val shape = if (isArtist) CircleShape else RoundedCornerShape(22.dp)
    val heading = title ?: overlay
    val align = if (isArtist) TextAlign.Center else TextAlign.Start
    val rim = if (skin.isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.07f)

    Column(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        horizontalAlignment = if (isArtist) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            AlbumArt(art, Modifier.fillMaxSize(), radius = if (isArtist) 500.dp else 22.dp, px = 400)
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.7f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.18f),
                            ),
                        ),
                    )
                    .border(1.dp, rim, shape),
            )
        }
        if (heading != null) {
            Text(
                text = heading,
                color = skin.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = align,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp, start = 4.dp, end = 4.dp),
            )
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = skin.textSecondary,
                fontSize = 12.5.sp,
                textAlign = align,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp, start = 4.dp, end = 4.dp),
            )
        }
    }
}
