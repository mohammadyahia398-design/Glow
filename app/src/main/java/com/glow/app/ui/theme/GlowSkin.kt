package com.glow.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Semantic colors shared by every Glow screen and reusable component. */
@Immutable
data class GlowSkin(
    val isDark: Boolean,
    val bgTop: Color,
    val bgBottom: Color,
    val text: Color,
    val textSecondary: Color,
    val accent: Color,
    val onAccent: Color,
    val accentText: Color,
    val panel: Color,
    val chip: Color,
    val divider: Color,
    val sheet: Color,
    val glassTint: Color,
    val glassBase: Color,
    val glassBorder: Color,
    val containerColor: Color = Color(0xFF151522),
    val onContainerColor: Color = Color(0xFFE4D4FF),
    val outlineVariantColor: Color = Color(0xFF26263A),
)

object GlowSkins {
    private val Void = Color(0xFF000002)
    private val Night = Color(0xFF04040A)
    private val Surface = Color(0xFF0B0B14)
    private val Lifted = Color(0xFF151522)
    private val Amber = Color(0xFFF042FF)
    private val Ivory = Color(0xFFE4D4FF)
    private val Muted = Color(0xFF9A90B8)
    private val LightInk = Color(0xFF0E0060)

    val Dark = GlowSkin(
        isDark = true,
        bgTop = Void,
        bgBottom = Night,
        text = Ivory,
        textSecondary = Muted,
        accent = Amber,
        onAccent = Color.White,
        accentText = Amber,
        panel = Surface,
        chip = Lifted,
        divider = Ivory.copy(alpha = 0.075f),
        sheet = Surface.copy(alpha = 0.97f),
        glassTint = Ivory,
        glassBase = Night,
        glassBorder = Ivory.copy(alpha = 0.34f),
        containerColor = Lifted,
        onContainerColor = Ivory,
        outlineVariantColor = Color(0xFF26263A),
    )

    val Light = GlowSkin(
        isDark = false,
        bgTop = Color(0xFFFEFDFF),
        bgBottom = Color(0xFFF6F1FD),
        text = LightInk,
        textSecondary = Color(0xFF5B5490),
        accent = Color(0xFFD62BEA),
        onAccent = Color.White,
        accentText = Color(0xFF7226FF),
        panel = Color(0xFFFFFFFF),
        chip = Color(0xFFF0E9FC),
        divider = LightInk.copy(alpha = 0.10f),
        sheet = Color(0xFFFFFFFF),
        glassTint = Color.White,
        glassBase = Color(0xFFFEFDFF),
        glassBorder = LightInk.copy(alpha = 0.18f),
        containerColor = Color(0xFFEBDDFF),
        onContainerColor = LightInk,
        outlineVariantColor = Color(0xFFDDD2F2),
    )
}

val LocalGlowSkin = staticCompositionLocalOf { GlowSkins.Dark }

/** تدرّج الأزرار الرئيسية: ماجنتا ← بنفسجي للون الهوية، وتدرّج بسيط للألوان المخصّصة. */
fun GlowSkin.accentBrush(): androidx.compose.ui.graphics.Brush =
    if (accent == Color(0xFFF042FF) || accent == Color(0xFFD62BEA)) GlowGradients.Accent
    else androidx.compose.ui.graphics.Brush.verticalGradient(
        listOf(androidx.compose.ui.graphics.lerp(accent, Color.White, 0.14f), accent),
    )
