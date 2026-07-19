package com.example.booktracker.app.hardware

import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import kotlinx.coroutines.tasks.await

/** One captured stroke point in the scratchpad's local coordinate space. */
data class StylusPoint(val x: Float, val y: Float, val t: Long)

/**
 * On-device handwriting recognition for the stylus scratchpad — ML Kit Digital
 * Ink, keyless and free (downloads a small model on first use; no network after).
 */
class HardwareIntegrations {

    /**
     * Recognizes [strokes] as text on-device. Downloads the en-US ink model on
     * first use, then returns the top candidate (null if nothing is recognized).
     */
    suspend fun recognizeHandwriting(strokes: List<List<StylusPoint>>): String? {
        if (strokes.all { it.isEmpty() }) return null
        val modelId = DigitalInkRecognitionModelIdentifier.fromLanguageTag("en-US") ?: return null
        val model = DigitalInkRecognitionModel.builder(modelId).build()
        val manager = RemoteModelManager.getInstance()
        if (!manager.isModelDownloaded(model).await()) {
            manager.download(model, DownloadConditions.Builder().build()).await()
        }
        val recognizer = DigitalInkRecognition.getClient(
            DigitalInkRecognizerOptions.builder(model).build()
        )
        val inkBuilder = Ink.builder()
        for (stroke in strokes) {
            if (stroke.isEmpty()) continue
            val strokeBuilder = Ink.Stroke.builder()
            for (p in stroke) strokeBuilder.addPoint(Ink.Point.create(p.x, p.y, p.t))
            inkBuilder.addStroke(strokeBuilder.build())
        }
        return try {
            recognizer.recognize(inkBuilder.build()).await().candidates.firstOrNull()?.text
        } finally {
            recognizer.close()
        }
    }
}
