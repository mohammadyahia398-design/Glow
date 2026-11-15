package com.glow.app.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.widget.Toast
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
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
import com.glow.app.ui.theme.Colorways
import com.glow.app.ui.theme.LocalGlowSkin
import com.glow.app.ui.theme.colorToHsv
import com.glow.app.ui.theme.hsvColor
import kotlinx.coroutines.delay

private enum class Dlg { SPEED, SLEEP, SHORT }

/** شاشة الإعدادات على طراز Samsung: عنوان كبير وبطاقات مدوّرة مجمّعة بأقسام. */
@Composable
fun SettingsScreen(
    store: SettingsStore,
    onBack: () -> Unit,
    onRescan: () -> Unit,
    sleepEndsAt: Long?,
    onSleepTimer: (Int) -> Unit,
    audioSessionId: () -> Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val skin = LocalGlowSkin.current
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

        Section("الألوان") {
            ColorwayGrid(store)
            BackgroundPicker(store)
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
            Item(IconKind.Equalizer, "المعادل الصوتي", "فتح") {
                val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                    putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId())
                    putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                    putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                }
                try {
                    context.startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, "لا يوجد معادل على هذا الجهاز", Toast.LENGTH_SHORT).show()
                }
            }
        }

        Section("المكتبة") {
            Item(
                icon = IconKind.Timer,
                title = "تخطي الأغاني القصيرة",
                value = null,
                chevron = false,
                onClick = { store.updateSkipShortSongs(!store.skipShortSongs) },
                trailing = {
                    Switch(
                        checked = store.skipShortSongs,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = skin.accent,
                            checkedBorderColor = Color.Transparent,
                            uncheckedThumbColor = skin.textSecondary,
                            uncheckedTrackColor = skin.chip,
                            uncheckedBorderColor = skin.textSecondary.copy(alpha = 0.5f),
                        ),
                    )
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
    Text(
        title,
        color = skin.text,
        fontSize = 19.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 26.dp, bottom = 10.dp),
    )
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(skin.panel),
    ) { content() }
}

/** صف إعداد: أيقونة، عنوان، قيمة اختيارية، ثم سهم أو عنصر مخصص (مثل Switch). */
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
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlowIcon(icon, skin.text, 24.dp)
        Spacer(Modifier.width(16.dp))
        Text(
            title,
            color = skin.text,
            fontSize = 16.sp,
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
            GlowIcon(IconKind.Chevron, skin.textSecondary, 18.dp)
        }
    }
}

@Composable
private fun Divider() {
    // يبدأ بعد الأيقونة (18 + 24 + 16)
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 58.dp)
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

/** شبكة بطاقات اللوحات السائلة (عمودان) مع بطاقة «مخصّص» في آخرها. */
@Composable
private fun ColorwayGrid(store: SettingsStore) {
    val skin = LocalGlowSkin.current
    val all = Colorways.Presets + Colorways.custom(store.customAccent, store.customBg)
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        all.chunked(2).forEach { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { cw ->
                    FluidPaletteCard(
                        colors = cw.blobs,
                        base = cw.bg,
                        label = cw.name,
                        selected = store.paletteId == cw.id,
                        borderColor = skin.accent,
                        badgeBg = skin.accent,
                        badgeFg = skin.onAccent,
                        onClick = { store.updatePalette(cw.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (store.paletteId == Colorways.CUSTOM_ID) {
            Text(
                "لون التمييز",
                color = skin.textSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp),
            )
            HsvPicker(store.customAccent) { store.updateCustomAccent(it) }
        }
    }
}

/** اختيار الخلفية: من اللوحة أو أسود أو أبيض أو لون مخصّص. */
@Composable
private fun BackgroundPicker(store: SettingsStore) {
    val skin = LocalGlowSkin.current
    val options = listOf(
        BgChoice.PALETTE to "اللوحة",
        BgChoice.BLACK to "أسود",
        BgChoice.WHITE to "أبيض",
        BgChoice.CUSTOM to "مخصّص",
    )
    Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Text(
            "الخلفية",
            color = skin.text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 10.dp),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { (choice, label) ->
                val on = store.bgChoice == choice
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (on) skin.accent else skin.chip)
                        .clickable { store.updateBgChoice(choice) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        color = if (on) skin.onAccent else skin.text,
                        fontSize = 14.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
        if (store.bgChoice == BgChoice.CUSTOM) {
            Text(
                "لون الخلفية",
                color = skin.textSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp),
            )
            HsvPicker(store.customBg) { store.updateCustomBg(it) }
        }
        if (!(store.paletteId == Colorways.Glow.id && store.bgChoice == BgChoice.PALETTE)) {
            Text(
                "خيار «المظهر» أعلاه يعمل مع لوحة «سماوي» فقط؛ الخلفية هنا تحدد الفاتح والداكن.",
                color = skin.textSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp),
            )
        }
    }
}

/** معاينة لون وثلاثة منزلقات: الصبغة والتشبّع والسطوع. */
@Composable
private fun HsvPicker(color: Color, onChange: (Color) -> Unit) {
    val skin = LocalGlowSkin.current
    val initial = remember { colorToHsv(color) }
    var h by remember { mutableFloatStateOf(initial[0]) }
    var s by remember { mutableFloatStateOf(initial[1]) }
    var v by remember { mutableFloatStateOf(initial[2]) }
    val sliderColors = SliderDefaults.colors(
        thumbColor = skin.accent,
        activeTrackColor = skin.accent,
        inactiveTrackColor = skin.chip,
    )
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(color),
        )
        Slider(
            value = h,
            onValueChange = { h = it; onChange(hsvColor(h, s, v)) },
            valueRange = 0f..360f,
            colors = sliderColors,
        )
        Slider(
            value = s,
            onValueChange = { s = it; onChange(hsvColor(h, s, v)) },
            valueRange = 0f..1f,
            colors = sliderColors,
        )
        Slider(
            value = v,
            onValueChange = { v = it; onChange(hsvColor(h, s, v)) },
            valueRange = 0f..1f,
            colors = sliderColors,
        )
    }
}
