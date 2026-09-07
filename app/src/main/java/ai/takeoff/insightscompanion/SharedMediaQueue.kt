package ai.takeoff.insightscompanion

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class SharedMediaQueue(context: Context) {
    data class Item(
        val localId: String,
        val url: String,
        val shortcode: String,
        val niche: String,
        val accountId: String?,
        val status: String,
        val stage: String,
        val progress: Int,
        val stageProgress: Int,
        val pipelineStagesJson: String?,
        val mediaKind: String,
        val jobId: String?,
        val pollToken: String?,
        val evidenceId: String?,
        val error: String?,
        val resultJson: String?,
        val createdAt: Long,
        val updatedAt: Long,
        val lane: Int,
    )

    companion object {
        private const val KEY = "shared_media_learning_queue_v1"
        private val LOCK = Any()
        private val urlRegex = Regex(
            "https?://(?:www\\.)?instagram\\.com/(?:reel|reels|p)/[A-Za-z0-9_-]+(?:/[^\\s]*)?",
            RegexOption.IGNORE_CASE,
        )
        private val terminalStatuses = setOf("completed", "dead_letter", "needs_media", "partial")
        private val mediaFallbackErrors = setOf("media_url_unavailable", "media_download_failed")

        fun extractUrls(text: String) = urlRegex.findAll(text).map { m ->
            val raw = m.value.substringBefore('?').trimEnd('/')
            val p = raw.split('/').filter { it.isNotBlank() }
            val type = if (p.any { it.equals("p", true) }) "p" else "reel"
            "https://www.instagram.com/$type/${p.last()}/"
        }.distinct().toList()

        fun shortcode(url: String) = url.trimEnd('/').substringAfterLast('/')
    }

    private val secret = SecretStore(context.applicationContext)

    fun enqueue(url: String, niche: String, accountId: String?): Item = synchronized(LOCK) {
        val canonical = extractUrls(url).firstOrNull() ?: throw IllegalArgumentException("invalid Instagram media URL")
        val arr = read()
        for (i in 0 until arr.length()) {
            val c = arr.optJSONObject(i) ?: continue
            if (c.optString("url") == canonical && c.optString("status") in setOf("queued", "submitting", "processing")) {
                return@synchronized parse(c)
            }
        }
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val lane = ((arr.length() % 3) + 3) % 3
        val o = JSONObject()
            .put("local_id", id)
            .put("url", canonical)
            .put("shortcode", shortcode(canonical))
            .put("niche", niche.ifBlank { "عمومی" })
            .put("account_id", accountId ?: JSONObject.NULL)
            .put("status", "queued")
            .put("stage", "queued")
            .put("progress", 0)
            .put("media_kind", "unknown")
            .put("created_at", now)
            .put("updated_at", now)
            .put("lane", lane)
        arr.put(o)
        write(arr)
        parse(o)
    }

    fun all(): List<Item> = synchronized(LOCK) {
        val a = read()
        (0 until a.length()).mapNotNull { a.optJSONObject(it)?.let(::parse) }.sortedByDescending { it.createdAt }
    }

    fun get(id: String): Item? = synchronized(LOCK) {
        val a = read()
        for (i in 0 until a.length()) {
            val o = a.optJSONObject(i) ?: continue
            if (o.optString("local_id") == id) return@synchronized parse(o)
        }
        null
    }

    fun mutate(id: String, change: (JSONObject) -> Unit): Item? = synchronized(LOCK) {
        val a = read()
        for (i in 0 until a.length()) {
            val o = a.optJSONObject(i) ?: continue
            if (o.optString("local_id") != id) continue
            change(o)
            o.put("updated_at", System.currentTimeMillis())
            write(a)
            return@synchronized parse(o)
        }
        null
    }

    fun attachServerJob(id: String, r: JSONObject) = mutate(id) { o ->
        o.put("job_id", r.optString("job_id"))
        r.optString("poll_token").takeIf { it.isNotBlank() }?.let { o.put("poll_token", it) }
        r.optString("evidence_id").takeIf { it.isNotBlank() }?.let { o.put("evidence_id", it) }
        o.put("status", r.optString("status", "processing"))
            .put("stage", r.optString("stage", "queued"))
            .put("progress", r.optInt("progress", r.optInt("overall_progress_percent", 0)).coerceIn(0, 100))
            .put("stage_progress", r.optInt("stage_progress_percent", 0).coerceIn(0, 100))
        r.optJSONArray("pipeline_stages")?.let { o.put("pipeline_stages", it.toString()) }
        o.put("media_kind", r.optString("media_kind", o.optString("media_kind", "unknown")))
        r.optString("error_code").takeIf { it.isNotBlank() }?.let { o.put("error", it) }
    }

    fun updateServerState(id: String, r: JSONObject) = mutate(id) { o ->
        val currentStatus = o.optString("status", "processing")
        val incomingStatus = r.optString("status", currentStatus)
        val recovery = r.optBoolean("retryable", false)
        val errorCode = r.optString("error_code")

        // Root fix for the former infinite retry storm: a non-retryable server
        // failure is terminal locally. Media acquisition failures become
        // needs_media so the UI can ask for the real file instead of polling forever.
        val mappedStatus = when {
            incomingStatus == "failed" && !recovery && errorCode in mediaFallbackErrors -> "needs_media"
            incomingStatus == "failed" && !recovery -> "dead_letter"
            else -> incomingStatus
        }

        val currentProgress = o.optInt("progress", 0).coerceIn(0, 100)
        val incomingProgress = r.optInt("progress", r.optInt("overall_progress_percent", currentProgress)).coerceIn(0, 100)
        val currentTerminal = currentStatus in terminalStatuses
        val incomingTerminal = mappedStatus in terminalStatuses

        // Never regress a terminal local state. Progress regression is ignored only
        // for genuinely non-terminal, non-retry responses.
        if ((currentTerminal && !incomingTerminal) || (!recovery && !incomingTerminal && incomingProgress < currentProgress)) {
            r.optString("poll_token").takeIf { it.isNotBlank() }?.let { o.put("poll_token", it) }
            return@mutate
        }

        o.put("status", mappedStatus)
        o.put(
            "stage",
            if (mappedStatus == "needs_media") "needs_media"
            else r.optString("display_stage", r.optString("stage", o.optString("stage", "processing"))),
        )
        o.put("progress", incomingProgress)
        o.put("stage_progress", r.optInt("stage_progress_percent", o.optInt("stage_progress", 0)).coerceIn(0, 100))
        r.optJSONArray("pipeline_stages")?.let { o.put("pipeline_stages", it.toString()) }
        r.optString("poll_token").takeIf { it.isNotBlank() }?.let { o.put("poll_token", it) }
        r.optString("media_kind").takeIf { it.isNotBlank() }?.let { o.put("media_kind", it) }
        if (errorCode.isNotBlank()) o.put("error", errorCode) else o.remove("error")
        r.optJSONObject("result")?.let { res ->
            o.put("result_json", res.toString())
            res.optString("evidence_id").takeIf { it.isNotBlank() }?.let { o.put("evidence_id", it) }
            res.optString("media_kind").takeIf { it.isNotBlank() }?.let { o.put("media_kind", it) }
        }
    }

    fun markTransient(id: String, error: String) = mutate(id) { o ->
        if (o.optString("status") !in terminalStatuses) {
            o.put("status", "processing")
            o.put("error", error.take(180))
        }
    }

    fun completeWithEvidence(id: String, e: JSONObject) = mutate(id) { o ->
        o.put("status", "completed").put("stage", "completed").put("progress", 100).put("result_json", e.toString())
        e.optString("evidence_id").takeIf { it.isNotBlank() }?.let { o.put("evidence_id", it) }
        e.optString("media_kind").takeIf { it.isNotBlank() }?.let { o.put("media_kind", it) }
        o.remove("error")
    }

    fun fail(id: String, error: String, terminal: Boolean) = mutate(id) { o ->
        o.put("status", if (terminal) "dead_letter" else "failed").put("stage", "failed").put("error", error.take(180))
    }

    fun restartExpiredServerJob(id: String) = mutate(id) { o ->
        val n = o.optInt("token_restart_count", 0)
        if (n >= 2) {
            o.put("status", "dead_letter").put("stage", "failed").put("error", "token_restart_exhausted")
        } else {
            o.put("token_restart_count", n + 1).put("status", "queued").put("stage", "queued").put("progress", 0).put("stage_progress", 0)
            listOf("job_id", "poll_token", "evidence_id", "pipeline_stages", "result_json", "error").forEach { o.remove(it) }
        }
    }

    fun retry(id: String) = mutate(id) { o ->
        val e = o.optString("error")
        o.put("status", "queued").put("stage", "queued").put("progress", 0)
        o.remove("error")
        if (
            o.optString("job_id").startsWith("cached:") ||
            e.contains("invalid_or_expired_poll_token") ||
            e.contains("job_token_mismatch") ||
            e.startsWith("HTTP 401")
        ) {
            o.remove("job_id")
            o.remove("poll_token")
            o.remove("pipeline_stages")
            o.put("token_restart_count", 0)
        }
    }

    fun trimCompleted(maxCompleted: Int = 120) = synchronized(LOCK) {
        val a = read()
        val k = JSONArray()
        var c = 0
        for (i in 0 until a.length()) {
            val o = a.optJSONObject(i) ?: continue
            val done = o.optString("status") == "completed"
            if (!done || c < maxCompleted) {
                k.put(o)
                if (done) c++
            }
        }
        write(k)
    }

    private fun parse(o: JSONObject) = Item(
        o.optString("local_id"),
        o.optString("url"),
        o.optString("shortcode"),
        o.optString("niche", "عمومی"),
        o.optString("account_id").takeIf { it.isNotBlank() },
        o.optString("status", "queued"),
        o.optString("stage", "queued"),
        o.optInt("progress", 0).coerceIn(0, 100),
        o.optInt("stage_progress", 0).coerceIn(0, 100),
        o.optString("pipeline_stages").takeIf { it.isNotBlank() },
        o.optString("media_kind", "unknown"),
        o.optString("job_id").takeIf { it.isNotBlank() },
        o.optString("poll_token").takeIf { it.isNotBlank() },
        o.optString("evidence_id").takeIf { it.isNotBlank() },
        o.optString("error").takeIf { it.isNotBlank() },
        o.optString("result_json").takeIf { it.isNotBlank() },
        o.optLong("created_at"),
        o.optLong("updated_at"),
        o.optInt("lane", 0).coerceIn(0, 2),
    )

    private fun read() = runCatching { JSONArray(secret.get(KEY) ?: "[]") }.getOrElse { JSONArray() }
    private fun write(a: JSONArray) = secret.put(KEY, a.toString())
}
