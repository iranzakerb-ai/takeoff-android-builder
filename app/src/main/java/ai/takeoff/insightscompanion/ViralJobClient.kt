package ai.takeoff.insightscompanion

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object ViralJobClient {
    private const val CONNECT_TIMEOUT_MS = 20_000
    private const val POLL_READ_TIMEOUT_MS = 295_000
    private const val MAX_ANALYSIS_MS = 30L * 60L * 1000L
    private const val MAX_POLLS = 40

    data class AnalyzeResult(val httpCode: Int, val errorBody: String = "")

    private fun connection(url: String, method: String, readTimeoutMs: Int = POLL_READ_TIMEOUT_MS): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = CONNECT_TIMEOUT_MS
        conn.readTimeout = readTimeoutMs
        conn.instanceFollowRedirects = false
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", "TakeOff-Insights/${BuildConfig.VERSION_NAME}")
        conn.setRequestProperty("Connection", "close")
        return conn
    }

    private fun read(conn: HttpURLConnection): Pair<Int, String> {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        return code to body
    }

    private fun writeJson(conn: HttpURLConnection, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.setFixedLengthStreamingMode(bytes.size)
        conn.outputStream.use { it.write(bytes) }
    }

    fun start(endpoint: String, reelUrl: String, niche: String): Pair<Int, String> {
        val base = PayloadClient.viralEndpoint(endpoint).trimEnd('/')
        val conn = connection("$base/v2/viral-jobs", "POST")
        val body = JSONObject()
            .put("url", reelUrl)
            .put("niche", niche)
            .put("source", "android_share")
            .toString()
        writeJson(conn, body)
        return try { read(conn) } finally { conn.disconnect() }
    }

    internal fun pollPath(jobId: String): String =
        "/v2/viral-jobs/${URLEncoder.encode(jobId, "UTF-8").replace("+", "%20")}/poll"

    internal fun pollPayload(token: String): String = JSONObject().put("token", token).toString()

    fun poll(endpoint: String, jobId: String, token: String): Pair<Int, String> {
        val base = PayloadClient.viralEndpoint(endpoint).trimEnd('/')
        val conn = connection(base + pollPath(jobId), "POST")
        writeJson(conn, pollPayload(token))
        return try { read(conn) } finally { conn.disconnect() }
    }

    internal fun stageLabel(stage: String): String = when (stage) {
        "server_prepare" -> "آماده‌سازی سرور"
        "media_resolve" -> "یافتن فایل اصلی ریلز"
        "media_download" -> "دریافت واقعی ویدیو"
        "ai_upload" -> "ارسال رسانه برای تحلیل"
        "audio_visual_analysis" -> "تحلیل صدا و تصویر"
        "behavioral_analysis" -> "تحلیل رفتاری و Retention"
        "learning_persist" -> "ثبت در حافظه یادگیری"
        "completed" -> "تکمیل تحلیل"
        "failed" -> "پردازش متوقف شد"
        else -> stage
    }

    internal fun errorMessageFa(code: String): String = when (code.trim().lowercase()) {
        "invalid_instagram_reel_url" -> "لینک اینستاگرام معتبر نیست."
        "media_url_unavailable" -> "فایل اصلی این ریلز در حال حاضر قابل دریافت نیست."
        "media_too_large" -> "حجم ویدیو بیش از حد مجاز برای تحلیل است."
        "media_download_failed" -> "دریافت ویدیو از منبع ناموفق بود."
        "gemini_key_unavailable" -> "سرویس تحلیل Gemini روی سرور پیکربندی نشده است."
        "gemini_http_429" -> "سرویس Gemini موقتاً شلوغ است؛ دوباره تلاش می‌شود."
        "gemini_analysis_failed", "gemini_invalid_json", "gemini_file_processing_failed", "gemini_file_processing_timeout" -> "تحلیل ویدیو توسط Gemini کامل نشد."
        "learning_store_failed", "learning_store_http_401" -> "ثبت نتیجه در حافظه یادگیری ناموفق بود."
        "invalid_or_expired_poll_token", "job_token_mismatch", "missing_poll_token" -> "نشست تحلیل منقضی یا نامعتبر شده است."
        "analysis_timeout" -> "زمان تحلیل بیش از حد مجاز شد؛ دوباره تلاش کنید."
        "invalid_start_response", "invalid_poll_response", "server_job_incomplete" -> "پاسخ سرور ناقص یا نامعتبر بود."
        "processing_failed", "server_processing_failed" -> "پردازش روی سرور کامل نشد."
        else -> if (code.startsWith("HTTP ", ignoreCase = true)) "سرور پاسخ ناموفق داد." else "خطای فنی در پردازش رخ داد."
    }

    private fun safeHttpError(code: Int, body: String): String {
        val detail = runCatching { JSONObject(body).optString("detail") }.getOrDefault("")
        return if (detail.isNotBlank()) errorMessageFa(detail) else "خطای HTTP $code"
    }

    internal fun normalizeForUi(raw: JSONObject): JSONObject {
        val out = JSONObject(raw.toString())
        val stage = out.optString("stage")
        val status = out.optString("status")
        out.put("stage_label", stageLabel(stage))
        out.put("type", if (status == "failed" || stage == "failed") "error" else "progress")
        if (out.has("error_code")) {
            out.put("technical_error_code", out.optString("error_code"))
            out.put("error_code", errorMessageFa(out.optString("error_code")))
        }
        if (out.has("media_downloaded_bytes") && !out.has("downloaded_bytes")) {
            out.put("downloaded_bytes", out.optLong("media_downloaded_bytes", 0L))
        }
        if (out.has("media_total_bytes") && !out.has("total_bytes")) {
            out.put("total_bytes", out.optLong("media_total_bytes", 0L))
        }
        return out
    }

    fun analyze(
        endpoint: String,
        reelUrl: String,
        niche: String,
        onEvent: (JSONObject) -> Unit,
    ): AnalyzeResult {
        val startedAt = System.currentTimeMillis()
        val (startCode, startBody) = runCatching { start(endpoint, reelUrl, niche) }
            .getOrElse { return AnalyzeResult(599, "ارتباط با سرور برقرار نشد.") }
        if (startCode !in 200..299) return AnalyzeResult(startCode, safeHttpError(startCode, startBody))

        var state = runCatching { JSONObject(startBody) }
            .getOrElse { return AnalyzeResult(502, errorMessageFa("invalid_start_response")) }
        var polls = 0

        while (true) {
            val event = normalizeForUi(state)
            onEvent(event)

            val status = state.optString("status")
            val stage = state.optString("stage")
            if (status == "completed" || stage == "completed") return AnalyzeResult(200)
            if (status == "failed" || stage == "failed") {
                return AnalyzeResult(200, errorMessageFa(state.optString("error_code", "server_processing_failed")))
            }
            if (System.currentTimeMillis() - startedAt > MAX_ANALYSIS_MS || polls >= MAX_POLLS) {
                return AnalyzeResult(598, errorMessageFa("analysis_timeout"))
            }

            val jobId = state.optString("job_id")
            val token = state.optString("poll_token")
            if (jobId.isBlank() || token.isBlank()) return AnalyzeResult(502, errorMessageFa("missing_poll_token"))

            if (state.optBoolean("retryable", false)) {
                val seconds = state.optInt("retry_after_seconds", 5).coerceIn(1, 30)
                Thread.sleep(seconds * 1000L)
            } else {
                Thread.sleep(250L)
            }

            val (pollCode, pollBody) = runCatching { poll(endpoint, jobId, token) }
                .getOrElse { return AnalyzeResult(599, "ارتباط با سرور در میانه تحلیل قطع شد.") }
            if (pollCode !in 200..299) return AnalyzeResult(pollCode, safeHttpError(pollCode, pollBody))
            state = runCatching { JSONObject(pollBody) }
                .getOrElse { return AnalyzeResult(502, errorMessageFa("invalid_poll_response")) }
            polls++
        }
    }
}
