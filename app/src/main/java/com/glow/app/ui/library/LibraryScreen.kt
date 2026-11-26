package com.glow.app.ui.library

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glow.app.media.Track
import com.glow.app.settings.LibraryStore
import com.glow.app.settings.Playlist
import com.glow.app.ui.common.AlbumArt
import com.glow.app.ui.common.CoverCell
import com.glow.app.ui.common.GlowChoiceDialog
import com.glow.app.ui.common.GlowDialog
import com.glow.app.ui.common.GlowLoading
import com.glow.app.ui.common.GlowTextButton
import com.glow.app.ui.common.LocalGlowSnackbar
import com.glow.app.ui.common.GlowDialogAction
import com.glow.app.ui.common.GlowChip
import com.glow.app.ui.common.GlowDialogRow
import com.glow.app.ui.common.GlowEmptyState
import com.glow.app.ui.common.GlowIcon
import com.glow.app.ui.common.GlowIconTap
import com.glow.app.ui.common.GlowSettingsButton
import com.glow.app.ui.common.GlowTopBar
import com.glow.app.ui.common.IconKind
import com.glow.app.media.addedLabel
import com.glow.app.media.displayTitle
import com.glow.app.ui.common.MediaRow
import com.glow.app.ui.common.formatTime
import com.glow.app.ui.theme.LocalGlowSkin

/** ألبوم أو فنان أو نوع أو قائمة: مجموعة مقاطع بغلاف واحد. */
data class MediaGroup(
    val title: String,
    val subtitle: String,
    val cover: Track,
    val tracks: List<Track>,
)

/** أقسام المكتبة. [barTabs] هي ما يظهر في الشريط السفلي؛ عدّلها لإضافة قسم أو إزالته. */
enum class LibraryTab(val label: String, val icon: IconKind) {
    Tracks("الأغاني", IconKind.Note),
    Albums("الألبومات", IconKind.Album),
    Artists("الفنانون", IconKind.Artist),
    Genres("الأنواع", IconKind.Note),
    Spotify("Spotify", IconKind.Note),
    Favorites("المفضلة", IconKind.Heart),
    Playlists("القوائم", IconKind.Playlist),
    VocalIsolation("عزل الصوت", IconKind.Note);

    companion object {
        val barTabs: List<LibraryTab> = listOf(Tracks, Albums, Artists, Favorites, Playlists)
    }
}

/** مسافة سفلية تترك القوائم ظاهرة فوق الشريط الزجاجي والمشغّل المصغّر. */
val LibraryBottomInset = 230.dp

private const val UNKNOWN_ARTIST = "فنان غير معروف"

private class NewPlaylist(val track: Track?)

/** ترتيب قائمة الأغاني والمفضلة. */
private enum class LibrarySort(val label: String) {
    Title("العنوان"),
    Artist("الفنان"),
    Newest("الأحدث"),
    Duration("المدة"),
}

private fun List<Track>.sortedFor(sort: LibrarySort): List<Track> = when (sort) {
    LibrarySort.Title -> sortedBy { it.title.lowercase() }
    LibrarySort.Artist -> sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))
    LibrarySort.Newest -> sortedByDescending { it.dateAddedSec }
    LibrarySort.Duration -> sortedByDescending { it.durationMs }
}

