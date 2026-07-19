package com.example.booktracker.wear.journal

import android.content.Intent
import android.speech.RecognizerIntent
import java.util.Locale

/**
 * Builds the system speech-recognition intent used to dictate a margin note on
 * the wrist. Uses the platform recognizer (ACTION_RECOGNIZE_SPEECH), which runs
 * in its own UI and needs no RECORD_AUDIO permission or paid speech service.
 */
object WristDictaphone {

    fun getSpeechToTextIntent(): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Dictate a note")
        }

    /** Pulls the best transcription out of a recognizer result, or null. */
    fun firstResult(data: Intent?): String? =
        data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.takeIf { it.isNotBlank() }
}
