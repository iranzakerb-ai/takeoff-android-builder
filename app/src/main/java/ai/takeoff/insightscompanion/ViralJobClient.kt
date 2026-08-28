package ai.takeoff.insightscompanion

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

object ViralJobClient {
    private fun connection(url: String, method: String, readTimeoutMs: Int = 75_000): HttpsURLConnection {
        val conn = URL(url).openConnection() as? HttpsURLConnection
            ?: throw IllegalArgumentException("HTTPS endpoint required")
        conn.requestMethod = method
        conn.connectTimeout = 15_000
        conn.readTimeout = readTimeoutMs
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", "TakeOff-Insights/${BuildConfig.VERSION_NAME}")
        return conn
    }

    private fun read(conn: HttpURLConnection): Pair<Int, String> {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        return code to body
    }

    private fun writeJson(conn: HttpsURLConnection, json: JSONObject) {
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        val bytes = json.toString().toByteArray(Charsets.UTF_8)
        conn.setFixedLengthStreamingMode(bytes.size)
        conn.outputStream.use { it.write(bytes) }
    }

    fun start(endpoint: String, reelUrl: String, niche: String): Pair<Int, String> {
        val base = PayloadClient.viralEndpoint(endpoint).trimEnd('/')
        val conn = connection("$base/v2/viral-jobs", "POST", 35_000)
        writeJson(conn, JSONObject()
            .put("url", reelUrl)
            .put("niche", niche)
            .put("source", "android_share"))
        return try { read(conn) } finally { conn.disconnect() }
    }

    fun poll(endpoint: String, jobId: String, token: String): Pair<Int, String> {
        val base = PayloadClient.viralEndpoint(endpoint).trimEnd('/')
        val encodedId = URLEncoder.encode(jobId, "UTF-8")
        val conn = connection("$base/v2/viral-jobs/$encodedId/poll", "POST", 75_000)
        writeJson(conn, JSONObject().put("token", token))
        return try { read(conn) } finally { conn.disconnect() }
    }
}
