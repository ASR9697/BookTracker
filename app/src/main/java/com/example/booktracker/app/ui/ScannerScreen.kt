package com.example.booktracker.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.booktracker.app.camera.BarcodeAnalyzer
import com.example.booktracker.app.data.remote.GoogleBooksClient
import com.example.booktracker.app.data.remote.ScannedBook
import java.util.concurrent.Executors

private sealed interface ScanState {
    data object Scanning : ScanState
    data class LookingUp(val isbn: String) : ScanState
    data class Found(val book: ScannedBook) : ScanState
    data class NotFound(val isbn: String) : ScanState
    data class LookupError(val isbn: String) : ScanState
}

@Composable
fun ScannerScreen(
    onClose: () -> Unit,
    onBookConfirmed: (ScannedBook) -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasPermission = it }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    var state by remember { mutableStateOf<ScanState>(ScanState.Scanning) }

    LaunchedEffect(state) {
        val lookingUp = state as? ScanState.LookingUp ?: return@LaunchedEffect
        state = try {
            val book = GoogleBooksClient.lookup(lookingUp.isbn)
            if (book != null) ScanState.Found(book) else ScanState.NotFound(lookingUp.isbn)
        } catch (e: Exception) {
            ScanState.LookupError(lookingUp.isbn)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (hasPermission) {
                CameraPreview(
                    onIsbnScanned = { isbn ->
                        if (state is ScanState.Scanning) state = ScanState.LookingUp(isbn)
                    },
                    modifier = Modifier.fillMaxSize()
                )
                Text(
                    "Point the camera at the barcode on the back cover",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 32.dp, vertical = 48.dp)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(8.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Camera permission is needed to scan book barcodes.",
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant permission")
                    }
                }
            }

            if (state is ScanState.LookingUp) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            TextButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                Text("Close", color = Color.White)
            }
        }
    }

    when (val current = state) {
        is ScanState.Found -> {
            val book = current.book
            AlertDialog(
                onDismissRequest = { state = ScanState.Scanning },
                title = { Text(book.title) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (book.authors.isNotEmpty()) {
                            Text(book.authors.joinToString(", "))
                        }
                        Text(
                            if (book.pageCount > 0) "${book.pageCount} pages"
                            else "Page count unknown",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text("ISBN ${book.isbn}", style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { onBookConfirmed(book) }) { Text("Add to Backlog") }
                },
                dismissButton = {
                    TextButton(onClick = { state = ScanState.Scanning }) { Text("Rescan") }
                }
            )
        }
        is ScanState.NotFound -> {
            AlertDialog(
                onDismissRequest = { state = ScanState.Scanning },
                title = { Text("No match found") },
                text = { Text("Google Books has no entry for ISBN ${current.isbn}. You can add the book manually from the main screen.") },
                confirmButton = {
                    TextButton(onClick = { state = ScanState.Scanning }) { Text("Rescan") }
                },
                dismissButton = {
                    TextButton(onClick = onClose) { Text("Close") }
                }
            )
        }
        is ScanState.LookupError -> {
            AlertDialog(
                onDismissRequest = { state = ScanState.Scanning },
                title = { Text("Lookup failed") },
                text = { Text("Couldn't reach Google Books. Check your internet connection and try again.") },
                confirmButton = {
                    TextButton(onClick = { state = ScanState.LookingUp(current.isbn) }) { Text("Retry") }
                },
                dismissButton = {
                    TextButton(onClick = onClose) { Text("Close") }
                }
            )
        }
        else -> Unit
    }
}

@Composable
private fun CameraPreview(
    onIsbnScanned: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    val providerHolder = remember { mutableStateOf<ProcessCameraProvider?>(null) }

    // The camera binds to the Activity lifecycle, which outlives this screen —
    // unbind explicitly when the preview leaves composition.
    DisposableEffect(Unit) {
        onDispose {
            providerHolder.value?.unbindAll()
            analyzerExecutor.shutdown()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val provider = providerFuture.get()
                providerHolder.value = provider

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(analyzerExecutor, BarcodeAnalyzer(onIsbnScanned)) }

                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}
