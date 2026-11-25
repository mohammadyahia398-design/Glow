package com.glow.app.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glow.app.settings.BgChoice
import com.glow.app.settings.SettingsStore
import com.glow.app.settings.ThemeMode
import com.glow.app.ui.common.GlowChoiceDialog
import com.glow.app.ui.common.GlowIcon
import com.glow.app.ui.common.IconKind
import com.glow.app.ui.common.GlowTopBar
import com.glow.app.ui.glass.components.GlassSwitch
import com.glow.app.ui.theme.Colorways
import androidx.compose.ui.graphics.Brush
import com.glow.app.liquid.backdrop.backdrops.rememberCanvasBackdrop
import com.glow.app.liquid.ui.LiquidToggle
import com.glow.app.ui.glass.core.rememberGlassTier
import com.glow.app.ui.theme.LocalGlowSkin
import com.glow.app.ui.theme.colorToHsv
import com.glow.app.ui.theme.hsvColor
import kotlinx.coroutines.delay

private enum class Dlg { SPEED, SLEEP, SHORT, EQUALIZER }

/** شاشة الإعدادات على طراز Samsung: عنوان كبير وبطاقات مدوّرة مجمّعة بأقسام. */
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
    val liquidBackdrop = rememberCanvasBackdrop {
        drawRect(Brush.verticalGradient(listOf(skin.bgTop, skin.bgBottom)))
    }
    var dlg by remember { mutableStateOf<Dlg?>(null) }

    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(sleepEndsAt) {
        while (sleepEndsAt != null) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val remainingMin = sleepEndsAt?.let { (((it - now).coerceAtLeast(0)) + 59_999) / 60_000 }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(bottom = 32.dp),
    ) {
        GlowTopBar(title = null, onBack = onBack)
        Text(
            "الإعدادات",
            color = skin.text,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
        )

        Section("المظهر") {
            AppearancePicker(store.themeMode) { store.updateThemeMode(it) }
        }

        Section("التشغيل") {
            Item(IconKind.Speed, "سرعة التشغيل", speedLabel(store.playbackSpeed)) { dlg = Dlg.SPEED }
            Divider()
            Item(
                IconKind.Moon,
                "مؤقّت النوم",
                if (remainingMin == null) "متوقف" else "متبقٍ $remainingMin د",
            ) { dlg = Dlg.SLEEP }
            Divider()
            Item(IconKind.Equalizer, "المعادل الصوتي", "تحكم متقدم") { dlg = Dlg.EQUALIZER }
        }

        Section("أدوات") {
            Item(IconKind.Note, "عزل الصوت", "فصل صوت المغني عن الموسيقى", onClick = onOpenVocalIsolation)
        }

        Section("المكتبة") {
            Item(
                icon = IconKind.Timer,
                title = "تخطي الأغاني القصيرة",
                value = null,
                chevron = false,
                onClick = { store.updateSkipShortSongs(!store.skipShortSongs) },
                trailing = {
                    if (glassTier.supportsRefraction) {
                        LiquidToggle(
                            selected = { store.skipShortSongs },
                            onSelect = { store.updateSkipShortSongs(it) },
                            backdrop = liquidBackdrop,
                            accentColor = skin.accent,
                            trackColor = skin.textSecondary.copy(alpha = 0.35f),
                            toggleOnTap = false, // النقر على الصف نفسه يبدّل الحالة
                        )
                    } else {
                        GlassSwitch(
                            checked = store.skipShortSongs,
                            onCheckedChange = { store.updateSkipShortSongs(it) },
                            checkedTrackColor = skin.accent,
                            uncheckedTrackColor = skin.textSecondary.copy(alpha = 0.35f),
                            rimColor = if (skin.isDark) Color.White else Color(0xFFFFFFFF),
                        )
                    }
                },
            )
            if (store.skipShortSongs) {
                Divider()
                Item(IconKind.Timer, "الحد الأدنى للمدة", "${store.skipShortSeconds} ثانية") { dlg = Dlg.SHORT }
            }
            Divider()
            Item(IconKind.Refresh, "إعادة فحص المكتبة", "تحديث", onClick = onRescan)
        }

        Section("حول") {
            val version = remember {
                runCatching {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                }.getOrNull() ?: "-"
            }
            Item(IconKind.Info, "Glow", "الإصدار $version", chevron = false, onClick = null)
        }
    }

    when (dlg) {
        Dlg.SPEED -> GlowChoiceDialog(
            "سرعة التشغيل",
            listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).map { it to speedLabel(it) },
            store.playbackSpeed,
            { store.updatePlaybackSpeed(it) },
        ) { dlg = null }

        Dlg.SLEEP -> GlowChoiceDialog(
            "مؤقّت النوم",
            listOf(
                0 to "إيقاف", 15 to "15 دقيقة", 30 to "30 دقيقة",
                45 to "45 دقيقة", 60 to "ساعة", 90 to "ساعة ونصف",
            ),
            if (sleepEndsAt == null) 0 else -1,
            { onSleepTimer(it) },
        ) { dlg = null }

        Dlg.SHORT -> GlowChoiceDialog(
            "الحد الأدنى للمدة",
            listOf(15, 30, 45, 60).map { it to "$it ثانية" },
            store.skipShortSeconds,
            { store.updateSkipShortSeconds(it) },
        ) { dlg = null }

        Dlg.EQUALIZER -> EqualizerSheet(
            audioSessionId = audioSessionId,
            onDismiss = { dlg = null },
        )

        null -> Unit
    }
}

