# توحيد بقايا Material (الدفعة 2)

## الجديد
- `ui/common/GlowSheet.kt`: `GlowBottomSheet` لوحة سفلية بهوية Glow (زوايا علوية 32dp، سطح داكن بلمعة وصبغة، مقبض كبسولة، حجب خلفي). تحتفظ بسلوك ModalBottomSheet (السحب والإغلاق) وتستبدل شكله فقط. على أندرويد 13+ يصبح سطحها مصدر الزجاج السائل لما بداخلها عبر LocalDialogBackdrop.

## التعديلات
- QueueSheet (قائمة التشغيل) وEqualizerSheet (المعادل): تستعملان GlowBottomSheet بدل ModalBottomSheet.
- EqualizerSheet: على أندرويد 13+ المفتاح والمنزلقات الزجاجية تنكسر خلف سطح اللوحة. قبل ذلك: GlassSwitch وGlowGlassSlider بدل Switch وSlider من Material.
- GlowChoiceDialog (قوائم السرعة والمؤقّت وغيرها): صفوف زجاجية بزر راديو بدل صفوف مسطحة. GlowDialogRow صار فيه معامل اختياري radio.
- VocalIsolationScreen: حقل عنوان الخادم كبسولة بحافة تتلوّن بلون التمييز عند التركيز بدل OutlinedTextField. أُزيلت استيرادات Material غير المستعملة.

## ما بقي من Material عمداً
- Text (النص)، وSurface في بطاقات شاشة عزل الصوت (StudioCard)، وHorizontalDivider.
- أزرار الشاشات (GlowFilledButton وغيرها) ما زالت مسطحة: دفعة منفصلة.
