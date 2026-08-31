package ai.takeoff.insightscompanion

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ServerOcrClient {
    data class Result(val code: Int, val body: JSONObject?, val raw: String)

    fun analyze(endpoint: String, companionKey: String, jpeg: ByteArray): Result {
        require(jpeg.isNotEmpty()) { "empty screenshot" }
        val base = PayloadClient.viralEndpoint(endpoint).trimEnd('/')
        val conn = URL("$base/v4/owner-ocr").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 20_000
            conn.readTimeout = 100_000
            conn.instanceFollowRedirects = false
            conn.doOutput = true
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Content-Type", "image/jpeg")
            conn.setRequestProperty("X-Takeoff-Companion-Key", companionKey)
            conn.setRequestProperty("User-Agent", "TakeOff-Insights/${BuildConfig.VERSION_NAME}")
            conn.setFixedLengthStreamingMode(jpeg.size)
            conn.outputStream.use { it.write(jpeg) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            return Result(code, runCatching { JSONObject(raw) }.getOrNull(), raw)
        } finally {
            conn.disconnect()
        }
    }
}
