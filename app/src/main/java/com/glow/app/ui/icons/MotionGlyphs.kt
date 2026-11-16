package com.glow.app.ui.icons

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import com.glow.app.ui.glass.core.LocalGlassConfig
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * الأيقونات المتحركة (الملف 4 من 4) + السجل الموحّد.
 *
 * كل أيقونة هنا تستقبل قيمة التقدّم t:
 *   - أيقونات التبديل (PlayPause, Heart, Star, SoundWaves): t من 0 إلى 1، استعمل ToggleGlyph.
 *   - أيقونات الحلقة (SoundPulse, Equalizer, Disc): t يدور من 0 إلى 1 باستمرار، استعمل LoopingGlyph.
 *   - Info ثابتة.
 *
 * الاستعمال:
 *     ToggleGlyph(MotionGlyphs.PlayPause, on = isPlaying, tint = color, size = 32.dp)
 *     ToggleGlyph(MotionGlyphs.Heart, on = liked, tint = color)
 *     LoopingGlyph(MotionGlyphs.Equalizer, active = isPlaying, tint = color)
 */

private val TwoPi = (2.0 * PI).toFloat()
private val PiF = PI.toFloat()

object MotionGlyphs {

    /** تحوّل تشغيل ⇄ إيقاف مؤقت. t=0 تشغيل، t=1 إيقاف مؤقت. */
    val PlayPause = glyph("playpause") { t ->
        val playA = 1f - window(t, 0f, 0.6f)
        if (playA > 0f) {
            val s = mix(1f, 0.55f, t.coerceIn(0f, 1f))
            val rot = mix(0f, -35f, t.coerceIn(0f, 1f))
            scaled(s, s, 12f, 12f) {
                rotated(rot, 12f, 12f) {
                    mFigure(path { polygon(PlayTriangle, 2f) }, playA)
                }
            }
        }
        val pauseA = window(t, 0.3f, 1f)
        if (pauseA > 0f) {
            val s = mix(0.55f, 1f, t.coerceAtLeast(0f))
            scaled(s, s, 12f, 12f) {
                mFigure(path { rrect(5.8f, 5f, 4.2f, 14f, 1.5f) }, pauseA)
                mFigure(path { rrect(14f, 5f, 4.2f, 14f, 1.5f) }, pauseA)
            }
        }
    }

    /** قلب إعجاب. t=0 فارغ، t=1 ممتلئ مع نبضة وشرارات. */
    val Heart = glyph("heart") { t ->
        val pop = 1f + 0.22f * sin(PiF * window(t, 0f, 0.75f))
        val fillAmt = window(t, 0.1f, 0.45f)
        scaled(pop, pop, 12f, 12.4f) {
            val p = path { heart() }
            mOutlined(p)
            if (fillAmt > 0f) fill(p, alpha = fillAmt)
        }
        mBurst(12f, 11f, t)
    }

    /** نجمة تقييم. t=0 فارغة، t=1 ممتلئة مع دوران خفيف وشرارات. */
    val Star = glyph("star") { t ->
        val k = window(t, 0f, 0.75f)
        val pop = 1f + 0.2f * sin(PiF * k)
        val rot = -18f * sin(PiF * k)
        val fillAmt = window(t, 0.1f, 0.45f)
        scaled(pop, pop, 12f, 12.7f) {
            rotated(rot, 12f, 12.7f) {
                val p = path { polygon(StarPoints, 1.3f) }
                mOutlined(p)
                if (fillAmt > 0f) fill(p, alpha = fillAmt)
            }
        }
        mBurst(12f, 12.7f, t)
    }

    /** مكبّر صوت يتدرّج بحسب المستوى. t=0 كتم، ثم موجة، ثم موجتان. */
    val SoundWaves = glyph("soundwaves") { t ->
        mSpeaker()
        val xA = 1f - window(t, 0f, 0.18f)
        if (xA > 0f) {
            line(16.2f, 9.6f, 21f, 14.4f, alpha = xA)
            line(21f, 9.6f, 16.2f, 14.4f, alpha = xA)
        }
        val w1 = window(t, 0.1f, 0.4f)
        if (w1 > 0f) {
            val s = mix(0.8f, 1f, w1)
            scaled(s, s, 12.4f, 12f) {
                stroke(path { arc(12.4f, 12f, 4.6f, -45f, 90f) }, alpha = w1)
            }
        }
        val w2 = window(t, 0.5f, 0.85f)
        if (w2 > 0f) {
            val s = mix(0.8f, 1f, w2)
            scaled(s, s, 12.4f, 12f) {
                stroke(path { arc(12.4f, 12f, 8.2f, -45f, 90f) }, tone = Tone.Soft, alpha = w2)
            }
        }
    }

