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
    val containerColor: Color = Color(0xFF1C2533),
    val onContainerColor: Color = Color(0xFFF6F1E9),
    val outlineVariantColor: Color = Color(0xFF303949),
)

object GlowSkins {
    private val Void = Color(0xFF090B10)
    private val Night = Color(0xFF0D111A)
    private val Surface = Color(0xFF151B26)
    private val Lifted = Color(0xFF1C2533)
    private val Amber = Color(0xFFFFA64D)
    private val Ivory = Color(0xFFF6F1E9)
    private val Muted = Color(0xFF9299A8)
    private val LightInk = Color(0xFF202735)

    val Dark = GlowSkin(
        isDark = true,
        bgTop = Void,
        bgBottom = Night,
        text = Ivory,
        textSecondary = Muted,
        accent = Amber,
        onAccent = Void,
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
        outlineVariantColor = Color(0xFF303949),
    )

    val Light = GlowSkin(
        isDark = false,
        bgTop = Color(0xFFF5F2ED),
        bgBottom = Color(0xFFECE8E1),
        text = LightInk,
        textSecondary = Color(0xFF687080),
        accent = Color(0xFFB96722),
        onAccent = Color.White,
        accentText = Color(0xFF9A5019),
        panel = Color(0xFFFDFBF7),
        chip = Color(0xFFE8E3DB),
        divider = LightInk.copy(alpha = 0.10f),
        sheet = Color(0xFFFCFAF6),
        glassTint = Color.White,
        glassBase = Color(0xFFF5F2ED),
        glassBorder = LightInk.copy(alpha = 0.18f),
        containerColor = Color(0xFFF1D7C0),
        onContainerColor = LightInk,
        outlineVariantColor = Color(0xFFD6CEC3),
    )
}

val LocalGlowSkin = staticCompositionLocalOf { GlowSkins.Dark }
