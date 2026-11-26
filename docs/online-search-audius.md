# البحث المجاني في الإنترنت (Audius)

- online/AudiusClient.kt: بحث tracks/search على api.audius.co بلا مفتاح (HttpURLConnection وorg.json). يتجاهل المقاطع غير القابلة للبث أو المقيّدة. رابط التشغيل: /v1/tracks/{id}/stream ويتبع ExoPlayer التحويل.
- ui/online/OnlineSearchScreen.kt: حقل بحث (يبدأ بعد التوقف عن الكتابة 450ms)، وحالات: فارغ، تحميل، فشل مع إعادة المحاولة، لا نتائج، قائمة. النقر يشغّل القائمة.
- LibraryScreen: شريحة «الإنترنت». GlowApp: شاشة showOnline مع زر رجوع.
- معرّف المقطع سالب مشتق من معرّف Audius. بلا غلاف حالياً.
