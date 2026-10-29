package com.example.booktracker.app.data

import android.content.Context
import android.net.Uri
import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookFormat
import com.example.booktracker.shared.models.Classification
import com.example.booktracker.shared.models.Creator
import com.example.booktracker.shared.models.CreatorRole
import com.example.booktracker.shared.models.DnfData
import com.example.booktracker.shared.models.LoanRecord
import com.example.booktracker.shared.models.MarginNote
import com.example.booktracker.shared.models.NoteType
import com.example.booktracker.shared.models.Ownership
import com.example.booktracker.shared.models.ProgressUnit
import com.example.booktracker.shared.models.Publication
import com.example.booktracker.shared.models.PurchaseLog
import com.example.booktracker.shared.models.ReviewKind
import com.example.booktracker.shared.models.ReviewState
import com.example.booktracker.shared.models.SeriesInfo
import com.example.booktracker.shared.models.Session
import com.example.booktracker.shared.models.VocabWord
import com.example.booktracker.shared.models.WishInfo
import com.example.booktracker.shared.models.WishPriority
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * User-managed local backups: the whole Room DB plus settings as one
 * versioned JSON document inside a .zip, written wherever the user points the
 * system file picker. No cloud, no accounts — restoring on a fresh install (or
 * another device) upserts by id, so it merges rather than duplicates.
 *
 * Format 2 carries every book field (v1 dropped ISBN, date added, publisher,
 * series, purchases and creator roles) and packs covers photographed on-device
 * into the zip so they survive a move to a new phone. Format 1 files still load.
 * Vocabulary words and review progress ride along as optional arrays; files
 * written before them simply have none.
 */
object BackupEngine {

    private const val FORMAT_VERSION = 2
    private const val ENTRY_NAME = "booktracker-backup.json"
    private const val COVER_DIR = "covers/"
    private const val NOTE_IMAGE_DIR = NoteImageStore.DIR + "/"

    data class Parsed(
        val snapshot: LibrarySnapshot,
        val settings: BackupSettings
    )

    /** Goal, day-boundary and Home layout preferences; null fields were absent from the file. */
    data class BackupSettings(
        val dailyGoal: Int? = null,
        val yearlyGoal: Int? = null,
        val monthlyGoalPages: Int? = null,
        val monthlyGoalBooks: Int? = null,
        val dayStartsAtHour: Int? = null,
        /** [HomeLayout.encode] form, decoded leniently on restore. */
        val homeLayout: String? = null,
        /** A book-cover avatar only; a photo file is device-local and not carried over. */
        val avatar: String? = null
    )

    suspend fun exportToUri(
        context: Context,
        uri: Uri,
        snapshot: LibrarySnapshot,
        settings: BackupSettings
    ) = withContext(Dispatchers.IO) {
        // Local cover files are stored by name inside the zip and referenced as
        // "covers/<name>" so restore can relocate them into the new app sandbox.
        val localCovers = snapshot.books
            .mapNotNull { book -> localCoverFile(book.coverUrl)?.let { book.id to it } }
            .filter { (_, file) -> file.isFile }
            .toMap()
        // Note photos travel the same way, as "note_images/<name>".
        val noteImages = snapshot.notes
            .flatMap { it.imageUris }
            .mapNotNull { NoteImageStore.localFile(it) }
            .filter { it.isFile }
            .distinctBy { it.name }

        val json = encodeDocument(snapshot, settings)
        val output = context.contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException("Cannot open backup destination")
        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(ENTRY_NAME))
            zip.write(json.toString(2).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            localCovers.values.forEach { file ->
                zip.putNextEntry(ZipEntry(COVER_DIR + file.name))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
            noteImages.forEach { file ->
                zip.putNextEntry(ZipEntry(NOTE_IMAGE_DIR + file.name))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    suspend fun importFromUri(context: Context, uri: Uri): Parsed =
        withContext(Dispatchers.IO) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("Cannot open backup file")
            val coverDir = File(context.filesDir, "covers").apply { mkdirs() }
            val restoredCovers = mutableMapOf<String, String>()
            val noteImageDir = NoteImageStore.dir(context)
            val restoredNoteImages = mutableMapOf<String, String>()
            var jsonText: String? = null

            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    when {
                        name.endsWith(".json") && jsonText == null ->
                            jsonText = zip.readBytes().toString(Charsets.UTF_8)
                        name.startsWith(COVER_DIR) && !entry.isDirectory -> {
                            // Only the base name is trusted, so a crafted entry can't
                            // escape the covers directory.
                            val safeName = File(name).name
                            if (safeName.isNotBlank()) {
                                val target = File(coverDir, safeName)
                                target.outputStream().use { zip.copyTo(it) }
                                restoredCovers[COVER_DIR + safeName] = "file://${target.absolutePath}"
                            }
                        }
                        name.startsWith(NOTE_IMAGE_DIR) && !entry.isDirectory -> {
                            val safeName = File(name).name
                            if (safeName.isNotBlank()) {
                                val target = File(noteImageDir, safeName)
                                target.outputStream().use { zip.copyTo(it) }
                                restoredNoteImages[NOTE_IMAGE_DIR + safeName] = "file://${target.absolutePath}"
                            }
                        }
                    }
                    entry = zip.nextEntry
                }
            }

            // Accepts either the .zip we write or a bare .json the user extracted.
            decodeDocument(jsonText ?: bytes.toString(Charsets.UTF_8)) { entry ->
                restoredCovers[entry] ?: restoredNoteImages[entry]
            }
        }

