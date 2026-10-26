package com.example.booktracker.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteImageStoreTest {

    @Test
    fun `small photos are decoded at full size`() {
        assertEquals(1, NoteImageStore.sampleSize(1200, 1600, NoteImageStore.MAX_EDGE_PX))
        assertEquals(1, NoteImageStore.sampleSize(2048, 1536, NoteImageStore.MAX_EDGE_PX))
    }

    @Test
    fun `large photos sample down without dropping below the target edge`() {
        // 12 MP camera photo, 4000 x 3000: half size keeps 2000 px, just under 2048, so stay at 1.
        assertEquals(1, NoteImageStore.sampleSize(4000, 3000, NoteImageStore.MAX_EDGE_PX))
        // 50 MP, 8160 x 6120: quarter size is 2040 (< 2048), so half is the largest safe step.
        assertEquals(2, NoteImageStore.sampleSize(8160, 6120, NoteImageStore.MAX_EDGE_PX))
        // Portrait orientation uses the long edge too: 16384 / 8 = 2048 still meets the target.
        assertEquals(8, NoteImageStore.sampleSize(6000, 16384, NoteImageStore.MAX_EDGE_PX))
    }

    @Test
    fun `only file URIs are treated as local`() {
        // Only the scheme is checked here; Uri.parse needs Android, so stick to the negative cases.
        assertNull(NoteImageStore.localFile("https://example.com/photo.jpg"))
        assertNull(NoteImageStore.localFile("content://media/external/images/1"))
        assertNull(NoteImageStore.localFile(""))
    }
}
