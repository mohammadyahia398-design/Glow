package com.glow.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.glow.app.settings.BgChoice

/**
 * لوحة ألوان قابلة للاختيار.
 *
 * @property blobs أربعة ألوان تسبح داخل البطاقة السائلة.
 * @property accent لون التمييز (الأزرار والتحديد).
 * @property bg الخلفية الافتراضية للوحة.
 */
@Immutable
data class Colorway(
    val id: String,
    val name: String,
    val blobs: List<Color>,
    val accent: Color,
    val bg: Color,
)

object Colorways {
    val Glow = Colorway(
        id = "glow",
        name = "وهج",
        blobs = listOf(Color(0xFFF042FF), Color(0xFF7226FF), Color(0xFF87F5F5), Color(0xFF160078)),
        accent = Color(0xFFF042FF),
        bg = GlowPalette.Void,
    )
    val Dusk = Colorway(
        id = "dusk",
        name = "شفق",
        blobs = listOf(Color(0xFFFF5A57), Color(0xFFE02F75), Color(0xFF6700A3), Color(0xFF1B2062)),
        accent = Color(0xFFFF5A57),
        bg = Color(0xFF050C38),
    )
    val Neon = Colorway(
        id = "neon",
        name = "نيون",
        blobs = listOf(Color(0xFFF042FF), Color(0xFF7226FF), Color(0xFF87F5F5), Color(0xFF160078)),
        accent = Color(0xFFF042FF),
        bg = Color(0xFF010030),
    )
    val Amber = Colorway(
        id = "amber",
        name = "عنبر",
        blobs = listOf(Color(0xFFF09410), Color(0xFFBC430D), Color(0xFFF0D0C7), Color(0xFF241705)),
        accent = Color(0xFFF09410),
        bg = Color(0xFF1A0F05),
    )
    val Emerald = Colorway(
        id = "emerald",
        name = "زمرّد",
        blobs = listOf(Color(0xFF36DFBD), Color(0xFF4FE566), Color(0xFF39887F), Color(0xFF354C4A)),
        accent = Color(0xFF36DFBD),
        bg = Color(0xFF0C1B1A),
    )
    val Ruby = Colorway(
        id = "ruby",
        name = "ياقوت",
        blobs = listOf(Color(0xFFF10222), Color(0xFF892325), Color(0xFFFFCAD0), Color(0xFF250202)),
        accent = Color(0xFFF10222),
        bg = Color(0xFF1A0101),
    )
    val Rose = Colorway(
        id = "rose",
        name = "ورد",
        blobs = listOf(Color(0xFFFF94B2), Color(0xFFF24455), Color(0xFFE5203A), Color(0xFF660F24)),
        accent = Color(0xFFF24455),
        bg = Color(0xFF2B0013),
    )

    /** اللوحات الجاهزة بترتيب العرض (بطاقة «مخصّص» تُضاف بعدها). */
    val Presets: List<Colorway> = listOf(Glow, Dusk, Neon, Amber, Emerald, Ruby, Rose)

    const val CUSTOM_ID = "custom"

    fun byId(id: String): Colorway = Presets.firstOrNull { it.id == id } ?: Glow

    /** لوحة مخصّصة: ألوانها الأربعة مشتقة من لون التمييز بتدوير الصبغة. */
    fun custom(accent: Color, bg: Color): Colorway = Colorway(
        id = CUSTOM_ID,
        name = "مخصّص",
        blobs = listOf(
            accent,
            hueShift(accent, 38f, 0.95f, 1.00f),
            hueShift(accent, -42f, 0.90f, 0.70f),
            hueShift(accent, 12f, 0.80f, 0.32f),
        ),
        accent = accent,
        bg = bg,
    )
}

/** يدوّر صبغة [c] بمقدار [deg] درجة، ويضرب التشبّع والسطوع بالمعاملين المعطيين. */
private fun hueShift(c: Color, deg: Float, satMul: Float, valMul: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(c.toArgb(), hsv)
    val h = ((hsv[0] + deg) % 360f + 360f) % 360f
    return hsvColor(h, (hsv[1] * satMul).coerceIn(0f, 1f), (hsv[2] * valMul).coerceIn(0f, 1f))
}

