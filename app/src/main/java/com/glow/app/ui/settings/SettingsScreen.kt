package com.glow.app.ui.settings

import androidx.compose.ui.geometry.isSpecified
import com.glow.app.ui.theme.accentBrush
import com.glow.app.ui.common.glowLiquid
import com.glow.app.ui.common.LocalLiquidBackdrop
import com.glow.app.liquid.backdrop.effects.vibrancy
import com.glow.app.liquid.backdrop.effects.lens
import com.glow.app.liquid.backdrop.effects.blur
import com.glow.app.liquid.backdrop.drawBackdrop
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glow.app.liquid.backdrop.Backdrop
import com.glow.app.liquid.backdrop.backdrops.layerBackdrop
import com.glow.app.liquid.backdrop.backdrops.rememberLayerBackdrop
import com.glow.app.liquid.ui.GlowGlassSegmentedBar
import com.glow.app.liquid.ui.LiquidGlassCard
import com.glow.app.liquid.ui.LiquidToggle
import com.glow.app.settings.SettingsStore
import com.glow.app.settings.ThemeMode
import com.glow.app.ui.common.GlowChoiceDialog
import com.glow.app.ui.common.GlowFilledButton
import com.glow.app.ui.common.GlowIcon
import com.glow.app.ui.common.GlowIconTap
import com.glow.app.ui.common.IconKind
import com.glow.app.ui.glass.components.GlassSwitch
import com.glow.app.ui.glass.core.rememberGlassTier
import com.glow.app.ui.theme.LocalGlowSkin
import kotlinx.coroutines.delay
import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import android.text.format.Formatter
import android.widget.Toast
import com.glow.app.ui.library.LibraryTab
import com.glow.app.FeatureFlags
import com.glow.app.GlowStrings
import com.glow.app.R

private enum class Dlg { SPEED, SLEEP, SHORT, EQUALIZER, LANGUAGE, START_TAB, GRID }

/** خلفية الزجاج السائل لبطاقات الإعدادات؛ null على الأجهزة التي لا تدعم الانكسار. */
private val LocalSettingsBackdrop = compositionLocalOf<Backdrop?> { null }

private val CardRadius = 22.dp
private val RowIconSize = 22.dp

/**
 * شاشة الإعدادات: شريط علوي بزر رجوع دائري وعنوان في الوسط، ثم بطاقات مجمّعة بصفوف متساوية
 * (أيقونة خطية، عنوان، قيمة، سهم أو مفتاح)، وزر رئيسي واحد في الأسفل.
 * لون التمييز محجوز للحالة الفعّالة فقط (المفتاح، الخيار المحدد، الزر الرئيسي).
 */
