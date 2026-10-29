package com.example.booktracker.app.data.local

import com.example.booktracker.shared.models.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private val gson = Gson()

fun SessionEntity.toModel(): Session = Session(
    id = id,
    bookId = bookId,
    startTime = startTime,
    endTime = endTime,
    durationSeconds = durationSeconds,
    startPage = startPage,
    endPage = endPage,
    pagesRead = pagesRead,
    environmentTag = environmentTag,
    zoneId = zoneId.orEmpty(),
    focus = focus ?: 0
)

fun Session.toEntity(): SessionEntity = SessionEntity(
    id = id,
    bookId = bookId,
    startTime = startTime,
    endTime = endTime,
    durationSeconds = durationSeconds,
    startPage = startPage,
    endPage = endPage,
    pagesRead = pagesRead,
    environmentTag = environmentTag,
    zoneId = zoneId.takeIf { it.isNotBlank() },
    focus = focus.takeIf { it > 0 }
)

fun BookEntity.toModel(): Book = Book(
    id = id,
    title = title,
    isbn = isbn,
    coverUrl = coverUrl,
    totalPages = totalPages,
    currentPage = currentPage,
    language = language,
    creators = gson.fromJson(creators, object : TypeToken<List<Creator>>() {}.type) ?: emptyList(),
    publication = publication?.let { gson.fromJson(it, Publication::class.java) },
    format = runCatching { BookFormat.valueOf(format) }.getOrDefault(BookFormat.Paperback),
    progressUnit = runCatching { ProgressUnit.valueOf(progressUnit) }.getOrDefault(ProgressUnit.Page),
    description = description,
    seriesInfo = seriesInfo?.let { gson.fromJson(it, SeriesInfo::class.java) },
    classification = gson.fromJson(classification, Classification::class.java) ?: Classification(emptyList(), emptyList()),
    status = status,
    rating = gson.fromJson(rating, object : TypeToken<Map<String, Float>>() {}.type) ?: emptyMap(),
    dnfData = dnfData?.let { gson.fromJson(it, DnfData::class.java) },
    purchaseLog = gson.fromJson(purchaseLog, object : TypeToken<List<PurchaseLog>>() {}.type) ?: emptyList(),
    loanRecord = gson.fromJson(loanRecord, object : TypeToken<List<LoanRecord>>() {}.type) ?: emptyList(),
    dateAdded = dateAdded,
    lastUpdated = lastUpdated,
    isFavorite = isFavorite,
    readCount = readCount,
    plannedDate = plannedDate,
    review = review,
    finishedAt = finishedAt,
    // MIGRATION_9_10 already marked purchased books Owned, so null here means unset.
    ownership = Ownership.fromStored(ownership, hasPurchase = false),
    finishBy = finishBy,
    readStartedAt = readStartedAt,
    audioMinutes = audioMinutes ?: 0,
    audioSpeed = audioSpeed ?: 1f,
    wish = wish?.let { runCatching { gson.fromJson(it, WishInfo::class.java) }.getOrNull() }?.withDefaults()
)

/** Gson skips Kotlin defaults, so a field missing from the JSON arrives as null; put the defaults back. */
@Suppress("SENSELESS_COMPARISON", "USELESS_ELVIS")
private fun WishInfo.withDefaults(): WishInfo = copy(
    priority = priority ?: WishPriority.WANT,
    currency = currency ?: "",
    store = store ?: "",
    recommendedBy = recommendedBy ?: "",
    note = note ?: ""
)

fun Book.toEntity(): BookEntity = BookEntity(
    id = id,
    title = title,
    isbn = isbn,
    coverUrl = coverUrl,
    totalPages = totalPages,
    currentPage = currentPage,
    language = language,
    creators = gson.toJson(creators),
    publication = publication?.let { gson.toJson(it) },
    format = format.name,
    progressUnit = progressUnit.name,
    description = description,
    seriesInfo = seriesInfo?.let { gson.toJson(it) },
    classification = gson.toJson(classification),
    status = status,
    rating = gson.toJson(rating),
    dnfData = dnfData?.let { gson.toJson(it) },
    purchaseLog = gson.toJson(purchaseLog),
    loanRecord = gson.toJson(loanRecord),
    dateAdded = dateAdded,
    lastUpdated = lastUpdated,
    isFavorite = isFavorite,
    readCount = readCount,
    plannedDate = plannedDate,
    review = review,
    finishedAt = finishedAt,
    ownership = ownership.name,
    finishBy = finishBy,
    readStartedAt = readStartedAt,
    audioMinutes = audioMinutes.takeIf { it > 0 },
    audioSpeed = audioSpeed.takeIf { audioMinutes > 0 },
    wish = wish?.let { gson.toJson(it) }
)

fun MarginNoteEntity.toModel(): MarginNote = MarginNote(
    id = id,
    bookId = bookId,
    timestamp = timestamp,
    pageNumber = pageNumber,
    content = content,
    type = runCatching { NoteType.valueOf(type) }.getOrDefault(NoteType.BOOK_CONTENT),
    isFavorite = isFavorite,
    imageUris = images?.let { gson.fromJson<List<String>>(it, object : TypeToken<List<String>>() {}.type) }.orEmpty()
)

fun MarginNote.toEntity(): MarginNoteEntity = MarginNoteEntity(
    id = id,
    bookId = bookId,
    timestamp = timestamp,
    pageNumber = pageNumber,
    content = content,
    type = type.name,
    isFavorite = isFavorite,
    images = imageUris.takeIf { it.isNotEmpty() }?.let { gson.toJson(it) }
)

fun ReviewStateEntity.toModel(): ReviewState = ReviewState(
    itemId = itemId,
    kind = runCatching { ReviewKind.valueOf(kind) }.getOrDefault(ReviewKind.NOTE),
    dueDay = dueDay,
    intervalDays = intervalDays,
    reps = reps,
    lapses = lapses,
    lastReviewedAt = lastReviewedAt
)

fun ReviewState.toEntity(): ReviewStateEntity = ReviewStateEntity(
    itemId = itemId,
    kind = kind.name,
    dueDay = dueDay,
    intervalDays = intervalDays,
    reps = reps,
    lapses = lapses,
    lastReviewedAt = lastReviewedAt
)

fun VocabWordEntity.toModel(): VocabWord = VocabWord(
    id = id,
    word = word,
    definition = definition,
    bookId = bookId,
    pageNumber = pageNumber,
    context = context,
    addedAt = addedAt
)

fun VocabWord.toEntity(): VocabWordEntity = VocabWordEntity(
    id = id,
    word = word,
    definition = definition,
    bookId = bookId,
    pageNumber = pageNumber,
    context = context,
    addedAt = addedAt
)
