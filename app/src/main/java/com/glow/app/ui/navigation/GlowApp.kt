package com.glow.app.ui.navigation

import com.glow.app.ui.common.LocalLiquidBackdrop
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.glow.app.GlowActions
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.glow.app.media.MediaStoreRepository
import com.glow.app.media.displayTitle
import com.glow.app.media.PlaybackConnection
import com.glow.app.media.PlayerState
import com.glow.app.media.Track
import com.glow.app.settings.LibraryStore
import com.glow.app.settings.SettingsStore
import com.glow.app.ui.settings.SettingsScreen
import com.glow.app.ui.album.CollectionScreen
import com.glow.app.ui.glass.core.GlassConfig
import com.glow.app.ui.glass.core.ProvideGlassConfig
import com.glow.app.ui.glass.core.rememberGlassTier
import com.glow.app.ui.glass.core.rememberSystemReduceMotion
import com.glow.app.ui.glass.surfaces.glassBackdropSource
import com.glow.app.ui.glass.surfaces.rememberGlassBackdrop
import com.glow.app.liquid.backdrop.backdrops.layerBackdrop
import com.glow.app.liquid.backdrop.backdrops.rememberLayerBackdrop
import com.glow.app.liquid.ui.GlowLiquidBottomBar
import com.glow.app.ui.common.GlowIcon
import com.glow.app.ui.common.GlowSnackbarHost
import com.glow.app.ui.common.GlowSnackbarState
import com.glow.app.ui.common.LocalGlowSnackbar
import com.glow.app.ui.glass.components.LiquidBarItem
import com.glow.app.ui.glass.components.LiquidBarIcon
import com.glow.app.ui.glass.components.LiquidGlassBar
import com.glow.app.ui.library.LibraryTab
import com.glow.app.ui.library.MediaGroup
import com.glow.app.ui.library.LibraryScreen
import com.glow.app.ui.nowplaying.ArtMiniPlayer
import com.glow.app.ui.nowplaying.ArtMiniPlayerHeight
import com.glow.app.ui.nowplaying.NowPlayingScreen
import com.glow.app.ui.online.OnlineSearchScreen
import com.glow.app.ui.vocal.VocalIsolationScreen
import com.glow.app.ui.common.GlowTopBar
import com.glow.app.ui.theme.LocalGlowSkin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.random.Random

/** ارتفاع الشريط السفلي مع هوامشه؛ يُرفَع به المشغّل المصغّر فوقه. */
private val BarLift = 84.dp


private val idleState = MutableStateFlow(PlayerState())
private val idleSleep = MutableStateFlow<Long?>(null)

/**
 * جذر التطبيق: صلاحيات + قراءة المكتبة + ربط الخدمة + التنقل (مكتبة ← ألبوم ← مشغّل كامل).
 * التنقل بحالة داخلية، وزر الرجوع يغلق المشغّل ثم الألبوم.
 */
