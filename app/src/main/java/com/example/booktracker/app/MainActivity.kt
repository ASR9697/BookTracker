package com.example.booktracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
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
        super.onCreate(savedInstanceState)
        setContent {
            BookTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BookTrackerApp(viewModel)
                }
            }
        }
    }
}
