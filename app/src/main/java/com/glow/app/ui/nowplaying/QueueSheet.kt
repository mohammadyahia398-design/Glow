package com.glow.app.ui.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glow.app.media.PlayerState
import com.glow.app.media.Track
import com.glow.app.media.displayTitle
import com.glow.app.ui.common.AlbumArt
import com.glow.app.ui.common.GlowRoundButton
import com.glow.app.ui.common.GlowTextButton
import com.glow.app.ui.icons.CoreGlyphs
import com.glow.app.ui.theme.LocalGlowSkin

/**
 * قائمة التشغيل الحية: اضغط أي مقطع لتشغيله، وزرّ واحد لإزالته من الطابور فقط.
 * أُزيلت أسهم النقل والأرقام لأنها زحمة؛ [onMove] باقٍ في التوقيع للتوافق ولسحب الترتيب لاحقاً.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    state: PlayerState,
    onDismiss: () -> Unit,
    onPlayAt: (Int) -> Unit,
    @Suppress("UNUSED_PARAMETER") onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onClearUpcoming: () -> Unit,
) {
    val skin = LocalGlowSkin.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = skin.panel,
        contentColor = skin.text,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("قائمة التشغيل", color = skin.text, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${state.queue.size} مقطع · ${state.upcomingCount} قادم",
                        color = skin.textSecondary,
                        fontSize = 13.sp,
                    )
                }
                if (state.upcomingCount > 0) {
                    GlowTextButton(onClick = onClearUpcoming) {
                        Text("مسح القادم", color = skin.accentText)
                    }
                }
            }

            if (state.queue.isEmpty()) {
                Text(
                    "قائمة التشغيل فارغة حالياً.",
                    color = skin.textSecondary,
                    modifier = Modifier.padding(vertical = 36.dp, horizontal = 8.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    itemsIndexed(state.queue, key = { _, track -> track.id }) { index, track ->
                        QueueTrackRow(
                            track = track,
                            current = index == state.queuePosition,
                            onPlay = { onPlayAt(index); onDismiss() },
                            onRemove = { onRemove(index) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueTrackRow(
    track: Track,
    current: Boolean,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
) {
    val skin = LocalGlowSkin.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (current) skin.accent.copy(alpha = 0.14f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onPlay)
            .padding(start = 10.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(track, Modifier.size(48.dp), radius = 14.dp)
        Column(
            Modifier.weight(1f).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                track.displayTitle,
                color = if (current) skin.accentText else skin.text,
                fontSize = 15.sp,
                fontWeight = if (current) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (current) "يُشغّل الآن" else track.artist,
                color = skin.textSecondary,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        GlowRoundButton(
            CoreGlyphs.Close, "إزالة من القائمة", onRemove,
            size = 34.dp, tint = skin.textSecondary,
        )
    }
}
