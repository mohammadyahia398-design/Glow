package com.glow.app.ui.nowplaying

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** لون غالب في الغلاف. share: نسبة مساحته إلى مساحة أكبر لون (الأكبر = 1). */
@Immutable
data class DominantColor(val color: Color, val share: Float)

private class HueBucket {
    var count = 0
    var sx = 0.0
    var sy = 0.0
    var sat = 0.0
    var value = 0.0
}

private class Cluster(val hue: Float, val sat: Float, val value: Float, var count: Int)

private fun hueGap(a: Float, b: Float): Float {
    val d = abs(a - b)
    return min(d, 360f - d)
}

/**
 * يستخرج حتى [max] ألوان غالبة من الغلاف مرتبة بحسب المساحة.
 * يتجاهل الرمادي والأسود والأبيض الباهت. يعيد قائمة فارغة عند الفشل أو إن كان الغلاف بلا ألوان واضحة.
 */
fun Bitmap.dominantColors(max: Int = 4): List<DominantColor> = runCatching {
    val src = if (config == Bitmap.Config.HARDWARE) {
        copy(Bitmap.Config.ARGB_8888, false)
    } else {
        this
    } ?: return@runCatching emptyList<DominantColor>()
    val n = 48
    val small = Bitmap.createScaledBitmap(src, n, n, true)
    val hsv = FloatArray(3)
    val buckets = Array(18) { HueBucket() }
    var counted = 0
    for (y in 0 until n) {
        for (x in 0 until n) {
            val c = small.getPixel(x, y)
            android.graphics.Color.colorToHSV(c, hsv)
            if (hsv[1] < 0.22f || hsv[2] < 0.22f) continue
            val b = buckets[(hsv[0] / 20f).toInt().coerceIn(0, 17)]
            val rad = hsv[0] * PI / 180.0
            b.count++
            b.sx += cos(rad)
            b.sy += sin(rad)
            b.sat += hsv[1]
            b.value += hsv[2]
            counted++
        }
    }
    // الغلاف شبه رمادي: نترك الخلفية لألوان الهوية.
    if (counted < n * n * 5 / 100) return@runCatching emptyList<DominantColor>()

    val clusters = ArrayList<Cluster>()
    for (b in buckets.filter { it.count > 0 }.sortedByDescending { it.count }) {
        var hue = (atan2(b.sy, b.sx) * 180.0 / PI).toFloat()
        if (hue < 0f) hue += 360f
        val near = clusters.firstOrNull { hueGap(it.hue, hue) < 30f }
        if (near != null) {
            near.count += b.count
        } else if (clusters.size < max) {
            clusters += Cluster(hue, (b.sat / b.count).toFloat(), (b.value / b.count).toFloat(), b.count)
        }
    }
    val top = clusters.maxOf { it.count }.toFloat()
    clusters
        .sortedByDescending { it.count }
        .filter { it.count / top >= 0.15f }
        .map { c ->
            DominantColor(
                color = Color.hsv(c.hue, c.sat.coerceIn(0.35f, 0.95f), c.value.coerceIn(0.75f, 1f)),
                share = c.count / top,
            )
        }
}.getOrDefault(emptyList())
