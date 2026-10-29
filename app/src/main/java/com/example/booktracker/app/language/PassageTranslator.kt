package com.example.booktracker.app.language

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await
import java.util.Locale

/**
 * On-device translation with ML Kit: the language of a passage is guessed
 * locally, and each language pair downloads a small model (~30 MB) the first
 * time it is used. After that it works offline. Free, keyless, nothing leaves
 * the phone.
 */
object PassageTranslator {

    /** ML Kit code of every language it can translate, by display name. */
    val languages: List<String> by lazy {
        TranslateLanguage.getAllLanguages().sortedBy { displayName(it) }
    }

    /** The phone's language when ML Kit can translate into it, else English. */
    fun deviceLanguage(): String =
        TranslateLanguage.fromLanguageTag(Locale.getDefault().language) ?: TranslateLanguage.ENGLISH

    fun displayName(code: String): String =
        Locale.forLanguageTag(code).getDisplayLanguage(Locale.getDefault()).ifBlank { code }

    /** ML Kit code of the passage's language, or null when it can't tell or can't translate it. */
    suspend fun identify(text: String): String? = runCatching {
        val client = LanguageIdentification.getClient()
        try {
            client.identifyLanguage(text).await()
                .takeIf { it != "und" }
                ?.let { TranslateLanguage.fromLanguageTag(it) }
        } finally {
            client.close()
        }
    }.getOrNull()

    /** [text] from [source] into [target], downloading the pair's model first if needed. */
    suspend fun translate(text: String, source: String, target: String): String {
        if (source == target) return text
        val translator = Translation.getClient(
            TranslatorOptions.Builder().setSourceLanguage(source).setTargetLanguage(target).build()
        )
        return try {
            translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
            translator.translate(text).await()
        } finally {
            translator.close()
        }
    }
}
