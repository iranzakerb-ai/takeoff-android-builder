package ai.takeoff.insightscompanion

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object SharedMediaClient {
    private const val CONNECT_TIMEOUT_MS = 20_000
    private const val PROCESS_READ_TIMEOUT_MS = 285_000

    data class Response(val code: Int, val body: JSONObject?, val raw: String)

    private fun enc(value: String) = URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

    private fun connection(url: String, method: String, readTimeout: Int, companionKey: String): HttpURLConnection {
        require(companionKey.isNotBlank()) { "companion credential required" }
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = CONNECT_TIMEOUT_MS
        conn.readTimeout = readTimeout
        conn.instanceFollowRedirects = false
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", "TakeOff-Insights/${BuildConfig.VERSION_NAME}")
        conn.setRequestProperty("X-Takeoff-Companion-Key", companionKey)
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

    fun start(
        endpoint: String,
        url: String,
        niche: String,
        accountId: String?,
        forceRefresh: Boolean = true,
        companionKey: String = "",
    ): Response {
        if (companionKey.isBlank()) return Response(401, JSONObject().put("detail", "companion credential required"), "")
        val base = PayloadClient.viralEndpoint(endpoint).trimEnd('/')
        val conn = connection("$base/v4/media-jobs", "POST", 70_000, companionKey)
        writeJson(
            conn,
            JSONObject()
                .put("url", url)
                .put("niche", niche)
                .put("account_id", accountId ?: JSONObject.NULL)
                .put("source", "android_share_v4")
                .put("force_refresh", forceRefresh),
        )
        return try { read(conn) } finally { conn.disconnect() }
    }

    fun process(endpoint: String, jobId: String, token: String, companionKey: String = ""): Response {
        if (companionKey.isBlank()) return Response(401, JSONObject().put("detail", "companion credential required"), "")
        val base = PayloadClient.viralEndpoint(endpoint).trimEnd('/')
        val conn = connection("$base/v4/media-jobs/${enc(jobId)}/process", "POST", PROCESS_READ_TIMEOUT_MS, companionKey)
        writeJson(conn, JSONObject().put("token", token))
        return try { read(conn) } finally { conn.disconnect() }
    }

    fun status(endpoint: String, jobId: String, token: String, companionKey: String = ""): Response {
        if (companionKey.isBlank()) return Response(401, JSONObject().put("detail", "companion credential required"), "")
        val base = PayloadClient.viralEndpoint(endpoint).trimEnd('/')
        val conn = connection("$base/v4/media-jobs/${enc(jobId)}?token=${enc(token)}", "GET", 30_000, companionKey)
        return try { read(conn) } finally { conn.disconnect() }
    }
}
