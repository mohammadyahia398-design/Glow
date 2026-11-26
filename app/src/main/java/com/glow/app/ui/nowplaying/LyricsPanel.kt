package com.glow.app.ui.nowplaying

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glow.app.media.LyricsRepository
import com.glow.app.media.LyricsResult
import com.glow.app.media.Track
import com.glow.app.ui.theme.LocalGlowSkin
import kotlin.math.abs

/** لوحة الكلمات: تحلّ محل الغلاف في شاشة التشغيل. السطر الحالي واضح والباقي خافت، والضغط على سطر ينتقل إليه. */
@Composable
fun LyricsPanel(
    track: Track,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    val skin = LocalGlowSkin.current
    val context = LocalContext.current.applicationContext
    var attempt by remember(track.id) { mutableIntStateOf(0) }
    val result by produceState<LyricsResult?>(null, track.id, attempt) {
        value = null
        value = LyricsRepository.load(context, track)
    }

    Box(
        modifier
            .clip(RoundedCornerShape(32.dp))
            .background(if (skin.isDark) Color.Black.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.45f)),
    ) {
        when (val r = result) {
            null -> Message("جارٍ البحث عن الكلمات…", contentColor)
            LyricsResult.None -> Message("لا توجد كلمات لهذه الأغنية", contentColor)
            LyricsResult.Failed -> Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("تعذّر تحميل الكلمات", color = contentColor.copy(alpha = 0.8f), fontSize = 15.sp)
                Text(
                    "إعادة المحاولة",
                    color = skin.accentText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable { attempt++ }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            is LyricsResult.Plain -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 28.dp),
            ) {
                itemsIndexed(r.lines) { _, line ->
                    Text(
                        text = line,
                        color = contentColor.copy(alpha = 0.9f),
                        fontSize = 18.sp,
                        lineHeight = 28.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            is LyricsResult.Synced -> SyncedLines(r, positionMs, onSeek, contentColor)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.Message(text: String, color: Color) {
    Text(
        text = text,
        color = color.copy(alpha = 0.8f),
        fontSize = 15.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.align(Alignment.Center).padding(24.dp),
    )
}

@Composable
private fun SyncedLines(
    result: LyricsResult.Synced,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    contentColor: Color,
) {
    val lines = result.lines
    val listState = rememberLazyListState()
    val active = lines.indexOfLast { it.timeMs <= positionMs }
    LaunchedEffect(active) {
        if (active >= 0) {
            listState.animateScrollToItem(active, scrollOffset = -(listState.layoutInfo.viewportSize.height / 3))
        }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        state = listState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 90.dp),
    ) {
        itemsIndexed(lines) { i, line ->
            val target = when (abs(i - active)) {
                0 -> 1f
                1 -> 0.55f
                else -> 0.35f
            }
            val alpha by animateFloatAsState(target, label = "lyricAlpha")
            Text(
                text = line.text,
                color = contentColor.copy(alpha = alpha),
                fontSize = 22.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSeek(line.timeMs) }
                    .padding(vertical = 8.dp),
            )
        }
    }
}