private fun themeLabel(m: ThemeMode) = when (m) {
    ThemeMode.AUTO -> "تلقائي (حسب النظام)"
    ThemeMode.LIGHT -> "فاتح"
    ThemeMode.DARK -> "داكن"
}

private fun speedLabel(s: Float) = if (s == 1f) "عادية (×1.0)" else "×$s"

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val skin = LocalGlowSkin.current
    val shape = RoundedCornerShape(24.dp)
    Text(
        title,
        color = skin.textSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 24.dp, bottom = 8.dp),
    )
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(skin.panel.copy(alpha = 0.98f), skin.panel.copy(alpha = 0.80f)),
                ),
            )
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = if (skin.isDark) 0.07f else 0.40f), Color.Transparent),
                        endY = size.height * 0.5f,
                    ),
                )
            }
            .border(1.dp, skin.glassBorder.copy(alpha = 0.55f), shape),
    ) { content() }
}

/** صف إعداد: أيقونة داخل مربّع بلون التمييز، عنوان، قيمة اختيارية، ثم سهم أو عنصر مخصص (مثل Switch). */
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
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(skin.accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            GlowIcon(icon, skin.accentText, 22.dp)
        }
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
    // يبدأ بعد مربّع الأيقونة (16 + 36 + 14)
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 66.dp, end = 16.dp)
            .height(0.5.dp)
            .background(LocalGlowSkin.current.divider),
    )
}

/** اختيار المظهر بثلاث بطاقات معاينة: النظام (نصفان) وفاتح وداكن. */
@Composable
private fun AppearancePicker(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = listOf(
        ThemeMode.AUTO to "النظام",
        ThemeMode.LIGHT to "فاتح",
        ThemeMode.DARK to "داكن",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        options.forEach { (mode, label) ->
            ThemeOption(
                mode = mode,
                label = label,
                selected = mode == selected,
                onClick = { onSelect(mode) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ThemeOption(
    mode: ThemeMode,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val skin = LocalGlowSkin.current
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.95f)
                .clip(shape)
                .border(
                    width = if (selected) 2.5.dp else 0.5.dp,
                    color = if (selected) skin.accent else skin.divider,
                    shape = shape,
                ),
        ) {
            Canvas(Modifier.matchParentSize()) {
                when (mode) {
                    ThemeMode.LIGHT -> paintPhone(dark = false)
                    ThemeMode.DARK -> paintPhone(dark = true)
                    ThemeMode.AUTO -> {
                        clipRect(left = 0f, top = 0f, right = size.width / 2f, bottom = size.height) {
                            paintPhone(dark = false)
                        }
                        clipRect(left = size.width / 2f, top = 0f, right = size.width, bottom = size.height) {
                            paintPhone(dark = true)
                        }
                    }
                }
            }
            if (selected) {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .size(24.dp)
                        .background(skin.accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    GlowIcon(IconKind.Check, skin.onAccent, 16.dp)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            label,
            color = if (selected) skin.text else skin.textSecondary,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
        Spacer(Modifier.height(4.dp))
    }
}

/** يرسم معاينة هاتف مصغّرة: خلفية، جزيرة علوية، شريطان، وبطاقة سفلية. */
private fun DrawScope.paintPhone(dark: Boolean) {
    val bg = if (dark) Color(0xFF26292E) else Color(0xFFF1F2F4)
    val island = if (dark) Color(0xFF050607) else Color(0xFF15171A)
    val bar = if (dark) Color(0xFF3A3E45) else Color(0xFFDADCE0)
    val card = if (dark) Color(0xFF181A1E) else Color(0xFFFFFFFF)
    val w = size.width
    val h = size.height
    drawRect(bg)
    drawRoundRect(
        island,
        topLeft = Offset(w * .32f, h * .09f),
        size = Size(w * .36f, h * .075f),
        cornerRadius = CornerRadius(h * .04f),
    )
    drawRoundRect(
        bar,
        topLeft = Offset(w * .16f, h * .28f),
        size = Size(w * .68f, h * .09f),
        cornerRadius = CornerRadius(h * .045f),
    )
    drawRoundRect(
        bar,
        topLeft = Offset(w * .16f, h * .42f),
        size = Size(w * .44f, h * .05f),
        cornerRadius = CornerRadius(h * .025f),
    )
    drawRoundRect(
        card,
        topLeft = Offset(w * .12f, h * .58f),
        size = Size(w * .76f, h * .5f),
        cornerRadius = CornerRadius(h * .08f),
    )
}
