# اللغات (عربي + إنجليزي)

- كل نصوص الواجهة انتقلت من الكود إلى موارد: `values/strings_ui.xml` (إنجليزي، الافتراضي) و`values-ar/strings_ui.xml` (عربي).
- الاختصارات والويدجت: `shortcut_strings.xml` و`widget_strings.xml` بالنسختين.
- الجهاز بالعربية يعرض العربية، وأي لغة أخرى تعرض الإنجليزية.
- أندرويد 13+: يمكن اختيار لغة التطبيق وحدها من إعدادات النظام (`xml/locales_config.xml`).
- `GlowStrings.get(R.string.key, ...)` يجلب النص من أي مكان (واجهة، خدمة، ويدجت). `GlowStrings.songs(n)` لعدد الأغاني بصيغة الجمع. تُهيَّأ من `GlowApplication`.
- ما لم يُترجم عمداً: اسم ملف واتساب الذي يُطابَق في Lyrics.kt (بيانات لا واجهة).
- لإضافة لغة: أنشئ `values-xx/strings_ui.xml` بنفس المفاتيح وأضفها في `locales_config.xml`.
