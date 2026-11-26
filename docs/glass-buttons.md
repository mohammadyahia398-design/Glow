# أزرار الشاشات الزجاجية (الدفعة 3)

- ملف جديد `ui/common/GlowGlassStyle.kt`: `Modifier.glowGlass(shape, fill, isDark, rimColor, glow)` يعطي تعبئة متدرجة، لمعة علوية، وحافة ضوء متدرجة، مع توهج اختياري بلون التمييز.
- لا يلتقط الخلفية، لأن هذه الأزرار داخل مصدر `layerBackdrop` المشترك في GlowApp ولا يجوز لعنصر زجاجي أن يلتقط مصدره نفسه. لذلك يعمل على كل إصدارات أندرويد بلا بديل منفصل.
- `GlowFilledButton`: تدرّج Magenta إلى Violet مع توهج. `GlowOutlinedButton` و`GlowTextButton`: زجاج بلون المحتوى وحافة بنفس اللون.
- `GlowChip`: المحدّدة بتدرّج التمييز مع توهج، وغير المحدّدة زجاج هادئ.
- `GlowRoundButton`: دائرة زجاجية. `GlowSettingsButton`: صار دائرياً (corner الافتراضي 50dp) بنفس الأسلوب.
- التواقيع كلها كما هي، فلا تغيير في الشاشات المستدعية.
