package com.glow.app.ui.nowplaying

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.glow.app.ui.common.GlowTextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glow.app.liquid.backdrop.backdrops.layerBackdrop
import com.glow.app.liquid.backdrop.backdrops.rememberLayerBackdrop
import com.glow.app.media.PlayerState
import com.glow.app.ui.common.AlbumArt
import com.glow.app.ui.common.GlowTopBar
import com.glow.app.ui.common.GlowIcon
import com.glow.app.ui.common.GlowIconTap
import com.glow.app.ui.common.IconKind
import com.glow.app.ui.glass.core.rememberGlassTier
import com.glow.app.ui.settings.EqualizerSheet
import com.glow.app.ui.common.rememberArtPalette
import com.glow.app.ui.theme.LocalGlowSkin

/** المشغّل الكامل: غلاف كبير وعنوان وعناصر التحكم، فوق خلفية حيّة بألوان الغلاف. */
@Composable
fun NowPlayingScreen(
    state: PlayerState,
    audioSessionId: Int,
    onClose: () -> Unit,
    onSeek: (Long) -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onPlayQueueItem: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    onClearUpcoming: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val skin = LocalGlowSkin.current
    var showQueue by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    // الزجاج السائل (أندرويد 13+): الخلفية الحيّة هي ما تنكسر خلف الشريط والأزرار.
    val useGlass = rememberGlassTier().supportsRefraction
    val liquidBackdrop = rememberLayerBackdrop()
    val track = state.current ?: return
    val palette = rememberArtPalette(track)
    val contentColor = playerContentColor(palette, skin)

    Box(modifier.fillMaxSize()) {
        AnimatedArtBackground(
            palette,
            Modifier
                .matchParentSize()
                .then(if (useGlass) Modifier.layerBackdrop(liquidBackdrop) else Modifier),
        )
        Column(
            Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GlowTopBar(title = null, onBack = onClose, backIcon = IconKind.Down)
            Spacer(Modifier.height(8.dp))
            AlbumArt(track, Modifier.fillMaxWidth().aspectRatio(1f), radius = 32.dp, px = 800)
            Spacer(Modifier.height(24.dp))
            Text(
                text = track.title,
                color = skin.text,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist,
                color = skin.textSecondary,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                GlowIconTap("فتح قائمة التشغيل", { showQueue = true }, size = 44.dp) {
                    GlowIcon(IconKind.Playlist, skin.accentText, 22.dp)
                }
                GlowTextButton(onClick = { showQueue = true }) {
                    Text("قائمة التشغيل · ${state.queue.size}", color = skin.accentText, fontSize = 13.sp)
                }
                Spacer(Modifier.weight(1f))
                GlowIconTap("فتح المعادل الصوتي", { showEqualizer = true }, size = 44.dp) {
                    GlowIcon(IconKind.Equalizer, skin.accentText, 22.dp)
                }
            }
            Spacer(Modifier.weight(1f))
            NowPlayingControls(
                state = state,
                contentColor = contentColor,
                onSeek = onSeek,
                onTogglePlay = onTogglePlay,
                onNext = onNext,
                onPrevious = onPrevious,
                onShuffle = onShuffle,
                onRepeat = onRepeat,
                liquidBackdrop = if (useGlass) liquidBackdrop else null,
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showEqualizer) {
        EqualizerSheet(
            audioSessionId = audioSessionId,
            onDismiss = { showEqualizer = false },
        )
    }

    if (showQueue) {
        QueueSheet(
            state = state,
            onDismiss = { showQueue = false },
            onPlayAt = onPlayQueueItem,
            onMove = onMoveQueueItem,
            onRemove = onRemoveQueueItem,
            onClearUpcoming = onClearUpcoming,
        )
    }
}
