package com.example.booktracker.app.camera

import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * Live OCR for the page-number scanner: hands back every recognised line with
 * where it sits in the upright frame, so [PageNumberDetector] can tell a lone
 * number in the corner from one in the middle of a sentence.
 */
class PageNumberAnalyzer(private val onLines: (List<PageNumberDetector.Line>) -> Unit) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var lastAnalyzedAt = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val now = SystemClock.elapsedRealtime()
        val mediaImage = imageProxy.image
        if (now - lastAnalyzedAt < 400 || mediaImage == null) {
            imageProxy.close()
            return
        }
        lastAnalyzedAt = now

        val rotation = imageProxy.imageInfo.rotationDegrees
        // Boxes come back in upright coordinates, so swap the sides for a sideways sensor.
        val width = (if (rotation % 180 == 0) imageProxy.width else imageProxy.height).toFloat()
        val height = (if (rotation % 180 == 0) imageProxy.height else imageProxy.width).toFloat()
        recognizer.process(InputImage.fromMediaImage(mediaImage, rotation))
            .addOnSuccessListener { text ->
                val lines = text.textBlocks.flatMap { it.lines }.mapNotNull { line ->
                    val box = line.boundingBox ?: return@mapNotNull null
                    PageNumberDetector.Line(
                        text = line.text,
                        centerY = (box.exactCenterY() / height).coerceIn(0f, 1f),
                        centerX = (box.exactCenterX() / width).coerceIn(0f, 1f)
                    )
                }
                onLines(lines)
            }
            .addOnCompleteListener { imageProxy.close() }
    }
}
