package com.example.booktracker.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.booktracker.shared.models.Ownership

/** One icon per ownership so the detail card, filters and Home tiles agree. */
val Ownership.icon: ImageVector
    get() = when (this) {
        Ownership.OWNED -> Icons.Filled.CheckCircle
        Ownership.GIFT -> Icons.Filled.CardGiftcard
        Ownership.WISHLIST -> Icons.Filled.BookmarkAdd
        Ownership.NONE -> Icons.AutoMirrored.Filled.HelpOutline
    }

/** The choices a reader picks from; NONE is "none of these", reached by tapping the selected one again. */
val SelectableOwnerships = listOf(Ownership.OWNED, Ownership.GIFT, Ownership.WISHLIST)
