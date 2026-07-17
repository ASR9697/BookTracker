package com.example.booktracker.app.data.local

import com.example.booktracker.shared.models.Book
import com.example.booktracker.shared.models.DnfData
import org.json.JSONArray
import org.json.JSONObject

fun BookEntity.toModel(): Book = Book(
    id = id,
    title = title,
    authors = authors.toStringList(),
    coverUrl = coverUrl,
    format = format,
    totalUnits = totalUnits,
    currentUnit = currentUnit,
    status = status,
    dnfData = dnfPercentage?.let { DnfData(it, dnfReason.orEmpty()) },
    lastUpdated = lastUpdated,
    rating = rating.toFloatMap()
)

fun Book.toEntity(): BookEntity = BookEntity(
    id = id,
    title = title,
    authors = JSONArray(authors).toString(),
    coverUrl = coverUrl,
    format = format,
    totalUnits = totalUnits,
    currentUnit = currentUnit,
    status = status,
    dnfPercentage = dnfData?.abandonedPercentage,
    dnfReason = dnfData?.reason,
    lastUpdated = lastUpdated,
    rating = JSONObject(rating.mapValues { it.value.toDouble() }).toString()
)

private fun String.toStringList(): List<String> {
    if (isBlank()) return emptyList()
    val array = JSONArray(this)
    return (0 until array.length()).map { array.getString(it) }
}

private fun String.toFloatMap(): Map<String, Float> {
    if (isBlank()) return emptyMap()
    val obj = JSONObject(this)
    val result = mutableMapOf<String, Float>()
    for (key in obj.keys()) {
        result[key] = obj.getDouble(key).toFloat()
    }
    return result
}