@Composable
fun SettingsScreen(
    store: SettingsStore,
    onBack: () -> Unit,
    onRescan: () -> Unit,
    sleepEndsAt: Long?,
    onSleepTimer: (Int) -> Unit,
    audioSessionId: Int,
    modifier: Modifier = Modifier,
    onOpenVocalIsolation: () -> Unit = {},
) {
    val skin = LocalGlowSkin.current
    val context = LocalContext.current
    val glassTier = rememberGlassTier()
    val liquidBackdrop = rememberLayerBackdrop()
    var dlg by remember { mutableStateOf<Dlg?>(null) }
    var currentLanguage by remember { mutableStateOf(readAppLanguage(context)) }
    var cacheBytes by remember { mutableStateOf(cacheSize(context)) }
    val cacheLabel = remember(cacheBytes) { Formatter.formatShortFileSize(context, cacheBytes) }

    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(sleepEndsAt) {
        while (sleepEndsAt != null) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val remainingMin = sleepEndsAt?.let { (((it - now).coerceAtLeast(0)) + 59_999) / 60_000 }
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "-"
    }

    CompositionLocalProvider(
        LocalSettingsBackdrop provides (if (glassTier.supportsRefraction) liquidBackdrop else null),
        LocalLiquidBackdrop provides (if (glassTier.supportsRefraction) liquidBackdrop else null),
    ) {
        Box(modifier.fillMaxSize()) {
            // خلفية هادئة بوهج واحد خفيف بلون التمييز؛ هي ما تنكسر خلف البطاقات الزجاجية.
            Box(
                Modifier
                    .matchParentSize()
                    .layerBackdrop(liquidBackdrop)
                    .drawBehind {
                        drawRect(Brush.verticalGradient(listOf(skin.bgTop, skin.bgBottom)))
                        drawRect(
                            Brush.radialGradient(
                                listOf(skin.accent.copy(alpha = 0.20f), Color.Transparent),
                                center = Offset(size.width * 0.92f, size.height * 0.10f),
                                radius = size.width * 0.9f,
                            ),
                        )
                        drawRect(
                            Brush.radialGradient(
                                listOf(Color(0xFF7226FF).copy(alpha = 0.16f), Color.Transparent),
                                center = Offset(size.width * 0.05f, size.height * 0.62f),
                                radius = size.width * 0.9f,
                            ),
                        )
                    },
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding(),
            ) {
                SettingsTopBar(onBack)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Group {
                        ThemeRow(store.themeMode) { store.updateThemeMode(it) }
                    }

                    Group {
                        Item(IconKind.Speed, GlowStrings.get(R.string.playback_speed), speedLabel(store.playbackSpeed)) { dlg = Dlg.SPEED }
                        Divider()
                        Item(
                            IconKind.Moon,
                            GlowStrings.get(R.string.sleep_timer),
                            if (remainingMin == null) GlowStrings.get(R.string.sleep_status_off) else GlowStrings.get(R.string.sleep_remaining, remainingMin),
                        ) { dlg = Dlg.SLEEP }
                        Divider()
                        Item(IconKind.Equalizer, GlowStrings.get(R.string.eq_title), null) { dlg = Dlg.EQUALIZER }
                    }

                    Group {
                        ToggleItem(
                            icon = IconKind.Timer,
                            title = GlowStrings.get(R.string.skip_short_title),
                            checked = store.skipShortSongs,
                            onChange = { store.updateSkipShortSongs(it) },
                            liquidBackdrop = liquidBackdrop,
                            supportsRefraction = glassTier.supportsRefraction,
                        )
                        if (store.skipShortSongs) {
                            Divider()
                            Item(IconKind.Timer, GlowStrings.get(R.string.min_duration), GlowStrings.get(R.string.seconds_fmt, store.skipShortSeconds)) { dlg = Dlg.SHORT }
                        }
                    }

                    Group {
                        ToggleItem(
                            icon = IconKind.Pause,
                            title = GlowStrings.get(R.string.settings_pause_unplug),
                            checked = store.pauseOnUnplug,
                            onChange = { store.updatePauseOnUnplug(it) },
                            liquidBackdrop = liquidBackdrop,
                            supportsRefraction = glassTier.supportsRefraction,
                        )
                    }

                    Group {
                        if (android.os.Build.VERSION.SDK_INT >= 33) {
                            Item(IconKind.Info, GlowStrings.get(R.string.settings_language), languageLabel(currentLanguage)) { dlg = Dlg.LANGUAGE }
                            Divider()
                        }
                        Item(IconKind.Playlist, GlowStrings.get(R.string.settings_start_tab), tabLabel(store.startTab)) { dlg = Dlg.START_TAB }
                        Divider()
                        Item(IconKind.Album, GlowStrings.get(R.string.settings_grid_size), GlowStrings.get(R.string.grid_columns_fmt, store.albumColumns)) { dlg = Dlg.GRID }
                    }

                    Group {
                        Item(IconKind.Refresh, GlowStrings.get(R.string.settings_clear_cache), cacheLabel) {
                            clearCache(context)
                            cacheBytes = cacheSize(context)
                            Toast.makeText(context, GlowStrings.get(R.string.settings_cache_cleared), Toast.LENGTH_SHORT).show()
                        }
                    }

                    if (FeatureFlags.VOCAL_ISOLATION) {
                        Group {
                            Item(IconKind.Note, GlowStrings.get(R.string.vocal_isolation), GlowStrings.get(R.string.separate_vocals), onClick = onOpenVocalIsolation)
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    GlowFilledButton(onClick = onRescan, modifier = Modifier.fillMaxWidth()) {
                        GlowIcon(IconKind.Refresh, skin.onAccent, 20.dp)
                        Text(GlowStrings.get(R.string.rescan))
                    }
                    Text(
                        GlowStrings.get(R.string.settings_version, version),
                        color = skin.textSecondary,
                        fontSize = 12.5.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp),
                    )
                }
            }
        }
    }

    when (dlg) {
        Dlg.SPEED -> GlowChoiceDialog(
            GlowStrings.get(R.string.playback_speed),
            listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).map { it to speedLabel(it) },
            store.playbackSpeed,
            { store.updatePlaybackSpeed(it) },
        ) { dlg = null }

        Dlg.SLEEP -> GlowChoiceDialog(
            GlowStrings.get(R.string.sleep_timer),
            listOf(
                0 to GlowStrings.get(R.string.sleep_option_off), 15 to GlowStrings.get(R.string.minutes_fmt, 15), 30 to GlowStrings.get(R.string.minutes_fmt, 30),
                45 to GlowStrings.get(R.string.minutes_fmt, 45), 60 to GlowStrings.get(R.string.hour_one), 90 to GlowStrings.get(R.string.hour_half),
            ),
            if (sleepEndsAt == null) 0 else -1,
            { onSleepTimer(it) },
        ) { dlg = null }

        Dlg.SHORT -> GlowChoiceDialog(
            GlowStrings.get(R.string.min_duration),
            listOf(15, 30, 45, 60).map { it to GlowStrings.get(R.string.seconds_fmt, it) },
            store.skipShortSeconds,
            { store.updateSkipShortSeconds(it) },
        ) { dlg = null }

        Dlg.LANGUAGE -> GlowChoiceDialog(
            GlowStrings.get(R.string.settings_language),
            listOf("" to GlowStrings.get(R.string.language_system), "ar" to "العربية", "en" to "English"),
            currentLanguage,
            { applyAppLanguage(context, it); currentLanguage = it },
        ) { dlg = null }

        Dlg.START_TAB -> GlowChoiceDialog(
            GlowStrings.get(R.string.settings_start_tab),
            LibraryTab.barTabs.map { it.name to it.label },
            store.startTab,
            { store.updateStartTab(it) },
        ) { dlg = null }

        Dlg.GRID -> GlowChoiceDialog(
            GlowStrings.get(R.string.settings_grid_size),
            listOf(2, 3).map { it to GlowStrings.get(R.string.grid_columns_fmt, it) },
            store.albumColumns,
            { store.updateAlbumColumns(it) },
        ) { dlg = null }

        Dlg.EQUALIZER -> EqualizerSheet(
            audioSessionId = audioSessionId,
            onDismiss = { dlg = null },
        )

        null -> Unit
    }
}

