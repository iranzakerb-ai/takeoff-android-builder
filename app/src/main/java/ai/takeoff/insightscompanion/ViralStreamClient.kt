package ai.takeoff.insightscompanion

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ViralStreamClient {
    const val PRODUCTION_ENDPOINT = "https://takeoff-virality-engine.onrender.com"
    private const val CONNECT_TIMEOUT_MS = 12_000
    private const val STREAM_READ_TIMEOUT_MS = 330_000
    private const val MAX_ATTEMPTS = 2

    data class StreamResult(val httpCode: Int, val errorBody: String = "")

    fun analyze(reelUrl: String, niche: String, onEvent: (JSONObject) -> Unit): StreamResult {
        var last = StreamResult(599, "connection_failed")
        repeat(MAX_ATTEMPTS) { attempt ->
            if (attempt > 0) onEvent(JSONObject().put("type","progress").put("stage","server_prepare").put("stage_label","بازیابی اتصال سرور").put("stage_progress_percent",5).put("overall_progress_percent",1))
            val result = runCatching { analyzeOnce(reelUrl, niche, onEvent) }.getOrElse { last = StreamResult(599, it.javaClass.simpleName); null }
            if (result != null) {
                last = result
                if (result.httpCode in 200..299 || result.httpCode in 400..499) return result
            }
        }
        return last
    }

    private fun analyzeOnce(reelUrl: String, niche: String, onEvent: (JSONObject) -> Unit): StreamResult {
        val conn = URL("$PRODUCTION_ENDPOINT/v2/viral-stream").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"; conn.connectTimeout = CONNECT_TIMEOUT_MS; conn.readTimeout = STREAM_READ_TIMEOUT_MS
            conn.doOutput = true; conn.instanceFollowRedirects = true
            conn.setRequestProperty("Accept", "application/x-ndjson, application/json")
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Connection", "keep-alive")
            conn.setRequestProperty("Accept-Encoding", "identity")
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
