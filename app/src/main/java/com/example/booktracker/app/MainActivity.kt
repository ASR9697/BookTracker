package com.example.booktracker.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BookTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BookTrackerApp()
                }
            }
        }
    }
}

@Composable
fun BookTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
        dynamicColor && !darkTheme -> dynamicLightColorScheme(LocalContext.current)
        darkTheme -> darkColorScheme() // Fallback MD3 dark colors
        else -> lightColorScheme() // Fallback MD3 light colors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookTrackerApp() {
    val pagerState = rememberPagerState(pageCount = { 3 })
    Column {
        Text("Dashboard Hero Section", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(16.dp))
        TabRow(selectedTabIndex = pagerState.currentPage) {
            Tab(selected = pagerState.currentPage == 0, onClick = {}, text = { Text("Backlog") })
            Tab(selected = pagerState.currentPage == 1, onClick = {}, text = { Text("Shortlist") })
            Tab(selected = pagerState.currentPage == 2, onClick = {}, text = { Text("Up Next") })
        }
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            when (page) {
                0 -> Text("Backlog Pipeline", Modifier.padding(16.dp))
                1 -> Text("Shortlist Pipeline", Modifier.padding(16.dp))
                2 -> Text("Up Next Pipeline", Modifier.padding(16.dp))
            }
        }
    }
}
