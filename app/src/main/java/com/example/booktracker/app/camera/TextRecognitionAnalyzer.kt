package com.example.booktracker.app.camera

import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class TextRecognitionAnalyzer(private val onText: (String) -> Unit) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var lastAnalyzedAt = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        // OCR is much heavier than barcode scanning; ~2 frames/sec keeps the
        // preview fluid and is plenty for a page held steady.
        val now = SystemClock.elapsedRealtime()
        if (now - lastAnalyzedAt < 500) {
            imageProxy.close()
            return
        }
        lastAnalyzedAt = now

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text.trim()
                    if (text.isNotEmpty()) onText(text)
                }
                .addOnFailureListener {
                    Log.e("TextRecognitionAnalyzer", "Text recognition failed", it)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
