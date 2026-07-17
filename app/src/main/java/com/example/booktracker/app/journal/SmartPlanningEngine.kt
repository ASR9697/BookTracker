package com.example.booktracker.app.journal

import com.example.booktracker.shared.models.MarginNote

class SmartPlanningEngine {
    fun checkShiftAwareWindDown(currentHour: Int, shiftEndHour: Int): Boolean {
        // e.g. prompt wind down 1 hour after shift ends
        return currentHour == (shiftEndHour + 1) % 24
    }

    fun getTransitRecommendation(transitDurationMins: Int, readingVelocity: Float): Int {
        // Recommends how many units can be read during a transit
        return (transitDurationMins * readingVelocity).toInt()
    }
}

class JournalingEngine {
    fun parseMarkdown(note: MarginNote): String {
        // Placeholder for markdown formatting rendering
        return "Parsed Markdown for Note at unit ${note.pageOrUnit}"
    }
}
