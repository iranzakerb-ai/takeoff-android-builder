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
import java.util.concurrent.TimeUnit

class SharedMediaWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val localId = inputData.getString("local_id").orEmpty()
        if (localId.isBlank()) return@withContext Result.failure()
        val queue = SharedMediaQueue(applicationContext)
        var item = queue.get(localId) ?: return@withContext Result.success()
        if (item.status == "completed") return@withContext Result.success()

        val prefs = applicationContext.getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE)
        val endpoint = PayloadClient.viralEndpoint(prefs.getString("endpoint", "").orEmpty())
        val companionKey = SecretStore(applicationContext).get("api_key").orEmpty()

        // Public Instagram media is not private Owner data. If this installation has
        // not been paired yet, use the existing public viral-evidence boundary rather
        // than dead-lettering the job with companion_credential_required. Private
        // Owner Insights paths remain credential-protected elsewhere in the app.
        if (companionKey.isBlank()) {
            return@withContext processPublicFallback(queue, item, endpoint)
        }

        try {
            if (item.jobId.isNullOrBlank()) {
                queue.mutate(localId) { it.put("status", "submitting").put("stage", "submitting").put("progress", 1) }
                val start = SharedMediaClient.start(
                    endpoint,
                    item.url,
                    item.niche,
                    item.accountId,
                    forceRefresh = true,
                    companionKey = companionKey,
                )
                if (start.code !in 200..299 || start.body == null) {
                    val terminal = start.code in 400..499 && start.code !in listOf(408, 425, 429)
                    queue.fail(localId, "HTTP ${start.code}: ${safeDetail(start.raw, start.body)}", terminal)
                    return@withContext if (terminal) Result.success() else Result.retry()
                }
                item = queue.attachServerJob(localId, start.body) ?: return@withContext Result.success()
            }

            item = queue.get(localId) ?: return@withContext Result.success()
            val jobId = item.jobId.orEmpty()
            val token = item.pollToken.orEmpty()
            if (jobId.isBlank() || token.isBlank()) {
                queue.fail(localId, "server_job_token_missing", true)
                return@withContext Result.success()
            }

            queue.mutate(localId) {
                it.put("status", "processing")
                    .put("progress", maxOf(2, it.optInt("progress", 0)))
            }
            val processed = SharedMediaClient.process(endpoint, jobId, token, companionKey)
            if (processed.code !in 200..299 || processed.body == null) {
                val terminal = processed.code in 400..499 && processed.code !in listOf(408, 409, 425, 429)
                queue.fail(localId, "HTTP ${processed.code}: ${safeDetail(processed.raw, processed.body)}", terminal)
                return@withContext if (terminal || runAttemptCount >= 5) Result.success() else Result.retry()
            }

            val updated = queue.updateServerState(localId, processed.body)
            return@withContext when (updated?.status) {
                "completed", "dead_letter" -> Result.success()
                "failed" -> if (runAttemptCount >= 5) Result.success() else Result.retry()
                null -> Result.success()
                else -> {
                    SharedMediaWork.enqueueContinuation(applicationContext, updated)
                    Result.success()
                }
            }
        } catch (error: Exception) {
            queue.fail(localId, error.javaClass.simpleName, runAttemptCount >= 5)
            return@withContext if (runAttemptCount >= 5) Result.success() else Result.retry()
        }
    }

    private fun processPublicFallback(queue: SharedMediaQueue, item: SharedMediaQueue.Item, endpoint: String): Result {
        val localId = item.localId
        return try {
            queue.mutate(localId) {
                it.put("status", "processing")
                    .put("stage", "public_analysis")
                    .put("progress", 12)
                    .remove("error")
            }
            val response = PayloadClient.postViralEvidence(endpoint, "", item.url, item.niche)
            if (response.first !in 200..299) {
                val root = runCatching { JSONObject(response.second) }.getOrNull()
                val detail = root?.optString("detail").orEmpty().ifBlank { root?.optString("error_code").orEmpty() }
                val terminal = response.first in 400..499 && response.first !in listOf(408, 425, 429)
                queue.fail(localId, "HTTP ${response.first}: ${detail.take(100).ifBlank { "server_error" }}", terminal)
                if (terminal || runAttemptCount >= 5) Result.success() else Result.retry()
            } else {
                val root = runCatching { JSONObject(response.second) }.getOrNull()
                    ?: throw IllegalStateException("invalid_public_analysis_response")
                val evidence = root.optJSONObject("result")
                    ?: root.optJSONObject("report")
                    ?: root
                queue.completeWithEvidence(localId, evidence)
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
        .addTag("takeoff-v4-media")
        .addTag("takeoff-v4-media-${item.localId}")
        .build()

    fun enqueue(context: Context, item: SharedMediaQueue.Item) {
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "takeoff-v4-media-lane-${item.lane.coerceIn(0, LANES - 1)}",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request(item),
        )
    }

    fun enqueueContinuation(context: Context, item: SharedMediaQueue.Item) {
        enqueue(context, item)
    }

    fun retry(context: Context, localId: String) {
        val queue = SharedMediaQueue(context)
        val item = queue.retry(localId) ?: return
        enqueue(context, item)
    }
}