/** شاشة المكتبة على طراز Samsung Music: ترويسة + تبويبات تُمرَّر أفقياً + لوح قائمة بزوايا مدوّرة. */
@Composable
fun LibraryScreen(
    tracks: List<Track>,
    loaded: Boolean,
    hasAudioAccess: Boolean,
    currentId: Long?,
    store: LibraryStore,
    barTab: LibraryTab,
    onRequestPermission: () -> Unit,
    onPlay: (List<Track>, Int) -> Unit,
    onQueueTrack: (Track, Boolean) -> Unit,
    onOpen: (MediaGroup) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenVocalIsolation: () -> Unit = {},
    onOpenOnline: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val skin = LocalGlowSkin.current
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var actionTrack by remember { mutableStateOf<Track?>(null) }
    var pickFor by remember { mutableStateOf<Track?>(null) }
    var newPlaylist by remember { mutableStateOf<NewPlaylist?>(null) }
    var sort by rememberSaveable { mutableStateOf(LibrarySort.Title) }
    var sortOpen by remember { mutableStateOf(false) }
    val snackbar = LocalGlowSnackbar.current
    var showGenres by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(barTab) { showGenres = false }
    val tab = if (showGenres) LibraryTab.Genres else barTab

    val byId = remember(tracks) { tracks.associateBy { it.id } }
    val shown = remember(tracks, query) {
        if (query.isBlank()) tracks else tracks.filter {
            it.title.contains(query, true) || it.artist.contains(query, true) ||
                it.album.contains(query, true)
        }
    }
    val sorted = remember(shown, sort) { shown.sortedFor(sort) }
    val albums = remember(shown) {
        shown.groupBy { it.albumId }.values
            .map { MediaGroup(it[0].album, it[0].artist, it[0], it) }
            .sortedBy { it.title.lowercase() }
    }
    val artists = remember(shown) {
        shown.groupBy { it.artist }
            .map { (name, list) -> MediaGroup(name, "", list[0], list) }
            .sortedBy { it.title.lowercase() }
    }
    val genres = remember(shown) {
        shown.filter { it.genre.isNotBlank() }.groupBy { it.genre }
            .map { (name, list) -> MediaGroup(name, "", list[0], list) }
            .sortedBy { it.title.lowercase() }
    }
    val favorites = remember(sorted, store.favorites, store.onlineTracks) {
        sorted.filter { it.id in store.favorites } + store.onlineTracks.filter { it.id in store.favorites }
    }
    val recent = remember(byId, store.recents) { store.recents.mapNotNull { byId[it] } }
    val most = remember(tracks, store.playCounts) {
        tracks.filter { (store.playCounts[it.id] ?: 0) > 0 }
            .sortedByDescending { store.playCounts[it.id] ?: 0 }
            .take(50)
    }
    val playlists = remember(byId, store.playlists) {
        store.playlists.map { p -> p to p.trackIds.mapNotNull { byId[it] } }
    }

    Column(modifier.fillMaxSize()) {
        GlowTopBar(
            title = "Glow",
            titleSize = 30.sp,
            titleWeight = FontWeight.Bold,
            actions = {
                GlowIconTap("بحث", {
                    searching = !searching
                    if (!searching) query = ""
                }, size = 44.dp, glass = true) { GlowIcon(IconKind.Search, if (searching) skin.accentText else skin.text) }
                GlowSettingsButton(onOpenSettings, Modifier.padding(start = 4.dp, end = 8.dp))
            },
        )
        if (searching) SearchField(query, { query = it }) { searching = false; query = "" }
        if (hasAudioAccess) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { GlowChip("الأنواع", tab == LibraryTab.Genres, { showGenres = !showGenres }) }
                item { GlowChip("عزل الصوت", false, onOpenVocalIsolation) }
                item { GlowChip("الإنترنت", false, onOpenOnline) }
            }
        }
        if (loaded && hasAudioAccess && tracks.isNotEmpty() &&
            (tab == LibraryTab.Tracks || tab == LibraryTab.Favorites)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Start,
            ) {
                GlowTextButton(onClick = { sortOpen = true }) { Text("الترتيب: " + sort.label) }
            }
        }


        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(skin.panel),
        ) {
            val pad = PaddingValues(top = 8.dp, bottom = LibraryBottomInset)
            val gridPad = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = LibraryBottomInset)
            when {
                !hasAudioAccess -> PermissionPrompt(onRequestPermission)
                !loaded && tab != LibraryTab.Spotify -> GlowLoading(
                    Modifier.align(Alignment.Center),
                    label = "جارٍ فحص المكتبة…",
                )
                loaded && tracks.isEmpty() -> GlowEmptyState(
                    title = "لا توجد موسيقى",
                    message = "لم نجد أي ملف صوتي على هذا الجهاز. أضف أغاني ثم أعد فتح Glow.",
                    icon = IconKind.Note,
                    actionLabel = "فتح الإعدادات",
                    onAction = onOpenSettings,
                )
                tab == LibraryTab.Spotify -> GlowEmptyState(
                    title = "Spotify قريباً",
                    message = "تكامل Spotify غير متاح بعد.",
                    icon = IconKind.Playlist,
                )
                tab == LibraryTab.Playlists -> PlaylistsTab(
                    recent = recent, most = most, playlists = playlists, pad = pad,
                    onOpen = onOpen, onNew = { newPlaylist = NewPlaylist(null) },
                )
                shown.isEmpty() && loaded -> GlowEmptyState(
                    title = "لا نتائج",
                    message = "جرّب كلمات بحث أخرى.",
                    icon = IconKind.Note,
                    badge = IconKind.Search,
                    actionLabel = "مسح البحث",
                    onAction = { query = "" },
                )
                tab == LibraryTab.Tracks -> TrackList(sorted, currentId, pad, onPlay) { actionTrack = it }
                tab == LibraryTab.Favorites -> if (favorites.isEmpty()) {
                    GlowEmptyState(
                        title = "لا مفضلات بعد",
                        message = "اضغط مطولاً على أي أغنية لإضافتها إلى المفضلة.",
                        icon = IconKind.Heart,
                    )
                } else {
                    TrackList(favorites, currentId, pad, onPlay) { actionTrack = it }
                }
                tab == LibraryTab.Genres -> if (genres.isEmpty()) {
                    GlowEmptyState(
                        title = "لا أنواع",
                        message = "لا تتوفر معلومات الأنواع على هذا الجهاز.",
                        icon = IconKind.Album,
                    )
                } else {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = pad) {
                        items(genres, key = { it.title }) { g ->
                            MediaRow(
                                art = g.cover, title = g.title, subtitle = null,
                                trailing = "${g.tracks.size} أغنية", highlight = false,
                                onClick = { onOpen(g) },
                            )
                        }
                    }
                }
                tab == LibraryTab.Albums -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = gridPad,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    gridItems(albums, key = { it.cover.albumId }) { c ->
                        CoverCell(
                            art = c.cover,
                            title = c.title,
                            subtitle = "${c.subtitle} | ${c.tracks.size} أغنية",
                            onClick = { onOpen(c) },
                        )
                    }
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = gridPad,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    gridItems(artists, key = { it.title }) { c ->
                        CoverCell(
                            art = c.cover,
                            title = null,
                            subtitle = "${c.tracks.size} أغنية",
                            overlay = c.title,
                            onClick = { onOpen(c) },
                        )
                    }
                }
            }
        }
    }

    if (sortOpen) {
        GlowChoiceDialog(
            title = "ترتيب الأغاني",
            options = LibrarySort.values().map { it to it.label },
            selected = sort,
            onSelect = { sort = it },
            onDismiss = { sortOpen = false },
        )
    }
    actionTrack?.let { t ->
        ActionsDialog(
            track = t,
            isFavorite = t.id in store.favorites,
            onToggleFavorite = {
                val wasFav = t.id in store.favorites
                store.toggleFavorite(t.id)
                snackbar?.show(
                    if (wasFav) "أُزيل من المفضلة" else "أُضيف إلى المفضلة",
                    "تراجع",
                ) { store.toggleFavorite(t.id) }
                actionTrack = null
            },
            onAddToPlaylist = { pickFor = t; actionTrack = null },
            onAddToQueue = { onQueueTrack(t, false); actionTrack = null },
            onPlayNext = { onQueueTrack(t, true); actionTrack = null },
            onDismiss = { actionTrack = null },
        )
    }
    pickFor?.let { t ->
        PickPlaylistDialog(
            playlists = store.playlists,
            onPick = { pl ->
                if (t.id in pl.trackIds) {
                    snackbar?.show("المقطع موجود بالفعل في «${pl.name}»")
                } else {
                    store.addToPlaylist(pl.id, t.id)
                    snackbar?.show("أُضيف إلى «${pl.name}»", "تراجع") { store.removeFromPlaylist(pl.id, t.id) }
                }
                pickFor = null
            },
            onNew = { newPlaylist = NewPlaylist(t); pickFor = null },
            onDismiss = { pickFor = null },
        )
    }
    newPlaylist?.let { np ->
        NameDialog(
            onCreate = { name ->
                val before = store.playlists.map { it.id }.toSet()
                store.createPlaylist(name, np.track?.id)
                val created = store.playlists.firstOrNull { it.id !in before }
                snackbar?.show("أُنشئت القائمة «${name.trim()}»", "تراجع") {
                    created?.let { store.deletePlaylist(it.id) }
                }
                newPlaylist = null
            },
            onDismiss = { newPlaylist = null },
        )
    }
}

