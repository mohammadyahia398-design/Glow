# الويدجت: وصف ومعاينة — الدفعة 8

- **وصف** يظهر في قائمة الأدوات: «مشغّل Glow: غلاف الأغنية الحالية وعنوانها وأزرار السابق والتشغيل والتالي.» (values/widget_strings.xml).
- **معاينة** حقيقية بدل الأيقونة (layout/glow_widget_preview.xml، أندرويد 12+): نفس شكل الويدجت بخلفية Indigo→Navy وغلاف تدرّج Magenta→Violet وأزرار التحكم.
- **حجم افتراضي** 4×2 خلايا (targetCellWidth وtargetCellHeight)، مع بقاء تغيير الحجم.
- لم أضف حداً أدنى أصغر للتصغير لأن التصميم الحالي (غلاف 86dp وثلاثة أزرار) لا يتسع لأصغر من الحجم الحالي.
- أندرويد أقدم من 12 يعرض أيقونة التطبيق في القائمة كالسابق (لا previewImage).
- ملفات: xml/glow_widget_info.xml (معدّل)، values/widget_strings.xml، layout/glow_widget_preview.xml، drawable/glow_widget_preview_art.xml (جديدة).
