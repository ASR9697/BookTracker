package com.example.booktracker.app.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

/**
 * The few Drive v3 calls sync needs, all inside the app's hidden appDataFolder:
 * a folder only this app can see, which doesn't count against what the reader
 * browses in Drive. Plain HTTPS with the reader's OAuth token; no Drive SDK.
 */
class DriveClient(private val token: String) {

    data class RemoteFile(val id: String, val name: String, val version: Long)

    class DriveException(val code: Int, message: String) : IOException(message)

    /** Every file the app keeps in Drive, by name. */
    suspend fun list(): Map<String, RemoteFile> = withContext(Dispatchers.IO) {
        val files = mutableMapOf<String, RemoteFile>()
        var pageToken: String? = null
        do {
            val query = buildString {
                append("spaces=appDataFolder&pageSize=1000&fields=")
                append(encode("nextPageToken,files(id,name,version)"))
                pageToken?.let { append("&pageToken=").append(encode(it)) }
            }
            val json = JSONObject(request("GET", "$API/files?$query").toString(Charsets.UTF_8))
            val array = json.optJSONArray("files")
            for (i in 0 until (array?.length() ?: 0)) {
                val file = array!!.getJSONObject(i)
                val remote = RemoteFile(file.getString("id"), file.getString("name"), file.optLong("version"))
                files[remote.name] = remote
            }
            pageToken = json.optString("nextPageToken").takeIf { it.isNotBlank() }
        } while (pageToken != null)
        files
    }

    /** The file's current version, which Drive bumps on every change. */
    suspend fun version(id: String): Long = withContext(Dispatchers.IO) {
        JSONObject(request("GET", "$API/files/$id?fields=version").toString(Charsets.UTF_8)).optLong("version")
    }

    suspend fun download(id: String): ByteArray = withContext(Dispatchers.IO) {
        request("GET", "$API/files/$id?alt=media")
    }

    /** Creates [name] in the app folder. */
    suspend fun create(name: String, mimeType: String, bytes: ByteArray): String = withContext(Dispatchers.IO) {
        val boundary = "nocturnal-${UUID.randomUUID()}"
        val metadata = JSONObject().put("name", name).put("parents", org.json.JSONArray().put("appDataFolder"))
        val body = ByteArrayOutputStream().apply {
            write("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n".toByteArray())
            write(metadata.toString().toByteArray())
            write("\r\n--$boundary\r\nContent-Type: $mimeType\r\n\r\n".toByteArray())
            write(bytes)
            write("\r\n--$boundary--\r\n".toByteArray())
        }.toByteArray()
        val response = request(
            "POST",
            "$UPLOAD/files?uploadType=multipart&fields=id",
            body,
            "multipart/related; boundary=$boundary"
        )
        JSONObject(response.toString(Charsets.UTF_8)).getString("id")
    }

    /** Replaces the content of an existing file. */
    suspend fun update(id: String, mimeType: String, bytes: ByteArray) = withContext(Dispatchers.IO) {
        // HttpURLConnection has no PATCH; Google APIs accept the override header.
        request("POST", "$UPLOAD/files/$id?uploadType=media", bytes, mimeType, mapOf("X-HTTP-Method-Override" to "PATCH"))
        Unit
    }

    private fun request(
        method: String,
        url: String,
        body: ByteArray? = null,
        contentType: String? = null,
        headers: Map<String, String> = emptyMap()
    ): ByteArray {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 60_000
            setRequestProperty("Authorization", "Bearer $token")
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
            if (body != null) {
                doOutput = true
                contentType?.let { setRequestProperty("Content-Type", it) }
                setFixedLengthStreamingMode(body.size)
            }
        }
        try {
            body?.let { bytes -> connection.outputStream.use { it.write(bytes) } }
            val code = connection.responseCode
            if (code !in 200..299) {
                val error = connection.errorStream?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
                throw DriveException(code, "Drive $method failed ($code): ${error.take(300)}")
            }
            return connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")

    private companion object {
        const val API = "https://www.googleapis.com/drive/v3"
        const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
    }
}
