package ai.takeoff.insightscompanion

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.io.IOException

object SharedMediaClient {
    private const val CONNECT_TIMEOUT_MS = 20_000
    // Server-side multimodal work is bounded to <120s. The client timeout stays
    // above that envelope but far below the old 285s stall window.
    private const val PROCESS_READ_TIMEOUT_MS = 145_000

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

    private fun executeWithNetworkRetry(
        open: () -> HttpURLConnection,
        json: JSONObject? = null,
        attempts: Int = 2,
    ): Response {
        val boundedAttempts = attempts.coerceIn(1, 3)
        var last: IOException? = null
        repeat(boundedAttempts) { attempt ->
            var conn: HttpURLConnection? = null
            try {
                conn = open()
                if (json != null) writeJson(conn, json)
                return read(conn)
            } catch (error: IOException) {
                last = error
                if (attempt + 1 < boundedAttempts) Thread.sleep(750L * (attempt + 1))
            } finally {
                conn?.disconnect()
            }
        }
        throw last ?: IOException("network_request_failed")
    }

    fun start(
        endpoint: String,
        url: String,
        niche: String,
        accountId: String?,
        forceRefresh: Boolean = true,
        companionKey: String = "",
    ): Response {
        val body = JSONObject()
            .put("url", url)
            .put("niche", niche)
            .put("account_id", accountId ?: JSONObject.NULL)
            .put("source", "android_share_v4")
            .put("force_refresh", forceRefresh)
        val endpoints = PayloadClient.candidateEndpoints(endpoint)
        var last: Response? = null
        for (ep in endpoints) {
            try {
                val res = executeWithNetworkRetry(
                    { connection("$ep/v4/media-jobs", "POST", 70_000, companionKey) },
                    body,
                    attempts = 2,
                )
                if (res.code in 200..299) return res
                last = res
                if (res.code != 404 && res.code !in 500..599) return res
            } catch (io: IOException) {
                if (ep == endpoints.last() && last == null) throw io
            }
        }
        return last ?: executeWithNetworkRetry(
            { connection("${endpoints.first()}/v4/media-jobs", "POST", 70_000, companionKey) },
            body,
            attempts = 2,
        )
    }

    fun process(endpoint: String, jobId: String, token: String, companionKey: String = ""): Response {
        // IMPORTANT: a process request advances a signed stateless capability.
        // If the network drops after the server advanced but before Android read
        // the response, blindly replaying the same token can fork/race the state.
        // WorkManager owns the retry so each network invocation is single-shot.
        val endpoints = PayloadClient.candidateEndpoints(endpoint)
        val body = JSONObject().put("token", token)
        var last: Response? = null
        for (ep in endpoints) {
            try {
                val res = executeWithNetworkRetry(
                    { connection("$ep/v4/media-jobs/${enc(jobId)}/process", "POST", PROCESS_READ_TIMEOUT_MS, companionKey) },
                    body,
                    attempts = 1,
                )
                if (res.code in 200..299) return res
                last = res
                if (res.code != 404 && res.code !in 502..504) return res
            } catch (io: IOException) {
                if (ep == endpoints.last() && last == null) throw io
            }
        }
        return last ?: executeWithNetworkRetry(
            { connection("${endpoints.first()}/v4/media-jobs/${enc(jobId)}/process", "POST", PROCESS_READ_TIMEOUT_MS, companionKey) },
            body,
            attempts = 1,
        )
    }

    fun status(endpoint: String, jobId: String, token: String, companionKey: String = ""): Response {
        // Status is read-only, so a small network retry is safe.
        val endpoints = PayloadClient.candidateEndpoints(endpoint)
        val body = JSONObject().put("token", token)
        var last: Response? = null
        for (ep in endpoints) {
            try {
                val res = executeWithNetworkRetry(
                    { connection("$ep/v4/media-jobs/${enc(jobId)}/status", "POST", 30_000, companionKey) },
                    body,
                    attempts = 2,
                )
                if (res.code in 200..299) return res
                last = res
                if (res.code != 404 && res.code !in 502..504) return res
            } catch (io: IOException) {
                if (ep == endpoints.last() && last == null) throw io
            }
        }
        return last ?: executeWithNetworkRetry(
            { connection("${endpoints.first()}/v4/media-jobs/${enc(jobId)}/status", "POST", 30_000, companionKey) },
            body,
            attempts = 2,
        )
    }
}