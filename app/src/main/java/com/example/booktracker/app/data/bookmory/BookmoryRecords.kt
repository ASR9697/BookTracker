package com.example.booktracker.app.data.bookmory

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName

/*
 * Bookmory backups (.bookmory) are a zip holding new_bookmory.db, a sembast
 * key-value store on SQLite: one `entry` table of (store, key, JSON value).
 * These records mirror the JSON for the stores we import. Every field is
 * nullable because Gson skips Kotlin defaults and older backups omit fields.
 */

data class BmBook(
    val title: String? = null,
    val author: String? = null,
    val authors: List<String>? = null,
    val image: String? = null,
    val publisher: String? = null,
    val isbn: String? = null,
    @SerializedName("publication_date") val publicationDate: String? = null,
    val language: String? = null,
    val description: String? = null,
    @SerializedName("page_type") val pageType: String? = null,
    @SerializedName("total_page") val totalPage: Double? = null,
    @SerializedName("cur_page") val curPage: Double? = null,
    val wishlist: Boolean? = null,
    @SerializedName("collection_keys") val collectionKeys: List<String>? = null,
    val reads: List<BmRead>? = null,
    val purchases: List<BmPurchase>? = null,
    /** Shape unknown (no sample yet); only its presence is used. */
    val gifts: List<Any>? = null,
    @SerializedName("status_list") val statusList: List<String>? = null,
    @SerializedName("book_type") val bookType: String? = null,
    @SerializedName("last_read_done_date") val lastReadDoneDate: Long? = null,
    @SerializedName("last_give_up_date") val lastGiveUpDate: Long? = null,
    @SerializedName("created_at") val createdAt: Long? = null,
    @SerializedName("updated_at") val updatedAt: Long? = null
)

/** One read-through of a book; a re-read is a second entry with a higher [nth]. */
data class BmRead(
    val nth: Int? = null,
    val start: Long? = null,
    val end: Long? = null,
    val star: Double? = null,
    val comment: String? = null,
    val status: String? = null,
    @SerializedName("page_log_list") val pageLogs: List<BmPageLog>? = null,
    @SerializedName("read_timer_list") val timers: List<BmTimer>? = null
)

/** "Reader was on [page] at [createdAt]": Bookmory's progress history. */
data class BmPageLog(
    val page: Double? = null,
    @SerializedName("created_at") val createdAt: Long? = null,
    /** IANA zone the phone was in, e.g. "Asia/Calcutta". */
    @SerializedName("recorded_tz_name") val zone: String? = null
)

data class BmTimer(
    @SerializedName("read_started_at") val startedAt: Long? = null,
    @SerializedName("elapsed_sec") val elapsedSec: Long? = null,
    @SerializedName("recorded_tz_name") val zone: String? = null
)

data class BmPurchase(
    val from: String? = null,
    val price: Double? = null,
    val memo: String? = null,
    @SerializedName("currency_code") val currencyCode: String? = null,
    @SerializedName("purchased_at") val purchasedAt: Long? = null
)

data class BmNote(
    val bid: String? = null,
    val page: Double? = null,
    @SerializedName("content_quill") val contentQuill: String? = null,
    /** Paths like "bookmory_data/images/notes/note_123.jpg"; the zip keeps them under images/notes/. */
    val images: List<String>? = null,
    val like: Boolean? = null,
    val type: String? = null,
    @SerializedName("created_at") val createdAt: Long? = null,
    @SerializedName("updated_at") val updatedAt: Long? = null
)

data class BmTag(
    val name: String? = null,
    val bids: List<String>? = null
)

data class BmCollection(
    val name: String? = null
)

/** The stores the importer reads, keyed by each record's sembast key. */
data class BookmoryRecords(
    val books: Map<String, BmBook> = emptyMap(),
    val notes: Map<String, BmNote> = emptyMap(),
    val tags: List<BmTag> = emptyList(),
    val collections: Map<String, BmCollection> = emptyMap(),
    /** Bookmory's "Day starts at" hour, from the user settings record. */
    val dayStartHour: Int? = null
) {
    companion object {
        private val gson = Gson()

        /**
         * Builds records from raw `entry` rows. Deleted rows and rows that don't
         * parse are skipped, so one bad record can't sink the whole import.
         */
        fun fromEntries(rows: List<BookmoryEntry>): BookmoryRecords {
            val live = rows.filter { !it.deleted && !it.value.isNullOrBlank() }
            fun <T> store(name: String, type: Class<T>): Map<String, T> = live
                .filter { it.store == name }
                .mapNotNull { row -> runCatching { gson.fromJson(row.value, type) }.getOrNull()?.let { row.key to it } }
                .toMap()
            return BookmoryRecords(
                books = store("books", BmBook::class.java),
                notes = store("notes", BmNote::class.java),
                tags = store("tags", BmTag::class.java).values.toList(),
                collections = store("collections", BmCollection::class.java),
                dayStartHour = live.filter { it.store == "_main" }.firstNotNullOfOrNull { row ->
                    runCatching {
                        JsonParser.parseString(row.value).asJsonObject.get("day_start_hour")?.asInt
                    }.getOrNull()
                }?.takeIf { it in 0..23 }
            )
        }

        /** Plain text from a Quill delta (a JSON array of insert ops); embeds are dropped. */
        fun quillToText(quill: String?): String {
            if (quill.isNullOrBlank()) return ""
            val ops = runCatching { JsonParser.parseString(quill) }.getOrNull() ?: return quill.trim()
            if (!ops.isJsonArray) return quill.trim()
            return buildString {
                ops.asJsonArray.forEach { op ->
                    val insert = op.takeIf { it.isJsonObject }?.asJsonObject?.get("insert")
                    if (insert != null && insert.isJsonPrimitive && insert.asJsonPrimitive.isString) append(insert.asString)
                }
            }.trim()
        }
    }
}

/** One row of Bookmory's `entry` table. Keys may be text or integers; both become strings. */
data class BookmoryEntry(
    val store: String,
    val key: String,
    val value: String?,
    val deleted: Boolean = false
)
