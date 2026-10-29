package com.example.booktracker.app.ui

import android.content.ClipData
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.example.booktracker.app.camera.findActivity

/**
 * Right-click (mouse or trackpad) does what a long-press does on touch. Put it
 * before the element's clickable: it takes the secondary press first, so the
 * click handler never sees it.
 */
fun Modifier.onSecondaryClick(onClick: () -> Unit): Modifier = composed {
    val current by rememberUpdatedState(onClick)
    pointerInput(Unit) {
        awaitEachGesture {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                event.changes.forEach { it.consume() }
                current()
            }
        }
    }
}

/**
 * Accepts images dragged in from another app (Photos or Files side by side in
 * split-screen). The drop grants read access to the dragged items, so they can
 * be imported like a gallery pick.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.imageDropTarget(onImages: (List<Uri>) -> Unit): Modifier = composed {
    val activity = LocalContext.current.findActivity()
    val current by rememberUpdatedState(onImages)
    val target = remember(activity) {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val drag = event.toAndroidDragEvent()
                activity?.requestDragAndDropPermissions(drag)
                val clip = drag.clipData ?: return false
                val uris = (0 until clip.itemCount).mapNotNull { clip.getItemAt(it).uri }
                if (uris.isEmpty()) return false
                current(uris)
                return true
            }
        }
    }
    dragAndDropTarget(
        shouldStartDragAndDrop = { event -> event.mimeTypes().any { it.startsWith("image/") } },
        target = target
    )
}

/** Lets a passage be dragged out as plain text, into a document or chat open beside the app. */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.draggableText(label: String, text: () -> String): Modifier =
    dragAndDropSource { _ ->
        DragAndDropTransferData(ClipData.newPlainText(label, text()))
    }
