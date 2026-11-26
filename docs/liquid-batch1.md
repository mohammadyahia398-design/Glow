# الدفعة 1: ليكويد جلاس للمكوّنات المشتركة

- ui/common/GlowLiquid.kt (جديد): LocalLiquidBackdrop و Modifier.glowLiquid(...)، سطح موحّد (عدسة + حافة + صبغة + انتفاخ يتبع الإصبع). إن كانت LocalLiquidBackdrop = null يرجع إلى glowGlass كما كان.
- GlowApp: طبقة خلفية منفصلة contentBackdrop (تدرّج + وهجان خفيفان) خلف محتوى الشاشات، وتُمرَّر عبر LocalLiquidBackdrop لمحتوى المصدر فقط (ليس للمشغّل الكامل ولا الشريط).
- يتضمن أيضاً تعديل إعادة الفحص (patch-10، docs/rescan-state.md).
- الإعدادات تمرّر خلفيتها الخاصة، والنوافذ واللوحات تمرّر LocalDialogBackdrop.
- محوَّل: GlowFilledButton/Outlined/Text/Round، GlowChip، GlowSettingsButton، GlowIconTap (فقط إن كان له خلفية أو glass = true: زر الرجوع في GlowTopBar وزر البحث في المكتبة)، شريط المظهر (عدسة منزلقة بنابض وانتفاخ).
- غير محوَّل عمداً: أزرار ⋮ داخل الصفوف وزر مسح البحث وأزرار المشغّل المصغّر.
- الأجهزة الأقدم من أندرويد 13: لا انكسار، يبقى الزجاج البديل.
