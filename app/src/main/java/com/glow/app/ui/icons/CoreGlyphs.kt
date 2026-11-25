package com.glow.app.ui.icons

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * الأيقونات الأساسية لواجهة Glow على محرّك IconEngine (شبكة 24×24، خط 1.8، أطراف مدوّرة).
 * تحلّ محلّ الرسم القديم في GlowIcon فتتناسق كل الأيقونات في السماكة والاستدارة والحجم البصري.
 * كل أيقونة تتبع النمط الحالي (خطّي / ممتلئ / ثنائي النغمة).
 */
object CoreGlyphs {

    /** رجوع (سهم نحو اليسار). تنعكس في RTL. */
    val Back = glyph("back", directional = true) {
        stroke(path {
            moveTo(14.8f, 5.2f)
            lineTo(8f, 12f)
            lineTo(14.8f, 18.8f)
        })
    }

    /** سهم للأسفل (إغلاق المشغّل). */
    val Down = glyph("down") {
        stroke(path {
            moveTo(5.2f, 9.2f)
            lineTo(12f, 16f)
            lineTo(18.8f, 9.2f)
        })
    }

    /** سهم للأعلى. */
    val Up = glyph("up") {
        stroke(path {
            moveTo(5.2f, 14.8f)
            lineTo(12f, 8f)
            lineTo(18.8f, 14.8f)
        })
    }

    /** إغلاق / حذف (×). */
    val Close = glyph("close") {
        line(6.5f, 6.5f, 17.5f, 17.5f)
        line(17.5f, 6.5f, 6.5f, 17.5f)
    }

    /** سهم صغير نحو الأمام (عنصر قابل للفتح). تنعكس في RTL. */
    val Chevron = glyph("chevron", directional = true) {
        stroke(path {
            moveTo(9.2f, 5.2f)
            lineTo(16f, 12f)
            lineTo(9.2f, 18.8f)
        })
    }

    /** علامة صح. */
    val Check = glyph("check") {
        stroke(path {
            moveTo(5f, 12.8f)
            lineTo(9.8f, 17.4f)
            lineTo(19f, 7.2f)
        })
    }

    /** بحث: عدسة ومقبض. */
    val Search = glyph("search") {
        ring(10.6f, 10.6f, 6.3f)
        line(15.3f, 15.3f, 20f, 20f)
    }

    /** المزيد: ثلاث نقاط عمودية. */
    val More = glyph("more") {
        dot(12f, 5.6f, 1.55f)
        dot(12f, 12f, 1.55f)
        dot(12f, 18.4f, 1.55f)
    }

    /** قائمة: ثلاثة أشرطة مدوّرة (نسب المرجع القديم). */
    val Menu = glyph("menu") {
        bar(7f, 7.1f, 10f, 1.9f, 0.95f)
        bar(7f, 11.05f, 10f, 1.9f, 0.95f)
        bar(7f, 15f, 10f, 1.9f, 0.95f)
    }

    /** الإعدادات: ترس بثمانية أسنان وفتحة في الوسط. */
    val Settings = glyph("settings") {
        val pts = ArrayList<Offset>(32)
        val tip = 9.6f
        val base = 7.6f
        for (i in 0 until 8) {
            val a = i * 45.0
            for ((deg, r) in listOf(-17.0 to base, -9.0 to tip, 9.0 to tip, 17.0 to base)) {
                val rad = (a + deg) * PI / 180.0
                pts.add(Offset(12f + r * cos(rad).toFloat(), 12f + r * sin(rad).toFloat()))
            }
        }
        shape(path { polygon(pts, 1.1f) })
        ring(12f, 12f, 3.1f)
    }

    /** فنان: رأس وكتفان. */
    val Artist = glyph("artist") {
        shape(path { circle(12f, 8f, 3.7f) })
        shape(path {
            moveTo(4.8f, 20f)
            cubicTo(4.8f, 15.8f, 8f, 13.8f, 12f, 13.8f)
            cubicTo(16f, 13.8f, 19.2f, 15.8f, 19.2f, 20f)
            close()
        })
    }

