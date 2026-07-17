package com.example.booktracker.shared.data

import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.BookStatus
import com.example.booktracker.shared.models.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MockDataInjector {
    suspend fun injectMockData(uid: String) {
        val db = FirebaseFirestore.getInstance()

        val userRef = db.collection("users").document(uid)
        userRef.set(User(uid = uid, displayName = "Debug User")).await()

        val bookRef = userRef.collection("books").document()
        val mockBook = Book(
            id = bookRef.id,
            title = "The Martian",
            authors = listOf("Andy Weir"),
            format = "PAGES",
            totalUnits = 369,
            currentUnit = 0,
            status = BookStatus.UP_NEXT.name
        )
        bookRef.set(mockBook).await()
    }
}
