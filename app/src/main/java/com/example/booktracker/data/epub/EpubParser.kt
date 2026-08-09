package com.example.booktracker.data.epub

import android.content.Context
import android.net.Uri
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class EpubManifestItem(val id: String, val href: String, val mediaType: String)

data class EpubBook(
    val title: String,
    val author: String,
    val manifest: Map<String, EpubManifestItem>,
    val spine: List<String>,
    val opfDir: String
)

class EpubParser {
    fun parse(context: Context, uri: Uri): EpubBook {
        var opfPath = ""
        
        // 1. Find OPF path from container.xml
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val zip = ZipInputStream(stream)
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                if (entry.name == "META-INF/container.xml") {
                    val factory = XmlPullParserFactory.newInstance()
                    factory.isNamespaceAware = true
                    val parser = factory.newPullParser()
                    parser.setInput(zip, "UTF-8")
                    var eventType = parser.eventType
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG && parser.name == "rootfile") {
                            opfPath = parser.getAttributeValue(null, "full-path") ?: ""
                            break
                        }
                        eventType = parser.next()
                    }
                    break
                }
                entry = zip.nextEntry
            }
        }

        if (opfPath.isEmpty()) throw IllegalArgumentException("Not a valid EPUB: missing container.xml")

        var title = "Unknown Title"
        var author = "Unknown Author"
        val manifest = mutableMapOf<String, EpubManifestItem>()
        val spine = mutableListOf<String>()
        val opfDir = if (opfPath.contains("/")) opfPath.substringBeforeLast("/") + "/" else ""

        // 2. Parse OPF
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val zip = ZipInputStream(stream)
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                if (entry.name == opfPath) {
                    val factory = XmlPullParserFactory.newInstance()
                    factory.isNamespaceAware = true
                    val parser = factory.newPullParser()
                    parser.setInput(zip, "UTF-8")
                    var eventType = parser.eventType
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            val name = parser.name
                            when (name) {
                                "title" -> title = parser.nextText()
                                "creator" -> author = parser.nextText()
                                "item" -> {
                                    val id = parser.getAttributeValue(null, "id")
                                    val href = parser.getAttributeValue(null, "href")
                                    val mediaType = parser.getAttributeValue(null, "media-type")
                                    if (id != null && href != null) {
                                        manifest[id] = EpubManifestItem(id, href, mediaType ?: "")
                                    }
                                }
                                "itemref" -> {
                                    val idref = parser.getAttributeValue(null, "idref")
                                    if (idref != null) spine.add(idref)
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                    break
                }
                entry = zip.nextEntry
            }
        }

        return EpubBook(title, author, manifest, spine, opfDir)
    }

    fun readChapterHtml(context: Context, uri: Uri, opfDir: String, href: String): String {
        val targetPath = opfDir + href
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val zip = ZipInputStream(stream)
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                if (entry.name == targetPath) {
                    return zip.bufferedReader().readText()
                }
                entry = zip.nextEntry
            }
        }
        return ""
    }
}
