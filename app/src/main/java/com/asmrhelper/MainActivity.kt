package com.asmrhelper

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.asmrhelper.player.PlayerManager
import com.asmrhelper.domain.repository.SettingsRepository
import com.asmrhelper.ui.navigation.AsmrNavHost
import com.asmrhelper.ui.theme.ASMRHelperTheme
import com.asmrhelper.ui.theme.ThemeMode
import com.asmrhelper.ui.theme.resolveDark
import com.asmrhelper.ui.theme.ThemePreset
import com.asmrhelper.ui.theme.LocalFrostedGlass
import com.asmrhelper.util.AppLocale
import com.asmrhelper.util.AppFont
import com.asmrhelper.util.ShareReceiver
import com.asmrhelper.util.ShortcutReceiver
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var playerManager: PlayerManager

    /** Android 13+ requires runtime permission for notifications. Without this,
     *  the foreground service notification is silently suppressed by the system. */
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied — service will check pref on next start */ }

    /** 应用内语言 override：按持久化的 `app_language` 包装 base context。 */
    override fun attachBaseContext(newBase: Context) {
        val lang = newBase.getSharedPreferences("asmr_settings", Context.MODE_PRIVATE)
            .getInt("app_language", 0)
        super.attachBaseContext(AppLocale.wrap(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()

        CrashHandler.showLastCrash(this)
        intent?.let { handleShareIntent(it) }
        handleShortcutIntent(intent)

        // 「自动播放」：打开软件后（非旋转等重建）自动续播上次的歌曲（默认关闭）
        if (savedInstanceState == null) {
            playerManager.autoPlayIfEnabled()
        }

        // Request notification permission on Android 13+ if not yet granted.
        // Only on first creation, not on recreation (rotation etc.) to avoid
        // repeatedly showing the dialog.
        if (savedInstanceState == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val prefs = getSharedPreferences("asmr_settings", MODE_PRIVATE)
        val presetOrdinal = prefs.getInt("theme_preset", 0)
        // 自定义字体：导入后本 Activity recreate，重新加载最新字体路径
        val fontFamily = AppFont.loadFontFamily(this)

        setContent {
            val ordinal by settingsRepository.getThemePresetOrdinal()
                .collectAsState(initial = presetOrdinal)
            val preset = ThemePreset.fromOrdinalOrDefault(ordinal)
            val themeModeOrdinal by settingsRepository.getThemeMode()
                .collectAsState(initial = 0)
            val darkTheme = ThemeMode.entries.getOrElse(themeModeOrdinal) { ThemeMode.FOLLOW_SYSTEM }
                .resolveDark(isSystemInDarkTheme())
            // 无障碍：显示大小缩放（默认 1.0 = 100%）
            val displayScale by settingsRepository.getDisplayScale()
                .collectAsState(initial = 1f)
            // 个人偏好：隐藏状态栏
            val hideStatusBar by settingsRepository.getHideStatusBar()
                .collectAsState(initial = false)
            // 外观与主题：透明磨砂质感
            val frostedGlass by settingsRepository.getFrostedGlassEnabled()
                .collectAsState(initial = false)

            // 浅色模式下系统状态栏/导航栏图标需切为深色，否则白图标压在浅灰背景上不可见
            val view = LocalView.current
            if (!view.isInEditMode) {
                val window = (view.context as Activity).window
                val insetsController = WindowCompat.getInsetsController(window, view)
                DisposableEffect(insetsController, hideStatusBar, darkTheme) {
                    // 让系统条可被短暂划出而非常驻，避免 edge-to-edge 下 hide 被立即回弹
                    insetsController.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    fun applyBars() {
                        insetsController.isAppearanceLightStatusBars = !darkTheme
                        insetsController.isAppearanceLightNavigationBars = !darkTheme
                        if (hideStatusBar) {
                            insetsController.hide(WindowInsetsCompat.Type.statusBars())
                        } else {
                            insetsController.show(WindowInsetsCompat.Type.statusBars())
                        }
                    }
                    applyBars()
                    // 从后台/设置返回时系统可能把状态栏重新显示，ON_RESUME 再应用一次
                    val activity = view.context as? ComponentActivity
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) applyBars()
                    }
                    activity?.lifecycle?.addObserver(observer)
                    onDispose { activity?.lifecycle?.removeObserver(observer) }
                }
            }

            ASMRHelperTheme(preset = preset, darkTheme = darkTheme, customFontFamily = fontFamily) {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density.density * displayScale,
                        density.fontScale * displayScale
                    ),
                    LocalFrostedGlass provides frostedGlass
                ) {
                    AsmrNavHost()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)  // so onCreate sees the latest intent on recreation
        handleShareIntent(intent)
        handleShortcutIntent(intent)
    }

    private fun handleShortcutIntent(intent: Intent?) {
        val action = intent?.getStringExtra("shortcut_action") ?: return
        ShortcutReceiver.receive(action)
    }

    private fun handleShareIntent(intent: Intent) {
        // Accept ACTION_SEND with text content. Some apps (e.g. Bilibili) may set
        // type to "text/plain", "text/html", or leave it null — be lenient.
        if (Intent.ACTION_SEND == intent.action) {
            val type = intent.type
            if (type != null && !type.startsWith("text/")) return
            val sharedUrl = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            if (sharedUrl.startsWith("http")) {
                // Use reactive StateFlow so Compose auto-navigates to video tab
                ShareReceiver.receive(sharedUrl)
            }
        }
    }
}