@Composable
private fun TrackList(
    list: List<Track>,
    currentId: Long?,
    pad: PaddingValues,
    onPlay: (List<Track>, Int) -> Unit,
    onLongPress: (Track) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = pad) {
        itemsIndexed(list, key = { _, t -> t.id }) { i, t ->
            MediaRow(
                art = t,
                title = t.displayTitle,
                subtitle = t.artist.takeIf { it != UNKNOWN_ARTIST } ?: t.addedLabel,
                trailing = formatTime(t.durationMs),
                highlight = t.id == currentId,
                onClick = { onPlay(list, i) },
                onLongClick = { onLongPress(t) },
            )
        }
    }
}

@Composable
private fun PlaylistsTab(
    recent: List<Track>,
    most: List<Track>,
    playlists: List<Pair<Playlist, List<Track>>>,
    pad: PaddingValues,
    onOpen: (MediaGroup) -> Unit,
    onNew: () -> Unit,
) {
    val skin = LocalGlowSkin.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = pad) {
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { SpecialCard("المشغّلة مؤخراً", recent, onOpen) }
                item { SpecialCard("الأكثر تشغيلاً", most, onOpen) }
            }
        }
        item {
            Text(
                "+  قائمة جديدة",
                color = skin.accentText,
                fontSize = 17.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNew)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            )
        }
        items(playlists, key = { it.first.id }) { (p, list) ->
            MediaRow(
                art = list.firstOrNull(),
                title = p.name,
                subtitle = null,
                trailing = "${list.size} أغنية",
                highlight = false,
                onClick = { if (list.isNotEmpty()) onOpen(MediaGroup(p.name, "", list[0], list)) },
            )
        }
    }
}

