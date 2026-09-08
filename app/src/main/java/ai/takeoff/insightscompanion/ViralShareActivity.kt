package ai.takeoff.insightscompanion

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

class ViralShareActivity : Activity() {
    companion object { const val EXTRA_ADDED_COUNT = "added_count" }

    private lateinit var queue: SharedMediaQueue
    private lateinit var listHost: LinearLayout
    private lateinit var summary: TextView
    private lateinit var empty: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    private val polling = AtomicBoolean(false)
    private var resumed = false
    private var lastRenderSignature = ""

    private val ticker = object : Runnable {
        override fun run() {
            if (!resumed) return
            render()
            refreshServerProgress()
            handler.postDelayed(this, 1500L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        queue = SharedMediaQueue(this)
        LovableUi.applyWindow(this)
        setContentView(buildUi())
        notifyAdded(intent.getIntExtra(EXTRA_ADDED_COUNT, 0))
        render(true)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notifyAdded(intent.getIntExtra(EXTRA_ADDED_COUNT, 0))
        render(true)
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        queue.all().filter { it.status in setOf("queued", "submitting", "processing", "failed") }
            .forEach { SharedMediaWork.revive(this, it) }
        SharedMediaWork.syncRemoteEvidence(this)
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    override fun onPause() {
        resumed = false
        handler.removeCallbacks(ticker)
        super.onPause()
    }

    private fun notifyAdded(count: Int) {
        if (count > 0) Toast.makeText(this, "${LovableUi.fa(count)} مورد به صف تحلیل اضافه شد", Toast.LENGTH_SHORT).show()
    }

    private fun buildUi(): View {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(LovableUi.background)
        }
        page.addView(LovableUi.run { topBar("صف تحلیل", "صف یادگیری چندرسانه‌ای • وضعیت زنده پردازش‌ها", back = { finish() }) })
        val scroll = ScrollView(this).apply { isFillViewport = true; overScrollMode = View.OVER_SCROLL_NEVER }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(LovableUi.run { dp(16) }, LovableUi.run { dp(16) }, LovableUi.run { dp(16) }, LovableUi.run { dp(28) })
        }
        root.addView(LovableUi.run { card(true) }.apply {
            val row = LinearLayout(this@ViralShareActivity).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; gravity = Gravity.CENTER_VERTICAL }
            row.addView(LovableUi.run { chip("۵ فرآیند همزمان (۵ Lane)", "secondary") })
            summary = LovableUi.run { text("در حال خواندن صف…", 12f, LovableUi.foreground, true) }
            row.addView(summary, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = LovableUi.run { dp(8) } })
            addView(row)
            addView(LovableUi.run { text("هر Share یک کار مستقل است. اگر یک مرحله طول بکشد، پردازش در پس‌زمینه ادامه پیدا می‌کند و UI نباید حالت گیرکرده نشان دهد.", 11f, LovableUi.muted) }.apply { setPadding(0, LovableUi.run { dp(8) }, 0, 0) })
        }, LovableUi.run { margin(bottom = 14) })

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        actions.addView(LovableUi.run { ghostButton("تحلیل جدید") { startActivity(Intent(this@ViralShareActivity, NewAnalysisActivity::class.java)) } }, LinearLayout.LayoutParams(0, LovableUi.run { dp(46) }, 1f).apply { marginEnd = LovableUi.run { dp(4) } })
        actions.addView(LovableUi.run { ghostButton("تازه‌سازی") {
            queue.all().filter { it.status in setOf("queued", "submitting", "processing", "failed") }.forEach { SharedMediaWork.revive(this@ViralShareActivity, it) }
            render(true); refreshServerProgress()
        } }, LinearLayout.LayoutParams(0, LovableUi.run { dp(46) }, 1f).apply { marginStart = LovableUi.run { dp(4) } })
        root.addView(actions, LovableUi.run { margin(bottom = 14) })

