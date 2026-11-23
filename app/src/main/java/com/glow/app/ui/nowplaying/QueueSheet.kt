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
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.glow.app.ui.theme.LocalGlowSkin

/** قائمة التشغيل الحية: تشغيل مباشر، نقل العناصر، وحذفها من الطابور فقط. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    state: PlayerState,
    onDismiss: () -> Unit,
    onPlayAt: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
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
                TextButton(onClick = onClearUpcoming, enabled = state.upcomingCount > 0) {
                    Text("مسح القادم", color = skin.accentText)
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
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    itemsIndexed(state.queue, key = { _, track -> track.id }) { index, track ->
                        QueueTrackRow(
                            track = track,
                            index = index,
                            current = index == state.queuePosition,
                            canMoveUp = index > 0,
                            canMoveDown = index < state.queue.lastIndex,
                            onPlay = { onPlayAt(index); onDismiss() },
                            onMoveUp = { onMove(index, index - 1) },
                            onMoveDown = { onMove(index, index + 1) },
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
    index: Int,
    current: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onPlay: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val skin = LocalGlowSkin.current
    val background = if (current) skin.accent.copy(alpha = 0.12f) else skin.chip.copy(alpha = 0.42f)

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .padding(start = 10.dp, end = 2.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(track, Modifier.size(46.dp), radius = 13.dp)
        Column(
            Modifier.weight(1f).clickable(onClick = onPlay).padding(horizontal = 11.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                track.displayTitle,
                color = if (current) skin.accentText else skin.text,
                fontSize = 14.sp,
                fontWeight = if (current) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (current) "يُشغّل الآن" else track.artist,
                color = skin.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(34.dp)) {
                Text("↑", color = if (canMoveUp) skin.text else skin.textSecondary.copy(alpha = 0.25f), fontSize = 17.sp)
            }
            Text("${index + 1}", color = skin.textSecondary, fontSize = 10.sp)
            IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(34.dp)) {
                Text("↓", color = if (canMoveDown) skin.text else skin.textSecondary.copy(alpha = 0.25f), fontSize = 17.sp)
            }
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(38.dp)) {
            Text("×", color = skin.textSecondary, fontSize = 21.sp)
        }
    }
}
