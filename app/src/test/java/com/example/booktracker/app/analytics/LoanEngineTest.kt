package com.example.booktracker.app.analytics

import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.LoanRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class LoanEngineTest {

    private val zone = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 10, 25)

    private fun localDateToEpochMillis(date: LocalDate): Long {
        return date.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    @Test
    fun `activeLoan returns latest unreturned loan`() {
        val returnedLoan = LoanRecord(
            loanDate = localDateToEpochMillis(LocalDate.of(2026, 9, 1)),
            dueDate = localDateToEpochMillis(LocalDate.of(2026, 9, 21)),
            lender = "Main Library",
            memo = "First borrow",
            returnDate = localDateToEpochMillis(LocalDate.of(2026, 9, 20))
        )
        val activeLoan = LoanRecord(
            loanDate = localDateToEpochMillis(LocalDate.of(2026, 10, 15)),
            dueDate = localDateToEpochMillis(LocalDate.of(2026, 11, 5)),
            lender = "Downtown Library",
            memo = "Second borrow",
            returnDate = null
        )

        val bookWithLoans = Book(
            id = "b1",
            title = "Dune",
            isbn = null,
            loanRecord = listOf(returnedLoan, activeLoan)
        )

        val active = LoanEngine.activeLoan(bookWithLoans)
        assertNotNull(active)
        assertEquals("Downtown Library", active?.lender)
        assertTrue(active?.isActive == true)
    }

    @Test
    fun `activeLoan returns null when all loans are returned`() {
        val returnedLoan = LoanRecord(
            loanDate = localDateToEpochMillis(LocalDate.of(2026, 9, 1)),
            dueDate = localDateToEpochMillis(LocalDate.of(2026, 9, 21)),
            lender = "Main Library",
            memo = "Returned already",
            returnDate = localDateToEpochMillis(LocalDate.of(2026, 9, 20))
        )
        val book = Book(id = "b2", title = "Hyperion", isbn = null, loanRecord = listOf(returnedLoan))
        assertNull(LoanEngine.activeLoan(book))
    }

    @Test
    fun `getLoanStatus identifies normal future due date`() {
        val loan = LoanRecord(
            loanDate = localDateToEpochMillis(today.minusDays(5)),
            dueDate = localDateToEpochMillis(today.plusDays(10)),
            lender = "Public Library",
            memo = ""
        )
        val status = LoanEngine.getLoanStatus(loan, today, zone)
        assertEquals(LoanEngine.LoanUrgency.NORMAL, status.urgency)
        assertEquals(10L, status.daysRemaining)
        assertEquals("Due 11/4", status.badgeText)
        assertTrue(status.detailText.contains("Due back to Public Library in 10 days"))
    }

    @Test
    fun `getLoanStatus identifies due soon and due today`() {
        // Due in 2 days
        val loanSoon = LoanRecord(
            loanDate = localDateToEpochMillis(today.minusDays(10)),
            dueDate = localDateToEpochMillis(today.plusDays(2)),
            lender = "City Library",
            memo = ""
        )
        val statusSoon = LoanEngine.getLoanStatus(loanSoon, today, zone)
        assertEquals(LoanEngine.LoanUrgency.DUE_SOON, statusSoon.urgency)
        assertEquals(2L, statusSoon.daysRemaining)
        assertEquals("Due in 2d", statusSoon.badgeText)

        // Due today
        val loanToday = LoanRecord(
            loanDate = localDateToEpochMillis(today.minusDays(14)),
            dueDate = localDateToEpochMillis(today),
            lender = "City Library",
            memo = ""
        )
        val statusToday = LoanEngine.getLoanStatus(loanToday, today, zone)
        assertEquals(LoanEngine.LoanUrgency.DUE_SOON, statusToday.urgency)
        assertEquals(0L, statusToday.daysRemaining)
        assertEquals("Due today!", statusToday.badgeText)
    }

    @Test
    fun `getLoanStatus identifies overdue loans`() {
        val overdueLoan = LoanRecord(
            loanDate = localDateToEpochMillis(today.minusDays(20)),
            dueDate = localDateToEpochMillis(today.minusDays(3)),
            lender = "State Library",
            memo = ""
        )
        val status = LoanEngine.getLoanStatus(overdueLoan, today, zone)
        assertEquals(LoanEngine.LoanUrgency.OVERDUE, status.urgency)
        assertEquals(-3L, status.daysRemaining)
        assertEquals("Overdue by 3d", status.badgeText)
        assertTrue(status.detailText.contains("Overdue to State Library by 3 days"))
    }

    @Test
    fun `getLoanStatus handles lent-out books with and without due dates`() {
        val lentWithDueDate = LoanRecord(
            loanDate = localDateToEpochMillis(today.minusDays(7)),
            dueDate = localDateToEpochMillis(today.plusDays(7)),
            lender = "Alice",
            memo = "Hardcover edition",
            isLentOut = true
        )
        val statusWithDue = LoanEngine.getLoanStatus(lentWithDueDate, today, zone)
        assertTrue(statusWithDue.isLentOut)
        assertEquals("Lent (Alice)", statusWithDue.badgeText)
        assertTrue(statusWithDue.detailText.contains("Lent to Alice · Due in 7 days"))

        val lentNoDueDate = LoanRecord(
            loanDate = localDateToEpochMillis(today.minusDays(7)),
            dueDate = 0L,
            lender = "Bob",
            memo = "",
            isLentOut = true
        )
        val statusNoDue = LoanEngine.getLoanStatus(lentNoDueDate, today, zone)
        assertEquals(LoanEngine.LoanUrgency.NORMAL, statusNoDue.urgency)
        assertEquals("Lent (Bob)", statusNoDue.badgeText)
        assertTrue(statusNoDue.detailText.contains("No due date"))
    }

    @Test
    fun `urgentLoans filters only overdue and due soon books`() {
        val normalLoan = LoanRecord(
            loanDate = localDateToEpochMillis(today),
            dueDate = localDateToEpochMillis(today.plusDays(14)),
            lender = "Library A",
            memo = ""
        )
        val dueSoonLoan = LoanRecord(
            loanDate = localDateToEpochMillis(today.minusDays(10)),
            dueDate = localDateToEpochMillis(today.plusDays(1)),
            lender = "Library B",
            memo = ""
        )
        val overdueLoan = LoanRecord(
            loanDate = localDateToEpochMillis(today.minusDays(20)),
            dueDate = localDateToEpochMillis(today.minusDays(2)),
            lender = "Library C",
            memo = ""
        )

        val books = listOf(
            Book(id = "b1", title = "Normal Book", isbn = null, loanRecord = listOf(normalLoan)),
            Book(id = "b2", title = "Soon Book", isbn = null, loanRecord = listOf(dueSoonLoan)),
            Book(id = "b3", title = "Overdue Book", isbn = null, loanRecord = listOf(overdueLoan))
        )

        val urgent = LoanEngine.urgentLoans(books, today, zone)
        assertEquals(2, urgent.size)
        // Overdue first (lowest days remaining: -2 < 1)
        assertEquals("Overdue Book", urgent[0].first.title)
        assertEquals(LoanEngine.LoanUrgency.OVERDUE, urgent[0].second.urgency)
        assertEquals("Soon Book", urgent[1].first.title)
        assertEquals(LoanEngine.LoanUrgency.DUE_SOON, urgent[1].second.urgency)
    }
}
