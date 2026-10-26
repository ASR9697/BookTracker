package com.example.booktracker.app.data.bookmory

import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookFormat
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.Creator
import com.example.booktracker.shared.models.CreatorRole
import com.example.booktracker.shared.models.NoteType
import com.example.booktracker.shared.models.Ownership
import com.example.booktracker.shared.models.RatingAxis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Built from the shapes in a real Bookmory backup (sembast store "books"/"notes"/"tags"). */
class BookmoryMapperTest {

    private val minute = 60_000L
    private val t0 = 1_784_755_000_000L

    private val bookJson = """
        {"title":"Kaizen: The Japanese Method","author":"Sarah Harvey","authors":["Sarah Harvey"],
         "image":"https://m.media-amazon.com/images/I/kaizen.jpg","publisher":"Bluebird","isbn":"9781529034561",
         "publication_date":"2019","language":"English","description":"Small steps.","page_type":"PAGE",
         "total_page":224.0,"cur_page":224.0,"wishlist":false,"collection_keys":["c1"],
         "reads":[{"nth":1,"start":$t0,"end":${t0 + 100 * minute},"star":4.5,"comment":"Lovely","status":"DONE",
           "page_log_list":[{"page":40.0,"created_at":${t0 + 30 * minute}},{"page":40.0,"created_at":${t0 + 40 * minute}},
                            {"page":90.0,"created_at":${t0 + 95 * minute}}],
           "read_timer_list":[{"read_started_at":${t0 + 5 * minute},"elapsed_sec":1500},
                              {"read_started_at":${t0 + 60 * minute},"elapsed_sec":1800},
                              {"read_started_at":${t0 + 300 * minute},"elapsed_sec":600}]}],
         "purchases":[{"from":"Amazon","price":299.0,"memo":"","currency_code":"INR","purchased_at":$t0}],
         "gifts":[],"status_list":["DONE"],"book_type":"hardCover","last_read_done_date":${t0 + 100 * minute},
         "created_at":$t0,"updated_at":${t0 + 100 * minute}}
    """.trimIndent()

    private fun records(extraBooks: List<BookmoryEntry> = emptyList()) = BookmoryRecords.fromEntries(
        listOf(
            BookmoryEntry("books", "k1", bookJson),
            BookmoryEntry(
                "notes", "n1",
                """{"bid":"k1","page":12.0,"content_quill":"[{\"insert\":\"Start small.\\n\"}]","images":["bookmory_data/images/notes/note_1.jpg"],"like":true,"type":"BOOK_CONTENT","created_at":$t0}"""
            ),
            BookmoryEntry("notes", "n2", """{"bid":"missing","page":1.0,"content_quill":"[{\"insert\":\"Orphan\"}]","type":"BOOK_CONTENT","created_at":$t0}"""),
            BookmoryEntry("notes", "n3", """{"bid":"k1","content_quill":"bad""", deleted = true),
            BookmoryEntry("tags", "1", """{"name":"Habits","bids":["k1"],"bid_count":1}"""),
            BookmoryEntry("collections", "c1", """{"name":"Favorite"}""")
        ) + extraBooks
    )

    @Test
    fun `new books carry metadata, status, rating, ownership and tags`() {
        val result = BookmoryMapper.map(records(), existing = emptyList()) { "file:///store/${it.substringAfterLast('/')}" }
        val book = result.snapshot.books.single()
        assertEquals("bm-k1", book.id)
        assertEquals("9781529034561", book.isbn)
        assertEquals(BookStatus.FINISHED.name, book.status)
        assertEquals(4.5f, book.rating[RatingAxis.OVERALL])
        assertEquals(BookFormat.Hardcover, book.format)
        assertEquals(Ownership.OWNED, book.ownership)
        assertEquals(listOf("Habits"), book.classification.tags)
        assertEquals(listOf("Favorite"), book.classification.collections)
        assertEquals(t0 + 100 * minute, book.finishedAt)
        assertEquals("Lovely", book.review)
        assertEquals("Amazon", book.purchaseLog.single().vendor)
    }

    @Test
    fun `page updates become sessions timed by the run they ended`() {
        val all = BookmoryMapper.map(records(), existing = emptyList()).snapshot.sessions
        val sessions = all.filter { it.pagesRead > 0 }
        // 0 -> 40 inside the first run, the repeated 40 is dropped, 40 -> 90 just after the second run.
        assertEquals(2, sessions.size)
        val (first, second) = sessions
        assertEquals(40, first.pagesRead)
        assertEquals(t0 + 5 * minute, first.startTime)
        assertEquals(1500, first.durationSeconds)
        assertEquals(50, second.pagesRead)
        assertEquals(40, second.startPage)
        assertEquals(1800, second.durationSeconds)
        assertTrue(all.all { it.bookId == "bm-k1" })
        // The third run matched no page update: it stays as 10 minutes of reading at page 90.
        val timeOnly = all.single { it.pagesRead == 0 }
        assertEquals(600, timeOnly.durationSeconds)
        assertEquals(90, timeOnly.startPage)
    }

