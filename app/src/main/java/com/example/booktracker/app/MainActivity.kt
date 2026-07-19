package com.example.booktracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.booktracker.app.ui.BookTrackerApp
import com.example.booktracker.app.ui.BookTrackerViewModel
import com.example.booktracker.app.ui.theme.BookTrackerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BookTrackerViewModel by viewModels {
        BookTrackerViewModel.factory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val useDynamicColor by viewModel.useDynamicColor.collectAsState()
            
            val isDarkTheme = when (themeMode) {
                com.example.booktracker.app.data.ThemeMode.LIGHT -> false
                com.example.booktracker.app.data.ThemeMode.DARK -> true
                else -> isSystemInDarkTheme()
            }
            
            val timerIsRunning by viewModel.timerIsRunning.collectAsState()
            val timeLeftSeconds by viewModel.timeLeftSeconds.collectAsState()
            val timerPhase by viewModel.timerPhase.collectAsState()

            val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) { /* permission result handled */ }
            
            androidx.compose.runtime.LaunchedEffect(Unit) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            val serviceStartedState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            var serviceStarted = serviceStartedState.value
            androidx.compose.runtime.LaunchedEffect(timerIsRunning) {
                val intent = android.content.Intent(this@MainActivity, com.example.booktracker.app.service.TimerService::class.java)
                if (timerIsRunning) {
                    intent.action = com.example.booktracker.app.service.TimerService.ACTION_START
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
                    serviceStarted = true
                } else if (serviceStarted) {
                    intent.action = com.example.booktracker.app.service.TimerService.ACTION_STOP
                    startService(intent)
                    serviceStarted = false
                }
            }

            androidx.compose.runtime.LaunchedEffect(timeLeftSeconds, timerPhase) {
                if (timerIsRunning) {
                    val intent = android.content.Intent(this@MainActivity, com.example.booktracker.app.service.TimerService::class.java).apply {
                        action = com.example.booktracker.app.service.TimerService.ACTION_UPDATE
                        putExtra(com.example.booktracker.app.service.TimerService.EXTRA_TIME_LEFT, timeLeftSeconds)
                        putExtra(com.example.booktracker.app.service.TimerService.EXTRA_PHASE, timerPhase.name)
                    }
                    startService(intent)
                }
            }

            BookTrackerTheme(darkTheme = isDarkTheme, dynamicColor = useDynamicColor) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BookTrackerApp(viewModel)
                }
            }
        }
    }
}
