package com.example.booktracker.app.ui

import com.example.booktracker.app.ui.theme.AppShape

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.booktracker.app.camera.PageNumberAnalyzer
import com.example.booktracker.app.camera.PageNumberDetector
import com.example.booktracker.app.ui.animations.rememberHaptics
import com.example.booktracker.shared.models.Book

/**
 * Point the camera at the corner of the page you're on and the app reads the
 * page number, so updating progress is one tap instead of scrolling a wheel.
 * Recognition runs on the phone; nothing leaves it.
 */
@Composable
fun PageScanDialog(book: Book, onDismiss: () -> Unit, onPage: (Int) -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        PageScanContent(book, onDismiss, onPage)
    }
}

@Composable
private fun PageScanContent(book: Book, onDismiss: () -> Unit, onPage: (Int) -> Unit) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }
    LaunchedEffect(Unit) { if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA) }

    var candidates by remember { mutableStateOf<List<PageNumberDetector.Candidate>>(emptyList()) }
    // A page only counts as found once two frames in a row agree, so a passing blur doesn't flash a number.
    var lastTop by remember { mutableStateOf<Int?>(null) }
    var found by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(found) { if (found != null) haptics.tick() }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (hasPermission) {
                CameraPreview(
                    analyzer = remember {
                        PageNumberAnalyzer { lines ->
                            val next = PageNumberDetector.candidates(lines, book.currentPage, book.totalPages)
                            val top = next.firstOrNull()?.page
                            if (top != null && top == lastTop) found = top
                            lastTop = top
                            if (next.isNotEmpty()) candidates = next.take(4)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                // Corner brackets hint at where page numbers live.
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp)
                        .height(360.dp)
                        .border(2.dp, Color.White.copy(alpha = 0.55f), AppShape.L)
                )
            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("The camera is needed to read the page number.", color = Color.White, textAlign = TextAlign.Center)
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) { Text("Allow camera") }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.TopStart).fillMaxWidth().statusBarsPadding().padding(8.dp)
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                }
                Text(
                    "Point at the page number, usually in a top or bottom corner",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.Black.copy(alpha = 0.5f), AppShape.XS)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp)
                ) {
                    AnimatedContent(
                        targetState = found,
                        transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.9f)) togetherWith fadeOut() },
                        label = "found page"
                    ) { page ->
                        if (page == null) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(64.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(12.dp))
                                Text("Looking for a page number…", style = MaterialTheme.typography.titleMedium)
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    if (book.totalPages > 0) "Page $page of ${book.totalPages}" else "Page $page",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                val delta = page - book.currentPage
                                Text(
                                    when {
                                        delta > 0 -> "$delta ${if (delta == 1) "page" else "pages"} since your bookmark"
                                        delta == 0 -> "Same as your bookmark"
                                        else -> "${-delta} before your bookmark"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    val others = candidates.map { it.page }.filter { it != found }.take(3)
                    if (others.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Not it?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            others.forEach { page ->
                                TextButton(onClick = { haptics.tick(); found = page }) { Text("p. $page") }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        enabled = found != null,
                        onClick = { found?.let { haptics.confirm(); onPage(it) } },
                        shape = AppShape.M,
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(found?.let { "Use page $it" } ?: "Use this page", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