    @Test
    fun `a page update with no timer run nearby is an untimed session`() {
        val read = BmRead(
            nth = 1,
            pageLogs = listOf(BmPageLog(page = 25.0, createdAt = t0)),
            timers = listOf(BmTimer(startedAt = t0 - 3 * 60 * minute, elapsedSec = 600))
        )
        val session = BookmoryMapper.sessionsForRead("b", "p", read).single { it.pagesRead > 0 }
        assertEquals(0, session.durationSeconds)
        assertEquals(t0, session.startTime)
        assertEquals(25, session.pagesRead)
    }

    @Test
    fun `notes keep text, favourite, page and photos while orphans and deleted rows are skipped`() {
        val result = BookmoryMapper.map(records(), existing = emptyList()) { "file:///store/${it.substringAfterLast('/')}" }
        val note = result.snapshot.notes.single()
        assertEquals("bm-note-n1", note.id)
        assertEquals("Start small.", note.content)
        assertEquals(12, note.pageNumber)
        assertEquals(true, note.isFavorite)
        assertEquals(NoteType.BOOK_CONTENT, note.type)
        assertEquals(listOf("file:///store/note_1.jpg"), note.imageUris)
        assertEquals(1, result.summary.notesSkipped)
        assertEquals(1, result.summary.photos)
    }

    @Test
    fun `a book already in the library gets the history and notes, not a duplicate`() {
        val mine = Book(
            id = "mine",
            title = "Kaizen",
            isbn = null,
            creators = listOf(Creator(CreatorRole.Author, "Sarah Harvey"))
        )
        val result = BookmoryMapper.map(records(), existing = listOf(mine))
        assertTrue(result.snapshot.books.isEmpty())
        assertEquals(1, result.summary.booksMatched)
        assertTrue(result.snapshot.sessions.all { it.bookId == "mine" })
        assertEquals("mine", result.snapshot.notes.single().bookId)
    }

    @Test
    fun `importing again adds nothing new`() {
        val first = BookmoryMapper.map(records(), existing = emptyList())
        val again = BookmoryMapper.map(
            records(),
            existing = first.snapshot.books,
            existingSessionIds = first.snapshot.sessions.map { it.id }.toSet(),
            existingNoteIds = first.snapshot.notes.map { it.id }.toSet()
        )
        assertEquals(0, again.summary.booksAdded)
        assertEquals(0, again.summary.sessions)
        assertEquals(0, again.summary.notes)
    }

    @Test
    fun `sessions keep their recorded zone and the day start comes across`() {
        val read = BmRead(
            nth = 1,
            pageLogs = listOf(BmPageLog(page = 10.0, createdAt = t0 + 20 * minute, zone = "Asia/Calcutta")),
            timers = listOf(
                BmTimer(startedAt = t0, elapsedSec = 900, zone = "Europe/Paris"),
                BmTimer(startedAt = t0 + 600 * minute, elapsedSec = 30, zone = "America/New_York")
            )
        )
        val sessions = BookmoryMapper.sessionsForRead("b", "p", read)
        // The timed session takes the timer's zone; a 30-second run is too short to keep.
        assertEquals(listOf("Europe/Paris"), sessions.map { it.zoneId })

        val withSettings = BookmoryRecords.fromEntries(
            listOf(BookmoryEntry("_main", "user", """{"day_start_hour":0,"theme":"dark"}"""))
        )
        assertEquals(0, withSettings.dayStartHour)
    }

    @Test
    fun `quill deltas flatten to text and embeds are dropped`() {
        val quill = """[{"insert":"Bold"," attributes":{"bold":true}},{"insert":" and plain\n"},{"insert":{"image":"x.jpg"}}]"""
        assertEquals("Bold and plain", BookmoryRecords.quillToText(quill))
        assertEquals("not json", BookmoryRecords.quillToText("not json"))
        assertEquals("", BookmoryRecords.quillToText(null))
    }

    @Test
    fun `statuses and formats map across`() {
        assertEquals(BookStatus.TO_READ, BookmoryMapper.bookStatus("NOT_STARTED"))
        assertEquals(BookStatus.DNF, BookmoryMapper.bookStatus("GIVE_UP"))
        assertEquals(BookFormat.Paperback, BookmoryMapper.bookFormat("paperBook"))
        assertEquals(BookFormat.`E-book`, BookmoryMapper.bookFormat("eBook"))
        assertEquals(BookFormat.Audiobook, BookmoryMapper.bookFormat("audioBook"))
    }
}
