package com.example.booktracker.wear.journal

import android.content.Intent
import android.speech.RecognizerIntent

class WristDictaphone {
    fun getSpeechToTextIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        }
    }
}
