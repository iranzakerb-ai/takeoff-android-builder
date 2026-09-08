package ai.takeoff.insightscompanion

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class StudioTaskWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val taskId = inputData.getString("task_id").orEmpty()
        if (taskId.isBlank()) return@withContext Result.failure()

        val store = StudioResultStore(applicationContext)
        val entry = store.get(taskId) ?: return@withContext Result.failure()

        val prefs = applicationContext.getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE)
        val endpoints = PayloadClient.candidateEndpoints(prefs.getString("endpoint", "").orEmpty())
        val key = SecretStore(applicationContext).get("api_key").orEmpty()

        val path = if (entry.type == "scenario") "/v4/scenario-studio/generate" else "/v4/ai-video-studio/generate"
        val body = JSONObject().apply {
            put("niche", entry.niche)
            put("business_description", entry.description)
            put("mode", entry.mode)
            if (entry.targetAudience.isNotBlank()) put("target_audience", entry.targetAudience)
            if (entry.mainOffer.isNotBlank()) put("main_offer", entry.mainOffer)
            if (entry.constraints.isNotBlank()) put("constraints", entry.constraints)
            put("actors_available", entry.actorCount)
            put("actor_count", entry.actorCount)
        }

        var success = false
        var responseJson: JSONObject? = null
        var lastError = "اتصال به سرور برقرار نشد."

        for (ep in endpoints) {
            try {
                val conn = URL(ep.trimEnd('/') + path).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 30_000
                conn.readTimeout = 300_000
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("User-Agent", "TakeOff-Insights/" + BuildConfig.VERSION_NAME)
                if (key.isNotBlank()) conn.setRequestProperty("X-Takeoff-Companion-Key", key)

                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                conn.disconnect()

                if (code in 200..299 && text.isNotBlank()) {
                    responseJson = JSONObject(text)
                    success = true
                    break
                } else {
                    lastError = "خطای سرور ($code): ${text.take(120)}"
                    if (code != 404 && code !in 502..504) break
                }
            } catch (e: Exception) {
                lastError = e.message ?: "خطای شبکه در ارتباط با سرور"
            }
        }

        if (success && responseJson != null) {
            var pdfPath: String? = null
            try {
                val exportResult = if (entry.type == "scenario") {
                    TakeoffPdfExporter.exportScenarioStudio(applicationContext, responseJson)
                } else {
                    TakeoffPdfExporter.exportAiVideoStudio(applicationContext, responseJson)
                }
                pdfPath = exportResult.second?.absolutePath
            } catch (_: Exception) {
                // PDF generation in background is a best-effort cache
            }

            store.update(taskId) { current ->
                current.copy(
                    status = "completed",
                    resultJson = responseJson.toString(),
                    pdfPath = pdfPath,
                    errorMessage = null,
                )
            }

            val notifTitle = if (entry.type == "scenario") "۱۰ سناریوی تیک‌آف آماده شد" else "پکیج ویدیوی هوش مصنوعی آماده شد"
            val notifDesc = "${entry.niche}: خروجی در بخش ذخیره‌ها آماده بازبینی و دریافت PDF است."
            sendStudioNotification(applicationContext, notifTitle, notifDesc, taskId)
            Result.success()
        } else {
            store.update(taskId) { current ->
                current.copy(
                    status = "failed",
                    errorMessage = lastError,
                )
            }
            sendStudioNotification(applicationContext, "تولید متوقف شد", "${entry.niche}: $lastError", taskId)
            Result.failure()
        }
    }

    private fun sendStudioNotification(context: Context, title: String, message: String, taskId: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channelId = "takeoff_studio_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "اعلان‌های استودیو تیک‌آف", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "اطلاع‌رسانی اتمام ساخت سناریوها و ویدیوهای AI"
            }
            nm.createNotificationChannel(channel)
        }

        val intent = Intent(context, SavedActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("selected_task_id", taskId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0),
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_takeoff_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        nm.notify(taskId.hashCode(), notification)
    }
}

object StudioTaskWork {
    private val connected = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun enqueue(context: Context, taskId: String) {
        val request = OneTimeWorkRequestBuilder<StudioTaskWorker>()
            .setInputData(Data.Builder().putString("task_id", taskId).build())
            .setConstraints(connected)
            .addTag("takeoff-studio-task")
            .addTag("takeoff-studio-task-$taskId")
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "takeoff-studio-$taskId",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }
}
