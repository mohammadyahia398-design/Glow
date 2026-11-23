package com.glow.app.media

import kotlin.random.Random

/**
 * حالات تكرار قائمة التشغيل.
 * Off ينهي القائمة عند آخر عنصر، All يعيد بناء دورة كاملة، وOne يعيد المقطع
 * الحالي عند الانتهاء التلقائي فقط؛ أما طلب التالي يظل انتقالاً يدوياً.
 */
enum class RepeatMode {
    Off, All, One;

    fun cycle(): RepeatMode = when (this) {
        Off -> All
        All -> One
        One -> Off
    }
}

/**
 * محرك ترتيب مستقل عن Android.
 *
 * يحتفظ المحرك بفهرس كل عنصر في القائمة الأصلية، ثم يبني ترتيب التشغيل فوقها.
 * هذا الفصل يسمح بالخلط وإعادة الترتيب والحذف دون نسخ بيانات Track أو فقدان
 * هوية العنصر الحالي. جميع مواضع الدوال التي تستقبل position هي مواضع في
 * ترتيب التشغيل الحالي، وليست فهارس القائمة الأصلية.
 */
class PlayQueue(private val random: Random = Random.Default) {

    private var order: List<Int> = emptyList()
    private var position: Int = -1

    /** هل الخلط مفعّل؟ */
    var shuffle: Boolean = false
        private set

    /** سياسة تكرار نهاية القائمة. */
    var repeat: RepeatMode = RepeatMode.Off

    /** عدد العناصر في ترتيب التشغيل. */
    val size: Int get() = order.size

    /** موضع العنصر الحالي في ترتيب التشغيل، أو -1 للقائمة الفارغة. */
    val currentPosition: Int get() = position

    /** فهرس العنصر الحالي في القائمة الأصلية. */
    val currentIndex: Int? get() = order.getOrNull(position)

    /** لقطة غير قابلة للتعديل من ترتيب الفهارس الحالي. */
    fun orderedIndices(): List<Int> = order.toList()

    /**
     * ينشئ قائمة جديدة.
     * عند طلب الخلط يبدأ الترتيب بالمقطع المختار ثم يوزع بقية العناصر عشوائياً.
     */
    fun load(size: Int, startIndex: Int = 0, shuffle: Boolean = false) {
        this.shuffle = shuffle
        if (size <= 0) {
            order = emptyList()
            position = -1
            return
        }
        val start = startIndex.coerceIn(0, size - 1)
        order = buildOrder(size, start)
        position = order.indexOf(start)
    }

    /** ينتقل إلى موضع محدد في ترتيب التشغيل، ويرجع فهرسه الأصلي. */
    fun select(queuePosition: Int): Int? {
        if (queuePosition !in order.indices) return null
        position = queuePosition
        return currentIndex
    }

    /**
     * ينقل عنصراً داخل ترتيب التشغيل دون تغيير العنصر الحالي.
     * fromPosition وtoPosition موضعان في الترتيب المرئي للمستخدم.
     */
    fun move(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition !in order.indices || toPosition !in order.indices) return false
        if (fromPosition == toPosition) return true

        val currentOriginalIndex = currentIndex
        val reordered = order.toMutableList()
        val moved = reordered.removeAt(fromPosition)
        reordered.add(toPosition, moved)
        order = reordered
        position = currentOriginalIndex?.let(order::indexOf) ?: -1
        return true
    }

    /**
     * يحذف عنصراً من موضعه المرئي.
     * @return فهرس العنصر المحذوف في القائمة الأصلية قبل الحذف.
     * عند حذف العنصر الحالي يصبح العنصر التالي في الموضع نفسه هو الحالي،
     * أو العنصر السابق إذا كان المحذوف آخر عنصر.
     */
    fun removeAt(queuePosition: Int): Int? {
        if (queuePosition !in order.indices) return null

        val removedOriginalIndex = order[queuePosition]
        val oldCurrentOriginalIndex = currentIndex
        val removedCurrent = removedOriginalIndex == oldCurrentOriginalIndex
        val oldPosition = position

        val remaining = order.toMutableList().apply { removeAt(queuePosition) }
        order = remaining.map { original ->
            if (original > removedOriginalIndex) original - 1 else original
        }

        position = when {
            order.isEmpty() -> -1
            removedCurrent -> queuePosition.coerceAtMost(order.lastIndex)
            queuePosition < oldPosition -> oldPosition - 1
            else -> oldPosition.coerceAtMost(order.lastIndex)
        }
        return removedOriginalIndex
    }

    /** يضيف فهارس جديدة إلى نهاية القائمة أو مباشرة بعد العنصر الحالي. */
    fun append(indices: List<Int>, playNext: Boolean = false) {
        val additions = indices.asSequence()
            .filter { it >= 0 && it !in order }
            .distinct()
            .toList()
        if (additions.isEmpty()) return

        if (order.isEmpty()) {
            order = additions
            position = 0
            return
        }

        order = if (playNext) {
            order.take(position + 1) + additions + order.drop(position + 1)
        } else {
            order + additions
        }
    }

    /**
     * @param auto true عند انتهاء المقطع تلقائياً؛ حينها يعاد المقطع نفسه
     * إذا كان Repeat One مفعلاً.
     * @return فهرس المقطع التالي أو null عند نهاية القائمة.
     */
    fun next(auto: Boolean): Int? {
        if (order.isEmpty()) return null
        if (auto && repeat == RepeatMode.One) return currentIndex

        if (position + 1 < order.size) {
            position++
        } else if (repeat == RepeatMode.All) {
            order = if (shuffle) order.shuffled(random) else order
            position = 0
        } else {
            return null
        }
        return currentIndex
    }

    /** يرجع إلى السابق، أو يبقى عند الأول حسب وضع التكرار. */
    fun previous(): Int? {
        if (order.isEmpty()) return null
        if (position > 0) {
            position--
        } else if (repeat == RepeatMode.All) {
            position = order.lastIndex
        }
        return currentIndex
    }

    /** يبدّل الخلط مع الاحتفاظ بالعنصر الحالي وبقية العناصر مرة واحدة. */
    fun setShuffle(enabled: Boolean) {
        if (enabled == shuffle) return
        shuffle = enabled
        val current = currentIndex ?: return
        order = if (enabled) {
            listOf(current) + order.filterNot { it == current }.shuffled(random)
        } else {
            order.sorted()
        }
        position = order.indexOf(current)
    }

    /** يعيد إنشاء الترتيب الأساسي، ويضع العنصر المحدد أولاً. */
    private fun buildOrder(size: Int, first: Int): List<Int> =
        if (shuffle) {
            listOf(first) + (0 until size).filter { it != first }.shuffled(random)
        } else {
            (0 until size).toList()
        }
}
