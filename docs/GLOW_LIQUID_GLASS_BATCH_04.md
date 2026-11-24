# Glow × Liquid Glass — الدفعة 4: شاشة التشغيل

- جديد `liquid/ui/LiquidCircleButton.kt`: زر دائري زجاجي بحجم يُحدَّد (نفس وصفة LiquidButton).
- معدّل `liquid/ui/LiquidSlider.kt`: معامل `onValueChangeFinished` + استعمال أحدث نسخة من الدوال (rememberUpdatedState).
- معدّل `ui/nowplaying/NowPlayingControls.kt`: شريط التقدم (LiquidSlider) وأزرار السابق/تشغيل/التالي زجاجية؛ الخلط والتكرار كما هي.
- معدّل `ui/nowplaying/NowPlayingScreen.kt`: `layerBackdrop` على الخلفية الحيّة فقط عند `GlassTier.Liquid`.
- ما دون أندرويد 13: يبقى `ArtSeekBar` والأزرار القديمة كما هي.
