package ai.takeoff.insightscompanion

import android.app.Activity
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

class ViralShareActivity : Activity() {
    companion object {
        const val EXTRA_ADDED_COUNT = "added_count"
    }

    private val bg0 = Color.rgb(5, 8, 15)
    private val bg1 = Color.rgb(12, 26, 39)
    private val glass = Color.argb(80, 255, 255, 255)
    private val glassStrong = Color.argb(108, 255, 255, 255)
    private val border = Color.argb(76, 255, 255, 255)
    private val accent = Color.rgb(79, 235, 216)
    private val muted = Color.rgb(188, 200, 214)
    private val success = Color.rgb(102, 235, 173)
    private val warning = Color.rgb(255, 194, 92)
    private val danger = Color.rgb(255, 125, 145)

    private lateinit var queue: SharedMediaQueue
    private lateinit var listHost: LinearLayout
    private lateinit var summary: TextView
    private lateinit var empty: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val polling = AtomicBoolean(false)
    private var resumed = false

    private val ticker = object : Runnable {
        override fun run() {
            if (!resumed) return
            render()
            refreshServerProgress()
            handler.postDelayed(this, 2_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        queue = SharedMediaQueue(this)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = bg0
        recoverOldCredentialFailures()
        setContentView(buildUi())
        val added = intent.getIntExtra(EXTRA_ADDED_COUNT, 0)
        if (added > 0) Toast.makeText(this, "$added ریلز وارد صف شد", Toast.LENGTH_SHORT).show()
        render()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recoverOldCredentialFailures()
        val added = intent.getIntExtra(EXTRA_ADDED_COUNT, 0)
        if (added > 0) Toast.makeText(this, "$added مورد دیگر اضافه شد", Toast.LENGTH_SHORT).show()
        render()
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        recoverOldCredentialFailures()
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    override fun onPause() {
        resumed = false
        handler.removeCallbacks(ticker)
        super.onPause()
    }

    private fun recoverOldCredentialFailures() {
        queue.recoverLegacyCredentialFailures().forEach { SharedMediaWork.enqueue(this, it) }
    }

    private fun buildUi(): View {
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(bg0, bg1, Color.rgb(6, 13, 25)),
            )
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(18), dp(26), dp(18), dp(30))
        }

        root.addView(glassCard(strong = true).apply {
            addView(label("یادگیری از ریلزهای وایرال", 25f, Color.WHITE, true))
            addView(label("از Instagram به تیک‌آف Share کن؛ دانلود و تحلیل روی سرور انجام می‌شود.", 13f, muted, false).apply {
                setPadding(0, dp(7), 0, dp(12))
            })
            summary = label("در حال خواندن…", 13f, accent, true)
            addView(summary)
        }, margin(dp(14)))

        empty = label("هنوز ریلزی نفرستادی.\nInstagram → Share → TakeOff", 15f, muted, false).apply {
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(44), dp(12), dp(44))
        }
        root.addView(empty)

        listHost = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        root.addView(listHost)

        root.addView(Button(this).apply {
            text = "بستن"
            isAllCaps = false
            textSize = 13f
            setTextColor(Color.WHITE)
            background = rounded(Color.argb(55, 255, 255, 255), 18, border)
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(8) })