    /**
     * The library as the versioned JSON document both backups and Drive sync use.
     * Local cover and note-photo files appear as "covers/<name>" and
     * "note_images/<name>" entries, so the files can travel next to it.
     */
    internal fun encodeDocument(snapshot: LibrarySnapshot, settings: BackupSettings): JSONObject = JSONObject().apply {
        put("version", FORMAT_VERSION)
        put("exportedAt", System.currentTimeMillis())
        put("settings", JSONObject().apply {
            settings.dailyGoal?.let { put("dailyGoalPages", it) }
            settings.yearlyGoal?.let { put("yearlyGoalBooks", it) }
            settings.monthlyGoalPages?.let { put("monthlyGoalPages", it) }
            settings.monthlyGoalBooks?.let { put("monthlyGoalBooks", it) }
            settings.dayStartsAtHour?.let { put("dayStartsAtHour", it) }
            settings.homeLayout?.let { put("homeLayout", it) }
            settings.avatar?.takeIf { it.startsWith(AvatarStore.COVER_PREFIX) }?.let { put("avatar", it) }
        })
        put("books", JSONArray(snapshot.books.map { book ->
            book.toJson(coverEntry = coverEntry(book))
        }))
        put("sessions", JSONArray(snapshot.sessions.map { it.toJson() }))
        put("notes", JSONArray(snapshot.notes.map { note -> note.toJson(imageEntries = imageEntries(note)) }))
        put("words", JSONArray(snapshot.words.map { it.toJson() }))
        put("reviews", JSONArray(snapshot.reviews.map { it.toJson() }))
    }

    /** "covers/<name>" for a cover photographed on this phone, if its file is still there. */
    internal fun coverEntry(book: Book): String? =
        localCoverFile(book.coverUrl)?.takeIf { it.isFile }?.let { COVER_DIR + it.name }

    /** "note_images/<name>" for each of the note's photos whose file is still there. */
    internal fun imageEntries(note: MarginNote): List<String> = note.imageUris.mapNotNull { uri ->
        NoteImageStore.localFile(uri)?.takeIf { it.isFile }?.let { NOTE_IMAGE_DIR + it.name }
    }

    /** Where an entry's file lives (or will live) on this phone, as a "file://" URI; null for anything else. */
    internal fun localUriForEntry(context: Context, entry: String): String? {
        val name = File(entry).name.takeIf { it.isNotBlank() } ?: return null
        return when {
            entry.startsWith(COVER_DIR) -> "file://${File(File(context.filesDir, "covers"), name).absolutePath}"
            entry.startsWith(NOTE_IMAGE_DIR) -> "file://${File(NoteImageStore.dir(context), name).absolutePath}"
            else -> null
        }
    }

    /** The entry for a cover or note photo stored on this phone; null for web URLs and anything else. */
    internal fun entryForLocalUri(uri: String): String? {
        NoteImageStore.localFile(uri)?.let { return NOTE_IMAGE_DIR + it.name }
        return localCoverFile(uri)?.takeIf { it.parentFile?.name == "covers" }?.let { COVER_DIR + it.name }
    }

