package com.example.booktracker.app.camera

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

/**
 * "Clean page" capture through Google's document scanner: it finds the page
 * corners, flattens the perspective and offers its cleaning filters (shadows,
 * fingers, stains) before handing back crisp JPEGs. The scanner UI and model
 * come from Play services, so nothing ships in the APK and no camera
 * permission is needed. Returns a function that opens the scanner.
 */
@Composable
fun rememberPageScanner(
    pageLimit: Int,
    onScanned: (List<Uri>) -> Unit,
    onError: (String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scanned by rememberUpdatedState(onScanned)
    val failed by rememberUpdatedState(onError)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val pages = GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.pages.orEmpty()
        scanned(pages.map { it.imageUri })
    }
    val client = remember(pageLimit) {
        GmsDocumentScanning.getClient(
            GmsDocumentScannerOptions.Builder()
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .setGalleryImportAllowed(true)
                .setPageLimit(pageLimit.coerceAtLeast(1))
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .build()
        )
    }
    return remember(client) {
        {
            val activity = context.findActivity()
            if (activity == null) {
                failed("The page scanner couldn't open here.")
            } else {
                client.getStartScanIntent(activity)
                    .addOnSuccessListener { sender -> launcher.launch(IntentSenderRequest.Builder(sender).build()) }
                    .addOnFailureListener { failed("The page scanner needs Google Play services. Try the camera instead.") }
            }
        }
    }
}

/** The activity behind a Compose context, which may be wrapped (themes, locales). */
internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
