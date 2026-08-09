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
    environmentTag = environmentTag
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
    environmentTag = environmentTag
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
    readCount = readCount
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
    readCount = readCount
)

fun MarginNoteEntity.toModel(): MarginNote = MarginNote(
    id = id,
    bookId = bookId,
    timestamp = timestamp,
    pageNumber = pageNumber,
    content = content,
    type = runCatching { NoteType.valueOf(type) }.getOrDefault(NoteType.BOOK_CONTENT),
    isFavorite = isFavorite
)

fun MarginNote.toEntity(): MarginNoteEntity = MarginNoteEntity(
    id = id,
    bookId = bookId,
    timestamp = timestamp,
    pageNumber = pageNumber,
    content = content,
    type = type.name,
    isFavorite = isFavorite
)
