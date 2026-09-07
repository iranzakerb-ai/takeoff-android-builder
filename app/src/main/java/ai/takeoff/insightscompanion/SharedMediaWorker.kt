package ai.takeoff.insightscompanion

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OutOfQuotaPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

private val MEDIA_TERMINAL_STATUSES = setOf("completed", "dead_letter", "needs_media", "partial")

class SharedMediaRecoveryWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val localId = inputData.getString("local_id").orEmpty()
        val item = SharedMediaQueue(applicationContext).get(localId) ?: return Result.success()
        if (item.status !in MEDIA_TERMINAL_STATUSES) SharedMediaWork.enqueue(applicationContext, item)
        return Result.success()
    }
}

class SharedMediaWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    companion object {
        // Free-tier Gemini is the expensive shared bottleneck. Keep download,
        // resolve and bookkeeping jobs independent/concurrent, but run exactly one
        // heavy multimodal provider request from this process at a time.
        private val AI_ANALYSIS_GATE = Semaphore(1, true)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val localId = inputData.getString("local_id").orEmpty()
        if (localId.isBlank()) return@withContext Result.failure()
        val queue = SharedMediaQueue(applicationContext)
        var item = queue.get(localId) ?: return@withContext Result.success()
        if (item.status in MEDIA_TERMINAL_STATUSES) return@withContext Result.success()

        val prefs = applicationContext.getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE)
        val endpoint = PayloadClient.viralEndpoint(prefs.getString("endpoint", "").orEmpty())
        val companionKey = SecretStore(applicationContext).get("api_key").orEmpty()

        try {
            if (item.jobId.isNullOrBlank()) {
                queue.mutate(localId) { it.put("status", "submitting").put("stage", "submitting").put("progress", 1) }
                val start = SharedMediaClient.start(endpoint, item.url, item.niche, item.accountId, true, companionKey)
                if (start.code !in 200..299 || start.body == null) {
                    val terminal = start.code in 400..499 && start.code !in listOf(408, 425, 429)
                    queue.fail(localId, "HTTP ${start.code}: ${safeDetail(start.raw, start.body)}", terminal)
                    if (!terminal) queue.get(localId)?.let { SharedMediaWork.enqueueContinuation(applicationContext, it, 15) }
                    return@withContext Result.success()
                }
                item = queue.attachServerJob(localId, start.body) ?: return@withContext Result.success()
            }

            SharedMediaWork.scheduleWatchdog(applicationContext, item)
            queue.mutate(localId) { it.put("status", "processing").put("progress", maxOf(2, it.optInt("progress", 0))) }

            // Keep each invocation short/fair. Provider backoff is expressed as a
            // delayed WorkManager successor; no immediate one-second retry loop.
            var steps = 0
            while (steps < 6) {
                item = queue.get(localId) ?: return@withContext Result.success()
                if (item.status in MEDIA_TERMINAL_STATUSES) {
                    SharedMediaWork.cancelWatchdog(applicationContext, localId)
                    return@withContext Result.success()
                }
                val jobId = item.jobId.orEmpty()
                val token = item.pollToken.orEmpty()
                if (jobId.isBlank() || token.isBlank()) {
                    queue.fail(localId, "server_job_token_missing", true)
                    SharedMediaWork.cancelWatchdog(applicationContext, localId)
                    return@withContext Result.success()
                }

                val needsAiGate = item.stage in setOf("visual_hook_analysis", "deep_analysis")
                val processed = if (needsAiGate) {
                    AI_ANALYSIS_GATE.acquire()
                    try { SharedMediaClient.process(endpoint, jobId, token, companionKey) }
                    finally { AI_ANALYSIS_GATE.release() }
                } else SharedMediaClient.process(endpoint, jobId, token, companionKey)

                if (processed.code !in 200..299 || processed.body == null) {
                    val detail = safeDetail(processed.raw, processed.body)
                    val expired = processed.code == 401 && (
                        detail.contains("invalid_or_expired_poll_token") ||
                        detail.contains("job_token_mismatch") ||
                        detail.contains("invalid_or_expired_media_job_token")
                    )
                    if (expired) {
                        val recovered = queue.restartExpiredServerJob(localId)
                        if (recovered?.status == "queued") SharedMediaWork.enqueueContinuation(applicationContext, recovered, 5)
                        else queue.fail(localId, "token_restart_exhausted", true)
                        return@withContext Result.success()
                    }
                    if (processed.code in 500..599 || processed.code in listOf(408, 425, 429)) {
                        val wait = processed.body?.optInt("retry_after_seconds", 15)?.coerceIn(5, 90) ?: 15
                        queue.markTransient(localId, "HTTP ${processed.code}: $detail")
                        queue.get(localId)?.let { SharedMediaWork.enqueueContinuation(applicationContext, it, wait) }
                        return@withContext Result.success()
                    }
                    val terminal = processed.code in 400..499
                    queue.fail(localId, "HTTP ${processed.code}: $detail", terminal)
                    if (!terminal) queue.get(localId)?.let { SharedMediaWork.enqueueContinuation(applicationContext, it, 15) }
                    return@withContext Result.success()
                }

                val updated = queue.updateServerState(localId, processed.body)
                when (updated?.status) {
                    "completed", "dead_letter", "needs_media", "partial" -> {
                        SharedMediaWork.cancelWatchdog(applicationContext, localId)
                        return@withContext Result.success()
                    }
                    null -> return@withContext Result.success()
                }
                if (processed.body.optBoolean("retryable", false)) {
                    val wait = processed.body.optInt("retry_after_seconds", 8).coerceIn(5, 90)
                    queue.markTransient(localId, processed.body.optString("error_code", "provider_backoff"))
                    queue.get(localId)?.takeIf { it.status !in MEDIA_TERMINAL_STATUSES }?.let {
                        SharedMediaWork.enqueueContinuation(applicationContext, it, wait)
                    }
                    return@withContext Result.success()
                }
                steps++
            }
            // Fair continuation only when the server is still actively processing.
            queue.get(localId)?.takeIf { it.status !in MEDIA_TERMINAL_STATUSES }?.let {
                SharedMediaWork.enqueueContinuation(applicationContext, it, 5)
            }
            Result.success()
        } catch (error: Exception) {
            queue.markTransient(localId, error.javaClass.simpleName)
            queue.get(localId)?.takeIf { it.status !in MEDIA_TERMINAL_STATUSES }?.let {
                SharedMediaWork.enqueueContinuation(applicationContext, it, 15)
            }
            Result.success()
        }
    }

    private fun safeDetail(raw: String, body: JSONObject?): String {
        val root = body ?: runCatching { JSONObject(raw) }.getOrNull()
        return root?.optString("detail").orEmpty()
            .ifBlank { root?.optString("error_code").orEmpty() }
            .take(100)
            .ifBlank { "server_error" }
    }
}