    /** مكبّر صوت بأمواج تنتشر باستمرار (حلقة). */
    val SoundPulse = glyph("soundpulse") { t ->
        mSpeaker()
        for (i in 0..1) {
            val ph = (t + i * 0.5f) % 1f
            val r = mix(3.2f, 9.2f, ph)
            val a = sin(PiF * ph)
            if (a > 0.02f) {
                stroke(
                    path { arc(12.4f, 12f, r, -45f, 90f) },
                    tone = if (i == 0) Tone.Strong else Tone.Soft,
                    alpha = a,
                )
            }
        }
    }

    /** موازن بأربعة أعمدة ترقص (حلقة). عند t=0 تظهر بارتفاعات متفاوتة. */
    val Equalizer = glyph("equalizer") { t ->
        for (i in 0..3) {
            val w = 0.5f + 0.5f * sin(TwoPi * (t * EqSpeeds[i] + EqPhases[i]))
            val h = mix(4f, 15f, w)
            bar(
                EqXs[i] - 1.5f, 19f - h, 3f, h, 1.5f,
                tone = if (i % 2 == 1) Tone.Soft else Tone.Strong,
            )
        }
    }

    /** قرص يدور (حلقة). t=1 دورة كاملة. */
    val Disc = glyph("disc") { t ->
        rotated(360f * t, 12f, 12f) {
            mFigure(path { circle(12f, 12f, 9.2f) })
            stroke(path { circle(12f, 12f, 5.8f) }, tone = Tone.Soft, alpha = 0.7f, widthUnits = 1f)
            stroke(path { arc(12f, 12f, 7.6f, -75f, 55f) }, widthUnits = 1.5f)
            stroke(path { arc(12f, 12f, 7.6f, 105f, 55f) }, tone = Tone.Soft, widthUnits = 1.5f)
            mFigure(path { circle(12f, 12f, 2.3f) })
        }
    }

    /** معلومات: دائرة وحرف i. */
    val Info = glyph("info") {
        mFigure(path { circle(12f, 12f, 9.2f) })
        dot(12f, 7.9f, 1.15f)
        line(12f, 11.2f, 12f, 16.4f)
    }

    /** كل الأيقونات المتحركة، للسجل. */
    val All: List<Glyph> = listOf(PlayPause, Heart, Star, SoundWaves, SoundPulse, Equalizer, Disc, Info)
}

/* ───────────── أدوات الاستعمال ───────────── */

/** أيقونة تبديل: تتحرك بين الحالتين (0 ⇄ 1) بنبض نابض، وتصبح فورية مع «تقليل الحركة». */
@Composable
fun ToggleGlyph(
    glyph: Glyph,
    on: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = GlyphDefaults.Size,
    style: IconStyle = LocalIconStyle.current,
    secondaryTint: Color = Color.Unspecified,
    contentDescription: String? = null,
) {
    val p = animateGlyphProgress(if (on) 1f else 0f, label = "toggle_" + glyph.name)
    GlowGlyph(
        glyph = glyph,
        modifier = modifier,
        tint = tint,
        size = size,
        style = style,
        secondaryTint = secondaryTint,
        progress = { p.value },
        contentDescription = contentDescription,
    )
}

/** قيمة تدور من 0 إلى 1 باستمرار ما دام [active]، وتثبت عند 0 إن لم يكن أو عند تقليل الحركة. */
@Composable
fun rememberGlyphLoop(active: Boolean, durationMillis: Int = 1600): State<Float> {
    val reduceMotion = LocalGlassConfig.current.reduceMotion
    val running = active && !reduceMotion
    if (!running) return remember { mutableStateOf(0f) }
    val transition = rememberInfiniteTransition(label = "glyphLoop")
    val raw = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "glyphLoopValue",
    )
    return remember { derivedStateOf { raw.value } }
}

/** أيقونة حلقة (موازن، أمواج، قرص): تتحرك ما دام [active]. */
@Composable
fun LoopingGlyph(
    glyph: Glyph,
    active: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = GlyphDefaults.Size,
    style: IconStyle = LocalIconStyle.current,
    secondaryTint: Color = Color.Unspecified,
    durationMillis: Int = 1600,
    contentDescription: String? = null,
) {
    val p = rememberGlyphLoop(active, durationMillis)
    GlowGlyph(
        glyph = glyph,
        modifier = modifier,
        tint = tint,
        size = size,
        style = style,
        secondaryTint = secondaryTint,
        progress = { p.value },
        contentDescription = contentDescription,
    )
}

