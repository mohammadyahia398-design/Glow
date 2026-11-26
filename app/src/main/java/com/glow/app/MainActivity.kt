package com.glow.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.glow.app.ui.common.GlowDialog
import com.glow.app.ui.common.GlowDialogAction
import com.glow.app.ui.theme.LocalGlowSkin
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.glow.app.settings.SettingsStore
import com.glow.app.settings.ThemeMode
import com.glow.app.ui.theme.GlowSkins
import com.glow.app.ui.navigation.GlowApp
import com.glow.app.ui.splash.GlowSplash
import com.glow.app.ui.theme.GlowTheme
import com.glow.app.ui.theme.resolveSkin

/** نقطة الدخول: تعرض التطبيق الحقيقي (المكتبة والألبوم والمشغّل) تحت شاشة البداية المتحركة. */
class MainActivity : ComponentActivity() {

    private val settings by lazy { SettingsStore(applicationContext) }

    /** أمر من اختصار الأيقونة ينتظر أن ينفّذه GlowApp مرة واحدة. */
    private var launchAction by mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchAction = GlowActions.from(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // شاشة النظام تطابق بداية GlowSplash، فنُزيلها فور الرسم الأول بلا أي انتقال ظاهر.
        CrashReporter.install(this)
        installSplashScreen().setOnExitAnimationListener { it.remove() }
        super.onCreate(savedInstanceState)
        // لا نقرأ الأمر بعد تدوير الشاشة أو إعادة الإنشاء، وإلا تكرّر.
        if (savedInstanceState == null) launchAction = GlowActions.from(intent)
        enableEdgeToEdge()
        setContent {
            val dark = when (settings.themeMode) {
                ThemeMode.AUTO -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            val skin = if (dark) GlowSkins.Dark else GlowSkins.Light
            GlowTheme(darkTheme = skin.isDark, customSkin = skin) {
                var showSplash by rememberSaveable { mutableStateOf(true) }
                // آخر انهيار (إن حدث) يُعرض مرة واحدة ليُنسخ ويُرسل.
                var crash by rememberSaveable { mutableStateOf(CrashReporter.take(this@MainActivity)) }
                val clipboard = LocalClipboardManager.current
                Box(Modifier.fillMaxSize().background(skin.bgTop)) {
                    // عند وجود انهيار سابق نعرضه وحده أولاً؛ لا نبني الواجهة قبل إغلاق النافذة،
                    // وإلا انهار التطبيق من جديد قبل أن يراها المستخدم.
                    if (crash == null) {
                        GlowApp(
                            settings,
                            Modifier.fillMaxSize(),
                            launchAction = launchAction,
                            onLaunchActionHandled = { launchAction = null },
                        )
                        if (showSplash) GlowSplash(onFinished = { showSplash = false })
                    }
                    crash?.let { report ->
                        GlowDialog(
                            title = "التطبيق أُغلق فجأة",
                            onDismiss = { crash = null },
                            actions = {
                                GlowDialogAction("إغلاق", { crash = null })
                                GlowDialogAction(
                                    "نسخ الخطأ",
                                    { clipboard.setText(AnnotatedString(report)) },
                                    primary = true,
                                )
                            },
                        ) {
                            Text(
                                "اضغط «نسخ الخطأ» ثم الصقه في محادثة Claude.",
                                color = LocalGlowSkin.current.textSecondary,
                                fontSize = 14.sp,
                            )
                            Text(
                                report,
                                color = LocalGlowSkin.current.text,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 14.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
