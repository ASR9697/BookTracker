package com.example.booktracker.app.data

import android.content.Context
import com.example.booktracker.shared.models.NoteType
import com.google.gson.Gson

/** Unsaved note text, so closing the app mid-note doesn't lose it. Photos are not kept in drafts. */
data class NoteDraft(val page: String, val text: String, val type: NoteType) {
    companion object {
        private val gson = Gson()

        fun toJson(draft: NoteDraft): String = gson.toJson(draft)

        /** Null for blank or damaged input; a draft with no text is never worth restoring. */
        fun parse(json: String?): NoteDraft? {
            if (json.isNullOrBlank()) return null
            val draft = runCatching { gson.fromJson(json, NoteDraft::class.java) }.getOrNull() ?: return null
            // Gson bypasses Kotlin's non-null guarantees, so check what an old or edited file could leave null.
            @Suppress("SENSELESS_COMPARISON")
            if (draft.text == null || draft.page == null || draft.type == null) return null
            return draft.takeIf { it.text.isNotBlank() }
        }
    }
}

/** One draft per key (a book id), in SharedPreferences: small, synchronous and fine to write as the reader types. */
object NoteDraftStore {

    private const val PREFS = "note_drafts"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(context: Context, key: String): NoteDraft? = NoteDraft.parse(prefs(context).getString(key, null))

    /** Saves the draft, or removes it when there's no text left. */
    fun save(context: Context, key: String, draft: NoteDraft) {
        val editor = prefs(context).edit()
        if (draft.text.isBlank()) editor.remove(key) else editor.putString(key, NoteDraft.toJson(draft))
        editor.apply()
    }

    fun clear(context: Context, key: String) {
        prefs(context).edit().remove(key).apply()
    }
}
