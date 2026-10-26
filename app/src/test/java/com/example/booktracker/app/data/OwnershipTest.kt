package com.example.booktracker.app.data

import com.example.booktracker.shared.models.Ownership
import org.junit.Assert.assertEquals
import org.junit.Test

class OwnershipTest {

    @Test
    fun `stored names round-trip`() {
        Ownership.entries.forEach { assertEquals(it, Ownership.fromStored(it.name, hasPurchase = false)) }
    }

    @Test
    fun `records from before ownership existed fall back on the purchase log`() {
        assertEquals(Ownership.OWNED, Ownership.fromStored(null, hasPurchase = true))
        assertEquals(Ownership.NONE, Ownership.fromStored(null, hasPurchase = false))
    }

    @Test
    fun `an explicit choice wins over a logged purchase`() {
        // A bought book later given away and re-wishlisted keeps the reader's choice.
        assertEquals(Ownership.WISHLIST, Ownership.fromStored("WISHLIST", hasPurchase = true))
        assertEquals(Ownership.NONE, Ownership.fromStored("NONE", hasPurchase = true))
    }

    @Test
    fun `unknown names are treated as missing`() {
        assertEquals(Ownership.OWNED, Ownership.fromStored("BORROWED", hasPurchase = true))
        assertEquals(Ownership.NONE, Ownership.fromStored("", hasPurchase = false))
    }
}
