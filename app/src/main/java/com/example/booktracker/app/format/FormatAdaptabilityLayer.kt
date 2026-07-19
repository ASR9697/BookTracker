package com.example.booktracker.app.format

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.booktracker.shared.models.Book
import java.util.Locale

/**
 * Maps a [Book.format] code to reader-facing unit labels, and drives an on-device
 * text-to-speech "read aloud". TTS uses Android's built-in engine — no network,
 * no account, no paid service — which is why the blueprint's cloud "TTS handoff"
 * was replaced with local playback. Progress semantics are unchanged: a unit is a
 * page, chapter, volume, or listening-hour depending on the book's format.
 */
object FormatAdaptabilityLayer {

    /** Ordered for the picker; first entry is the default. Values match [Book.format] codes. */
    val FORMATS = listOf("PAGES", "CHAPTERS", "VOLUMES", "HOURS")

    /** Plural noun for section labels/axes, e.g. "Pages". */
    fun getDisplayUnit(format: String): String = when (format.uppercase()) {
        "PAGES" -> "Pages"
        "VOLUMES" -> "Volumes"
        "CHAPTERS" -> "Chapters"
        "HOURS" -> "Hours"
        else -> "Units"
    }

    fun getDisplayUnit(book: Book): String = getDisplayUnit(book.format)

    /** Compact position prefix, e.g. "p." / "ch." / "vol." / "hr". */
    fun unitAbbrev(format: String): String = when (format.uppercase()) {
        "PAGES" -> "p."
        "VOLUMES" -> "vol."
        "CHAPTERS" -> "ch."
        "HOURS" -> "hr"
        else -> "#"
    }

    /** Lowercase inline count, e.g. "12 pages". */
    fun countLabel(format: String, count: Int): String =
        "$count ${getDisplayUnit(format).lowercase()}"

    /** The blurb spoken by read-aloud: title, author, then the summary. */
    fun readAloudText(book: Book): String = buildString {
        append(book.title)
        if (book.authors.isNotEmpty()) append(", by ${book.authors.joinToString(", ")}")
        append(". ")
        append(
            book.description.ifBlank { "No summary is available for this book." }
        )
    }

    /**
     * Fire-and-forget read-aloud for callers with no Compose lifecycle to manage:
     * spins up a short-lived engine that shuts itself down when playback ends.
     * UI that toggles playback should use [rememberReadAloud] instead.
     */
    fun startTTSHandoff(context: Context, book: Book) {
        val text = readAloudText(book)
        val appContext = context.applicationContext
        var engine: TextToSpeech? = null
        engine = TextToSpeech(appContext) { status ->
            val tts = engine
            if (status == TextToSpeech.SUCCESS && tts != null) {
                tts.language = Locale.getDefault()
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) { tts.shutdown() }
                    @Suppress("OVERRIDE_DEPRECATION")
                    override fun onError(utteranceId: String?) { tts.shutdown() }
                })
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "handoff")
            } else {
                tts?.shutdown()
            }
        }
    }
}

/**
 * A Compose-scoped read-aloud controller. Owns one [TextToSpeech] engine for the
 * lifetime of the composition and exposes a play/stop toggle plus live speaking
 * state. Engine init is async, so a request made before init completes is queued.
 */
class ReadAloudController(private val context: Context) {

    private val speakingState = mutableStateOf(false)
    val isSpeaking: State<Boolean> = speakingState

    private val main = Handler(Looper.getMainLooper())
    private var ready = false
    private var pending: String? = null
    
    private var tts: TextToSpeech? = null

    private fun initTts() {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { engine ->
                    engine.language = Locale.getDefault()
                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) { main.post { speakingState.value = true } }
                        override fun onDone(utteranceId: String?) { main.post { speakingState.value = false } }
                        @Suppress("OVERRIDE_DEPRECATION")
                        override fun onError(utteranceId: String?) { main.post { speakingState.value = false } }
                    })
                }
                ready = true
                pending?.let { speakNow(it); pending = null }
            }
        }
    }

    fun toggle(text: String) {
        if (speakingState.value) {
            stop()
        } else if (ready) {
            speakNow(text)
        } else {
            pending = text
            initTts()
        }
    }

    private fun speakNow(text: String) {
        speakingState.value = true
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    fun stop() {
        tts?.stop()
        speakingState.value = false
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }

    companion object {
        private const val UTTERANCE_ID = "read_aloud"
    }
}

@Composable
fun rememberReadAloud(): ReadAloudController {
    val context = LocalContext.current
    val controller = remember { ReadAloudController(context) }
    DisposableEffect(Unit) { onDispose { controller.shutdown() } }
    return controller
}