        empty = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(LovableUi.run { dp(24) }, LovableUi.run { dp(52) }, LovableUi.run { dp(24) }, LovableUi.run { dp(52) })
            addView(LovableUi.run { chip("صف خالی است", "muted") })
            addView(LovableUi.run { text("هر ویدیویی که برای تحلیل بفرستی اینجا دیده می‌شود.", 12f, LovableUi.muted) }.apply { gravity = Gravity.CENTER; setPadding(0, LovableUi.run { dp(12) }, 0, LovableUi.run { dp(16) }) })
            addView(LovableUi.run { primaryButton("افزودن تحلیل جدید") { startActivity(Intent(this@ViralShareActivity, NewAnalysisActivity::class.java)) } }, LinearLayout.LayoutParams(-1, LovableUi.run { dp(50) }))
        }
        root.addView(empty)
        listHost = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        root.addView(listHost)
        scroll.addView(root)
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        return page
    }

    private fun render(force: Boolean = false) {
        val items = queue.all()
        val signature = items.joinToString("|") { "${it.localId}:${it.status}:${it.stage}:${it.progress}:${it.stageProgress}:${it.error.orEmpty()}:${it.resultJson?.length ?: 0}" }
        if (!force && signature == lastRenderSignature) return
        lastRenderSignature = signature
        val active = items.count { it.status in setOf("queued", "submitting", "processing", "failed") }
        val done = items.count { it.status == "completed" }
        val failed = items.count { it.status == "dead_letter" }
        summary.text = "${LovableUi.fa(active)} فعال • ${LovableUi.fa(done)} کامل • ${LovableUi.fa(failed)} نیازمند بررسی"
        empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        listHost.removeAllViews()
        items.forEachIndexed { index, item ->
            val card = jobCard(item)
            listHost.addView(card, LovableUi.run { margin(bottom = 10) })
            card.alpha = 0f
            card.translationY = LovableUi.run { dp(12) }.toFloat()
            ObjectAnimator.ofFloat(card, View.ALPHA, 0f, 1f).apply {
                duration = 260
                startDelay = (index * 35L).coerceAtMost(180L)
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
            ObjectAnimator.ofFloat(card, View.TRANSLATION_Y, card.translationY, 0f).apply {
                duration = 340
                startDelay = (index * 35L).coerceAtMost(180L)
                interpolator = OvershootInterpolator(0.7f)
                start()
            }
        }
    }

    private fun jobCard(item: SharedMediaQueue.Item): View = LovableUi.run { card() }.apply {
        val top = LinearLayout(this@ViralShareActivity).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(TextView(this@ViralShareActivity).apply {
            text = when (item.mediaKind) { "reel", "video_post" -> "▷"; "carousel", "mixed" -> "▱"; "photo" -> "□"; else -> "◌" }
            textSize = 20f; gravity = Gravity.CENTER; setTextColor(LovableUi.secondaryText)
            background = LovableUi.run { rounded(LovableUi.secondarySoft, 14, LovableUi.secondarySoft, 0) }
        }, LinearLayout.LayoutParams(LovableUi.run { dp(46) }, LovableUi.run { dp(46) }))
        top.addView(LinearLayout(this@ViralShareActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(LovableUi.run { text(item.shortcode.ifBlank { "Instagram" }, 12.5f, LovableUi.foreground, true) })
            addView(LovableUi.run { text(kindFa(item.mediaKind), 10.5f, LovableUi.muted) })
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = LovableUi.run { dp(9) }; marginEnd = LovableUi.run { dp(9) } })
        top.addView(LovableUi.run { chip(stateFa(item), statusTone(item.status)) })
        addView(top)

        val targetProgress = item.progress.coerceIn(0, 100)
        val startProgress = (targetProgress - 8).coerceAtLeast(0)
        val progress = ProgressBar(this@ViralShareActivity, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100; this.progress = startProgress; isIndeterminate = false
            progressTintList = android.content.res.ColorStateList.valueOf(if (item.status in setOf("failed", "dead_letter")) LovableUi.danger else LovableUi.primary)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(LovableUi.mutedBg)
        }
        addView(progress, LinearLayout.LayoutParams(-1, LovableUi.run { dp(6) }).apply { topMargin = LovableUi.run { dp(12) }; bottomMargin = LovableUi.run { dp(8) } })
        ObjectAnimator.ofInt(progress, "progress", startProgress, targetProgress).apply {
            duration = 620
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        addView(LovableUi.run { text("${stageFa(item.stage)} • ${LovableUi.fa(item.progress)}٪", 11f, LovableUi.muted) })

        val pipeline = item.pipelineStagesJson?.let { runCatching { JSONArray(it) }.getOrNull() }
        if (pipeline != null) addView(pipelineView(pipeline), LinearLayout.LayoutParams(-1, -2).apply { topMargin = LovableUi.run { dp(10) } })

        val result = item.resultJson?.let { runCatching { JSONObject(it) }.getOrNull() }
        if (result != null) {
            val analysis = result.optJSONObject("analysis") ?: result
            val metrics = result.optJSONObject("public_metrics") ?: result.optJSONObject("public_metrics_json")
            val hook = analysis.optJSONObject("hook_intelligence")
            if (metrics != null) addView(LovableUi.run { text(metricsLine(metrics), 11.5f, LovableUi.foreground, true) }.apply { setPadding(0, LovableUi.run { dp(10) }, 0, 0) })
            compactHook(hook).takeIf { it.isNotBlank() }?.let { addView(LovableUi.run { text("قلاب: $it", 11f, LovableUi.muted) }.apply { setPadding(0, LovableUi.run { dp(5) }, 0, 0) }) }
            val origin = analysis.optJSONObject("media_origin")
            origin?.optString("classification")?.takeIf { it.isNotBlank() }?.let { addView(LovableUi.run { chip(originFa(it), "secondary") }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = LovableUi.run { dp(8) } }) }
            if (item.status == "completed") addView(fullReport(item, result), LinearLayout.LayoutParams(-1, -2).apply { topMargin = LovableUi.run { dp(10) } })
        }
        item.error?.let { addView(LovableUi.run { text(errorFa(it), 11f, LovableUi.danger) }.apply { setPadding(0, LovableUi.run { dp(8) }, 0, 0) }) }

        val actions = LinearLayout(this@ViralShareActivity).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        when {
            item.status == "completed" && item.resultJson != null -> {
                actions.addView(LovableUi.run { primaryButton("باز کردن نمای V5") {
                    startActivity(Intent(this@ViralShareActivity, AnalysisDetailActivity::class.java).putExtra(AnalysisDetailActivity.EXTRA_LOCAL_ID, item.localId))
                } }, LinearLayout.LayoutParams(0, LovableUi.run { dp(46) }, 1f))
            }
            item.status in setOf("failed", "dead_letter") -> {
                actions.addView(LovableUi.run { ghostButton("تلاش مجدد") { SharedMediaWork.retry(this@ViralShareActivity, item.localId); render(true) } }, LinearLayout.LayoutParams(0, LovableUi.run { dp(46) }, 1f))
            }
            else -> {
                actions.addView(LovableUi.run { text("پردازش در پس‌زمینه ادامه دارد", 11f, LovableUi.primary, true) }.apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(0, LovableUi.run { dp(46) }, 1f))
            }
        }
        addView(actions, LinearLayout.LayoutParams(-1, -2).apply { topMargin = LovableUi.run { dp(10) } })
    }

    private fun fullReport(item: SharedMediaQueue.Item, result: JSONObject): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setPadding(LovableUi.run { dp(11) }, LovableUi.run { dp(10) }, LovableUi.run { dp(11) }, LovableUi.run { dp(10) })
        background = LovableUi.run { rounded(LovableUi.mutedBg, 17, LovableUi.mutedBg, 0) }
        val analysis = result.optJSONObject("analysis") ?: result
        addView(LovableUi.run { text("گزارش کامل بالا نمایش داده شده", 10f, LovableUi.success, true) })
        val sections = listOf(
            Triple("کالبدشکافی قلاب", analysis.opt("hook_intelligence") ?: analysis.opt("hook"), "primary"),
            Triple("AI Forensics", analysis.opt("media_origin"), "secondary"),
            Triple("بازسازی سناریو", analysis.opt("scenario_reconstruction") ?: analysis.opt("scenario_structure"), "primary"),
            Triple("فرضیه‌های ریتنشن", analysis.opt("retention_analysis") ?: analysis.opt("retention_hypotheses"), "secondary"),
            Triple("یادگیری‌های قابل استفاده", analysis.opt("reusable_patterns"), "primary"),
        )
        sections.forEach { (title, raw, tone) ->
            val sectionText = compactJson(raw)
            if (sectionText.isNotBlank()) {
                addView(LovableUi.run { chip(title, tone) }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = LovableUi.run { dp(7) } })
                addView(LovableUi.run { text(sectionText.take(320), 10.5f, LovableUi.foreground) }.apply { setPadding(0, LovableUi.run { dp(4) }, 0, 0) })
            }
        }
    }

    private fun compactJson(value: Any?): String = when (value) {
        null, JSONObject.NULL -> ""
        is String -> value
        is JSONArray -> buildString {
            for (i in 0 until value.length().coerceAtMost(3)) {
                val part = compactJson(value.opt(i))
                if (part.isNotBlank()) append(if (isEmpty()) "" else " • ").append(part)
            }
        }
        is JSONObject -> buildString {
            val keys = value.keys()
            var count = 0
            while (keys.hasNext() && count < 5) {
                val key = keys.next()
                val part = compactJson(value.opt(key))
                if (part.isNotBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append(part)
                    count++
                }
            }
        }
        else -> value.toString()
    }

    private fun semanticKey(stage: String): String = when (stage) {
        "queued", "submitting", "server_prepare", "url_validation" -> "server_prepare"
        "media_resolve", "resolve", "metadata_extract", "media_resolve_retry" -> "media_resolve"
        "media_download", "download", "media_download_retry", "media_verify", "ai_upload", "upload", "ai_ready" -> "media_download"
        "visual_hook_analysis", "dialogue_transcription", "audio_music_analysis", "scenario_reconstruction",
        "behavioral_analysis", "retention_modeling", "learning_candidate", "deep_analysis", "server_processing",
        "audio_visual_analysis" -> "deep_analysis"
        "learning_persist", "learning" -> "learning_persist"
        "completed" -> "completed"
        else -> "deep_analysis"
    }

    private fun semanticLabel(key: String): String = when (key) {
        "server_prepare" -> "آماده‌سازی تحلیل"
        "media_resolve" -> "دریافت منبع"
        "media_download" -> "دریافت و آماده‌سازی رسانه"
        "deep_analysis" -> "تحلیل عمیق چندوجهی"
        "learning_persist" -> "ثبت یادگیری"
        "completed" -> "گزارش آماده است"
        else -> "در حال تحلیل"
    }

    private fun pipelineView(stages: JSONArray): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(LovableUi.run { dp(10) }, LovableUi.run { dp(8) }, LovableUi.run { dp(10) }, LovableUi.run { dp(8) })
        background = LovableUi.run { rounded(LovableUi.mutedBg, 16, LovableUi.mutedBg, 0) }

        val order = listOf("server_prepare", "media_resolve", "media_download", "deep_analysis", "learning_persist", "completed")
        val states = linkedMapOf<String, MutableList<String>>()
        for (i in 0 until stages.length()) {
            val step = stages.optJSONObject(i) ?: continue
            val key = semanticKey(step.optString("key"))
            states.getOrPut(key) { mutableListOf() }.add(step.optString("state", "pending"))
        }
        order.forEach { key ->
            val group = states[key].orEmpty()
            val state = when {
                group.any { it == "active" } -> "active"
                group.isNotEmpty() && group.all { it == "completed" } -> "completed"
                else -> "pending"
            }
            val icon = when (state) { "completed" -> "✓"; "active" -> "●"; else -> "○" }
            val color = when (state) { "completed" -> LovableUi.success; "active" -> LovableUi.primary; else -> LovableUi.muted }
            addView(LovableUi.run { text("$icon ${semanticLabel(key)}", 10.5f, color, state != "pending") }.apply {
                setPadding(0, LovableUi.run { dp(2) }, 0, LovableUi.run { dp(2) })
            })
        }
    }

    private fun refreshServerProgress() {
        if (!polling.compareAndSet(false, true)) return
        val candidates = queue.all().filter { it.status in setOf("submitting", "processing", "failed") && !it.jobId.isNullOrBlank() && !it.pollToken.isNullOrBlank() }.take(8)
        if (candidates.isEmpty()) { polling.set(false); return }
        val endpoint = PayloadClient.viralEndpoint(getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE).getString("endpoint", "").orEmpty())
        Thread {
            try {
                candidates.forEach { item ->
                    val response = runCatching { SharedMediaClient.status(endpoint, item.jobId!!, item.pollToken!!) }.getOrNull() ?: return@forEach
                    if (response.code in 200..299 && response.body != null) queue.updateServerState(item.localId, response.body)
                }
            } finally {
                polling.set(false)
                runOnUiThread { if (resumed) render() }
            }
        }.start()
    }

    private fun stageFa(stage: String): String = when (stage) {
        "failed" -> "موقتاً متوقف"
        else -> semanticLabel(semanticKey(stage))
    }

    private fun stateFa(item: SharedMediaQueue.Item) = when (item.status) {
        "queued" -> "در صف"
        "submitting", "processing" -> "در حال تحلیل"
        "completed" -> "تکمیل شد"
        "failed" -> "قابل Retry"
        "dead_letter" -> "نیازمند بررسی"
        else -> item.status
    }

    private fun statusTone(status: String) = when (status) {
        "completed" -> "success"
        "failed", "dead_letter" -> "danger"
        "queued" -> "muted"
        else -> "primary"
    }

    private fun kindFa(kind: String) = when (kind) {
        "reel" -> "ریلز"
        "video_post" -> "ویدیو"
        "photo" -> "عکس"
        "carousel" -> "کاروسل"
        "mixed" -> "ترکیبی"
        else -> "در حال تشخیص"
    }

    private fun originFa(value: String) = when (value) {
        "FILMED_LIVE_ACTION" -> "فیلمبرداری واقعی"
        "FULLY_AI_GENERATED" -> "کاملاً AI"
        "AI_ASSISTED" -> "واقعی + AI"
        "HYBRID_AI_AND_FILMED" -> "ترکیبی AI + واقعی"
        "CGI_ANIMATION" -> "CGI / انیمیشن"
        "SCREEN_RECORDING" -> "ضبط صفحه"
        else -> "منشأ نامشخص"
    }

    private fun compactHook(hook: JSONObject?): String {
        if (hook == null) return ""
        for (key in listOf("dominant_hook", "exact_hook", "hook", "spoken_hook", "visual_hook")) {
            val v = hook.opt(key)?.toString()?.trim().orEmpty()
            if (v.isNotBlank()) return v.take(170)
        }
        return ""
    }

    private fun metricsLine(metrics: JSONObject): String {
        fun value(key: String): String = if (!metrics.has(key) || metrics.isNull(key)) "—" else compact(metrics.optDouble(key, Double.NaN))
        return "بازدید ${value("views")} • لایک ${value("likes")} • اشتراک ${value("shares")} • نظر ${value("comments")}"
    }

    private fun compact(v: Double): String = when {
        !v.isFinite() -> "—"
        v >= 1_000_000_000 -> String.format(java.util.Locale.US, "%.1fB", v / 1_000_000_000)
        v >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", v / 1_000_000)
        v >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", v / 1_000)
        else -> v.toLong().toString()
    }

    private fun errorFa(error: String): String = when {
        error.contains("media_url_unavailable") -> "فایل عمومی این محتوا از مسیرهای Instagram قابل دریافت نبود. در حال آزمایش مسیر جایگزین دریافت."
        error.contains("media_download_failed") -> "دریافت فایل موقتاً قطع شد؛ در حال آزمایش مسیر جایگزین دریافت."
        error.contains("invalid_or_expired_poll_token") -> "ارتباط امن منقضی شد؛ بازیابی خودکار فعال است."
        error.contains("gemini") -> "موتور تحلیل موقتاً پاسخ نداد؛ سیستم دوباره تلاش می‌کند."
        else -> "پردازش موقتاً با مشکل روبه‌رو شد؛ در صورت امکان دوباره تلاش می‌شود."
    }
}
