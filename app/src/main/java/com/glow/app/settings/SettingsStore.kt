package com.glow.app.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

enum class ThemeMode { AUTO, LIGHT, DARK }

/** مصدر لون الخلفية: من اللوحة، أو أسود، أو أبيض، أو لون يختاره المستخدم. */
enum class BgChoice { PALETTE, BLACK, WHITE, CUSTOM }

/** إعدادات التطبيق المحفوظة في SharedPreferences، وتُقرأ كحالة Compose. */
class SettingsStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("glow_settings", Context.MODE_PRIVATE)

    var themeMode by mutableStateOf(
        ThemeMode.values().getOrElse(prefs.getInt("theme_mode", 0)) { ThemeMode.AUTO },
    )
        private set

    var playbackSpeed by mutableStateOf(prefs.getFloat("speed", 1f))
        private set

    var skipShortSongs by mutableStateOf(prefs.getBoolean("skip_short", false))
        private set

    var skipShortSeconds by mutableStateOf(prefs.getInt("skip_short_sec", 30))
        private set

    var paletteId by mutableStateOf(prefs.getString("palette_id", "glow") ?: "glow")
        private set

    var bgChoice by mutableStateOf(
        BgChoice.values().getOrElse(prefs.getInt("bg_choice", 0)) { BgChoice.PALETTE },
    )
        private set

    var customAccent by mutableStateOf(Color(prefs.getInt("custom_accent", 0xFFFF5A57.toInt())))
        private set

    var customBg by mutableStateOf(Color(prefs.getInt("custom_bg", 0xFF050C38.toInt())))
        private set

    /** التبويب الذي يفتح عليه التطبيق (اسم قيمة LibraryTab). */
    var startTab by mutableStateOf(prefs.getString("start_tab", "Tracks") ?: "Tracks")
        private set

    /** عدد أعمدة شبكة الألبومات والفنانين: 2 أو 3. */
    var albumColumns by mutableStateOf(prefs.getInt("album_columns", 2).coerceIn(2, 3))
        private set

    /** إيقاف التشغيل مؤقتاً عند فصل السماعة أو البلوتوث. */
    var pauseOnUnplug by mutableStateOf(prefs.getBoolean(PREF_PAUSE_ON_UNPLUG, true))
        private set

    fun updateStartTab(v: String) {
        startTab = v
        prefs.edit().putString("start_tab", v).apply()
    }

    fun updateAlbumColumns(v: Int) {
        albumColumns = v.coerceIn(2, 3)
        prefs.edit().putInt("album_columns", albumColumns).apply()
    }

    fun updatePauseOnUnplug(v: Boolean) {
        pauseOnUnplug = v
        prefs.edit().putBoolean(PREF_PAUSE_ON_UNPLUG, v).apply()
    }

    fun updatePalette(v: String) {
        paletteId = v
        prefs.edit().putString("palette_id", v).apply()
    }

    fun updateBgChoice(v: BgChoice) {
        bgChoice = v
        prefs.edit().putInt("bg_choice", v.ordinal).apply()
    }

    fun updateCustomAccent(v: Color) {
        customAccent = v
        prefs.edit().putInt("custom_accent", v.toArgb()).apply()
    }

    fun updateCustomBg(v: Color) {
        customBg = v
        prefs.edit().putInt("custom_bg", v.toArgb()).apply()
    }

    fun updateThemeMode(v: ThemeMode) {
        themeMode = v
        prefs.edit().putInt("theme_mode", v.ordinal).apply()
    }

    fun updatePlaybackSpeed(v: Float) {
        playbackSpeed = v
        prefs.edit().putFloat("speed", v).apply()
    }

    fun updateSkipShortSongs(v: Boolean) {
        skipShortSongs = v
        prefs.edit().putBoolean("skip_short", v).apply()
    }

    fun updateSkipShortSeconds(v: Int) {
        skipShortSeconds = v
        prefs.edit().putInt("skip_short_sec", v).apply()
    }

    companion object {
        const val PREFS_NAME = "glow_settings"
        const val PREF_PAUSE_ON_UNPLUG = "pause_on_unplug"
    }
}
