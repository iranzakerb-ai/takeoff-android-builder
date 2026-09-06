package ai.takeoff.insightscompanion

import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object SharedMediaClient {
    private const val CONNECT_TIMEOUT_MS = 20_000
    private const val PROCESS_READ_TIMEOUT_MS = 145_000
    private const val UPLOAD_READ_TIMEOUT_MS = 300_000

    data class Response(val code: Int, val body: JSONObject?, val raw: String)

    private fun enc(value: String) = URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

    private fun connection(url: String, method: String, readTimeout: Int, companionKey: String): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = CONNECT_TIMEOUT_MS
        conn.readTimeout = readTimeout
        conn.instanceFollowRedirects = false
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", "TakeOff-Insights/${BuildConfig.VERSION_NAME}")
        if (companionKey.isNotBlank()) conn.setRequestProperty("X-Takeoff-Companion-Key", companionKey)
        conn.setRequestProperty("Connection", "close")
        return conn
    }

    private fun writeJson(conn: HttpURLConnection, body: JSONObject) {
        val bytes = body.toString().toByteArray(Charsets.UTF_8)
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.setFixedLengthStreamingMode(bytes.size)
        conn.outputStream.use { it.write(bytes) }
    }

    private fun read(conn: HttpURLConnection): Response {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        val body = runCatching { JSONObject(raw) }.getOrNull()
        return Response(code, body, raw)
    }

    private fun executeWithNetworkRetry(open: () -> HttpURLConnection, json: JSONObject? = null, attempts: Int = 2): Response {
        var last: IOException? = null
        repeat(attempts.coerceIn(1, 3)) { attempt ->
            var conn: HttpURLConnection? = null
            try {
                conn = open()
                if (json != null) writeJson(conn, json)
                return read(conn)
            } catch (error: IOException) {
                last = error
                if (attempt + 1 < attempts) Thread.sleep(750L * (attempt + 1))
            } finally { conn?.disconnect() }
        }
        throw last ?: IOException("network_request_failed")
    }

    fun start(endpoint: String, url: String, niche: String, accountId: String?, forceRefresh: Boolean = true, companionKey: String = ""): Response {
        val base = PayloadClient.viralEndpoint(endpoint).trimEnd('/')
        val body = JSONObject().put("url", url).put("niche", niche)
            .put("account_id", accountId ?: JSONObject.NULL)
            .put("source", "android_share_v4").put("force_refresh", forceRefresh)
        return executeWithNetworkRetry({ connection("$base/v4/media-jobs", "POST", 70_000, companionKey) }, body, 2)
    }

    fun uploadDirectMedia(endpoint: String, url: String, niche: String, file: File, mime: String?, companionKey: String): Response {
        if (companionKey.isBlank()) return Response(401, JSONObject().put("detail", "companion credential required"), "")
        require(file.isFile && file.length() > 0L) { "shared media missing" }
        val base = PayloadClient.viralEndpoint(endpoint).trimEnd('/')
        val target = "$base/v2/viral-evidence/upload?url=${enc(url)}&niche=${enc(niche)}"
        val conn = connection(target, "POST", UPLOAD_READ_TIMEOUT_MS, companionKey)
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", mime?.takeIf { it.startsWith("video/") } ?: "video/mp4")
        conn.setFixedLengthStreamingMode(file.length())
        return try {
            file.inputStream().buffered().use { input -> conn.outputStream.buffered().use { output -> input.copyTo(output, 1024 * 1024) } }
            read(conn)
        } finally { conn.disconnect() }
    }

    fun process(endpoint: String, jobId: String, token: String, companionKey: String = ""): Response {
        val base = PayloadClient.viralEndpoint(endpoint).trimEnd('/')
        // A process call advances a signed capability. Never replay the same token
        // inside one network call after an ambiguous disconnect; WorkManager owns retry.
        return executeWithNetworkRetry(
            { connection("$base/v4/media-jobs/${enc(jobId)}/process", "POST", PROCESS_READ_TIMEOUT_MS, companionKey) },
            JSONObject().put("token", token),
            1,
        )
    }

    fun status(endpoint: String, jobId: String, token: String, companionKey: String = ""): Response {
        val base = PayloadClient.viralEndpoint(endpoint).trimEnd('/')
        return executeWithNetworkRetry(
            { connection("$base/v4/media-jobs/${enc(jobId)}/status", "POST", 30_000, companionKey) },
            JSONObject().put("token", token),
            2,
        )
    }
}
