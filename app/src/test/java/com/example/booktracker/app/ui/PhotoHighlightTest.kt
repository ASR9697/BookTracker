package com.example.booktracker.app.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhotoHighlightTest {

    @Test
    fun `a portrait photo is centred with bars at the sides`() {
        // 1000 x 2000 photo in a 1000 x 1000 area: scale 0.5, so 500 wide and centred.
        val shown = fitRect(IntSize(1000, 1000), 1000, 2000)
        assertEquals(Rect(250f, 0f, 750f, 1000f), shown)
    }

    @Test
    fun `a drag maps to photo space whichever way it goes`() {
        val shown = Rect(250f, 0f, 750f, 1000f)
        val forward = toNormalized(shown, Offset(250f, 100f), Offset(750f, 200f))
        val backward = toNormalized(shown, Offset(750f, 200f), Offset(250f, 100f))
        assertEquals(Rect(0f, 0.1f, 1f, 0.2f), forward)
        assertEquals(forward, backward)
    }

    @Test
    fun `a drag that runs off the photo is clipped to it`() {
        val shown = Rect(250f, 0f, 750f, 1000f)
        assertEquals(Rect(0f, 0.5f, 1f, 0.6f), toNormalized(shown, Offset(0f, 500f), Offset(1000f, 600f)))
    }

    @Test
    fun `stray touches and drags outside the photo make no highlight`() {
        val shown = Rect(250f, 0f, 750f, 1000f)
        assertNull(toNormalized(shown, Offset(400f, 400f), Offset(402f, 401f)))
        assertNull(toNormalized(shown, Offset(10f, 100f), Offset(20f, 300f)))
        assertNull(toNormalized(Rect.Zero, Offset(0f, 0f), Offset(50f, 50f)))
    }
}