    /**
     * Reads a document written by [encodeDocument]. [resolveEntry] maps a file
     * entry to a local URI; a cover with none keeps its original URL, a note
     * photo with none is dropped.
     */
    internal fun decodeDocument(text: String, resolveEntry: (String) -> String?): Parsed {
        val root = JSONObject(text)
        val settings = root.optJSONObject("settings")
        return Parsed(
            snapshot = LibrarySnapshot(
                books = root.optJSONArray("books").mapObjects { it.toBook(resolveEntry) },
                sessions = root.optJSONArray("sessions").mapObjects { it.toSession() },
                notes = root.optJSONArray("notes").mapObjects { it.toNote(resolveEntry) },
                words = root.optJSONArray("words").mapObjects { it.toWord() },
                reviews = root.optJSONArray("reviews").mapObjects { it.toReviewState() }
            ),
            settings = BackupSettings(
                dailyGoal = settings?.optIntOrNull("dailyGoalPages"),
                yearlyGoal = settings?.optIntOrNull("yearlyGoalBooks"),
                monthlyGoalPages = settings?.optIntOrNull("monthlyGoalPages"),
                monthlyGoalBooks = settings?.optIntOrNull("monthlyGoalBooks"),
                dayStartsAtHour = settings?.optIntOrNull("dayStartsAtHour"),
                homeLayout = settings?.optString("homeLayout")?.takeIf { it.isNotBlank() },
                avatar = settings?.optString("avatar")?.takeIf { it.startsWith(AvatarStore.COVER_PREFIX) }
            )
        )
    }

    private fun localCoverFile(coverUrl: String): File? =
        coverUrl.takeIf { it.startsWith("file://") }?.let { File(Uri.parse(it).path ?: return null) }

