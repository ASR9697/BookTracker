package com.example.booktracker.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.booktracker.shared.models.Ownership

/** One icon per ownership so the detail card, filters and Home tiles agree. */
val Ownership.icon: ImageVector
    get() = when (this) {
        Ownership.OWNED -> Icons.Rounded.CheckCircle
        Ownership.GIFT -> Icons.Rounded.CardGiftcard
        Ownership.WISHLIST -> Icons.Rounded.BookmarkAdd
        Ownership.NONE -> Icons.AutoMirrored.Rounded.HelpOutline
    }

/** The choices a reader picks from; NONE is "none of these", reached by tapping the selected one again. */
val SelectableOwnerships = listOf(Ownership.OWNED, Ownership.GIFT, Ownership.WISHLIST)
