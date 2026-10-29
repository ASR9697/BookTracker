package com.example.booktracker.app

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.KeyboardShortcutGroup
import android.view.KeyboardShortcutInfo
import android.view.Menu
import android.view.WindowManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import java.time.LocalTime
import kotlinx.coroutines.delay
import com.example.booktracker.app.ui.ADD_BOOK_ACTION
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.example.booktracker.app.data.ServiceLocator
import com.example.booktracker.app.service.TimerService
import com.example.booktracker.app.ui.BookTrackerApp
import com.example.booktracker.app.ui.BookTrackerViewModel
import com.example.booktracker.app.ui.theme.BookTrackerTheme
import com.example.booktracker.app.widget.DeepLink

class MainActivity : FragmentActivity() {

    private val viewModel: BookTrackerViewModel by viewModels {
        BookTrackerViewModel.factory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // A widget tap names the screen to open; a recreated activity already handled it.
        if (savedInstanceState == null) viewModel.openRoute(DeepLink.routeFrom(intent))
        // Hold the splash until the lock preference is known, so locked content
        // never flashes on screen before the prompt.
        splashScreen.setKeepOnScreenCondition { viewModel.appLockSetting.value == null }
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val useDynamicColor by viewModel.useDynamicColor.collectAsState()
            val appPalette by viewModel.appPalette.collectAsState()

            val prefs by viewModel.prefs.collectAsState()
            // Checked each minute, so Nocturnal mode turns the lights down without reopening the app.
            var hour by remember { mutableIntStateOf(LocalTime.now().hour) }
            LaunchedEffect(Unit) {
                while (true) {
                    delay(60_000L - System.currentTimeMillis() % 60_000L)
                    hour = LocalTime.now().hour
                }
            }
            val isDarkTheme = prefs.isNocturnal(hour) || when (themeMode) {
                com.example.booktracker.app.data.ThemeMode.LIGHT -> false
                com.example.booktracker.app.data.ThemeMode.DARK -> true
                else -> isSystemInDarkTheme()
            }
            LaunchedEffect(prefs.secureScreen) {
                if (prefs.secureScreen) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
            val systemDensity = LocalDensity.current

            val timerBook by viewModel.activeTimerBook.collectAsState()
            val timerBookId = timerBook?.id
            val serviceStarted = remember { mutableStateOf(false) }
            LaunchedEffect(timerBookId) {
                val intent = Intent(this@MainActivity, TimerService::class.java)
                if (timerBookId != null) {
                    intent.action = TimerService.ACTION_START
                    startForegroundService(intent)
                    serviceStarted.value = true
                } else if (serviceStarted.value) {
                    intent.action = TimerService.ACTION_STOP
                    startService(intent)
                    serviceStarted.value = false
                }
            }

            val lockSetting by viewModel.appLockSetting.collectAsState()
            val lockRequired = lockSetting == true && canUseDeviceAuth()
            // Turning the lock on from Settings shouldn't lock the reader out mid-session.
            var previousLockSetting by remember { mutableStateOf(lockSetting) }
            LaunchedEffect(lockSetting) {
                if (previousLockSetting == false && lockSetting == true) AppLockState.unlocked = true
                previousLockSetting = lockSetting
            }

            // The reader's text size multiplies the system's, so accessibility sizes still apply.
            CompositionLocalProvider(LocalDensity provides Density(systemDensity.density, systemDensity.fontScale * prefs.textScale)) {
            BookTrackerTheme(darkTheme = isDarkTheme, dynamicColor = useDynamicColor, palette = appPalette) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when {
                        lockSetting == null -> Unit // splash is still showing
                        !lockRequired || AppLockState.unlocked -> BookTrackerApp(viewModel)
                        else -> {
                            LaunchedEffect(Unit) { promptUnlock() }
                            LockedScreen(onUnlock = ::promptUnlock)
                        }
                    }
                }
            }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.openRoute(DeepLink.routeFrom(intent))
    }

