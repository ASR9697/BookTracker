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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.booktracker.app.camera.TextRecognitionAnalyzer
import java.util.concurrent.Executors

/**
 * On-device OCR capture: point the camera at a physical page, freeze the live
 * recognized text, and confirm to hand it back for a margin note. All local
 * (ML Kit Latin text recognition) — no network, no keys.
 */
@Composable
fun OcrCaptureScreen(
    onClose: () -> Unit,
    onTextConfirmed: (String) -> Unit
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

    // The most recent non-blank OCR read; frozen while the confirm sheet is up.
    var liveText by remember { mutableStateOf("") }
    var frozen by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (hasPermission) {
                CameraPreview(
                    analyzer = remember { TextRecognitionAnalyzer { if (!frozen) liveText = it } },
                    modifier = Modifier.fillMaxSize()
                )
                // Capture frame; text seen inside it is what gets recognized.
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(24.dp)
                        .border(
                            width = 2.dp,
                            color = Color.White.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(vertical = 80.dp)
                )
                Text(
                    "Hold steady over the passage you want to keep",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = 32.dp, vertical = 24.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                )

                // Live preview of the current read + capture button.
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (liveText.isNotBlank()) {
                        Text(
                            liveText.take(160) + if (liveText.length > 160) "…" else "",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            maxLines = 4,
                            modifier = Modifier
                                .background(
                                    Color.Black.copy(alpha = 0.6f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        )
                    }
                    Button(
                        enabled = liveText.isNotBlank(),
                        onClick = { frozen = true }
                    ) {
                        Text("Capture text")
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Camera permission is needed to scan text from a page.",
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant permission")
                    }
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(4.dp)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close scanner", tint = Color.White)
            }
        }
    }

    if (frozen) {
        OcrConfirmSheet(
            initialText = liveText,
            onDismiss = { frozen = false },
            onConfirm = { edited ->
                onTextConfirmed(edited)
            }
        )
    }
}

// OCR is imperfect (line breaks, stray glyphs), so the captured text is always
// editable before it becomes a note rather than saved blindly.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OcrConfirmSheet(
    initialText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember { mutableStateOf(initialText) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Captured text", style = MaterialTheme.typography.titleLarge)
            Text(
                "Tidy it up if the scan added stray characters, then save it as a note.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Note text") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                enabled = text.isNotBlank(),
                onClick = { onConfirm(text.trim()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Save as note")
            }
        }
    }
}


