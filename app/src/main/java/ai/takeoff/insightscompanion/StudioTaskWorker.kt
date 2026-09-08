package ai.takeoff.insightscompanion

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class StudioTaskWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getString("task_id") ?: return Result.failure()
        val store = StudioResultStore(applicationContext)
        val entry = store.get(taskId) ?: return Result.failure()

        if (entry.status == "completed" && !entry.resultJson.isNullOrBlank()) {
            return Result.success()
        }

        store.update(taskId) { it.copy(status = "processing") }

        val prefs = applicationContext.getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE)
        val endpoints = PayloadClient.candidateEndpoints(prefs.getString("endpoint", "").orEmpty())
        val key = SecretStore(applicationContext).get("api_key").orEmpty()

        val path = if (entry.type == "scenario") "/v4/scenario-studio/generate" else "/v4/ai-video-studio/generate"
        val body = JSONObject().apply {
            put("niche", entry.niche)
            put("business_description", entry.description)
            put("mode", entry.mode)
            if (entry.targetAudience.isNotBlank()) {
                put("audience", entry.targetAudience)
                put("target_audience", entry.targetAudience)
            }
            if (entry.mainOffer.isNotBlank()) {
                put("offer", entry.mainOffer)
                put("main_offer", entry.mainOffer)
            }
            if (entry.constraints.isNotBlank()) {
                put("constraints", entry.constraints)
                put("production_constraints", entry.constraints)
            }
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

        if (!success || responseJson == null) {
            responseJson = if (entry.type == "scenario") {
                LocalStudioEngine.generateScenarioPackage(
                    entry.niche, entry.description, entry.mode,
                    entry.targetAudience, entry.mainOffer, entry.constraints, entry.actorCount
                )
            } else {
                LocalStudioEngine.generateAiVideoPackage(
                    entry.niche, entry.description, entry.mode,
                    entry.targetAudience, entry.mainOffer, entry.constraints, entry.actorCount
                )
            }
            success = true
        }

        var pdfPath: String? = null
        if (entry.type == "scenario") {
            try {
                val exportResult = TakeoffPdfExporter.exportScenarioStudio(applicationContext, responseJson)
                pdfPath = exportResult.second?.absolutePath
            } catch (_: Exception) {
                // PDF generation in background is best-effort cache
            }
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
        val notifDesc = if (entry.type == "scenario") "${entry.niche}: خروجی در بخش ذخیره‌ها آماده بازبینی و دریافت PDF است."
                        else "${entry.niche}: پرامپت‌های کاراکترها و سکانس‌ها آماده کپی هستند."
        sendStudioNotification(applicationContext, notifTitle, notifDesc, taskId)
        return Result.success()
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
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        nm.notify(taskId.hashCode(), notif)
    }
}

object StudioTaskWork {
    fun enqueue(context: Context, taskId: String) {
        val request = OneTimeWorkRequestBuilder<StudioTaskWorker>()
            .setInputData(workDataOf("task_id" to taskId))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "studio_task_" + taskId,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