@Composable
fun GlowApp(
    settings: SettingsStore,
    modifier: Modifier = Modifier,
    launchAction: String? = null,
    onLaunchActionHandled: () -> Unit = {},
) {
    val skin = LocalGlowSkin.current
    val context = LocalContext.current
    val appContext = context.applicationContext
    val repository = remember { MediaStoreRepository(appContext) }
    val connection = remember { PlaybackConnection(appContext) }
    val library = remember { LibraryStore(appContext) }
    val snackbar = remember { GlowSnackbarState() }

    DisposableEffect(connection) {
        connection.bind()
        onDispose { connection.unbind() }
    }

    val controller by connection.controller.collectAsState()
    val stateFlow = remember(controller) { controller?.state ?: idleState }
    val playerState by stateFlow.collectAsState()

    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissions = remember(audioPermission) {
        buildList {
            add(audioPermission)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }
    var hasAudioAccess by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result -> hasAudioAccess = result[audioPermission] == true }

    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var rescan by remember { mutableIntStateOf(0) }
    LaunchedEffect(hasAudioAccess, rescan) {
        if (hasAudioAccess) {
            val isRescan = rescan > 0
            if (isRescan) loaded = false
            tracks = repository.loadTracks()
            loaded = true
            if (isRescan) snackbar.show("تم تحديث المكتبة، عدد الأغاني: ${tracks.size}")
        }
    }
    val visibleTracks = remember(tracks, settings.skipShortSongs, settings.skipShortSeconds) {
        if (settings.skipShortSongs) {
            tracks.filter { it.durationMs >= settings.skipShortSeconds * 1000L }
        } else {
            tracks
        }
    }
    val sleepFlow = remember(controller) { controller?.sleepEndsAt ?: idleSleep }
    val sleepEndsAt by sleepFlow.collectAsState()
    LaunchedEffect(controller, settings.playbackSpeed) {
        controller?.setSpeed(settings.playbackSpeed)
    }

    var collection by remember { mutableStateOf<MediaGroup?>(null) }
    var showPlayer by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showVocal by remember { mutableStateOf(false) }
    var showOnline by remember { mutableStateOf(false) }
    var libraryTab by remember { mutableStateOf(LibraryTab.Tracks) }
    val barItems = remember {
        LibraryTab.barTabs.map { t ->
            LiquidBarItem(t.name, t.label) { tint -> LiquidBarIcon(t.icon, tint, 26.dp) }
        }
    }
    // أول معالج مسجَّل أقل أولوية: يُغلق المشغّل ثم الألبوم ثم الإعدادات.
    BackHandler(enabled = showSettings) { showSettings = false }
    BackHandler(enabled = showVocal) { showVocal = false }
    BackHandler(enabled = showOnline) { showOnline = false }
    BackHandler(enabled = showPlayer) { showPlayer = false }
    BackHandler(enabled = !showPlayer && collection != null) { collection = null }

    // اختصارات أيقونة التطبيق: تشغيل عشوائي، أو فتح البحث في الإنترنت.
    LaunchedEffect(launchAction, loaded, controller, hasAudioAccess) {
        when (launchAction) {
            null -> Unit
            GlowActions.OPEN_ONLINE -> {
                showPlayer = false
                showSettings = false
                showVocal = false
                collection = null
                showOnline = true
                onLaunchActionHandled()
            }
            GlowActions.SHUFFLE_ALL -> {
                if (!hasAudioAccess) {
                    onLaunchActionHandled()
                } else if (loaded && controller != null) {
                    if (visibleTracks.isNotEmpty()) {
                        showOnline = false
                        controller?.setShuffle(true)
                        connection.play(visibleTracks, Random.nextInt(visibleTracks.size))
                    }
                    onLaunchActionHandled()
                }
            }
            else -> onLaunchActionHandled()
        }
    }

    val tier = rememberGlassTier()
    val reduceMotion = rememberSystemReduceMotion()
    val glassConfig = remember(tier, reduceMotion, skin) {
        GlassConfig(
            tier = tier,
            tint = skin.glassTint,
            fallbackBase = skin.glassBase,
            reduceMotion = reduceMotion,
        )
    }
    val backdrop = rememberGlassBackdrop()
    // الشريط الزجاجي السائل (أندرويد 13+): يلتقط محتوى الشاشة المجاور له، وما دون ذلك يبقى على الشريط القديم.
    val liquidBackdrop = rememberLayerBackdrop()
    // خلفية هادئة منفصلة للأزرار الزجاجية داخل الشاشات (لا يجوز أن يلتقط العنصر المصدر الذي يحتويه).
    val contentBackdrop = rememberLayerBackdrop()
    val useLiquidBar = tier.supportsRefraction
    val current = playerState.current
    LaunchedEffect(current?.id) { current?.let { library.recordPlay(it.id) } }
    LaunchedEffect(controller) {
        controller?.playbackErrors?.collect { f ->
            val name = f.track?.displayTitle?.let { "«$it»" } ?: "الأغنية"
            val text = "تعذّر تشغيل $name: ${f.reason}" + if (f.stopped) "" else " — تم التخطي"
            val failed = f.track
            if (failed != null) snackbar.show(text, "إعادة المحاولة") { controller?.retryTrack(failed) }
            else snackbar.show(text)
        }
    }

    ProvideGlassConfig(glassConfig) {
      CompositionLocalProvider(LocalGlowSnackbar provides snackbar) {
        val barVisible = !showSettings && !showVocal && !showOnline && collection == null && !showPlayer
        val miniVisible = current != null && !showPlayer
        Box(modifier.fillMaxSize()) {
            Box(
                Modifier
                    .matchParentSize()
                    .glassBackdropSource(backdrop)
                    .then(if (useLiquidBar) Modifier.layerBackdrop(liquidBackdrop) else Modifier)
                    .background(Brush.verticalGradient(listOf(skin.bgTop, skin.bgBottom))),
            ) {
                Box(
                    Modifier
                        .matchParentSize()
                        .layerBackdrop(contentBackdrop)
                        .drawBehind {
                            drawRect(Brush.verticalGradient(listOf(skin.bgTop, skin.bgBottom)))
                            drawRect(
                                Brush.radialGradient(
                                    listOf(skin.accent.copy(alpha = 0.10f), Color.Transparent),
                                    center = Offset(size.width * 0.90f, size.height * 0.06f),
                                    radius = size.width * 0.9f,
                                ),
                            )
                            drawRect(
                                Brush.radialGradient(
                                    listOf(Color(0xFF7226FF).copy(alpha = 0.08f), Color.Transparent),
                                    center = Offset(size.width * 0.08f, size.height * 0.55f),
                                    radius = size.width * 0.8f,
                                ),
                            )
                        },
                )
                CompositionLocalProvider(
                    LocalLiquidBackdrop provides (if (useLiquidBar) contentBackdrop else null),
                ) {
                val open = collection
                if (showVocal) {
                    Column(Modifier.fillMaxSize()) {
                        GlowTopBar(title = null, onBack = { showVocal = false })
                        VocalIsolationScreen(Modifier.weight(1f))
                    }
                } else if (showOnline) {
                    Column(Modifier.fillMaxSize()) {
                        GlowTopBar(title = "البحث في الإنترنت", onBack = { showOnline = false })
                        OnlineSearchScreen(
                            currentId = current?.id,
                            isPlaying = playerState.isPlaying,
                            store = library,
                            onPlay = { list, i -> connection.play(list, i) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else if (showSettings) {
                    SettingsScreen(
                        store = settings,
                        onBack = { showSettings = false },
                        onRescan = { rescan++ },
                        sleepEndsAt = sleepEndsAt,
                        onSleepTimer = { controller?.setSleepTimer(it) },
                        audioSessionId = playerState.audioSessionId,
                        onOpenVocalIsolation = { showVocal = true },
                    )
                } else if (open == null) {
                    LibraryScreen(
                        tracks = visibleTracks,
                        loaded = loaded,
                        hasAudioAccess = hasAudioAccess,
                        currentId = current?.id,
                        isPlaying = playerState.isPlaying,
                        store = library,
                        barTab = libraryTab,
                        onRequestPermission = { launcher.launch(permissions) },
                        onPlay = { list, i -> connection.play(list, i) },
                        onQueueTrack = { track, playNext ->
                            val queued = stateFlow.value.queue
                            when {
                                queued.any { it.id == track.id } ->
                                    snackbar.show("المقطع موجود بالفعل في الطابور")
                                stateFlow.value.current == null -> {
                                    controller?.addToQueue(listOf(track))
                                    snackbar.show("بدأ تشغيل المقطع")
                                }
                                else -> {
                                    if (playNext) controller?.playNext(listOf(track))
                                    else controller?.addToQueue(listOf(track))
                                    snackbar.show(
                                        if (playNext) "سيُشغَّل بعد المقطع الحالي" else "أُضيف إلى نهاية الطابور",
                                        "تراجع",
                                    ) {
                                        val pos = stateFlow.value.queue.indexOfFirst { it.id == track.id }
                                        if (pos >= 0) controller?.removeQueueItem(pos)
                                    }
                                }
                            }
                        },
                        onOpen = { collection = it },
                        onOpenSettings = { showSettings = true },
                        onOpenVocalIsolation = { showVocal = true },
                        onOpenOnline = { showOnline = true },
                    )
                } else {
                    CollectionScreen(
                        collection = open,
                        currentId = current?.id,
                        onBack = { collection = null },
                        onPlay = { i -> connection.play(open.tracks, i) },
                        onShuffle = {
                            // Random.nextInt(0) يرمي استثناءً إن كانت المجموعة فارغة.
                            if (open.tracks.isNotEmpty()) {
                                controller?.setShuffle(true)
                                connection.play(open.tracks, Random.nextInt(open.tracks.size))
                            }
                        },
                    )
                }
                }
            }

            // تلاشٍ خفيف عند الحافة السفلية: خارج المصدر، فلا يلتقطه الزجاج وتبقى القائمة تنكسر خلف الشريط.
            BottomFade(skin.bgBottom, barVisible)

            val miniLift by animateDpAsState(if (barVisible) BarLift else 0.dp, label = "miniLift")

            if (miniVisible && current != null) {
                ArtMiniPlayer(
                    track = current,
                    isPlaying = playerState.isPlaying,
                    progress = { playerState.progress },
                    onTogglePlay = { controller?.togglePlay() },
                    onNext = { controller?.next() },
                    onPrevious = { controller?.previous() },
                    onClick = { showPlayer = true },
                    onSeek = { f -> controller?.seekTo((f * playerState.durationMs).toLong()) },
                    backdrop = if (useLiquidBar) liquidBackdrop else null,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp + miniLift),
                )
            }

            // الشريط الزجاجي السفلي: أخٌ للمصدر وفوقه، فيلتقط الخلفية المشتركة.
            AnimatedVisibility(
                visible = barVisible,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                val barModifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                if (useLiquidBar) {
                    GlowLiquidBottomBar(
                        items = barItems,
                        selectedId = libraryTab.name,
                        onSelect = { id -> libraryTab = LibraryTab.valueOf(id) },
                        backdrop = liquidBackdrop,
                        modifier = barModifier,
                    )
                } else {
                    LiquidGlassBar(
                        items = barItems,
                        selectedId = libraryTab.name,
                        onSelect = { id -> libraryTab = LibraryTab.valueOf(id) },
                        backdrop = backdrop,
                        modifier = barModifier,
                    )
                }
            }

            // رسائل التأكيد: فوق الشريط والمشغّل المصغّر، وتحت المشغّل الكامل.
            val snackLift by animateDpAsState(
                (if (barVisible) BarLift else 0.dp) + (if (miniVisible) ArtMiniPlayerHeight + 12.dp else 0.dp),
                label = "snackLift",
            )
            GlowSnackbarHost(
                state = snackbar,
                backdrop = if (useLiquidBar) liquidBackdrop else null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp + snackLift),
            )

            AnimatedVisibility(
                visible = showPlayer && current != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                NowPlayingScreen(
                    state = playerState,
                    audioSessionId = playerState.audioSessionId,
                    onClose = { showPlayer = false },
                    onSeek = { controller?.seekTo(it) },
                    onTogglePlay = { controller?.togglePlay() },
                    onNext = { controller?.next() },
                    onPrevious = { controller?.previous() },
                    onShuffle = { controller?.setShuffle(!playerState.shuffle) },
                    onRepeat = { controller?.cycleRepeat() },
                    onPlayQueueItem = { controller?.playQueueItem(it) },
                    onMoveQueueItem = { from, to -> controller?.moveQueueItem(from, to) },
                    onRemoveQueueItem = { controller?.removeQueueItem(it) },
                    onClearUpcoming = { controller?.clearUpcoming() },
                )
            }
        }
      }
    }
}

/**
 * تلاشٍ ناعم بلون الخلفية عند أسفل الشاشة يحسّن وضوح حافة الشريط.
 * يُرسم خارج مصدر الخلفية الزجاجية عمداً: الزجاج يلتقط القائمة كما هي
 * فتظهر صفوفها وأغلفتها منكسرة عبر الشريط، بدل لون مسطح.
 */
@Composable
private fun BoxScope.BottomFade(color: Color, visible: Boolean) {
    val navHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.BottomCenter),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(BarLift + navHeight)
                .background(Brush.verticalGradient(listOf(color.copy(alpha = 0f), color.copy(alpha = 0.85f)))),
        )
    }
}
