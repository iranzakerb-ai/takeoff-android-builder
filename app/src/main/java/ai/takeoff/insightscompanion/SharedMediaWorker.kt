package ai.takeoff.insightscompanion

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class SharedMediaWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val localId = inputData.getString("local_id").orEmpty()
        if (localId.isBlank()) return@withContext Result.failure()
        val queue = SharedMediaQueue(applicationContext)
        var item = queue.get(localId) ?: return@withContext Result.success()
        if (item.status == "completed") return@withContext Result.success()

        val prefs = applicationContext.getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE)
        val endpoint = PayloadClient.viralEndpoint(prefs.getString("endpoint", "").orEmpty())
        val companionKey = SecretStore(applicationContext).get("api_key").orEmpty()

        // Highest-fidelity path first: if Android actually received a video stream and
        // this installation is paired, analyze those real bytes instead of re-fetching Instagram.
        val mediaPath = item.localMediaPath
        if (!mediaPath.isNullOrBlank() && companionKey.isNotBlank()) {
            val file = File(mediaPath)
            if (file.isFile && file.length() > 0L) {
                return@withContext processDirectMedia(queue, item, endpoint, companionKey, file)
            }
        }

        // URL-only public shares remain usable without Owner credentials. The server
        // now has layered public/direct/provider resolution, but metadata-only results
        // are never promoted to learned/completed on the phone.
        if (companionKey.isBlank()) return@withContext processPublicFallback(queue, item, endpoint)

        try {
            if (item.jobId.isNullOrBlank()) {
                queue.mutate(localId) { it.put("status", "submitting").put("stage", "submitting").put("progress", 4) }
                val start = SharedMediaClient.start(endpoint, item.url, item.niche, item.accountId, true, companionKey)
                if (start.code !in 200..299 || start.body == null) {
                    val terminal = start.code in 400..499 && start.code !in listOf(408, 425, 429)
                    queue.fail(localId, "HTTP ${start.code}: ${safeDetail(start.raw, start.body)}", terminal)
                    return@withContext if (terminal) Result.success() else Result.retry()
                }
                item = queue.attachServerJob(localId, start.body) ?: return@withContext Result.success()
            }

            item = queue.get(localId) ?: return@withContext Result.success()
            val jobId = item.jobId.orEmpty(); val token = item.pollToken.orEmpty()
            if (jobId.isBlank() || token.isBlank()) {
                queue.fail(localId, "server_job_token_missing", true)
                return@withContext Result.success()
            }

            queue.mutate(localId) { it.put("status", "processing").put("progress", maxOf(8, it.optInt("progress", 0))) }
            val processed = SharedMediaClient.process(endpoint, jobId, token, companionKey)
            if (processed.code !in 200..299 || processed.body == null) {
                val terminal = processed.code in 400..499 && processed.code !in listOf(408, 409, 425, 429)
                queue.fail(localId, "HTTP ${processed.code}: ${safeDetail(processed.raw, processed.body)}", terminal)
                return@withContext if (terminal || runAttemptCount >= 5) Result.success() else Result.retry()
            }

            val updated = queue.updateServerState(localId, processed.body)
            return@withContext when (updated?.status) {
                "completed", "dead_letter", "needs_media", "partial" -> Result.success()
                "failed" -> if (runAttemptCount >= 5) Result.success() else Result.retry()
                null -> Result.success()
                else -> { SharedMediaWork.enqueueContinuation(applicationContext, updated); Result.success() }
            }
        } catch (error: Exception) {
            queue.fail(localId, error.javaClass.simpleName, runAttemptCount >= 5)
            return@withContext if (runAttemptCount >= 5) Result.success() else Result.retry()
        }
    }

    private fun processDirectMedia(queue: SharedMediaQueue, item: SharedMediaQueue.Item, endpoint: String, companionKey: String, file: File): Result {
        val localId = item.localId
        return try {
            queue.mutate(localId) { it.put("status", "processing").put("stage", "media_upload").put("progress", 32).remove("error") }
            val response = SharedMediaClient.uploadDirectMedia(endpoint, item.url, item.niche, file, item.localMediaMime, companionKey)
            if (response.code !in 200..299 || response.body == null) {
                val terminal = response.code in 400..499 && response.code !in listOf(408, 425, 429)
                queue.fail(localId, "HTTP ${response.code}: ${safeDetail(response.raw, response.body)}", terminal)
                if (terminal || runAttemptCount >= 4) Result.success() else Result.retry()
            } else {
                val root = response.body
                val evidence = root.optJSONObject("report") ?: root.optJSONObject("result") ?: root
                if (!SharedMediaQueue.evidenceIsVideoBacked(evidence)) {
                    queue.markNeedsMedia(localId, evidence, "direct_media_analysis_insufficient")
                } else {
                    queue.mutate(localId) { it.put("stage", "learning_persist").put("progress", 92) }
                    queue.completeWithEvidence(localId, evidence)
                    runCatching { file.delete() }
                }
                Result.success()
            }
        } catch (error: Exception) {
            queue.fail(localId, error.javaClass.simpleName, runAttemptCount >= 4)
            if (runAttemptCount >= 4) Result.success() else Result.retry()
        }
    }

    private fun processPublicFallback(queue: SharedMediaQueue, item: SharedMediaQueue.Item, endpoint: String): Result {
        val localId = item.localId
        return try {
            queue.mutate(localId) { it.put("status", "processing").put("stage", "media_fetch").put("progress", 18).remove("error") }
            val response = PayloadClient.postViralEvidence(endpoint, "", item.url, item.niche)
            if (response.first !in 200..299) {
                val root = runCatching { JSONObject(response.second) }.getOrNull()
                val detail = root?.optString("detail").orEmpty().ifBlank { root?.optString("error_code").orEmpty() }
                val terminal = response.first in 400..499 && response.first !in listOf(408, 425, 429)
                queue.fail(localId, "HTTP ${response.first}: ${detail.take(100).ifBlank { "server_error" }}", terminal)
                if (terminal || runAttemptCount >= 5) Result.success() else Result.retry()
            } else {
                val root = runCatching { JSONObject(response.second) }.getOrNull() ?: throw IllegalStateException("invalid_public_analysis_response")
                val evidence = root.optJSONObject("result") ?: root.optJSONObject("report") ?: root
                val backendStatus = root.optString("status").ifBlank { evidence.optString("status") }
                val videoBacked = SharedMediaQueue.evidenceIsVideoBacked(evidence)
                if (backendStatus == "partial" || !videoBacked) {
                    val reason = evidence.optJSONObject("evidence_quality")?.optString("reason").orEmpty().ifBlank { "public_web_media_url_unavailable" }
                    queue.markNeedsMedia(localId, evidence, reason)
                } else {
                    queue.mutate(localId) { it.put("stage", "learning_persist").put("progress", 94) }
                    queue.completeWithEvidence(localId, evidence)
                }
                Result.success()
            }
        } catch (error: Exception) {
            queue.fail(localId, error.javaClass.simpleName, runAttemptCount >= 5)
            if (runAttemptCount >= 5) Result.success() else Result.retry()
        }
    }

    private fun safeDetail(raw: String, body: JSONObject?): String {
        val root = body ?: runCatching { JSONObject(raw) }.getOrNull()
        val detail = root?.optString("detail").orEmpty().ifBlank { root?.optString("error_code").orEmpty() }
        return detail.take(100).ifBlank { "server_error" }
    }
}

object SharedMediaWork {
    private const val LANES = 3
    private fun request(item: SharedMediaQueue.Item) = OneTimeWorkRequestBuilder<SharedMediaWorker>()
        .setInputData(Data.Builder().putString("local_id", item.localId).build())
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
        .addTag("takeoff-v4-media").addTag("takeoff-v4-media-${item.localId}").build()

    fun enqueue(context: Context, item: SharedMediaQueue.Item) {
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork("takeoff-v4-media-lane-${item.lane.coerceIn(0, LANES - 1)}", ExistingWorkPolicy.APPEND_OR_REPLACE, request(item))
    }
    fun enqueueContinuation(context: Context, item: SharedMediaQueue.Item) = enqueue(context, item)
    fun retry(context: Context, localId: String) { SharedMediaQueue(context).retry(localId)?.let { enqueue(context, it) } }
}