    private fun Book.toJson(coverEntry: String?): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        isbn?.let { put("isbn", it) }
        // v1 readers only understand "authors"; keep it alongside the full creator list.
        put("authors", JSONArray(authors))
        put("creators", JSONArray(creators.map { creator ->
            JSONObject().put("role", creator.role.name).put("name", creator.name)
        }))
        put("coverUrl", coverEntry ?: coverUrl)
        put("format", format.name)
        put("progressUnit", progressUnit.name)
        put("language", language)
        put("totalPages", totalPages)
        put("currentPage", currentPage)
        put("status", status)
        dnfData?.let {
            put("dnfPercentage", it.abandonedPercentage.toDouble())
            put("dnfReason", it.reason)
        }
        put("dateAdded", dateAdded)
        put("lastUpdated", lastUpdated)
        finishedAt?.let { put("finishedAt", it) }
        put("rating", JSONObject(rating.mapValues { it.value.toDouble() }))
        put("description", description)
        put("genres", JSONArray(genres))
        put("collections", JSONArray(classification.collections))
        publication?.let {
            put("publisher", it.publisher)
            put("publishedDate", it.date)
        }
        seriesInfo?.let {
            put("series", JSONObject().apply {
                put("isPartOfSeries", it.isPartOfSeries)
                put("name", it.seriesName)
                put("position", it.position)
            })
        }
        put("isFavorite", isFavorite)
        put("readCount", readCount)
        put("ownership", ownership.name)
        plannedDate?.let { put("plannedDate", it) }
        review?.let { put("review", it) }
        finishBy?.let { put("finishBy", it) }
        readStartedAt?.let { put("readStartedAt", it) }
        if (audioMinutes > 0) {
            put("audioMinutes", audioMinutes)
            put("audioSpeed", audioSpeed.toDouble())
        }
        if (purchaseLog.isNotEmpty()) {
            put("purchaseLog", JSONArray(purchaseLog.map { it.toJson() }))
        }
        if (loanRecord.isNotEmpty()) {
            put("loanRecord", JSONArray(loanRecord.map { it.toJson() }))
        }
        wish?.let { put("wish", it.toJson()) }
    }

    private fun WishInfo.toJson(): JSONObject = JSONObject().apply {
        put("priority", priority.name)
        price?.let { put("price", it) }
        put("currency", currency)
        put("store", store)
        put("recommendedBy", recommendedBy)
        put("note", note)
        releaseDate?.let { put("releaseDate", it) }
        put("addedAt", addedAt)
    }

    private fun JSONObject.toWish(): WishInfo = WishInfo(
        priority = runCatching { WishPriority.valueOf(optString("priority")) }.getOrDefault(WishPriority.WANT),
        price = if (has("price")) optDouble("price") else null,
        currency = optString("currency"),
        store = optString("store"),
        recommendedBy = optString("recommendedBy"),
        note = optString("note"),
        releaseDate = if (has("releaseDate")) optLong("releaseDate") else null,
        addedAt = optLong("addedAt")
    )

    private fun PurchaseLog.toJson(): JSONObject = JSONObject().apply {
        put("date", date)
        put("vendor", vendor)
        put("price", price)
        put("currency", currency)
        put("memo", memo)
    }

    private fun JSONObject.toPurchaseLog(): PurchaseLog = PurchaseLog(
        date = optLong("date"),
        vendor = optString("vendor"),
        price = optDouble("price", 0.0),
        currency = optString("currency"),
        memo = optString("memo")
    )

    private fun LoanRecord.toJson(): JSONObject = JSONObject().apply {
        put("loanDate", loanDate)
        put("dueDate", dueDate)
        put("lender", lender)
        put("memo", memo)
        returnDate?.let { put("returnDate", it) }
        put("isLentOut", isLentOut)
    }

    private fun JSONObject.toLoanRecord(): LoanRecord = LoanRecord(
        loanDate = optLong("loanDate"),
        dueDate = optLong("dueDate"),
        lender = optString("lender"),
        memo = optString("memo"),
        returnDate = if (has("returnDate")) optLong("returnDate") else null,
        isLentOut = optBoolean("isLentOut", false)
    )

    private fun JSONObject.toBook(resolveEntry: (String) -> String?): Book {
        val creators = optJSONArray("creators")?.let { array ->
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                val name = obj.optString("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                Creator(
                    role = runCatching { CreatorRole.valueOf(obj.optString("role")) }
                        .getOrDefault(CreatorRole.Author),
                    name = name
                )
            }
        } ?: optJSONArray("authors").mapStrings().map { Creator(CreatorRole.Author, it) }

        val cover = optString("coverUrl").let { resolveEntry(it) ?: it }
        val series = optJSONObject("series")?.let {
            SeriesInfo(
                isPartOfSeries = it.optBoolean("isPartOfSeries", true),
                seriesName = it.optString("name"),
                position = it.optInt("position")
            )
        }

        return Book(
            id = getString("id"),
            title = optString("title"),
            isbn = optStringOrNull("isbn"),
            creators = creators,
            coverUrl = cover,
            format = runCatching { BookFormat.valueOf(optString("format")) }.getOrDefault(BookFormat.Paperback),
            progressUnit = runCatching { ProgressUnit.valueOf(optString("progressUnit")) }
                .getOrDefault(ProgressUnit.Page),
            language = optString("language"),
            totalPages = optInt("totalPages"),
            currentPage = optInt("currentPage"),
            status = optString("status"),
            dnfData = if (has("dnfPercentage")) {
                DnfData(getDouble("dnfPercentage").toFloat(), optString("dnfReason"))
            } else null,
            dateAdded = optLong("dateAdded"),
            lastUpdated = optLong("lastUpdated"),
            finishedAt = if (has("finishedAt")) optLong("finishedAt") else null,
            rating = optJSONObject("rating")?.let { obj ->
                obj.keys().asSequence().associateWith { obj.getDouble(it).toFloat() }
            } ?: emptyMap(),
            description = optString("description"),
            classification = Classification(
                optJSONArray("collections").mapStrings(),
                optJSONArray("genres").mapStrings()
            ),
            publication = Publication(optString("publisher"), optString("publishedDate")),
            seriesInfo = series,
            isFavorite = optBoolean("isFavorite", false),
            readCount = optInt("readCount", 0),
            plannedDate = if (has("plannedDate")) optLong("plannedDate") else null,
            review = if (has("review")) optString("review") else null,
            finishBy = if (has("finishBy")) optLong("finishBy") else null,
            readStartedAt = if (has("readStartedAt")) optLong("readStartedAt") else null,
            audioMinutes = optInt("audioMinutes", 0),
            audioSpeed = optDouble("audioSpeed", 1.0).toFloat(),
            purchaseLog = optJSONArray("purchaseLog").mapObjects { it.toPurchaseLog() },
            loanRecord = optJSONArray("loanRecord").mapObjects { it.toLoanRecord() },
            wish = optJSONObject("wish")?.toWish(),
            // Backups from before ownership existed: a logged purchase means Owned.
            ownership = Ownership.fromStored(
                optString("ownership").takeIf { it.isNotBlank() },
                hasPurchase = optJSONArray("purchaseLog")?.length()?.let { it > 0 } == true
            )
        )
    }

    private fun Session.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("bookId", bookId)
        put("startTime", startTime)
        put("endTime", endTime)
        put("startPage", startPage)
        put("endPage", endPage)
        put("pagesRead", pagesRead)
        put("durationSeconds", durationSeconds)
        put("environmentTag", environmentTag)
        if (zoneId.isNotBlank()) put("zoneId", zoneId)
        if (focus > 0) put("focus", focus)
    }

    private fun JSONObject.toSession(): Session = Session(
        id = getString("id"),
        bookId = optString("bookId"),
        startTime = optLong("startTime"),
        endTime = optLong("endTime"),
        startPage = optInt("startPage"),
        endPage = optInt("endPage"),
        pagesRead = optInt("pagesRead"),
        durationSeconds = optInt("durationSeconds"),
        environmentTag = optString("environmentTag"),
        zoneId = optString("zoneId"),
        focus = optInt("focus")
    )

    private fun MarginNote.toJson(imageEntries: List<String>): JSONObject = JSONObject().apply {
        put("id", id)
        put("bookId", bookId)
        put("timestamp", timestamp)
        put("pageNumber", pageNumber)
        put("content", content)
        put("type", type.name)
        put("markdownContent", markdownContent)
        put("isFavorite", isFavorite)
        if (imageEntries.isNotEmpty()) put("images", JSONArray(imageEntries))
    }

    /** Photos whose files didn't make it into the zip are dropped rather than left dangling. */
    private fun JSONObject.toNote(resolveEntry: (String) -> String?): MarginNote = MarginNote(
        id = getString("id"),
        bookId = optString("bookId"),
        timestamp = optLong("timestamp"),
        pageNumber = optInt("pageNumber"),
        content = optString("content"),
        type = runCatching { NoteType.valueOf(optString("type")) }.getOrDefault(NoteType.BOOK_CONTENT),
        markdownContent = optString("markdownContent"),
        isFavorite = optBoolean("isFavorite"),
        imageUris = optJSONArray("images")
            ?.let { arr -> (0 until arr.length()).mapNotNull { resolveEntry(arr.optString(it)) } }
            .orEmpty()
    )

    private fun VocabWord.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("word", word)
        put("definition", definition)
        bookId?.let { put("bookId", it) }
        put("pageNumber", pageNumber)
        put("context", context)
        put("addedAt", addedAt)
    }

    private fun JSONObject.toWord(): VocabWord = VocabWord(
        id = getString("id"),
        word = optString("word"),
        definition = optString("definition"),
        bookId = optStringOrNull("bookId"),
        pageNumber = optInt("pageNumber"),
        context = optString("context"),
        addedAt = optLong("addedAt")
    )

    private fun ReviewState.toJson(): JSONObject = JSONObject().apply {
        put("itemId", itemId)
        put("kind", kind.name)
        put("dueDay", dueDay)
        put("intervalDays", intervalDays)
        put("reps", reps)
        put("lapses", lapses)
        put("lastReviewedAt", lastReviewedAt)
    }

    private fun JSONObject.toReviewState(): ReviewState = ReviewState(
        itemId = getString("itemId"),
        kind = runCatching { ReviewKind.valueOf(optString("kind")) }.getOrDefault(ReviewKind.NOTE),
        dueDay = optLong("dueDay"),
        intervalDays = optInt("intervalDays"),
        reps = optInt("reps"),
        lapses = optInt("lapses"),
        lastReviewedAt = optLong("lastReviewedAt")
    )

    private fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        return (0 until length()).map { transform(getJSONObject(it)) }
    }

    private fun JSONArray?.mapStrings(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).map { getString(it) }
    }

    private fun JSONObject.optIntOrNull(key: String): Int? =
        if (has(key)) getInt(key) else null

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null
}