private fun speedLabel(s: Float) = if (s == 1f) GlowStrings.get(R.string.speed_normal) else "×$s"

/** شريط علوي: زر رجوع دائري عند الطرف وعنوان في الوسط تماماً. */
@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    val skin = LocalGlowSkin.current
    Box(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        GlowIconTap(GlowStrings.get(R.string.back), onBack, Modifier.align(Alignment.CenterStart), size = 44.dp, background = skin.chip) {
            GlowIcon(IconKind.Back, skin.text, 22.dp)
        }
        Text(
            GlowStrings.get(R.string.settings),
            color = skin.text,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** بطاقة مجمّعة: زجاج سائل (أو سطح صلب بديل) وحافة واحدة موحّدة فوقه. */
@Composable
private fun Group(content: @Composable ColumnScope.() -> Unit) {
    val skin = LocalGlowSkin.current
    val shape = RoundedCornerShape(CardRadius)
    val glassBackdrop = LocalSettingsBackdrop.current
    Box(Modifier.fillMaxWidth()) {
        if (glassBackdrop != null) {
            LiquidGlassCard(
                backdrop = glassBackdrop,
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = CardRadius,
                surfaceColor = skin.panel.copy(alpha = if (skin.isDark) 0.68f else 0.74f),
            ) { content() }
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(skin.panel.copy(alpha = 0.97f)),
            ) { content() }
        }
        Box(
            Modifier
                .matchParentSize()
                .border(1.dp, skin.glassBorder.copy(alpha = 0.22f), shape),
        )
    }
}

/** صف إعداد موحّد: أيقونة خطية، عنوان، قيمة اختيارية، ثم سهم أو عنصر مخصص (مثل المفتاح). */
@Composable
private fun Item(
    icon: IconKind,
    title: String,
    value: String?,
    chevron: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)?,
) {
    val skin = LocalGlowSkin.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlowIcon(icon, skin.text.copy(alpha = 0.78f), RowIconSize)
        Spacer(Modifier.width(14.dp))
        Text(
            title,
            color = skin.text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Spacer(Modifier.width(12.dp))
            Text(value, color = skin.textSecondary, fontSize = 14.sp)
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        } else if (chevron) {
            Spacer(Modifier.width(6.dp))
            GlowIcon(IconKind.Chevron, skin.textSecondary.copy(alpha = 0.7f), 18.dp)
        }
    }
}

