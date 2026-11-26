package com.glow.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * الألوان الخام لهوية Glow.
 *
 * قاعدة الدفعة 1: الأسود والأبيض المحايد يسيطران، وستة ألوان فقط من اللوحة الرسمية مسموحة:
 * F042FF (تمييز) · 7226FF (بنفسجي) · 87F5F5 (لمسة نادرة) · E5E8F5 (نص) · FFE5F1 (لمسة فاتحة) · 010030 (حبر الوضع الفاتح).
 * باقي اللوحة الرسمية محفوظ في الوثيقة ولا يُستعمل في الواجهة حتى لا يشتت البصر.
 */
object GlowPalette {
    // Canonical Luminous Minimalism palette (from the project handoff).
    val Void = Color(0xFF000002)
    val Night = Color(0xFF04040A)
    val Surface = Color(0xFF0B0B14)
    val SurfaceLifted = Color(0xFF151522)
    val Amber = Color(0xFFF042FF)
    val Coral = Color(0xFFB07CFF)
    val Twilight = Color(0xFF7226FF)
    val Mint = Color(0xFF87F5F5)
    val Ivory = Color(0xFFE5E8F5)
    val Muted = Color(0xFFA3A7B8)

    // Backward-compatible aliases used by existing screens and effects.
    val Charcoal = Void
    val TwilightBlue = Twilight
    val Teal = Mint
    val TwilightSoft = Color(0xFFC9B2FF)
    val Blush = Color(0xFFFFE5F1)
    val Navy = Color(0xFF010030)
    val OutlineSolid = Color(0xFF26263A)

    // Shared glass tokens. Strong optical effects are implemented in the glass batch.
    val Hairline = Color(0x26E5E8F5)
    val GlassFill = Color(0xA60B0B14)
    val GlassHighlight = Color(0x40E5E8F5)
    val GlassEdge = Color(0x66E5E8F5)
    val GlassShadow = Color(0x66000000)
}


/** ألوان الحالات المزاجية (Mood) للبطاقات والتدرجات. */
object GlowMoodColors {
    val Sleep = GlowPalette.Amber
    val Focus = GlowPalette.TwilightBlue
    val Reflect = GlowPalette.Coral
    val Nature = GlowPalette.Teal
}

/** تدرجات جاهزة. */
object GlowGradients {
    /** تدرّج التمييز الرسمي: ماجنتا ← بنفسجي (الأزرار الرئيسية والخيار المحدد). */
    val Accent: Brush = Brush.linearGradient(listOf(Color(0xFFF042FF), Color(0xFF7226FF)))

    /** خلفية التطبيق الأساسية. */
    val Backdrop: Brush = Brush.verticalGradient(
        listOf(GlowPalette.Charcoal, GlowPalette.Night)
    )

    /** ألوان الشفق للتوهج الشبكي (Aurō Mesh Glow). */
    val Aurora: List<Color> = listOf(
        GlowPalette.Amber,
        GlowPalette.Coral,
        GlowPalette.TwilightBlue,
    )

    /** توهج شعاعي ناعم يتلاشى إلى الشفافية. */
    fun halo(color: Color, alpha: Float = 0.35f, radius: Float = Float.POSITIVE_INFINITY): Brush =
        Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
            radius = radius,
        )
}

/** رموز لونية إضافية خارج Material ColorScheme، متاحة عبر GlowTheme.colors. */
@Immutable
data class GlowColors(
    val amberGlow: Color = GlowPalette.Amber,
    val coralGlow: Color = GlowPalette.Coral,
    val twilightGlow: Color = GlowPalette.TwilightBlue,
    val tealGlow: Color = GlowPalette.Teal,
    val hairline: Color = GlowPalette.Hairline,
    val glassFill: Color = GlowPalette.GlassFill,
    val glassHighlight: Color = GlowPalette.GlassHighlight,
    val textPrimary: Color = GlowPalette.Ivory,
    val textSecondary: Color = GlowPalette.Muted,
)

internal val LocalGlowColors = staticCompositionLocalOf { GlowColors() }
