package com.glow.app.ui.nowplaying

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.compose.ui.unit.sp
import com.glow.app.media.PlayerState
import com.glow.app.media.Track
import com.glow.app.media.displayTitle
import com.glow.app.ui.common.AlbumArt
import com.glow.app.ui.common.GlowBottomSheet
import com.glow.app.ui.common.GlowRoundButton
import com.glow.app.ui.common.GlowTextButton
import com.glow.app.ui.icons.CoreGlyphs
import com.glow.app.ui.theme.LocalGlowSkin
import com.glow.app.GlowStrings
import com.glow.app.R

private val QueueRowHeight = 64.dp
private val QueueRowGap = 4.dp

/**
 * قائمة التشغيل الحية: اضغط أي مقطع لتشغيله، واضغط مطولاً ثم اسحب لتغيير ترتيبه،
 * وزرّ واحد لإزالته من الطابور.
 */
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
    val haptic = LocalHapticFeedback.current
    val stepPx = with(LocalDensity.current) { (QueueRowHeight + QueueRowGap).toPx() }
    // نسخة محلية تُعرض أثناء السحب وبعده مباشرة، وتُستبدل بالحقيقية عندما يتغير الطابور.
    var local by remember(state.queue) { mutableStateOf(state.queue) }
    var dragId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val currentId = state.queue.getOrNull(state.queuePosition)?.id
    val dragFrom = local.indexOfFirst { it.id == dragId }
    val dragTarget =
        if (dragFrom >= 0) (dragFrom + (dragOffset / stepPx).roundToInt()).coerceIn(0, local.lastIndex) else -1

    fun startDrag(id: Long) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        dragId = id
        dragOffset = 0f
    }

    fun endDrag() {
        val from = local.indexOfFirst { it.id == dragId }
        if (from >= 0) {
            val to = (from + (dragOffset / stepPx).roundToInt()).coerceIn(0, local.lastIndex)
            if (to != from) {
                val moved = local.toMutableList()
                moved.add(to, moved.removeAt(from))
                local = moved
                onMove(from, to)
            }
        }
        dragId = null
        dragOffset = 0f
    }

    fun cancelDrag() {
        dragId = null
        dragOffset = 0f
    }

    GlowBottomSheet(onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(GlowStrings.get(R.string.queue_title), color = skin.text, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Text(
                        GlowStrings.get(R.string.queue_summary, state.queue.size, state.upcomingCount),
                        color = skin.textSecondary,
                        fontSize = 13.sp,
                    )
                    Text(
                        GlowStrings.get(R.string.queue_drag_hint),
                        color = skin.textSecondary.copy(alpha = 0.8f),
                        fontSize = 11.5.sp,
                    )
                }
                if (state.upcomingCount > 0) {
                    GlowTextButton(onClick = onClearUpcoming) {
                        Text(GlowStrings.get(R.string.clear_upcoming), color = skin.accentText)
                    }
                }
            }

            if (state.queue.isEmpty()) {
                Text(
                    GlowStrings.get(R.string.queue_empty),
                    color = skin.textSecondary,
                    modifier = Modifier.padding(vertical = 36.dp, horizontal = 8.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(QueueRowGap),
                ) {
                    itemsIndexed(local, key = { _, track -> track.id }) { index, track ->
                        val dragging = index == dragFrom
                        val shift = when {
                            dragFrom < 0 || dragging -> 0f
                            dragFrom < dragTarget && index in (dragFrom + 1)..dragTarget -> -stepPx
                            dragFrom > dragTarget && index in dragTarget until dragFrom -> stepPx
                            else -> 0f
                        }
                        val animatedShift by animateFloatAsState(shift, label = "queueShift")
                        QueueTrackRow(
                            track = track,
                            current = track.id == currentId,
                            onPlay = { onPlayAt(index); onDismiss() },
                            onRemove = { onRemove(index) },
                            handleModifier = Modifier
                                .pointerInput(Unit) { detectTapGestures { } }
                                .pointerInput(track.id) {
                                    detectDragGestures(
                                        onDragStart = { startDrag(track.id) },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragOffset += amount.y
                                        },
                                        onDragEnd = { endDrag() },
                                        onDragCancel = { cancelDrag() },
                                    )
                                },
                            modifier = Modifier
                                .zIndex(if (dragging) 1f else 0f)
                                .graphicsLayer {
                                    translationY = if (dragging) dragOffset else animatedShift
                                    if (dragging) {
                                        scaleX = 1.03f
                                        scaleY = 1.03f
                                    }
                                }
                                .pointerInput(track.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { startDrag(track.id) },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragOffset += amount.y
                                        },
                                        onDragEnd = { endDrag() },
                                        onDragCancel = { cancelDrag() },
                                    )
                                },
                        )
                    }
                }
            }
        }
    }
}

/** مقبض سحب مرئي (ست نقاط): السحب منه يبدأ فوراً بلا ضغط مطوّل. */
@Composable
private fun DragHandle(color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Box(
        modifier
            .size(width = 30.dp, height = QueueRowHeight)
            .semantics { contentDescription = GlowStrings.get(R.string.drag_to_reorder) },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(width = 12.dp, height = 18.dp)) {
            val r = 1.6.dp.toPx()
            for (col in 0..1) {
                for (row in 0..2) {
                    drawCircle(
                        color = color.copy(alpha = 0.75f),
                        radius = r,
                        center = Offset(size.width * (0.25f + 0.5f * col), size.height * (0.1667f + 0.3333f * row)),
                    )
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
    handleModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    val skin = LocalGlowSkin.current
    Row(
        modifier
            .fillMaxWidth()
            .height(QueueRowHeight)
            .clip(RoundedCornerShape(18.dp))
            .background(if (current) skin.accent.copy(alpha = 0.14f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(role = Role.Button, onClick = onPlay)
            .padding(start = 10.dp, end = 8.dp),
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
                if (current) GlowStrings.get(R.string.now_playing) else track.artist,
                color = skin.textSecondary,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DragHandle(skin.textSecondary, handleModifier)
        GlowRoundButton(
            CoreGlyphs.Close, GlowStrings.get(R.string.remove_from_queue), onRemove,
            size = 34.dp, tint = skin.textSecondary,
        )
    }
}
