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
        private val urlRegex = Regex("https?://(?:www\\.)?instagram\\.com/(?:reel|reels|p)/[A-Za-z0-9_-]+(?:/[^\\s]*)?", RegexOption.IGNORE_CASE)

        fun extractUrls(text: String): List<String> = urlRegex.findAll(text)
            .map { match ->
                val raw = match.value.substringBefore('?').trimEnd('/')
                val parts = raw.split('/').filter { it.isNotBlank() }
                val type = if (parts.any { it.equals("p", true) }) "p" else "reel"
                val shortcode = parts.last()
                "https://www.instagram.com/$type/$shortcode/"
            }
            .distinct()
            .toList()

        fun shortcode(url: String): String = url.trimEnd('/').substringAfterLast('/')

        internal fun trimCompletedItems(source: JSONArray, maxCompleted: Int): JSONArray {
            val limit = maxCompleted.coerceAtLeast(0)
            var completedTotal = 0
            for (i in 0 until source.length()) {
                if (source.optJSONObject(i)?.optString("status") == "completed") completedTotal++
            }
            var completedToDrop = (completedTotal - limit).coerceAtLeast(0)
            val keep = JSONArray()
            for (i in 0 until source.length()) {
                val obj = source.optJSONObject(i) ?: continue
                if (obj.optString("status") == "completed" && completedToDrop > 0) {
                    completedToDrop--
                    continue
                }
                keep.put(obj)
            }
            return keep
        }

        internal fun evidenceIsVideoBacked(root: JSONObject): Boolean {
            val evidence = root.optJSONObject("evidence_quality")
            if (evidence?.has("video_analyzed") == true) return evidence.optBoolean("video_analyzed", false)
            val mode = root.optString("analysis_mode")
            if ("multimodal" in mode || "device_capture" in mode || "video" in mode && "metadata_only" !in mode) return true
            val status = root.optString("status")
            if (status == "partial") return false
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
            if (current.optString("url") == canonical && current.optString("status") in setOf("queued", "submitting", "processing", "partial", "needs_media")) {
                return@synchronized parse(current)
            }
        }
        val now = System.currentTimeMillis()
        val localId = UUID.randomUUID().toString()
        val lane = ((arr.length() % 3) + 3) % 3
        val obj = JSONObject()
            .put("local_id", localId)
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
        arr.put(obj)
        write(arr)
        parse(obj)
    }

    fun all(): List<Item> = synchronized(LOCK) {
        val arr = read()
        (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.let(::parse) }
            .sortedByDescending { it.createdAt }
    }

    fun get(localId: String): Item? = synchronized(LOCK) {
        val arr = read()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            if (obj.optString("local_id") == localId) return@synchronized parse(obj)
        }
        null
    }

    fun mutate(localId: String, change: (JSONObject) -> Unit): Item? = synchronized(LOCK) {
        val arr = read()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            if (obj.optString("local_id") != localId) continue
            change(obj)
            obj.put("updated_at", System.currentTimeMillis())
            write(arr)
            return@synchronized parse(obj)
        }
        null
    }

    fun attachLocalMedia(localId: String, path: String, mime: String?): Item? = mutate(localId) { obj ->
        obj.put("local_media_path", path)
        obj.put("local_media_mime", mime ?: "video/mp4")
        obj.put("media_kind", "reel")
        obj.put("status", "queued")
        obj.put("stage", "media_ready")
        obj.put("progress", maxOf(18, obj.optInt("progress", 0)))
        obj.remove("error")
        obj.remove("job_id")
        obj.remove("poll_token")
    }

    fun newestAwaitingMedia(): Item? = all().firstOrNull { it.status in setOf("needs_media", "partial", "failed") || it.stage == "media_required" }

    fun recoverLegacyCredentialFailures(): List<Item> = synchronized(LOCK) {
        val arr = read()
        val recovered = mutableListOf<Item>()
        var changed = false
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val status = obj.optString("status")
            if (status !in setOf("failed", "dead_letter")) continue
            val error = obj.optString("error").lowercase()
            val obsoleteCredentialFailure = listOf("companion_credential_required", "invalid companion key", "master companion key required", "http 401", "401 unauthorized").any { it in error }
            if (!obsoleteCredentialFailure) continue
            obj.put("status", "queued").put("stage", "queued").put("progress", 0)
            obj.remove("error"); obj.remove("job_id"); obj.remove("poll_token")
            obj.put("updated_at", System.currentTimeMillis())
            recovered += parse(obj)
            changed = true
        }
        if (changed) write(arr)
        recovered
    }

    fun attachServerJob(localId: String, root: JSONObject): Item? = mutate(localId) { obj ->
        obj.put("job_id", root.optString("job_id"))
        root.optString("poll_token").takeIf { it.isNotBlank() }?.let { obj.put("poll_token", it) }
        root.optString("evidence_id").takeIf { it.isNotBlank() }?.let { obj.put("evidence_id", it) }
        obj.put("status", root.optString("status", "processing"))
        obj.put("stage", root.optString("stage", "queued"))
        obj.put("progress", root.optInt("progress", 0).coerceIn(0, 100))
        obj.put("media_kind", root.optString("media_kind", obj.optString("media_kind", "unknown")))
        root.optString("error_code").takeIf { it.isNotBlank() }?.let { obj.put("error", it) }
    }

    fun updateServerState(localId: String, root: JSONObject): Item? = mutate(localId) { obj ->
        obj.put("status", root.optString("status", obj.optString("status", "processing")))
        obj.put("stage", root.optString("stage", obj.optString("stage", "processing")))
        obj.put("progress", root.optInt("progress", obj.optInt("progress", 0)).coerceIn(0, 100))
        root.optString("media_kind").takeIf { it.isNotBlank() }?.let { obj.put("media_kind", it) }
        root.optString("error_code").takeIf { it.isNotBlank() }?.let { obj.put("error", it) }
        val result = root.optJSONObject("result")
        if (result != null) {
            obj.put("result_json", result.toString())
            result.optString("evidence_id").takeIf { it.isNotBlank() }?.let { obj.put("evidence_id", it) }
            result.optString("media_kind").takeIf { it.isNotBlank() }?.let { obj.put("media_kind", it) }
        }
    }

    fun completeWithEvidence(localId: String, evidence: JSONObject): Item? = mutate(localId) { obj ->
        obj.put("status", "completed").put("stage", "completed").put("progress", 100)
        obj.put("result_json", evidence.toString())
        evidence.optString("evidence_id").takeIf { it.isNotBlank() }?.let { obj.put("evidence_id", it) }
        evidence.optString("media_kind").takeIf { it.isNotBlank() }?.let { obj.put("media_kind", it) }
        obj.remove("error")
    }

    fun markNeedsMedia(localId: String, evidence: JSONObject?, reason: String = "public_web_media_url_unavailable"): Item? = mutate(localId) { obj ->
        obj.put("status", "needs_media")
        obj.put("stage", "media_required")
        obj.put("progress", 22)
        obj.put("error", reason.take(180))
        if (evidence != null) obj.put("result_json", evidence.toString())
    }

    fun fail(localId: String, error: String, terminal: Boolean): Item? = mutate(localId) { obj ->
        obj.put("status", if (terminal) "dead_letter" else "failed")
        obj.put("stage", "failed")
        obj.put("error", error.take(180))
    }

    fun retry(localId: String): Item? = mutate(localId) { obj ->
        obj.put("status", "queued").put("stage", "queued").put("progress", 0)
        obj.remove("error")
        if (obj.optString("job_id").startsWith("cached:")) { obj.remove("job_id"); obj.remove("poll_token") }
    }

    fun trimCompleted(maxCompleted: Int = 120) = synchronized(LOCK) { write(trimCompletedItems(read(), maxCompleted)) }

    private fun parse(obj: JSONObject) = Item(
        localId = obj.optString("local_id"),
        url = obj.optString("url"),
        shortcode = obj.optString("shortcode"),
        niche = obj.optString("niche", "عمومی"),
        accountId = obj.optString("account_id").takeIf { it.isNotBlank() },
        status = obj.optString("status", "queued"),
        stage = obj.optString("stage", "queued"),
        progress = obj.optInt("progress", 0).coerceIn(0, 100),
        mediaKind = obj.optString("media_kind", "unknown"),
        localMediaPath = obj.optString("local_media_path").takeIf { it.isNotBlank() },
        localMediaMime = obj.optString("local_media_mime").takeIf { it.isNotBlank() },
        jobId = obj.optString("job_id").takeIf { it.isNotBlank() },
        pollToken = obj.optString("poll_token").takeIf { it.isNotBlank() },
        evidenceId = obj.optString("evidence_id").takeIf { it.isNotBlank() },
        error = obj.optString("error").takeIf { it.isNotBlank() },
        resultJson = obj.optString("result_json").takeIf { it.isNotBlank() },
        createdAt = obj.optLong("created_at"), updatedAt = obj.optLong("updated_at"), lane = obj.optInt("lane", 0).coerceIn(0, 2),
    )

    private fun read(): JSONArray = runCatching { JSONArray(secret.get(KEY) ?: "[]") }.getOrElse { JSONArray() }
    private fun write(arr: JSONArray) = secret.put(KEY, arr.toString())
}
