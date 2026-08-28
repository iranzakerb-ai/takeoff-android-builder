package ai.takeoff.insightscompanion

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

object ViralJobClient {
    private fun connection(url: String, method: String): HttpsURLConnection {
        val conn = URL(url).openConnection() as? HttpsURLConnection
            ?: throw IllegalArgumentException("HTTPS endpoint required")
        conn.requestMethod = method
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
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

    fun start(endpoint: String, reelUrl: String, niche: String): Pair<Int, String> {
        val base = PayloadClient.viralEndpoint(endpoint).trimEnd('/')
        val conn = connection("$base/v2/viral-jobs", "POST")
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        val body = JSONObject()
            .put("url", reelUrl)
            .put("niche", niche)
            .put("source", "android_share")
            .toString()
            .toByteArray(Charsets.UTF_8)
        conn.setFixedLengthStreamingMode(body.size)
        conn.outputStream.use { it.write(body) }
        return try { read(conn) } finally { conn.disconnect() }
    }

    fun poll(endpoint: String, jobId: String, token: String): Pair<Int, String> {
        val base = PayloadClient.viralEndpoint(endpoint).trimEnd('/')
        val encodedId = URLEncoder.encode(jobId, "UTF-8")
        val encodedToken = URLEncoder.encode(token, "UTF-8")
        val conn = connection("$base/v2/viral-jobs/$encodedId?token=$encodedToken", "GET")
        return try { read(conn) } finally { conn.disconnect() }
    }
}