        scroll.addView(root)
        return scroll
    }

    private fun render() {
        if (!::listHost.isInitialized) return
        val items = queue.all()
        val active = items.count { it.status in setOf("queued", "submitting", "processing", "failed") }
        val completed = items.count { it.status == "completed" }
        val errors = items.count { it.status == "dead_letter" }
        summary.text = "در حال تحلیل $active  •  یادگرفته‌شده $completed${if (errors > 0) "  •  نیاز به تلاش مجدد $errors" else ""}"
        empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        listHost.removeAllViews()
        items.forEach { listHost.addView(jobCard(it), margin(dp(10))) }
    }

    private fun jobCard(item: SharedMediaQueue.Item): View = glassCard().apply {
        addView(label(item.shortcode.ifBlank { "Instagram Reel" }, 17f, Color.WHITE, true))
        addView(label(stateFa(item.status), 12.5f, stateColor(item.status), true).apply {
            setPadding(0, dp(6), 0, dp(9))
        })

        val progress = ProgressBar(this@ViralShareActivity, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            this.progress = item.progress
            progressTintList = ColorStateList.valueOf(accent)
            progressBackgroundTintList = ColorStateList.valueOf(Color.argb(42, 255, 255, 255))
        }
        addView(progress, LinearLayout.LayoutParams(-1, dp(6)).apply { bottomMargin = dp(8) })
        addView(label(progressFa(item), 11.8f, muted, false))

        item.error?.let {
            addView(label(errorFa(it), 12f, danger, false).apply { setPadding(0, dp(8), 0, 0) })
        }

        val result = item.resultJson?.let { raw -> runCatching { JSONObject(raw) }.getOrNull() }
        if (item.status == "completed" && result != null) {
            val quick = quickResult(result)
            if (quick.isNotBlank()) {
                addView(label(quick, 12.5f, Color.WHITE, false).apply { setPadding(0, dp(9), 0, 0) })
            }
            addView(primary("دیدن نتیجه تحلیل") { showDetail(item, result) }, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(12) })
        } else if (item.status in setOf("failed", "dead_letter")) {
            addView(primary("تلاش مجدد") {
                SharedMediaWork.retry(this@ViralShareActivity, item.localId)
                render()
            }, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(12) })
        }
    }

    private fun quickResult(root: JSONObject): String {
        val analysis = root.optJSONObject("analysis") ?: root
        val hook = textFrom(analysis.opt("hook_intelligence") ?: root.opt("hook"), listOf("exact_hook", "hook", "opening", "promise", "visual_hook"))
        val scenario = textFrom(analysis.opt("scenario_reconstruction") ?: root.opt("scenario"), listOf("summary", "scenario", "structure"))
        return buildString {
            if (hook.isNotBlank()) append("هوک: ").append(hook.take(120))
            if (scenario.isNotBlank()) {
                if (isNotEmpty()) append("\n")
                append("سناریو: ").append(scenario.take(140))
            }
        }
    }

    private fun showDetail(item: SharedMediaQueue.Item, root: JSONObject) {
        val analysis = root.optJSONObject("analysis") ?: root
        val text = buildString {
            section("هوک", pretty(analysis.opt("hook_intelligence") ?: root.opt("hook")))
            section("سناریو", pretty(analysis.opt("scenario_reconstruction") ?: root.opt("scenario")))
            section("دیالوگ و متن", pretty(analysis.opt("dialogue_and_text") ?: root.opt("dialogue")))
            section("کال تو اکشن", pretty(analysis.opt("cta") ?: analysis.opt("call_to_action") ?: root.opt("cta")))
            section("تصویر و تدوین", pretty(analysis.opt("visual_grammar") ?: analysis.opt("camera_and_editing")))
            section("صدا و موسیقی", pretty(analysis.opt("audio_music")))
            section("مکانیزم‌های رفتاری", pretty(analysis.opt("behavioral_mechanisms") ?: analysis.opt("reusable_mechanisms")))
            section("فرضیه ریتنشن", pretty(analysis.opt("retention_hypotheses")))
            section("چرا وایرال شده", pretty(analysis.opt("share_save_comment_hypotheses") ?: analysis.opt("virality_hypotheses")))
        }.ifBlank { "تحلیل ذخیره شده است، اما خلاصه قابل نمایش در این نسخه پیدا نشد." }

        AlertDialog.Builder(this)
            .setTitle("تحلیل ${item.shortcode}")
            .setMessage(text)
            .setPositiveButton("بستن", null)
            .show()
    }

    private fun refreshServerProgress() {
        if (!polling.compareAndSet(false, true)) return
        val candidates = queue.all().filter {
            it.status in setOf("submitting", "processing", "failed") &&
                !it.jobId.isNullOrBlank() && !it.pollToken.isNullOrBlank()
        }.take(8)
        if (candidates.isEmpty()) {
            polling.set(false)
            return
        }
        val endpoint = PayloadClient.VIRAL_PRODUCTION_ENDPOINT
        val companionKey = SecretStore(this).get("api_key").orEmpty()
        if (companionKey.isBlank()) {
            polling.set(false)
            return
        }
        Thread {
            try {
                candidates.forEach { item ->
                    val response = runCatching {
                        SharedMediaClient.status(endpoint, item.jobId!!, item.pollToken!!, companionKey)
                    }.getOrNull() ?: return@forEach
                    if (response.code in 200..299 && response.body != null) {
                        queue.updateServerState(item.localId, response.body)
                    }
                }
            } finally {
                polling.set(false)
                runOnUiThread { if (resumed) render() }
            }
        }.start()
    }

    private fun stateFa(status: String): String = when (status) {
        "completed" -> "یادگیری کامل شد"
        "queued" -> "در صف"
        "submitting" -> "در حال ارسال"
        "processing" -> "در حال تحلیل"
        "failed" -> "موقتاً متوقف شد"
        "dead_letter" -> "نیاز به تلاش مجدد"
        else -> "در حال پردازش"
    }

    private fun stateColor(status: String): Int = when (status) {
        "completed" -> success
        "failed", "dead_letter" -> warning
        else -> accent
    }

    private fun progressFa(item: SharedMediaQueue.Item): String = when (item.stage) {
        "queued" -> "منتظر شروع"
        "submitting" -> "در حال ساخت Job"
        "public_analysis" -> "در حال دریافت و تحلیل ریلز"
        "media_fetch", "download" -> "در حال دریافت ویدیو"
        "transcription" -> "در حال فهم دیالوگ"
        "visual_analysis" -> "در حال تحلیل تصویر"
        "learning_persist" -> "در حال ثبت در حافظه"
        "completed" -> "تمام شد"
        else -> "${item.progress}٪ پیشرفت"
    }

    private fun errorFa(raw: String): String {
        val e = raw.lowercase()
        return when {
            "companion_credential_required" in e || "invalid companion key" in e || "http 401" in e ->
                "این مورد از نسخه قدیمی مانده بود و خودکار برای مسیر جدید بازیابی می‌شود."
            "media_url_unavailable" in e -> "فایل این ریلز فعلاً از Instagram قابل دریافت نیست."
            "429" in e -> "سرور موقتاً شلوغ است؛ دوباره تلاش می‌شود."
            "timeout" in e -> "زمان تحلیل بیش از حد شد؛ دوباره تلاش کن."
            else -> "پردازش کامل نشد؛ دوباره تلاش کن."
        }
    }

    private fun textFrom(value: Any?, preferredKeys: List<String>): String = when (value) {
        null, JSONObject.NULL -> ""
        is String -> value.trim()
        is JSONObject -> {
            preferredKeys.firstNotNullOfOrNull { key -> value.optString(key).trim().takeIf { it.isNotBlank() } }
                ?: value.toString()
        }
        else -> value.toString()
    }

    private fun pretty(value: Any?): String = when (value) {
        null, JSONObject.NULL -> ""
        is String -> value
        is JSONObject -> buildString {
            val keys = value.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val child = pretty(value.opt(key))
                if (child.isNotBlank()) append("• ").append(child).append('\n')
            }
        }.trim()
        is JSONArray -> buildString {
            for (i in 0 until value.length()) {
                val child = pretty(value.opt(i))
                if (child.isNotBlank()) append("• ").append(child).append('\n')
            }
        }.trim()
        else -> value.toString()
    }

    private fun StringBuilder.section(title: String, value: String) {
        if (value.isBlank() || value == "null") return
        if (isNotEmpty()) append("\n\n")
        append(title).append("\n").append(value)
    }

    private fun glassCard(strong: Boolean = false) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setPadding(dp(17), dp(16), dp(17), dp(16))
        background = rounded(if (strong) glassStrong else glass, 24, border)
        elevation = dp(4).toFloat()
    }

    private fun label(value: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        gravity = Gravity.START
        if (bold) typeface = Typeface.DEFAULT_BOLD
        setLineSpacing(0f, 1.18f)
    }

    private fun primary(value: String, click: () -> Unit) = Button(this).apply {
        text = value
        isAllCaps = false
        textSize = 13.5f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.rgb(4, 24, 24))
        background = rounded(accent, 17, accent)
        setOnClickListener { click() }
    }

    private fun rounded(fill: Int, radius: Int, stroke: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(fill)
        cornerRadius = dp(radius).toFloat()
        setStroke(dp(1), stroke)
    }

    private fun margin(bottom: Int) = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = bottom }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
