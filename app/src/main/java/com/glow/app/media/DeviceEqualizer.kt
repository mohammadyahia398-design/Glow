package com.glow.app.media

import android.media.audiofx.AudioEffect
import android.media.audiofx.Equalizer
import com.glow.app.GlowStrings
import com.glow.app.R

/** لقطة قابلة للعرض من حالة المعادل الحالية. */
data class EqualizerSnapshot(
    val enabled: Boolean,
    val presetIndex: Int?,
    val levelsMillibels: List<Int>,
)

/** يحتفظ بتأثير المعادل طوال عمر جلسة المشغّل، لا طوال عمر نافذة الإعدادات. */
private object EqualizerEffectPool {
    private val effects = mutableMapOf<Int, Equalizer>()

    @Synchronized
    fun obtain(sessionId: Int): Equalizer? {
        if (sessionId <= 0) return null
        effects[sessionId]?.let { return it }
        val created = runCatching { Equalizer(0, sessionId) }.getOrNull() ?: return null
        effects[sessionId] = created
        return created
    }

    @Synchronized
    fun release(sessionId: Int) {
        effects.remove(sessionId)?.let { runCatching { it.release() } }
    }
}

/**
 * واجهة آمنة فوق Android Equalizer المرتبط بجلسة صوت واحدة.
 * لا تنشئ مؤثراً للجلسة صفر، وتتعامل مع الأجهزة التي لا توفر Equalizer
 * أو تمنع إنشاءه دون إسقاط التطبيق.
 */
class DeviceEqualizer(audioSessionId: Int) {

    private val effect: Equalizer? = EqualizerEffectPool.obtain(audioSessionId)

    val available: Boolean get() = effect != null

    val bandCount: Int
        get() = runCatching { effect?.numberOfBands?.toInt() ?: 0 }.getOrDefault(0)

    val levelRange: IntRange
        get() = runCatching {
            val range = effect?.bandLevelRange ?: return@runCatching -1500..1500
            range[0].toInt()..range[1].toInt()
        }.getOrDefault(-1500..1500)

    val presetNames: List<String>
        get() = runCatching {
            val instance = effect ?: return@runCatching emptyList()
            val count = instance.numberOfPresets.toInt()
            buildList {
                add(GlowStrings.get(R.string.eq_custom))
                for (index in 0 until count) {
                    add(instance.getPresetName(index.toShort()).orEmpty())
                }
            }
        }.getOrDefault(emptyList())

    val enabled: Boolean
        get() = runCatching { effect?.enabled == true }.getOrDefault(false)

    val currentPresetIndex: Int?
        get() = runCatching { effect?.currentPreset?.toInt() }.getOrNull()

    /** حدود النطاق بالهرتز؛ قد لا يعيد المحرك نطاقاً صالحاً لبعض الفرق. */
    fun bandFrequencyRangeHz(band: Int): IntRange? = runCatching {
        val instance = effect ?: return@runCatching null
        if (band !in 0 until instance.numberOfBands.toInt()) return@runCatching null
        val limits = instance.getBandFreqRange(band.toShort())
        if (limits.size < 2) return@runCatching null
        (limits[0] / 1000).coerceAtLeast(0)..(limits[1] / 1000).coerceAtLeast(0)
    }.getOrNull()

    fun setEnabled(value: Boolean): Boolean = runCatching {
        val instance = effect ?: return@runCatching false
        instance.setEnabled(value) == AudioEffect.SUCCESS
    }.getOrDefault(false)

    fun centerFrequencyHz(band: Int): Int? = runCatching {
        val instance = effect ?: return@runCatching null
        if (band !in 0 until instance.numberOfBands.toInt()) return@runCatching null
        instance.getCenterFreq(band.toShort()).coerceAtLeast(0) / 1000
    }.getOrNull()

    fun bandLevel(band: Int): Int? = runCatching {
        val instance = effect ?: return@runCatching null
        if (band !in 0 until instance.numberOfBands.toInt()) return@runCatching null
        instance.getBandLevel(band.toShort()).toInt()
    }.getOrNull()

    fun setBandLevel(band: Int, levelMillibels: Int): Boolean = runCatching {
        val instance = effect ?: return@runCatching false
        if (band !in 0 until instance.numberOfBands.toInt()) return@runCatching false
        val range = instance.bandLevelRange
        val safe = levelMillibels.coerceIn(range[0].toInt(), range[1].toInt())
        instance.setBandLevel(band.toShort(), safe.toShort())
        true
    }.getOrDefault(false)

    fun applyPreset(index: Int): Boolean = runCatching {
        val instance = effect ?: return@runCatching false
        if (index !in 0 until instance.numberOfPresets.toInt()) return@runCatching false
        instance.usePreset(index.toShort())
        true
    }.getOrDefault(false)

    /** يضبط جميع النطاقات إلى قيمة واحدة ضمن الحدود التي يدعمها الجهاز. */
    fun setAllBands(levelMillibels: Int): Boolean {
        if (!available || bandCount == 0) return false
        var success = true
        repeat(bandCount) { band ->
            if (!setBandLevel(band, levelMillibels)) success = false
        }
        return success
    }

    /** إعادة ضبط مسطحة لا تعتمد على وجود preset باسم Flat في الجهاز. */
    fun resetFlat(): Boolean = setAllBands(0)

    /** لقطة متسقة تُستخدم لتحديث واجهة المعادل دون الاحتفاظ بكائن Android نفسه. */
    fun snapshot(): EqualizerSnapshot = EqualizerSnapshot(
        enabled = enabled,
        presetIndex = currentPresetIndex,
        levelsMillibels = List(bandCount) { bandLevel(it) ?: 0 },
    )

    companion object {
        /** يستدعيها مالك المشغّل عند تحرير جلسة الصوت، بعد انتهاء التشغيل فعلياً. */
        fun releaseSession(audioSessionId: Int) = EqualizerEffectPool.release(audioSessionId)
    }
}
