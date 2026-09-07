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
        val mediaKind: String,
        val localMediaPath: String?,
        val localMediaMime: String?,
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
        private val TERMINAL = setOf("completed", "dead_letter", "needs_media", "partial")
        private val MEDIA_REQUIRED_ERRORS = setOf("media_url_unavailable", "media_download_failed", "instagram_direct_media_url_unavailable")
        private val urlRegex = Regex("https?://(?:www\\.)?instagram\\.com/(?:reel|reels|p)/[A-Za-z0-9_-]+(?:/[^\\s]*)?", RegexOption.IGNORE_CASE)

        fun extractUrls(text: String): List<String> = urlRegex.findAll(text)
            .map { match ->
                val raw = match.value.substringBefore('?').trimEnd('/')
                val parts = raw.split('/').filter { it.isNotBlank() }
                val type = if (parts.any { it.equals("p", true) }) "p" else "reel"
                "https://www.instagram.com/$type/${parts.last()}/"
            }.distinct().toList()

        fun shortcode(url: String): String = url.trimEnd('/').substringAfterLast('/')

        internal fun trimCompletedItems(source: JSONArray, maxCompleted: Int): JSONArray {
            val limit = maxCompleted.coerceAtLeast(0)
            var completedTotal = 0
            for (i in 0 until source.length()) if (source.optJSONObject(i)?.optString("status") == "completed") completedTotal++
            var completedToDrop = (completedTotal - limit).coerceAtLeast(0)
            val keep = JSONArray()
            for (i in 0 until source.length()) {
                val obj = source.optJSONObject(i) ?: continue
                if (obj.optString("status") == "completed" && completedToDrop > 0) { completedToDrop--; continue }
                keep.put(obj)
            }
            return keep
        }

        internal fun evidenceIsVideoBacked(root: JSONObject): Boolean {
            val evidence = root.optJSONObject("evidence_quality")
            if (evidence?.has("video_analyzed") == true) return evidence.optBoolean("video_analyzed", false)
            val mode = root.optString("analysis_mode")
            if ("multimodal" in mode || "device_capture" in mode || "video" in mode && "metadata_only" !in mode) return true
            if (root.optString("status") == "partial") return false
            val privateStatus = root.optJSONObject("estimated_private_metrics")?.optString("status").orEmpty()
            return privateStatus != "insufficient_video_evidence" && root.optString("reason") != "public_web_media_url_unavailable"
        }
    }

    private val secret = SecretStore(context.applicationContext)

    fun enqueue(url: String, niche: String, accountId: String?): Item = synchronized(LOCK) {
        val canonical = extractUrls(url).firstOrNull() ?: throw IllegalArgumentException("invalid Instagram media URL")
        val arr = read()
        for (i in 0 until arr.length()) {
            val current = arr.optJSONObject(i) ?: continue
            if (current.optString("url") == canonical && current.optString("status") in setOf("queued", "submitting", "processing", "partial", "needs_media")) return@synchronized parse(current)
        }
        val now = System.currentTimeMillis()
        val obj = JSONObject().put("local_id", UUID.randomUUID().toString()).put("url", canonical)
            .put("shortcode", shortcode(canonical)).put("niche", niche.ifBlank { "عمومی" })
            .put("account_id", accountId ?: JSONObject.NULL).put("status", "queued").put("stage", "queued")
            .put("progress", 0).put("media_kind", "unknown").put("created_at", now).put("updated_at", now)
            .put("lane", ((arr.length() % 3) + 3) % 3)
        arr.put(obj); write(arr); parse(obj)
    }

    fun all(): List<Item> = synchronized(LOCK) { val arr = read(); (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.let(::parse) }.sortedByDescending { it.createdAt } }
    fun get(localId: String): Item? = synchronized(LOCK) { val arr = read(); for (i in 0 until arr.length()) { val obj = arr.optJSONObject(i) ?: continue; if (obj.optString("local_id") == localId) return@synchronized parse(obj) }; null }
    fun mutate(localId: String, change: (JSONObject) -> Unit): Item? = synchronized(LOCK) {
        val arr = read(); for (i in 0 until arr.length()) { val obj = arr.optJSONObject(i) ?: continue; if (obj.optString("local_id") != localId) continue; change(obj); obj.put("updated_at", System.currentTimeMillis()); write(arr); return@synchronized parse(obj) }; null
    }

    fun attachLocalMedia(localId: String, path: String, mime: String?): Item? = mutate(localId) { obj ->
        obj.put("local_media_path", path).put("local_media_mime", mime ?: "video/mp4").put("media_kind", "reel")
            .put("status", "queued").put("stage", "media_ready").put("progress", maxOf(18, obj.optInt("progress", 0)))
        obj.remove("error"); obj.remove("job_id"); obj.remove("poll_token")
    }
    fun newestAwaitingMedia(): Item? = all().firstOrNull { it.status in setOf("needs_media", "partial", "failed") || it.stage == "media_required" }

    fun recoverLegacyCredentialFailures(): List<Item> = synchronized(LOCK) {
        val arr = read(); val recovered = mutableListOf<Item>(); var changed = false
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue; if (obj.optString("status") !in setOf("failed", "dead_letter")) continue
            val error = obj.optString("error").lowercase()
            if (listOf("companion_credential_required", "invalid companion key", "master companion key required", "http 401", "401 unauthorized").none { it in error }) continue
            obj.put("status", "queued").put("stage", "queued").put("progress", 0); obj.remove("error"); obj.remove("job_id"); obj.remove("poll_token"); obj.put("updated_at", System.currentTimeMillis()); recovered += parse(obj); changed = true
        }
        if (changed) write(arr); recovered
    }

    fun attachServerJob(localId: String, root: JSONObject): Item? = mutate(localId) { obj ->
        obj.put("job_id", root.optString("job_id")); root.optString("poll_token").takeIf { it.isNotBlank() }?.let { obj.put("poll_token", it) }
        root.optString("evidence_id").takeIf { it.isNotBlank() }?.let { obj.put("evidence_id", it) }
        obj.put("status", root.optString("status", "processing")).put("stage", root.optString("display_stage", root.optString("stage", "queued")))
            .put("progress", root.optInt("progress", root.optInt("overall_progress_percent", 0)).coerceIn(0, 100))
            .put("media_kind", root.optString("media_kind", obj.optString("media_kind", "unknown")))
        val error = root.optString("error_code"); if (error.isNotBlank()) obj.put("error", error) else obj.remove("error")
    }

    fun updateServerState(localId: String, root: JSONObject): Item? = mutate(localId) { obj ->
        val currentStatus = obj.optString("status", "processing")
        val rawIncomingStatus = root.optString("status", currentStatus)
        val errorCode = root.optString("error_code")
        val recovery = root.optBoolean("retryable", false)
        val incomingStatus = when {
            rawIncomingStatus == "failed" && !recovery && errorCode in MEDIA_REQUIRED_ERRORS -> "needs_media"
            rawIncomingStatus == "failed" && !recovery -> "dead_letter"
            else -> rawIncomingStatus
        }
        val currentProgress = obj.optInt("progress", 0).coerceIn(0, 100)
        val incomingProgress = root.optInt("progress", root.optInt("overall_progress_percent", currentProgress)).coerceIn(0, 100)
        val currentTerminal = currentStatus in TERMINAL
        val incomingTerminal = incomingStatus in TERMINAL
        if ((currentTerminal && !incomingTerminal) || (!recovery && !incomingTerminal && incomingProgress < currentProgress)) {
            root.optString("poll_token").takeIf { it.isNotBlank() }?.let { obj.put("poll_token", it) }
            return@mutate
        }
        obj.put("status", incomingStatus)
            .put("stage", if (incomingStatus == "needs_media") "media_required" else root.optString("display_stage", root.optString("stage", obj.optString("stage", "processing"))))
            .put("progress", if (recovery) maxOf(currentProgress, incomingProgress) else incomingProgress)
        root.optString("poll_token").takeIf { it.isNotBlank() }?.let { obj.put("poll_token", it) }
        root.optString("media_kind").takeIf { it.isNotBlank() }?.let { obj.put("media_kind", it) }
        if (errorCode.isNotBlank()) obj.put("error", errorCode) else obj.remove("error")
        root.optJSONObject("result")?.let { result ->
            obj.put("result_json", result.toString())
            result.optString("evidence_id").takeIf { it.isNotBlank() }?.let { obj.put("evidence_id", it) }
            result.optString("media_kind").takeIf { it.isNotBlank() }?.let { obj.put("media_kind", it) }
        }
    }

    fun markTransient(localId: String, error: String): Item? = mutate(localId) { obj ->
        if (obj.optString("status") !in TERMINAL) { obj.put("status", "processing"); obj.put("error", error.take(180)) }
    }

    fun restartExpiredServerJob(localId: String): Item? = mutate(localId) { obj ->
        val count = obj.optInt("token_restart_count", 0)
        if (count >= 2) obj.put("status", "dead_letter").put("stage", "failed").put("error", "token_restart_exhausted")
        else {
            obj.put("token_restart_count", count + 1).put("status", "queued").put("stage", "queued").put("progress", 0)
            listOf("job_id", "poll_token", "evidence_id", "result_json", "error").forEach { obj.remove(it) }
        }
    }

    fun completeWithEvidence(localId: String, evidence: JSONObject): Item? = mutate(localId) { obj ->
        obj.put("status", "completed").put("stage", "completed").put("progress", 100).put("result_json", evidence.toString())
        evidence.optString("evidence_id").takeIf { it.isNotBlank() }?.let { obj.put("evidence_id", it) }; evidence.optString("media_kind").takeIf { it.isNotBlank() }?.let { obj.put("media_kind", it) }; obj.remove("error")
    }
    fun markNeedsMedia(localId: String, evidence: JSONObject?, reason: String = "public_web_media_url_unavailable"): Item? = mutate(localId) { obj -> obj.put("status", "needs_media").put("stage", "media_required").put("progress", 22).put("error", reason.take(180)); if (evidence != null) obj.put("result_json", evidence.toString()) }
    fun fail(localId: String, error: String, terminal: Boolean): Item? = mutate(localId) { obj -> obj.put("status", if (terminal) "dead_letter" else "failed").put("stage", "failed").put("error", error.take(180)) }
    fun retry(localId: String): Item? = mutate(localId) { obj ->
        val error = obj.optString("error")
        obj.put("status", "queued").put("stage", "queued").put("progress", 0).remove("error")
        if (obj.optString("job_id").startsWith("cached:") || error.contains("invalid_or_expired_poll_token") || error.contains("job_token_mismatch") || error.startsWith("HTTP 401")) {
            obj.remove("job_id"); obj.remove("poll_token"); obj.put("token_restart_count", 0)
        }
    }
    fun trimCompleted(maxCompleted: Int = 120) = synchronized(LOCK) { write(trimCompletedItems(read(), maxCompleted)) }

    private fun parse(obj: JSONObject) = Item(
        obj.optString("local_id"), obj.optString("url"), obj.optString("shortcode"), obj.optString("niche", "عمومی"), obj.optString("account_id").takeIf { it.isNotBlank() },
        obj.optString("status", "queued"), obj.optString("stage", "queued"), obj.optInt("progress", 0).coerceIn(0, 100), obj.optString("media_kind", "unknown"),
        obj.optString("local_media_path").takeIf { it.isNotBlank() }, obj.optString("local_media_mime").takeIf { it.isNotBlank() }, obj.optString("job_id").takeIf { it.isNotBlank() },
        obj.optString("poll_token").takeIf { it.isNotBlank() }, obj.optString("evidence_id").takeIf { it.isNotBlank() }, obj.optString("error").takeIf { it.isNotBlank() },
        obj.optString("result_json").takeIf { it.isNotBlank() }, obj.optLong("created_at"), obj.optLong("updated_at"), obj.optInt("lane", 0).coerceIn(0, 2),
    )
    private fun read(): JSONArray = runCatching { JSONArray(secret.get(KEY) ?: "[]") }.getOrElse { JSONArray() }
    private fun write(arr: JSONArray) = secret.put(KEY, arr.toString())
}