/* ───────────── السجل الموحّد ───────────── */

/** كل أيقونات Glow في مكان واحد، للبحث بالاسم ولمعرض الأيقونات. */
object GlyphRegistry {

    val Music: List<Glyph> = listOf(
        MusicGlyphs.AlbumFrame, MusicGlyphs.RewindTwin, MusicGlyphs.AlbumDisc,
        MusicGlyphs.Headphones, MusicGlyphs.AlbumBars, MusicGlyphs.ForwardTwin,
        MusicGlyphs.NoteRing, MusicGlyphs.HeadphonesWave, MusicGlyphs.Mic,
        MusicGlyphs.NoteDouble, MusicGlyphs.MicAlert, MusicGlyphs.MicMuted,
        MusicGlyphs.Note, MusicGlyphs.NoteAdd, MusicGlyphs.MicOff, MusicGlyphs.SearchList,
    )

    val All: List<Glyph> = TransportGlyphs.All + Music + MotionGlyphs.All

    private val index: Map<String, Glyph> = All.associateBy { it.name }

    fun find(name: String): Glyph? = index[name]
}

/* ───────────── أدوات داخل الملف ───────────── */

private val PlayTriangle = listOf(Offset(7.6f, 5f), Offset(7.6f, 19f), Offset(19f, 12f))

private val StarPoints: List<Offset> = List(10) { i ->
    val ang = (-PI / 2.0 + i * PI / 5.0).toFloat()
    val rad = if (i % 2 == 0) 9.6f else 4.3f
    Offset(12f + cos(ang) * rad, 12.7f + sin(ang) * rad)
}

private val EqXs = floatArrayOf(5.5f, 10f, 14.5f, 19f)
private val EqPhases = floatArrayOf(0f, 0.37f, 0.71f, 0.18f)
private val EqSpeeds = floatArrayOf(1f, 2f, 1f, 3f)

/** جسم مغلق حسب النمط: خط، تعبئة، أو تعبئة خافتة مع حدّ قوي. */
private fun GlyphCanvas.mFigure(p: Path, alpha: Float = 1f) {
    when (spec.style) {
        IconStyle.Line -> stroke(p, alpha = alpha)
        IconStyle.Solid -> fill(p, alpha = alpha)
        IconStyle.Duotone -> {
            fill(p, Tone.Soft, alpha)
            stroke(p, alpha = alpha)
        }
    }
}

/** حدّ فارغ في كل الأنماط (يبقى الفرق واضحاً بين الحالتين)، مع تعبئة خافتة في Duotone. */
private fun GlyphCanvas.mOutlined(p: Path) {
    if (spec.style == IconStyle.Duotone) fill(p, Tone.Soft)
    stroke(p)
}

/** شرارات تنتشر حول مركز وتتلاشى. */
private fun GlyphCanvas.mBurst(cx: Float, cy: Float, t: Float) {
    val k = window(t, 0.25f, 0.95f)
    if (k <= 0f || k >= 1f) return
    val a = sin(PiF * k)
    val r = mix(8.6f, 10.8f, k)
    for (i in 0 until 6) {
        val ang = i * TwoPi / 6f - PiF / 2f
        dot(cx + cos(ang) * r, cy + sin(ang) * r, 0.7f, alpha = a)
    }
}

/** جسم مكبّر الصوت دون موجات. */
private fun GlyphCanvas.mSpeaker() {
    mFigure(
        path {
            polygon(
                listOf(
                    Offset(4f, 9.6f),
                    Offset(7.6f, 9.6f),
                    Offset(12.4f, 5.6f),
                    Offset(12.4f, 18.4f),
                    Offset(7.6f, 14.4f),
                    Offset(4f, 14.4f),
                ),
                1.2f,
            )
        },
    )
}

/** مسار القلب على الشبكة 24×24. */
private fun UnitPath.heart() {
    moveTo(12f, 20.2f)
    cubicTo(5.2f, 15.2f, 2.6f, 11.8f, 2.6f, 8.4f)
    cubicTo(2.6f, 5.6f, 4.7f, 3.6f, 7.3f, 3.6f)
    cubicTo(9.2f, 3.6f, 11f, 4.6f, 12f, 6.3f)
    cubicTo(13f, 4.6f, 14.8f, 3.6f, 16.7f, 3.6f)
    cubicTo(19.3f, 3.6f, 21.4f, 5.6f, 21.4f, 8.4f)
    cubicTo(21.4f, 11.8f, 18.8f, 15.2f, 12f, 20.2f)
    close()
}
