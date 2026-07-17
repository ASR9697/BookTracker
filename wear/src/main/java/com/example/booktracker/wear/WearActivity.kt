package com.example.booktracker.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.ambient.AmbientLifecycleObserver
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

class WearActivity : ComponentActivity() {
    
    private val ambientObserver = AmbientLifecycleObserver(
        this,
        object : AmbientLifecycleObserver.AmbientLifecycleCallback {
            override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
                isAmbient.value = true
            }
            override fun onExitAmbient() {
                isAmbient.value = false
            }
            override fun onUpdateAmbient() {}
        }
    )

    private var isAmbient = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(ambientObserver)
        
        setContent {
            WearApp(isAmbient.value)
        }
    }
}

@Composable
fun WearApp(isAmbient: Boolean) {
    var currentUnit by remember { mutableStateOf(0) }

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isAmbient) Color.Black else MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            if (isAmbient) {
                Text("Ambient Mode: Reading", color = Color.White)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { currentUnit += 1 }, modifier = Modifier.padding(8.dp)) {
                        Text("+1")
                    }
                    Text(
                        text = "$currentUnit", 
                        style = MaterialTheme.typography.display1,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Button(onClick = { currentUnit += 10 }, modifier = Modifier.padding(8.dp)) {
                        Text("+10")
                    }
                }
            }
        }
    }
}