/** لون من صبغة (0..360) وتشبّع وسطوع (0..1). */
fun hsvColor(h: Float, s: Float, v: Float): Color =
    Color(android.graphics.Color.HSVToColor(floatArrayOf(h.coerceIn(0f, 360f), s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))))

/** يفكّك لوناً إلى صبغة وتشبّع وسطوع. */
fun colorToHsv(c: Color): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(c.toArgb(), hsv)
    return hsv
}

/**
 * يبني سمة كاملة من لون تمييز ولون خلفية. تُضبط اللمعة والنصوص والأسطح تلقائياً
 * حسب سطوع الخلفية، فيبقى الزجاج ظاهراً على أي لون.
 */
fun skinFor(accent: Color, bg: Color): GlowSkin {
    val dark = bg.luminance() < 0.5f
    val onAccent = if (accent.luminance() > 0.45f) Color(0xFF0B0B10) else Color.White
    return if (dark) {
        val text = lerp(Color.White, accent, 0.05f)
        val bottom = lerp(bg, accent, 0.12f)
        val accentText = lerp(accent, Color.White, 0.35f)
        GlowSkin(
            isDark = true,
            bgTop = bg,
            bgBottom = bottom,
            text = text,
            textSecondary = lerp(text, bg, 0.45f),
            accent = accent,
            onAccent = onAccent,
            accentText = accentText,
            panel = lerp(bg, Color.White, 0.06f),
            chip = lerp(bg, Color.White, 0.12f),
            divider = text.copy(alpha = 0.07f),
            sheet = lerp(bg, Color.White, 0.09f),
            glassTint = Color.White,
            glassBase = bottom,
            glassBorder = Color.White,
            containerColor = lerp(bg, accent, 0.25f),
            onContainerColor = accentText,
            outlineVariantColor = lerp(bg, accent, 0.30f),
        )
    } else {
        val deep = lerp(accent, Color.Black, 0.65f)
        val text = lerp(Color(0xFF0B0B10), accent, 0.10f)
        GlowSkin(
            isDark = false,
            bgTop = bg,
            bgBottom = lerp(bg, accent, 0.10f),
            text = text,
            textSecondary = text.copy(alpha = 0.65f),
            accent = accent,
            onAccent = onAccent,
            accentText = lerp(accent, Color.Black, 0.40f),
            panel = lerp(bg, accent, 0.07f),
            chip = deep.copy(alpha = 0.08f),
            divider = deep.copy(alpha = 0.10f),
            sheet = Color.White,
            glassTint = Color.White,
            glassBase = bg,
            glassBorder = deep,
            containerColor = lerp(bg, accent, 0.30f),
            onContainerColor = Color(0xFF0B0B10),
            outlineVariantColor = lerp(bg, accent, 0.40f),
        )
    }
}

/**
 * يختار السمة النهائية. اللوحة الافتراضية مع خلفيتها تعطي السمتين الأصليتين
 * (داكن/فاتح حسب [darkTheme])، وأي اختيار آخر يولّد سمة جديدة.
 */
fun resolveSkin(
    paletteId: String,
    bgChoice: BgChoice,
    customAccent: Color,
    customBg: Color,
    darkTheme: Boolean,
): GlowSkin {
    if (paletteId == Colorways.Glow.id && bgChoice == BgChoice.PALETTE) {
        return if (darkTheme) GlowSkins.Dark else GlowSkins.Light
    }
    val way = if (paletteId == Colorways.CUSTOM_ID) {
        Colorways.custom(customAccent, customBg)
    } else {
        Colorways.byId(paletteId)
    }
    val bg = when (bgChoice) {
        BgChoice.PALETTE -> way.bg
        BgChoice.BLACK -> Color.Black
        BgChoice.WHITE -> Color.White
        BgChoice.CUSTOM -> customBg
    }
    return skinFor(way.accent, bg)
}
