package com.glow.app.ui.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glow.app.media.Track
import com.glow.app.ui.theme.LocalGlowSkin

fun formatTime(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

/**
 * صف موحّد: مقطع أو ألبوم أو فنان. يظهر [leading] (مثل رقم المقطع) بدل الغلاف إن وُجد.
 * الصف الجاري تشغيله: خلفية بلون التمييز، وعلى الغلاف مؤشر أعمدة متحرك عند [playing].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaRow(
    art: Track?,
    title: String,
    subtitle: String?,
    trailing: String?,
    highlight: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    playing: Boolean = false,
) {
    val skin = LocalGlowSkin.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "rowScale",
    )
    val rowShape = RoundedCornerShape(20.dp)
    val tint = if (highlight) skin.accent.copy(alpha = if (skin.isDark) 0.14f else 0.10f) else Color.Transparent

    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 1.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(rowShape)
            .background(tint)
            .combinedClickable(
                interactionSource = source,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (leading != null) {
                Box(Modifier.width(32.dp), contentAlignment = Alignment.Center) { leading() }
            } else {
                Box(Modifier.size(56.dp)) {
                    AlbumArt(art, Modifier.fillMaxSize(), radius = 15.dp)
                    if (highlight) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(15.dp))
                                .background(Color.Black.copy(alpha = 0.42f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            NowPlayingBars(Color.White, playing, Modifier.size(20.dp))
                        }
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (highlight) skin.accentText else skin.text,
                    fontSize = 16.sp,
                    fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = skin.textSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            if (trailing != null) {
                Text(
                    text = trailing,
                    color = if (highlight) skin.accentText else skin.textSecondary,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                )
            }
        }
    }
}