@Composable
private fun Divider() {
    // يبدأ بعد الأيقونة (16 + 22 + 14) وينتهي بنفس هامش الصف.
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 52.dp, end = 16.dp)
            .height(0.5.dp)
            .background(LocalGlowSkin.current.divider),
    )
}

/**
 * المظهر: كبسولة زجاجية فيها عدسة ليكويد جلاس بلون التمييز تنزلق إلى الخيار المحدد
 * بحركة نابضية، وتنتفخ قليلاً أثناء الانتقال ثم تستقر.
 */
@Composable
private fun ThemeRow(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val skin = LocalGlowSkin.current
    val backdrop = LocalLiquidBackdrop.current
    val options = listOf(
        ThemeMode.AUTO to GlowStrings.get(R.string.theme_system),
        ThemeMode.LIGHT to GlowStrings.get(R.string.theme_light),
        ThemeMode.DARK to GlowStrings.get(R.string.theme_dark),
    )
    val index = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    val pill = RoundedCornerShape(50)
    val accent = skin.accentBrush()
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(
            GlowStrings.get(R.string.appearance),
            color = skin.text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(12.dp))
        if (backdrop != null) {
            // نفس مكوّن الشريط السفلي: زجاجي، يُنقر ويُسحب.
            GlowGlassSegmentedBar(
                labels = options.map { it.second },
                selectedIndex = index,
                onSelect = { onSelect(options[it].first) },
                backdrop = backdrop,
            )
            return@Column
        }
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(50.dp)
                .glowLiquid(
                    pill,
                    SolidColor(skin.text),
                    skin.isDark,
                    fillAlpha = if (skin.isDark) 0.07f else 0.05f,
                    interactive = false,
                )
                .padding(4.dp),
        ) {
            val segW = maxWidth / options.size
            val target = segW * index
            val x by animateDpAsState(
                target,
                spring(dampingRatio = 0.66f, stiffness = 360f),
                label = "themeLensX",
            )
            val moving = kotlin.math.abs((x - target).value) > 0.5f
            val bulge by animateFloatAsState(
                if (moving) 1.10f else 1f,
                spring(dampingRatio = 0.55f, stiffness = 420f),
                label = "themeLensBulge",
            )
            Box(
                Modifier
                    .offset(x = x)
                    .width(segW)
                    .fillMaxHeight()
                    .then(
                        if (backdrop != null) {
                            Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { pill },
                                effects = {
                                    vibrancy()
                                    blur(2f.dp.toPx())
                                    if (size.isSpecified && size.minDimension > 0f) {
                                        lens(size.minDimension * 0.3f, size.minDimension * 0.6f)
                                    }
                                },
                                layerBlock = {
                                    scaleX = bulge
                                    scaleY = bulge
                                },
                                onDrawSurface = {
                                    drawRect(accent, alpha = 0.9f)
                                    drawRect(
                                        Brush.verticalGradient(
                                            listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
                                            endY = size.height * 0.5f,
                                        ),
                                    )
                                },
                            )
                        } else {
                            Modifier
                                .graphicsLayer {
                                    scaleX = bulge
                                    scaleY = bulge
                                }
                                .clip(pill)
                                .background(accent)
                        },
                    ),
            )
            Row(Modifier.fillMaxSize()) {
                options.forEachIndexed { i, (mode, label) ->
                    val on = i == index
                    val tc by animateColorAsState(
                        if (on) skin.onAccent else skin.textSecondary,
                        label = "themeSegText",
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(pill)
                            .semantics { this.selected = on }
                            .clickable(
                                interactionSource = null,
                                indication = null,
                                role = Role.Tab,
                            ) { onSelect(mode) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            color = tc,
                            fontSize = 14.sp,
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

private fun tabLabel(name: String): String =
    (LibraryTab.barTabs.firstOrNull { it.name == name } ?: LibraryTab.Tracks).label

private fun languageLabel(tag: String): String = when (tag) {
    "ar" -> "العربية"
    "en" -> "English"
    else -> GlowStrings.get(R.string.language_system)
}

/** لغة التطبيق الحالية ("" = لغة النظام). تعمل من أندرويد 13 فما فوق. */
private fun readAppLanguage(context: Context): String {
    if (Build.VERSION.SDK_INT < 33) return ""
    val locales = context.getSystemService(LocaleManager::class.java)?.applicationLocales
    return locales?.takeIf { !it.isEmpty }?.get(0)?.language ?: ""
}

private fun applyAppLanguage(context: Context, tag: String) {
    if (Build.VERSION.SDK_INT < 33) return
    context.getSystemService(LocaleManager::class.java)?.applicationLocales =
        if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
}

private fun cacheSize(context: Context): Long =
    listOfNotNull(context.cacheDir, context.externalCacheDir).sumOf { dir ->
        dir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
    }

private fun clearCache(context: Context) {
    listOfNotNull(context.cacheDir, context.externalCacheDir).forEach { dir ->
        dir.listFiles()?.forEach { runCatching { it.deleteRecursively() } }
    }
}

/** صف إعداد بمفتاح: زجاج سائل حيث يُدعم الانكسار، وإلا المفتاح العادي. النقر على الصف يبدّل الحالة. */
@Composable
private fun ToggleItem(
    icon: IconKind,
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    liquidBackdrop: Backdrop,
    supportsRefraction: Boolean,
) {
    val skin = LocalGlowSkin.current
    Item(
        icon = icon,
        title = title,
        value = null,
        chevron = false,
        onClick = { onChange(!checked) },
        trailing = {
            if (supportsRefraction) {
                LiquidToggle(
                    selected = { checked },
                    onSelect = onChange,
                    backdrop = liquidBackdrop,
                    accentColor = skin.accent,
                    trackColor = skin.textSecondary.copy(alpha = 0.35f),
                    toggleOnTap = false,
                )
            } else {
                GlassSwitch(
                    checked = checked,
                    onCheckedChange = onChange,
                    checkedTrackColor = skin.accent,
                    uncheckedTrackColor = skin.textSecondary.copy(alpha = 0.35f),
                    rimColor = Color.White,
                )
            }
        },
    )
}
