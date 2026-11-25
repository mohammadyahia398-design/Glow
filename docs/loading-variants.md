# حالات التحميل (Loading Variants)
ملف جديد: ui/common/GlowLoading.kt، بلون التمييز فقط وحواف دائرية، وتتجمّد مع reduceMotion.
- GlowRingLoader: حلقة صغيرة بقوس لامع يطول ويقصر (للأزرار والصفوف).
- GlowBarsLoader: أعمدة موسيقية بموجة هادئة (للحالات الصوتية).
- GlowOrbsLoader: ثلاث كرات ضوء ضبابية تتنفّس (للتحميل الكبير، بلغة خلفية المشغّل).
- GlowProgressBar: شريحة ضوء منزلقة، أو تعبئة ناعمة عند تمرير progress.
- GlowLoading(style, label): مؤشر + نص جاهز للشاشات.
مُطبَّق الآن في VocalIsolationScreen بدل مؤشري Material.
