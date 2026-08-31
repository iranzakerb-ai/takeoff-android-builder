package ai.takeoff.insightscompanion

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class CaptureService : Service() {
    companion object {
        const val ACTION_START = "ai.takeoff.insights.START"
        const val ACTION_CAPTURE = "ai.takeoff.insights.CAPTURE"
        const val ACTION_SYNC = "ai.takeoff.insights.SYNC"
        const val ACTION_STOP = "ai.takeoff.insights.STOP"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        private const val CHANNEL = "takeoff_insights_capture"
        private const val NOTIFICATION_ID = 2401
        private const val CAPTURE_SETTLE_MS = 1250L
        private const val FRAME_WARMUP_MS = 350L
        private const val CAPTURE_TIMEOUT_MS = 18_000L
        private const val MAX_OCR_ATTEMPTS = 5
    }

    private data class OcrCandidate(
        val rawText: String,
        val metrics: Map<String, Double>,
        val validation: CaptureValidator.Result,
        val engine: String = "mlkit_on_device",
        val fallbackJpeg: ByteArray? = null,
    )

    private var projection: MediaProjection? = null
    private val busy = AtomicBoolean(false)
    private val handler by lazy { Handler(Looper.getMainLooper()) }

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "ثبت هوشمند آمار تیک‌آف", NotificationManager.IMPORTANCE_LOW).apply {
                description = "ثبت و ارسال امن آمار ریلز"
                setSound(null, null)
                enableVibration(false)
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startProjection(intent)
            ACTION_CAPTURE -> captureOnce()
            ACTION_SYNC -> Thread { syncQueue() }.start()
            ACTION_STOP -> stopSelfSafely()
        }
        return START_NOT_STICKY
    }

    @Suppress("DEPRECATION")
    private fun startProjection(intent: Intent) {
        val code = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
        val data = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA) ?: return stopSelfSafely()
        val n = notification("آماده ثبت؛ وارد Insights همان ریلز شو.")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        else startForeground(NOTIFICATION_ID, n)
        projection?.stop()
        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = mgr.getMediaProjection(code, data)?.also { p ->
            p.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() { projection = null; stopSelf() }
            }, handler)
        }
        TakeoffSound.play(TakeoffSound.Cue.SUCCESS)
        Thread { syncQueue(silent = true) }.start()
    }

    private fun notification(message: String): Notification {
        val captureIntent = PendingIntent.getActivity(
            this, 1,
            Intent(this, CaptureTriggerActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or Intent.FLAG_ACTIVITY_NO_HISTORY),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val syncIntent = PendingIntent.getService(this, 2, Intent(this, CaptureService::class.java).setAction(ACTION_SYNC), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stopIntent = PendingIntent.getService(this, 3, Intent(this, CaptureService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val prefs = getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE)
        val shortcode = prefs.getString("shortcode", "").orEmpty()
        val account = prefs.getString("armed_account", "").orEmpty().ifBlank { prefs.getString("account", "").orEmpty() }
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle(if (shortcode.isBlank()) "تیک‌آف • @$account" else "تیک‌آف • @$account • $shortcode")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, "ثبت این صفحه", captureIntent)
            .addAction(0, "ارسال صف", syncIntent)
            .addAction(0, "توقف", stopIntent)
            .build()
    }

    private fun captureOnce() {
        val p = projection ?: return reject("اول از داخل تیک‌آف جلسه ثبت را شروع کن.")
        if (!busy.compareAndSet(false, true)) return
        TakeoffSound.play(TakeoffSound.Cue.CAPTURE)
        updateNotification("در حال خواندن Insights… صفحه را یک لحظه ثابت نگه دار.")
        handler.postDelayed({
            if (projection == null) {
                busy.set(false); reject("جلسه ثبت بسته شده؛ دوباره شروعش کن.")
            } else beginFrameCapture(p)
        }, CAPTURE_SETTLE_MS)
    }

    private fun beginFrameCapture(p: MediaProjection) {
        val dm = resources.displayMetrics
        val width = dm.widthPixels; val height = dm.heightPixels; val density = dm.densityDpi
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3)
        var display: VirtualDisplay? = null
        val finished = AtomicBoolean(false); val ocrInFlight = AtomicBoolean(false); val attempts = AtomicInteger(0)
        val serverFallbackStarted = AtomicBoolean(false)
        val startedAt = SystemClock.elapsedRealtime(); var bestEvidence = ""

        fun cleanup() { runCatching { display?.release() }; display = null; runCatching { reader.close() } }
        fun finishRejected(message: String) {
            if (!finished.compareAndSet(false, true)) return
            cleanup(); busy.set(false); reject(message)
        }
        fun finishAccepted(candidate: OcrCandidate) {
            if (!finished.compareAndSet(false, true)) return
            cleanup(); busy.set(false)
            val payload = buildPayload(candidate.metrics, candidate.rawText, candidate.validation.pageHint, candidate.engine)
            LocalInsightStore(this).markCapture(payload, "review")
            updateNotification(if (candidate.engine == "mlkit_on_device") "خوانده شد؛ اعداد را در تیک‌آف بررسی و تأیید کن." else "OCR فارسی سرور انجام شد؛ اعداد را حتماً بررسی و تأیید کن.")
            TakeoffSound.play(TakeoffSound.Cue.SUCCESS)
            startActivity(Intent(this, CaptureReviewActivity::class.java)
                .putExtra("payload", payload.toString())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }
        fun tryServerFallback(candidate: OcrCandidate, fallbackMessage: String) {
            val jpeg = candidate.fallbackJpeg
            val prefs = getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE)
            val endpoint = prefs.getString("endpoint", "").orEmpty()
            val key = SecretStore(this).get("api_key").orEmpty()
            if (jpeg.isNullOrEmpty() || endpoint.isBlank() || key.isBlank() || !serverFallbackStarted.compareAndSet(false, true)) {
                finishRejected(fallbackMessage); return
            }
            updateNotification("OCR محلی کافی نبود؛ تحلیل فارسی امن روی سرور در حال اجراست…")
            Thread {
                val response = runCatching { ServerOcrClient.analyze(endpoint, key, jpeg) }.getOrNull()
                val root = response?.body
                val rawMetrics = root?.optJSONObject("metrics")
                val metrics = linkedMapOf<String, Double>()
                if (rawMetrics != null) {
                    val keys = rawMetrics.keys()
                    while (keys.hasNext()) {
                        val name = keys.next(); val value = rawMetrics.optDouble(name, Double.NaN)
                        if (value.isFinite() && value >= 0.0) metrics[name] = value
                    }
                }
                val confidence = root?.optDouble("confidence", 0.0) ?: 0.0
                val pageHint = root?.optString("page_hint", "details").orEmpty().ifBlank { "details" }
                val labels = root?.optJSONArray("observed_labels")
                val rawText = buildString {
                    if (labels != null) for (i in 0 until labels.length()) append(labels.optString(i)).append('\n')
                }.trim()
                handler.post {
                    if (finished.get()) return@post
                    if (response?.code in 200..299 && metrics.size >= 2 && confidence >= 0.55) {
                        finishAccepted(OcrCandidate(rawText, metrics, CaptureValidator.Result(true, pageHint, "server_multimodal_ocr_fallback"), "gemini_server_ocr_fallback", null))
                    } else {
                        finishRejected("OCR محلی و سرور هیچ جفت برچسب/عدد مطمئنی پیدا نکردند؛ صفحه Insights را واضح‌تر باز کن و دوباره بزن.")
                    }
                }
            }.start()
        }
        fun handleRejectedCandidate(candidate: OcrCandidate, attempt: Int) {
            val keys = candidate.metrics.keys.sorted().take(6).joinToString(",")
            bestEvidence = if (keys.isBlank()) "برچسب آماری معتبر پیدا نشد" else keys
            if (attempt >= MAX_OCR_ATTEMPTS) tryServerFallback(candidate, rejectionMessage(candidate.validation.reason, candidate.metrics, attempt))
            else { ocrInFlight.set(false); updateNotification("فریم $attempt کافی نبود؛ خودکار دوباره می‌خوانم…") }
        }

        reader.setOnImageAvailableListener({ ir ->
            val image = ir.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                if (finished.get() || serverFallbackStarted.get()) return@setOnImageAvailableListener
                if (SystemClock.elapsedRealtime() - startedAt < FRAME_WARMUP_MS) return@setOnImageAvailableListener
                if (!ocrInFlight.compareAndSet(false, true)) return@setOnImageAvailableListener
                val bitmap = runCatching { bitmapFromImage(image, width, height) }.getOrElse { ocrInFlight.set(false); return@setOnImageAvailableListener }
                val attempt = attempts.incrementAndGet()
                recognizeCandidate(bitmap) { candidate, error ->
                    if (finished.get()) return@recognizeCandidate
                    when {
                        candidate?.validation?.accepted == true -> finishAccepted(candidate)
                        candidate != null -> handleRejectedCandidate(candidate, attempt)
                        else -> {
                            bestEvidence = error ?: "خطای OCR"
                            if (attempt >= MAX_OCR_ATTEMPTS) finishRejected("بعد از $attempt تلاش، فریم قابل پردازش نبود.")
                            else { ocrInFlight.set(false); updateNotification("خواندن فریم $attempt ناموفق بود؛ دوباره تلاش می‌کنم…") }
                        }
                    }
                }
            } finally { image.close() }
        }, handler)

        display = p.createVirtualDisplay("TakeOffInsightsBurst", width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader.surface, null, handler)
        handler.postDelayed({
            if (!finished.get() && !serverFallbackStarted.get()) finishRejected("زمان ثبت تمام شد • ${bestEvidence.ifBlank { "صفحه قابل‌خواندن نبود" }} • دوباره امتحان کن.")
        }, CAPTURE_TIMEOUT_MS)
    }

    private fun bitmapFromImage(image: Image, width: Int, height: Int): Bitmap {
        val plane = image.planes[0]; val pixelStride = plane.pixelStride; val rowStride = plane.rowStride
        val paddedWidth = width + (rowStride - pixelStride * width) / pixelStride
        val padded = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
        padded.copyPixelsFromBuffer(plane.buffer)
        return try { Bitmap.createBitmap(padded, 0, 0, width, height) } finally { padded.recycle() }
    }

    private fun recognizeCandidate(bitmap: Bitmap, done: (OcrCandidate?, String?) -> Unit) {
        val jpeg = runCatching {
            ByteArrayOutputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out); out.toByteArray() }
        }.getOrNull()
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { text ->
                bitmap.recycle()
                val spatialLines = text.textBlocks.flatMap { block ->
                    block.lines.mapNotNull { line -> line.boundingBox?.let { box -> MetricParser.OcrLine(line.text, box.left, box.top, box.right, box.bottom) } }
                }
                val parsed = linkedMapOf<String, Double>()
                parsed.putAll(MetricParser.parse(text.text)); parsed.putAll(MetricParser.parseStructured(spatialLines))
                val validation = CaptureValidator.validate(text.text, parsed)
                recognizer.close(); done(OcrCandidate(text.text, parsed, validation, "mlkit_on_device", jpeg), null)
            }
            .addOnFailureListener { error -> bitmap.recycle(); recognizer.close(); done(null, error.javaClass.simpleName) }
    }

    private fun rejectionMessage(reason: String, parsed: Map<String, Double>, attempts: Int): String {
        val evidence = parsed.keys.sorted().take(8).joinToString(",")
        return when (reason) {
            "reel_insights_context_missing" -> if (parsed.isEmpty()) "ثبت رد شد؛ Insights همان ریلز را باز کن و دوباره بزن." else "صفحه کامل Insights تشخیص داده نشد • $evidence"
            "summary_needs_distribution_plus_two_metrics" -> "خلاصه باید Views و حداقل دو معیار دیگر داشته باشد • $evidence"
            "retention_chart_has_no_labeled_metrics" -> "نمودار ریتنشن دیده شد اما عدد برچسب‌دار کافی نیست؛ تیک‌آف شکل نمودار را حدس نمی‌زند."
            else -> "بعد از $attempts تلاش، این بخش داده برچسب‌دار کافی نداشت • $evidence"
        }
    }

    private fun buildPayload(metrics: Map<String, Double>, rawText: String, pageHint: String, engine: String): JSONObject {
        val prefs = getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE)
        val account = prefs.getString("armed_account", "").orEmpty().ifBlank { prefs.getString("account", "").orEmpty() }
        val metricJson = JSONObject(); val countFields = setOf("views","viewers","reach","likes","comments","reposts","shares","saves","follows","profile_visits")
        metrics.forEach { (name, value) -> metricJson.put(name, if (name in countFields) value.toLong() else value) }
        val shortcode = prefs.getString("shortcode", "").orEmpty()
        val local = LocalInsightStore(this).reels(account).firstOrNull { it.shortcode == shortcode }
        return JSONObject()
            .put("account_id", account)
            .put("shortcode", shortcode)
            .put("url", prefs.getString("reel_url", "").orEmpty())
            .put("observed_at", System.currentTimeMillis() / 1000.0)
            .put("source", "owner_device_media_projection_ocr")
            .put("metrics", metricJson)
            .put("execution_fidelity", local?.executionFidelity ?: "unknown")
            .apply { local?.scenarioId?.let { put("scenario_id", it) } }
            .put("ocr", JSONObject().put("engine", engine).put("page_hint",pageHint).put("raw_text_sha256",sha256(rawText)))
    }

    private fun syncQueue(silent: Boolean = false) {
        val prefs = getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE)
        val endpoint = prefs.getString("endpoint", "").orEmpty(); val key = SecretStore(this).get("api_key").orEmpty()
        if (endpoint.isBlank() || key.isBlank()) return
        val queue = PayloadQueue(this); val local = LocalInsightStore(this)
        var recorded = 0; var skipped = 0; var lastFailure = ""; var globalStop = false
        for (payload in queue.all()) {
            if (globalStop) break
            try {
                val captureId = payload.optString("capture_id")
                val (code, body) = PayloadClient.post(endpoint, key, payload)
                if (code !in 200..299) {
                    lastFailure = if (code == 409) "یک ثبت هنوز به سناریو/انتشار وصل نشده" else "خطای سرور HTTP $code"
                    if (code == 401 || code == 403 || code >= 500) globalStop = true else skipped++
                    continue
                }
                if (!PayloadClient.isDurablyAcknowledgedResponse(body, captureId)) {
                    lastFailure = "رسید پایدار یک ثبت تأیید نشد"; skipped++; continue
                }
                val status = JSONObject(body).optString("status")
                if (status != "recorded") {
                    lastFailure = "یک ثبت هنوز برای یادگیری آماده نیست"; skipped++; continue
                }
                queue.remove(captureId); recorded++
                local.updateState(payload.optString("account_id"), payload.optString("shortcode"), "recorded")
            } catch (_: Exception) {
                lastFailure = "ارتباط با سرور قطع شد"
                globalStop = true
            }
        }
        val remaining = queue.size()
        if (recorded > 0) TakeoffSound.play(TakeoffSound.Cue.SYNC)
        else if (lastFailure.isNotBlank() && !silent) TakeoffSound.play(TakeoffSound.Cue.WARNING)
        if (!silent) updateNotification(when {
            remaining == 0 && recorded > 0 -> "همگام شد • $recorded ثبت قطعی وارد حافظه یادگیری شد."
            remaining == 0 -> "صف خالی است."
            recorded > 0 && skipped > 0 -> "$recorded ثبت ارسال شد • $skipped مورد فعلاً محفوظ ماند • $remaining باقی‌مانده"
            lastFailure.isNotBlank() -> "$remaining ثبت روی گوشی محفوظ است • $lastFailure"
            else -> "$remaining ثبت هنوز در صف امن است."
        })
    }

    private fun reject(message: String) { TakeoffSound.play(TakeoffSound.Cue.ERROR); updateNotification(message) }
    private fun sha256(text: String) = java.security.MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
    private fun updateNotification(message: String) { handler.post { getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(message)) } }
    private fun stopSelfSafely() {
        TakeoffSound.play(TakeoffSound.Cue.WARNING); projection?.stop(); projection = null
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
