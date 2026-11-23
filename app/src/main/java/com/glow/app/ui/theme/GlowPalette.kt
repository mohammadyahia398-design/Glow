package com.glow.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * الألوان الخام لهوية Glow — Luminous Minimalism.
 *
 * الألوان الستة الأولى مأخوذة حرفياً من المرجع البصري،
 * أما المشتقة (Night, Surface, ...) فهي درجات مدروسة من Charcoal لبناء عمق الواجهة.
 */
object GlowPalette {
    // Canonical Luminous Minimalism palette (from the project handoff).
    val Void = Color(0xFF090B10)
    val Night = Color(0xFF0D111A)
    val Surface = Color(0xFF151B26)
    val SurfaceLifted = Color(0xFF1C2533)
    val Amber = Color(0xFFFFA64D)
    val Coral = Color(0xFFFF7168)
    val Twilight = Color(0xFF687DEB)
    val Mint = Color(0xFF82D5BE)
    val Ivory = Color(0xFFF6F1E9)
    val Muted = Color(0xFF9299A8)

    // Backward-compatible aliases used by existing screens and effects.
    val Charcoal = Void
    val TwilightBlue = Twilight
    val Teal = Mint
    val TwilightSoft = Color(0xFFAAB5FF)
    val OutlineSolid = Color(0xFF303949)

    // Shared glass tokens. Strong optical effects are implemented in the glass batch.
    val Hairline = Color(0x26F6F1E9)
    val GlassFill = Color(0xA6151B26)
    val GlassHighlight = Color(0x40F6F1E9)
    val GlassEdge = Color(0x66F6F1E9)
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
