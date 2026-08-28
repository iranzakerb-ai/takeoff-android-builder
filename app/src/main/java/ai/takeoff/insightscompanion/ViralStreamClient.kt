package ai.takeoff.insightscompanion

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ViralStreamClient {
    const val PRODUCTION_ENDPOINT = "https://takeoff-seven-puce.vercel.app"
    private const val CONNECT_TIMEOUT_MS = 20_000
    private const val STREAM_READ_TIMEOUT_MS = 330_000

    data class StreamResult(val httpCode: Int, val errorBody: String = "")

    fun analyze(reelUrl: String, niche: String, onEvent: (JSONObject) -> Unit): StreamResult {
        val target = URL("$PRODUCTION_ENDPOINT/v2/viral-stream")
        val conn = target.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"; conn.connectTimeout = CONNECT_TIMEOUT_MS; conn.readTimeout = STREAM_READ_TIMEOUT_MS
            conn.doOutput = true; conn.instanceFollowRedirects = false
            conn.setRequestProperty("Accept", "application/x-ndjson, application/json")
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Connection", "keep-alive")
            val payload = JSONObject().put("url", reelUrl).put("niche", niche).put("source", "android_share_stream").toString()
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            if (code !in 200..299) return StreamResult(code, conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty())
            conn.inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.filter { it.isNotBlank() }.forEach { raw -> runCatching { JSONObject(raw) }.getOrNull()?.let(onEvent) }
            }
            return StreamResult(code)
        } finally { conn.disconnect() }
    }
}
