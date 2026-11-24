# Glow × Liquid Glass — الدفعة 3: الشريط السفلي

- جديد `liquid/ui/LiquidBottomTabs.kt`: منقول من حزمة Kyant (Apache 2.0) ومكيَّف لـ Kotlin 2.0 / Compose 1.7
  (بلا `context(...)` ولا `fast*` ولا `Capsule`، وبلا ColorFilter على graphicsLayer: نسخة التبويبات الملوّنة تُمرَّر من المستدعي).
- جديد `liquid/ui/GlowLiquidBottomBar.kt`: يستعمل `LiquidBarItem` نفسها، ويعكس الترتيب في RTL (الحساب الداخلي LTR كالشريط القديم).
- معدّل `ui/navigation/GlowApp.kt`:
  - `layerBackdrop` يوضع على صندوق المحتوى (أخٌ للشريط، لا أب له) فقط عند `GlassTier.Liquid`.
  - أندرويد 13+ ← `GlowLiquidBottomBar`، وما دونه ← `LiquidGlassBar` القديم كما هو.
- الشريط القديم والمشغّل المصغّر والـ insets لم تتغير (BarLift = 84dp).
