package com.glow.app.ui.online

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.glow.app.ui.common.GlowRoundButton
import com.glow.app.ui.common.NowPlayingBars
import com.glow.app.ui.common.glowLiquid
import com.glow.app.ui.icons.CoreGlyphs
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glow.app.media.Track
import com.glow.app.online.AudiusClient
import com.glow.app.online.TrackDownloader
import com.glow.app.settings.LibraryStore
import com.glow.app.ui.common.AlbumArt
import com.glow.app.ui.common.GlowDialog
import com.glow.app.ui.common.GlowDialogAction
import com.glow.app.ui.common.GlowDialogRow
import com.glow.app.ui.common.GlowEmptyState
import com.glow.app.ui.common.GlowIconTap
import com.glow.app.ui.common.LocalGlowSnackbar
import com.glow.app.ui.common.GlowIcon
import com.glow.app.ui.common.GlowLoading
import com.glow.app.ui.common.IconKind
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
    store: LibraryStore,
    onPlay: (List<Track>, Int) -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
) {
    val skin = LocalGlowSkin.current
    var query by remember { mutableStateOf("") }
    var retry by remember { mutableStateOf(0) }
    var state by remember { mutableStateOf<OnlineState>(OnlineState.Idle) }
    val focus = remember { FocusRequester() }
    val context = LocalContext.current
    val snackbar = LocalGlowSnackbar.current
    var menuFor by remember { mutableStateOf<Track?>(null) }
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
                .glowLiquid(
                    shape = RoundedCornerShape(26.dp),
                    fill = Brush.verticalGradient(listOf(skin.chip, skin.chip)),
                    isDark = skin.isDark,
                    fillAlpha = 0.6f,
                    interactive = false,
                )
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
                            val isCurrent = t.id == currentId
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 1.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isCurrent) skin.accent.copy(alpha = if (skin.isDark) 0.14f else 0.10f)
                                        else Color.Transparent,
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) { onPlay(s.tracks, i) }
                                    .padding(start = 10.dp, end = 6.dp, top = 7.dp, bottom = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Box(Modifier.size(56.dp)) {
                                    AlbumArt(t, Modifier.fillMaxSize(), radius = 15.dp)
                                    if (isCurrent) {
                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(15.dp))
                                                .background(Color.Black.copy(alpha = 0.42f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            NowPlayingBars(Color.White, isPlaying, Modifier.size(20.dp))
                                        }
                                    }
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        t.title,
                                        color = if (isCurrent) skin.accentText else skin.text,
                                        fontSize = 16.sp,
                                        fontWeight = if (isCurrent) androidx.compose.ui.text.font.FontWeight.SemiBold
                                        else androidx.compose.ui.text.font.FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        if (t.durationMs > 0) t.artist + " · " + formatTime(t.durationMs) else t.artist,
                                        color = skin.textSecondary,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                GlowRoundButton(
                                    CoreGlyphs.More, "المزيد", { menuFor = t },
                                    size = 38.dp, tint = skin.textSecondary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    menuFor?.let { t ->
        val isFav = t.id in store.favorites
        GlowDialog(
            title = t.title,
            onDismiss = { menuFor = null },
            actions = { GlowDialogAction("إغلاق", { menuFor = null }) },
        ) {
            GlowDialogRow(if (isFav) "إزالة من المفضلة" else "إضافة إلى المفضلة", {
                val added = store.toggleOnlineFavorite(t)
                snackbar?.show(if (added) "أُضيف إلى المفضلة" else "أُزيل من المفضلة")
                menuFor = null
            })
            if (t.downloadUrl != null) {
                GlowDialogRow("تحميل", {
                    val ok = TrackDownloader.start(context, t)
                    snackbar?.show(if (ok) "بدأ التحميل إلى مجلد Music/Glow" else "تعذّر بدء التحميل")
                    menuFor = null
                })
            } else {
                Text(
                    "التحميل غير متاح لهذه الأغنية (صاحبها لم يسمح به).",
                    color = skin.textSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
    }
}
