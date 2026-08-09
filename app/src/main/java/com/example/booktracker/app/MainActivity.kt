package com.example.booktracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
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

class MainActivity : ComponentActivity() {

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

            BookTrackerTheme(darkTheme = isDarkTheme, dynamicColor = useDynamicColor) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BookTrackerApp(viewModel, windowSizeClass)
                }
            }
        }
    }
}

