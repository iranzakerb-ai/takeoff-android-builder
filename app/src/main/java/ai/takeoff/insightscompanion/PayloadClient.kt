package ai.takeoff.insightscompanion

import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object PayloadClient {
    const val PRODUCTION_ENDPOINT = "https://takeoff-seven-puce.vercel.app"
    internal const val VIRAL_ANALYSIS_READ_TIMEOUT_MS = 300_000

    internal fun validateEndpoint(endpoint: String) {
        val url = runCatching { URL(endpoint.trim()) }.getOrElse { throw IllegalArgumentException("Invalid TakeOff endpoint") }
        val scheme = url.protocol.lowercase(); val host = url.host.trim().lowercase()
        require(host.isNotBlank()) { "Invalid TakeOff endpoint" }
        require(url.userInfo.isNullOrBlank()) { "Endpoint must not contain embedded credentials" }
        val allowed = scheme == "https" || (scheme == "http" && isTrustedLanHost(host))
        require(allowed) { "Use HTTPS except for literal trusted LAN endpoints" }
    }

    internal fun viralEndpoint(endpoint: String): String {
        val raw = endpoint.trim().trimEnd('/')
        if (raw.isBlank()) return PRODUCTION_ENDPOINT
        val parsed = runCatching { URL(raw) }.getOrNull() ?: return PRODUCTION_ENDPOINT
        val host = parsed.host.trim().lowercase()
        return if (parsed.protocol.equals("http", true) && isTrustedLanHost(host)) raw else PRODUCTION_ENDPOINT
    }

    private fun isTrustedLanHost(host: String): Boolean {
        if (host == "localhost") return true
        val parts = host.split('.'); if (parts.size != 4) return false
        val octets = parts.map { part ->
            if (part.isEmpty() || part.length > 3 || part.any { !it.isDigit() }) return false
            part.toIntOrNull()?.takeIf { it in 0..255 } ?: return false
        }
        return octets[0] == 10 || (octets[0] == 192 && octets[1] == 168) ||
            (octets[0] == 172 && octets[1] in 16..31) || octets[0] == 127
    }

    internal fun disableRedirects(conn: HttpURLConnection) {
        conn.instanceFollowRedirects = false
    }

    internal fun isDurablyAcknowledgedResponse(body: String, expectedCaptureId: String? = null): Boolean {
        if (body.isBlank()) return false
        return runCatching {
            val root = JSONObject(body); val receiptId = root.optString("receipt_id"); val disposition = root.optString("status")
            val idMatches = expectedCaptureId.isNullOrBlank() || receiptId == expectedCaptureId
            root.optBoolean("acknowledged", false) && root.optString("durability") == "persistent" &&
                receiptId.isNotBlank() && disposition == "recorded" && idMatches
        }.getOrDefault(false)
    }

    fun post(endpoint: String, companionKey: String, payload: JSONObject): Pair<Int, String> =
        request(endpoint.trimEnd('/') + "/v2/owner-outcomes/device", companionKey, "POST", payload.toString())

    fun postViralEvidence(endpoint: String, companionKey: String, url: String, niche: String): Pair<Int, String> {
        val payload = JSONObject().put("url", url).put("niche", niche).put("source", "android_share")
        return request(
            viralEndpoint(endpoint) + "/v2/viral-evidence",
            companionKey,
            "POST",
            payload.toString(),
            readTimeoutMs = VIRAL_ANALYSIS_READ_TIMEOUT_MS,
            connectTimeoutMs = 20_000,
        )
    }

    fun uploadViralCapture(endpoint: String, companionKey: String, url: String, niche: String, file: File): Pair<Int, String> {
        require(file.isFile && file.length() > 0L) { "capture file missing" }
        val target = viralEndpoint(endpoint) + "/v2/viral-evidence/upload?url=${enc(url)}&niche=${enc(niche)}"
        val base = URL(target)
        validateEndpoint("${base.protocol}://${base.authority}")
        val conn = base.openConnection() as HttpURLConnection
        try {
            disableRedirects(conn)
            conn.requestMethod = "POST"
            conn.connectTimeout = 25_000
            conn.readTimeout = VIRAL_ANALYSIS_READ_TIMEOUT_MS
            conn.doOutput = true
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Content-Type", "application/octet-stream")
            conn.setRequestProperty("X-Takeoff-Companion-Key", companionKey)
            conn.setRequestProperty("Connection", "close")
            if (BuildCompat.canUseFixedLengthLong()) conn.setFixedLengthStreamingMode(file.length())
            else conn.setChunkedStreamingMode(256 * 1024)
            file.inputStream().buffered(256 * 1024).use { input ->
                conn.outputStream.buffered(256 * 1024).use { output -> input.copyTo(output, 256 * 1024) }
            }
            return read(conn)
        } finally {
            conn.disconnect()
        }
    }

    fun getViralEvidence(endpoint: String, companionKey: String, evidenceId: String): Pair<Int, String> =
        request(viralEndpoint(endpoint) + "/v2/viral-evidence/${enc(evidenceId)}", companionKey, readTimeoutMs = 30_000)

    fun getRecentViralEvidence(endpoint: String, companionKey: String, niche: String, limit: Int = 30): Pair<Int, String> =
        request(viralEndpoint(endpoint) + "/v2/viral-evidence?niche=${enc(niche)}&limit=${limit.coerceIn(1,200)}", companionKey, readTimeoutMs = 30_000)

    fun getPending(endpoint: String, companionKey: String, accountId: String): Pair<Int, String> =
        request(endpoint.trimEnd('/') + "/v2/owner-outcomes/pending?account_id=${enc(accountId)}", companionKey)

    fun getRecent(endpoint: String, companionKey: String, accountId: String, limit: Int = 40): Pair<Int, String> =
        request(endpoint.trimEnd('/') + "/v2/owner-outcomes/recent?account_id=${enc(accountId)}&limit=${limit.coerceIn(1,100)}", companionKey)

    fun getHealth(endpoint: String): Pair<Int, String> = request(endpoint.trimEnd('/') + "/health", "")
    fun getReadiness(endpoint: String, niche: String): Pair<Int, String> = request(endpoint.trimEnd('/') + "/v2/readiness/${enc(niche)}", "")
    fun getPatterns(endpoint: String, niche: String): Pair<Int, String> = request(endpoint.trimEnd('/') + "/v2/pattern-memory/${enc(niche)}", "")
    fun getScenario(endpoint: String, scenarioId: String): Pair<Int, String> = request(endpoint.trimEnd('/') + "/v2/scenarios/${enc(scenarioId)}", "")

    private fun enc(value: String) = URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

    private fun request(
        url: String,
        companionKey: String,
        method: String = "GET",
        body: String? = null,
        readTimeoutMs: Int = 18_000,
        connectTimeoutMs: Int = 12_000,
    ): Pair<Int, String> {
        val base = URL(url); validateEndpoint("${base.protocol}://${base.authority}")
        val conn = base.openConnection() as HttpURLConnection
        try {
            disableRedirects(conn)
            conn.requestMethod = method
            conn.connectTimeout = connectTimeoutMs
            conn.readTimeout = readTimeoutMs
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Connection", "close")
            if (companionKey.isNotBlank()) conn.setRequestProperty("X-Takeoff-Companion-Key", companionKey)
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            return read(conn)
        } finally {
            conn.disconnect()
        }
    }

    private fun read(conn: HttpURLConnection): Pair<Int, String> {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        return code to text
    }
}

private object BuildCompat {
    fun canUseFixedLengthLong(): Boolean = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT
}
