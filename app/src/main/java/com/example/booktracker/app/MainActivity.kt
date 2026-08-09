package com.example.booktracker.app

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.Modifier
import com.example.booktracker.app.ui.BookTrackerApp
import com.example.booktracker.app.ui.BookTrackerViewModel
import com.example.booktracker.app.ui.theme.BookTrackerTheme
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

class MainActivity : FragmentActivity() {

    private val viewModel: BookTrackerViewModel by viewModels {
        BookTrackerViewModel.factory(this)
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val themeMode by viewModel.themeMode.collectAsState()
            val useDynamicColor by viewModel.useDynamicColor.collectAsState()
            
            val isDarkTheme = when (themeMode) {
                com.example.booktracker.app.data.ThemeMode.LIGHT -> false
                com.example.booktracker.app.data.ThemeMode.DARK -> true
                else -> isSystemInDarkTheme()
            }
            
            val timerBookId by viewModel.activeTimerBook.collectAsState()
            val serviceStartedState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

            val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) { /* permission result handled */ }
            
            androidx.compose.runtime.LaunchedEffect(Unit) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            
            androidx.compose.runtime.LaunchedEffect(timerBookId) {
                val intent = android.content.Intent(this@MainActivity, com.example.booktracker.app.service.TimerService::class.java)
                if (timerBookId != null) {
                    intent.action = com.example.booktracker.app.service.TimerService.ACTION_START
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
                    serviceStartedState.value = true
                } else if (serviceStartedState.value) {
                    intent.action = com.example.booktracker.app.service.TimerService.ACTION_STOP
                    startService(intent)
                    serviceStartedState.value = false
                }
            }

            val useAppLock by viewModel.useAppLock.collectAsState()
            var isAuthenticated by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            
            androidx.compose.runtime.LaunchedEffect(useAppLock) {
                if (!useAppLock) {
                    isAuthenticated = true
                } else {
                    val biometricManager = BiometricManager.from(this@MainActivity)
                    if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS) {
                        val executor = ContextCompat.getMainExecutor(this@MainActivity)
                        val biometricPrompt = BiometricPrompt(this@MainActivity, executor,
                            object : BiometricPrompt.AuthenticationCallback() {
                                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                    super.onAuthenticationSucceeded(result)
                                    isAuthenticated = true
                                }
                            })
                        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                            .setTitle("App Locked")
                            .setSubtitle("Authenticate to access BookTracker")
                            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                            .build()
                        biometricPrompt.authenticate(promptInfo)
                    } else {
                        isAuthenticated = true
                    }
                }
            }

            BookTrackerTheme(darkTheme = isDarkTheme, dynamicColor = useDynamicColor) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (isAuthenticated) {
                        BookTrackerApp(viewModel, windowSizeClass)
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = "Locked",
                                modifier = Modifier.size(64.dp),
                                tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