    /**
     * Keyboard shortcuts for tablets with a keyboard and Chromebooks. They come here
     * rather than through Compose because Compose only sees keys while something
     * is focused. Each opens a screen the same way a widget tap does.
     */
    override fun onKeyShortcut(keyCode: Int, event: KeyEvent): Boolean {
        val route = SHORTCUTS.firstOrNull { it.keyCode == keyCode }?.route
        if (!event.isCtrlPressed || route == null) return super.onKeyShortcut(keyCode, event)
        viewModel.openRoute(route)
        return true
    }

    /** Lists the shortcuts in Android's keyboard shortcut helper (Ctrl + /). */
    override fun onProvideKeyboardShortcuts(data: MutableList<KeyboardShortcutGroup>, menu: Menu?, deviceId: Int) {
        super.onProvideKeyboardShortcuts(data, menu, deviceId)
        data += KeyboardShortcutGroup(
            getString(R.string.app_name),
            SHORTCUTS.map { KeyboardShortcutInfo(it.label, it.keyCode, KeyEvent.META_CTRL_ON) }
        )
    }

    override fun onStart() {
        super.onStart()
        ServiceLocator.appInForeground.value = true
        val stoppedAt = AppLockState.stoppedAtElapsed
        val relockAfter = viewModel.prefs.value.lockDelaySeconds * 1000L
        if (stoppedAt > 0L && SystemClock.elapsedRealtime() - stoppedAt >= relockAfter) {
            AppLockState.unlocked = false
        }
    }

    override fun onStop() {
        super.onStop()
        ServiceLocator.appInForeground.value = false
        // Configuration changes stop and recreate the activity; they shouldn't lock.
        if (!isChangingConfigurations) AppLockState.stoppedAtElapsed = SystemClock.elapsedRealtime()
    }

    private fun canUseDeviceAuth(): Boolean =
        BiometricManager.from(this).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    private fun promptUnlock() {
        if (!canUseDeviceAuth()) {
            // Screen lock was removed after enabling the app lock; there is
            // nothing to authenticate against, so don't strand the reader.
            AppLockState.unlocked = true
            return
        }
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    AppLockState.unlocked = true
                    AppLockState.stoppedAtElapsed = 0L
                }
            }
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock ${getString(R.string.app_name)}")
                .setSubtitle("Confirm it's you to open your library")
                .setAllowedAuthenticators(AUTHENTICATORS)
                .build()
        )
    }

    private class Shortcut(val keyCode: Int, val route: String, val label: String)

    private companion object {
        val SHORTCUTS = listOf(
            Shortcut(KeyEvent.KEYCODE_F, "search", "Search your library"),
            Shortcut(KeyEvent.KEYCODE_N, ADD_BOOK_ACTION, "Add a book"),
            Shortcut(KeyEvent.KEYCODE_R, "review", "Daily review"),
            Shortcut(KeyEvent.KEYCODE_COMMA, "settings", "Settings"),
            Shortcut(KeyEvent.KEYCODE_1, "home", "Home"),
            Shortcut(KeyEvent.KEYCODE_2, "library", "Library"),
            Shortcut(KeyEvent.KEYCODE_3, "memorize", "Notes"),
            Shortcut(KeyEvent.KEYCODE_4, "stats", "Stats"),
            Shortcut(KeyEvent.KEYCODE_5, "profile", "Profile")
        )
        const val AUTHENTICATORS =
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}

/**
 * Process-scoped on purpose: it survives rotation (no prompt on every config
 * change) but not process death, so a restored task can't skip the lock the way
 * a value in savedInstanceState would.
 */
private object AppLockState {
    var unlocked by mutableStateOf(false)
    var stoppedAtElapsed = 0L
}

@androidx.compose.runtime.Composable
private fun LockedScreen(onUnlock: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Icon(
            Icons.Rounded.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text("Your library is locked", style = MaterialTheme.typography.titleLarge)
        Text(
            "Use your fingerprint, face or screen lock to continue.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(onClick = onUnlock) { Text("Unlock") }
    }
}