    /** قلب (المفضلة). */
    val Heart = glyph("heart_core") {
        shape(path {
            moveTo(12f, 20f)
            cubicTo(6f, 15.8f, 3.2f, 12.4f, 3.2f, 8.8f)
            cubicTo(3.2f, 6.2f, 5.2f, 4.2f, 7.7f, 4.2f)
            cubicTo(9.5f, 4.2f, 11.1f, 5.2f, 12f, 6.8f)
            cubicTo(12.9f, 5.2f, 14.5f, 4.2f, 16.3f, 4.2f)
            cubicTo(18.8f, 4.2f, 20.8f, 6.2f, 20.8f, 8.8f)
            cubicTo(20.8f, 12.4f, 18f, 15.8f, 12f, 20f)
            close()
        })
    }

    /** قائمة تشغيل: ثلاثة أسطر ونوتة. */
    val Playlist = glyph("playlist") {
        line(4f, 6.5f, 15f, 6.5f)
        line(4f, 11.5f, 15f, 11.5f)
        line(4f, 16.5f, 10f, 16.5f, tone = Tone.Soft)
        dot(16.4f, 17.4f, 2.1f)
        line(18.5f, 17.4f, 18.5f, 9.2f)
        line(18.5f, 9.2f, 21.4f, 10.4f)
    }

    /** السرعة: عدّاد نصف دائري وإبرة. */
    val Speed = glyph("speed") {
        stroke(path { arc(12f, 14f, 8.2f, 180f, 180f) })
        line(12f, 14f, 16.2f, 9.2f)
        dot(12f, 14f, 1.4f)
    }

    /** مؤقّت النوم: هلال. */
    val Moon = glyph("moon") {
        shape(path {
            arc(12f, 12f, 9f, 5f, 260f, moveTo = true)
            arc(16.84f, 7.16f, 7f, 216.5f, -163f, moveTo = false)
            close()
        })
    }

    /** مؤقّت: ساعة بزر علوي وعقرب. */
    val Timer = glyph("timer") {
        ring(12f, 13.4f, 7.4f)
        line(9.6f, 3.6f, 14.4f, 3.6f)
        line(12f, 13.4f, 12f, 9.6f)
    }

    /** تحديث: قوس دائري برأس سهم. */
    val Refresh = glyph("refresh") {
        val r = 7.6f
        val endDeg = 210.0
        stroke(path { arc(12f, 12f, r, 300f, 270f) })
        val rad = endDeg * PI / 180.0
        val ex = 12f + r * cos(rad).toFloat()
        val ey = 12f + r * sin(rad).toFloat()
        // اتجاه المماس (مع عقارب الساعة) والعمودي عليه.
        val tx = (-sin(rad)).toFloat()
        val ty = cos(rad).toFloat()
        val nx = ty * -1f
        val ny = tx
        val tipX = ex + tx * 1.0f
        val tipY = ey + ty * 1.0f
        stroke(path {
            moveTo(tipX - tx * 3.2f + nx * 2.8f, tipY - ty * 3.2f + ny * 2.8f)
            lineTo(tipX, tipY)
            lineTo(tipX - tx * 3.2f - nx * 2.8f, tipY - ty * 3.2f - ny * 2.8f)
        })
    }

    /** موازن صوتي ثابت: أربعة أعمدة بارتفاعات مختلفة. */
    val Equalizer = glyph("equalizer_static") {
        val xs = floatArrayOf(5.5f, 10f, 14.5f, 19f)
        val hs = floatArrayOf(8f, 15f, 11f, 5.5f)
        for (i in 0..3) {
            bar(
                xs[i] - 1.5f, 19f - hs[i], 3f, hs[i], 1.5f,
                tone = if (i % 2 == 1) Tone.Soft else Tone.Strong,
            )
        }
    }
}
