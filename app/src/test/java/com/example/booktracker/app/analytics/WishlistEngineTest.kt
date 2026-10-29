package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.Creator
import com.example.booktracker.shared.models.CreatorRole
import com.example.booktracker.shared.models.Ownership
import com.example.booktracker.shared.models.PurchaseLog
import com.example.booktracker.shared.models.SeriesInfo
import com.example.booktracker.shared.models.WishInfo
import com.example.booktracker.shared.models.WishPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class WishlistEngineTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private fun day(date: LocalDate) = date.atStartOfDay(zone).toInstant().toEpochMilli()

    private fun wished(
        id: String,
        priority: WishPriority = WishPriority.WANT,
        price: Double? = null,
        addedAt: Long = 0,
        author: String = "A",
        release: LocalDate? = null
    ) = Book(
        id = id, title = id, isbn = null,
        creators = listOf(Creator(CreatorRole.Author, author)),
        ownership = Ownership.WISHLIST,
        wish = WishInfo(priority = priority, price = price, currency = "INR", addedAt = addedAt, releaseDate = release?.let(::day))
    )

    private fun inSeries(id: String, name: String, position: Int, status: BookStatus) = Book(
        id = id, title = id, isbn = null, status = status.name,
        seriesInfo = SeriesInfo(true, name, position)
    )

    @Test
    fun `priority sort puts must-haves first and newest first within a level`() {
        val list = listOf(
            wished("maybe", WishPriority.MAYBE, addedAt = 9),
            wished("old must", WishPriority.MUST, addedAt = 1),
            wished("new must", WishPriority.MUST, addedAt = 5)
        )
        assertEquals(listOf("new must", "old must", "maybe"), WishlistEngine.sorted(list, WishlistEngine.Sort.PRIORITY).map { it.id })
    }

    @Test
    fun `price sort leaves unpriced books last`() {
        val list = listOf(wished("none"), wished("dear", price = 900.0), wished("cheap", price = 200.0))
        assertEquals(listOf("cheap", "dear", "none"), WishlistEngine.sorted(list, WishlistEngine.Sort.PRICE).map { it.id })
    }

    @Test
    fun `total adds up priced books only`() {
        val total = WishlistEngine.total(listOf(wished("a", price = 200.0), wished("b", price = 300.5), wished("c")))
        assertEquals(500.5, total.byCurrency.getValue("INR"), 0.001)
        assertEquals(2, total.priced)
        assertEquals(3, total.count)
    }

    @Test
    fun `a series gap is the number after the furthest one started`() {
        val books = listOf(
            inSeries("e1", "The Expanse", 1, BookStatus.FINISHED),
            inSeries("e2", "The Expanse", 2, BookStatus.READING),
            inSeries("d1", "Dune", 1, BookStatus.FINISHED),
            inSeries("d2", "Dune", 2, BookStatus.TO_READ),
            inSeries("n1", "Untouched", 1, BookStatus.TO_READ)
        )
        val gaps = WishlistEngine.seriesGaps(books)
        assertEquals(listOf("The Expanse" to 3), gaps.map { it.seriesName to it.nextPosition })
    }

    @Test
    fun `wished books in a started series are recognised`() {
        val started = inSeries("e1", "The Expanse", 1, BookStatus.FINISHED)
        val wanted = wished("e2").copy(seriesInfo = SeriesInfo(true, "the expanse ", 2))
        assertTrue(WishlistEngine.inStartedSeries(wanted, listOf(started, wanted)))
        assertFalse(WishlistEngine.inStartedSeries(wished("loner"), listOf(started)))
    }

    @Test
    fun `spending counts only the month asked for`() {
        val book = Book(
            id = "b", title = "b", isbn = null,
            purchaseLog = listOf(
                PurchaseLog(day(LocalDate.of(2026, 10, 3)), "Store", 300.0, "INR", ""),
                PurchaseLog(day(LocalDate.of(2026, 10, 28)), "Store", 150.0, "INR", ""),
                PurchaseLog(day(LocalDate.of(2026, 9, 30)), "Store", 999.0, "INR", "")
            )
        )
        assertEquals(450.0, WishlistEngine.spentIn(listOf(book), YearMonth.of(2026, 10), zone).getValue("INR"), 0.001)
    }

    @Test
    fun `release day matches wished books only on that day`() {
        val today = LocalDate.of(2026, 10, 30)
        val books = listOf(wished("out today", release = today), wished("next week", release = today.plusDays(7)))
        assertEquals(listOf("out today"), WishlistEngine.releasedOn(books, today, zone).map { it.id })
        assertTrue(WishlistEngine.isUpcoming(books[1], today, zone))
        assertFalse(WishlistEngine.isUpcoming(books[0], today, zone))
    }

    @Test
    fun `share text groups by priority`() {
        val text = WishlistEngine.shareText(listOf(wished("Dune", WishPriority.MAYBE, author = "Herbert"), wished("Hail Mary", WishPriority.MUST)), "Sam")
        assertEquals("Sam's book wishlist\n\nMust have\n• Hail Mary by A\n\nMaybe\n• Dune by Herbert", text)
    }
}