@Composable
private fun SpecialCard(title: String, list: List<Track>, onOpen: (MediaGroup) -> Unit) {
    val skin = LocalGlowSkin.current
    Column(
        Modifier
            .width(150.dp)
            .clickable(enabled = list.isNotEmpty()) { onOpen(MediaGroup(title, "", list[0], list)) },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AlbumArt(list.firstOrNull(), Modifier.size(150.dp), radius = 28.dp, px = 400)
        Text(
            title, color = skin.text, fontSize = 16.sp, maxLines = 1,
            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        Text(
            "${list.size} أغنية", color = skin.textSecondary, fontSize = 13.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ActionRow(text: String, onClick: () -> Unit) {
    GlowDialogRow(text, onClick)
}

@Composable
private fun ActionsDialog(
    track: Track,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onAddToQueue: () -> Unit,
    onPlayNext: () -> Unit,
    onDismiss: () -> Unit,
) {
    GlowDialog(
        title = track.displayTitle,
        onDismiss = onDismiss,
        actions = { GlowDialogAction("إغلاق", onDismiss) },
    ) {
        ActionRow(if (isFavorite) "إزالة من المفضلة" else "إضافة إلى المفضلة", onToggleFavorite)
        ActionRow("تشغيل بعد المقطع الحالي", onPlayNext)
        ActionRow("إضافة إلى نهاية الطابور", onAddToQueue)
        ActionRow("إضافة إلى قائمة تشغيل", onAddToPlaylist)
    }
}

@Composable
private fun PickPlaylistDialog(
    playlists: List<Playlist>,
    onPick: (Playlist) -> Unit,
    onNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    GlowDialog(
        title = "إضافة إلى قائمة",
        onDismiss = onDismiss,
        actions = { GlowDialogAction("إلغاء", onDismiss) },
    ) {
        ActionRow("+  قائمة جديدة", onNew)
        playlists.forEach { p -> ActionRow(p.name) { onPick(p) } }
    }
}

@Composable
private fun NameDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    val skin = LocalGlowSkin.current
    var name by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    GlowDialog(
        title = "قائمة جديدة",
        onDismiss = onDismiss,
        actions = {
            GlowDialogAction("إلغاء", onDismiss)
            GlowDialogAction("إنشاء", { onCreate(name) }, enabled = name.isNotBlank(), primary = true)
        },
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(skin.chip)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            if (name.isEmpty()) Text("اسم القائمة", color = skin.textSecondary, fontSize = 15.sp)
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle = TextStyle(color = skin.text, fontSize = 15.sp),
                cursorBrush = SolidColor(skin.accentText),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, onClose: () -> Unit) {
    val skin = LocalGlowSkin.current
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(skin.chip)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlowIcon(IconKind.Search, skin.textSecondary, 20.dp)
        Box(Modifier.weight(1f).padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text("ابحث في مكتبتك", color = skin.textSecondary, fontSize = 15.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = skin.text, fontSize = 15.sp),
                cursorBrush = SolidColor(skin.accentText),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        }
        GlowIconTap(if (value.isEmpty()) "إغلاق البحث" else "مسح", {
            if (value.isEmpty()) onClose() else onChange("")
        }, size = 44.dp) {
            Text("✕", color = skin.textSecondary, fontSize = 16.sp, modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun PermissionPrompt(onRequestPermission: () -> Unit) {
    GlowEmptyState(
        title = "نحتاج إذن الموسيقى",
        message = "يحتاج Glow إذن قراءة الموسيقى لعرض مكتبتك.",
        icon = IconKind.Note,
        actionLabel = "السماح",
        onAction = onRequestPermission,
    )
}

@Composable
private fun CenteredMessage(text: String) {
    val skin = LocalGlowSkin.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = skin.textSecondary, fontSize = 16.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp))
    }
}
