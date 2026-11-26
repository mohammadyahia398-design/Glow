package com.glow.app.ui.online

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glow.app.media.Track
import com.glow.app.online.AudiusClient
import com.glow.app.ui.common.GlowEmptyState
import com.glow.app.ui.common.GlowIcon
import com.glow.app.ui.common.GlowLoading
import com.glow.app.ui.common.IconKind
import com.glow.app.ui.common.MediaRow
import com.glow.app.ui.common.formatTime
import com.glow.app.ui.library.LibraryBottomInset
import com.glow.app.ui.theme.LocalGlowSkin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

private sealed interface OnlineState {
    data object Idle : OnlineState
    data object Loading : OnlineState
    data object Failed : OnlineState
    data class Done(val tracks: List<Track>) : OnlineState
}

/** البحث المجاني في الإنترنت (Audius). يبدأ البحث تلقائياً بعد التوقف عن الكتابة. */
@Composable
fun OnlineSearchScreen(
    currentId: Long?,
    onPlay: (List<Track>, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val skin = LocalGlowSkin.current
    var query by remember { mutableStateOf("") }
    var retry by remember { mutableStateOf(0) }
    var state by remember { mutableStateOf<OnlineState>(OnlineState.Idle) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    LaunchedEffect(query, retry) {
        val q = query.trim()
        if (q.length < 2) {
            state = OnlineState.Idle
            return@LaunchedEffect
        }
        state = OnlineState.Loading
        delay(450)
        state = try {
            OnlineState.Done(AudiusClient.search(q))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            OnlineState.Failed
        }
    }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(skin.chip)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlowIcon(IconKind.Search, skin.textSecondary, 20.dp)
            Box(Modifier.weight(1f).padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text("ابحث عن أغنية أو فنان", color = skin.textSecondary, fontSize = 15.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = skin.text, fontSize = 15.sp),
                    cursorBrush = SolidColor(skin.accentText),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            }
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(skin.panel),
        ) {
            when (val s = state) {
                OnlineState.Idle -> GlowEmptyState(
                    title = "موسيقى مجانية",
                    message = "اكتب اسم أغنية أو فنان للبحث في Audius.",
                    icon = IconKind.Search,
                )
                OnlineState.Loading -> GlowLoading(Modifier.align(Alignment.Center), label = "جارٍ البحث…")
                OnlineState.Failed -> GlowEmptyState(
                    title = "تعذّر البحث",
                    message = "تحقق من الاتصال بالإنترنت ثم حاول مرة أخرى.",
                    icon = IconKind.Search,
                    actionLabel = "إعادة المحاولة",
                    onAction = { retry++ },
                )
                is OnlineState.Done -> if (s.tracks.isEmpty()) {
                    GlowEmptyState(
                        title = "لا نتائج",
                        message = "جرّب كلمات أخرى.",
                        icon = IconKind.Search,
                    )
                } else {
                    LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = LibraryBottomInset)) {
                        itemsIndexed(s.tracks, key = { _, t -> t.id }) { i, t ->
                            MediaRow(
                                art = null,
                                title = t.title,
                                subtitle = t.artist,
                                trailing = if (t.durationMs > 0) formatTime(t.durationMs) else null,
                                highlight = t.id == currentId,
                                onClick = { onPlay(s.tracks, i) },
                                leading = { GlowIcon(IconKind.Note, skin.accentText, 22.dp) },
                            )
                        }
                    }
                }
            }
        }
    }
}
