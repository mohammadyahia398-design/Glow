package com.glow.app

/**
 * مفاتيح تفعيل الميزات. تعطيل ميزة هنا يخفي مداخلها من الواجهة دون حذف كودها.
 */
object FeatureFlags {
    /** عزل الصوت معطَّل حالياً. غيّر القيمة إلى true لإعادة إظهار الشاشة ومداخلها. */
    const val VOCAL_ISOLATION = false
}
