# زر ⋮ للأغاني الإنترنتية: المفضلة والتحميل

- OnlineSearchScreen: صف جديد بغلاف وزر ⋮ يفتح نافذة: إضافة/إزالة المفضلة، وتحميل (إن سمح صاحب الأغنية).
- LibraryStore: onlineTracks وtoggleOnlineFavorite تحفظ بيانات الأغنية في glow_library (مفتاح online) فتظهر في تبويب المفضلة.
- LibraryScreen: المفضلة تضم أغاني الإنترنت المحفوظة.
- online/TrackDownloader.kt: تنزيل عبر DownloadManager إلى Music/Glow. تظهر الأغنية في المكتبة بعد إعادة الفحص من الإعدادات.
- Track: حقل downloadUrl. AudiusClient: يملؤه فقط إن كان is_downloadable وغير مقيّد.
- إصلاح: قائمة الإنترنت تعرض الغلاف (كانت تعرض أيقونة).