object SharedMediaWork {
    private val connected = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    private fun request(item: SharedMediaQueue.Item, delaySeconds: Int = 0): androidx.work.OneTimeWorkRequest {
        val builder = OneTimeWorkRequestBuilder<SharedMediaWorker>()
            .setInputData(Data.Builder().putString("local_id", item.localId).build())
            .setConstraints(connected)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .addTag("takeoff-v4-media")
            .addTag("takeoff-v4-media-${item.localId}")
        if (delaySeconds > 0) {
            builder.setInitialDelay(delaySeconds.toLong(), TimeUnit.SECONDS)
        } else {
            builder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        }
        return builder.build()
    }

    private fun watchdogRequest(item: SharedMediaQueue.Item) = OneTimeWorkRequestBuilder<SharedMediaRecoveryWorker>()
        .setInputData(Data.Builder().putString("local_id", item.localId).build())
        .setConstraints(connected)
        .setInitialDelay(90, TimeUnit.SECONDS)
        .addTag("takeoff-v4-watchdog")
        .addTag("takeoff-v4-watchdog-${item.localId}")
        .build()

    fun enqueue(context: Context, item: SharedMediaQueue.Item) {
        if (item.status in MEDIA_TERMINAL_STATUSES) return
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "takeoff-v4-media-${item.localId}",
            ExistingWorkPolicy.KEEP,
            request(item),
        )
    }

    fun enqueueContinuation(context: Context, item: SharedMediaQueue.Item, delaySeconds: Int = 0) {
        if (item.status in MEDIA_TERMINAL_STATUSES) return
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "takeoff-v4-media-${item.localId}",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request(item, delaySeconds),
        )
    }

    fun revive(context: Context, item: SharedMediaQueue.Item) {
        if (item.status !in MEDIA_TERMINAL_STATUSES) {
            enqueue(context, item)
            scheduleWatchdog(context, item)
        }
    }

    fun scheduleWatchdog(context: Context, item: SharedMediaQueue.Item) {
        if (item.status in MEDIA_TERMINAL_STATUSES) return
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "takeoff-v4-watchdog-${item.localId}",
            ExistingWorkPolicy.REPLACE,
            watchdogRequest(item),
        )
    }

    fun cancelWatchdog(context: Context, localId: String) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork("takeoff-v4-watchdog-$localId")
    }

    fun retry(context: Context, localId: String) {
        val queue = SharedMediaQueue(context)
        queue.retry(localId)?.let { enqueue(context, it) }
    }
}
