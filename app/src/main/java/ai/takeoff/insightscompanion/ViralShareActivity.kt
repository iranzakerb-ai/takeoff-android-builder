package ai.takeoff.insightscompanion

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
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
import java.text.DecimalFormat
import java.util.concurrent.atomic.AtomicBoolean

class ViralShareActivity : Activity() {
    companion object {
        const val EXTRA_ADDED_COUNT = "added_count"
    }

    private val bg0 = Color.rgb(4, 7, 13)
    private val bg1 = Color.rgb(8, 19, 28)
    private val glass = Color.argb(172, 18, 28, 40)
    private val glassStrong = Color.argb(214, 14, 24, 35)
    private val glassSoft = Color.argb(112, 27, 43, 58)
    private val border = Color.argb(105, 123, 222, 220)
    private val accent = Color.rgb(54, 239, 218)
    private val blue = Color.rgb(111, 171, 255)
    private val muted = Color.rgb(176, 190, 207)
    private val warning = Color.rgb(255, 196, 92)
    private val danger = Color.rgb(255, 111, 132)
    private val success = Color.rgb(91, 236, 171)

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
            handler.postDelayed(this, 1_500L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        queue = SharedMediaQueue(this)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = bg0
        setContentView(buildUi())
        val added = intent.getIntExtra(EXTRA_ADDED_COUNT, 0)
        if (added > 0) Toast.makeText(this, "$added مورد وارد صف یادگیری شد", Toast.LENGTH_SHORT).show()
        render()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val added = intent.getIntExtra(EXTRA_ADDED_COUNT, 0)
        if (added > 0) Toast.makeText(this, "$added مورد دیگر به صف اضافه شد", Toast.LENGTH_SHORT).show()
        render()
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    override fun onPause() {
        resumed = false
        handler.removeCallbacks(ticker)
        super.onPause()
    }

    private fun buildUi(): View {
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(bg0, bg1, Color.rgb(5, 12, 22)))
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(16), dp(22), dp(16), dp(30))
        }
        root.addView(glassCard(strong = true).apply {
            addView(label("TAKEOFF • V4 LEARNING CONSOLE", 11f, accent, true))
            addView(label("صف یادگیری چندرسانه‌ای", 27f, Color.WHITE, true).apply { setPadding(0, dp(5), 0, 0) })
            addView(label("Reel • Video • Photo • Carousel • AI Forensics", 12f, muted, false).apply { setPadding(0, dp(6), 0, dp(12)) })
            summary = label("در حال خواندن صف…", 14f, Color.WHITE, true)
            addView(summary)
            addView(label("هر Share یک Job مستقل است؛ Share جدید پردازش قبلی را متوقف نمی‌کند. سه Lane می‌توانند همزمان کار کنند.", 12f, muted, false).apply { setPadding(0, dp(7), 0, 0) })
        }, margin(dp(12)))

        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            addView(button("تازه‌سازی", false) { render(); refreshServerProgress() }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(5) })
            addView(button("بستن", false) { finish() }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginStart = dp(5) })
        }, margin(dp(14)))

        empty = label("هنوز چیزی در صف نیست. از Instagram روی Share بزن و «تیک‌آف» را انتخاب کن.", 13.5f, muted, false).apply {
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(28), dp(18), dp(28))
        }
        root.addView(empty)
        listHost = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        root.addView(listHost)
        scroll.addView(root)
        return scroll
    }

    private fun render() {
        val items = queue.all()
        val active = items.count { it.status in setOf("queued", "submitting", "processing", "failed") }
        val completed = items.count { it.status == "completed" }
        val errors = items.count { it.status == "dead_letter" }
        summary.text = "فعال $active  •  تکمیل $completed  •  نیازمند بررسی $errors  •  کل ${items.size}"
        empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        listHost.removeAllViews()
        items.forEach { listHost.addView(jobCard(it), margin(dp(10))) }
    }

    private fun jobCard(item: SharedMediaQueue.Item): View = glassCard().apply {
        val top = LinearLayout(this@ViralShareActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(label(kindFa(item.mediaKind), 12f, kindColor(item.mediaKind), true).apply {
            background = pill(Color.argb(70, Color.red(kindColor(item.mediaKind)), Color.green(kindColor(item.mediaKind)), Color.blue(kindColor(item.mediaKind))))
            setPadding(dp(10), dp(5), dp(10), dp(5))
        })
        top.addView(label(item.shortcode.ifBlank { "Instagram" }, 15f, Color.WHITE, true).apply { setPadding(dp(10), 0, 0, 0) }, LinearLayout.LayoutParams(0, -2, 1f))
        addView(top)

        addView(label(stateFa(item), 13f, stateColor(item.status), true).apply { setPadding(0, dp(9), 0, dp(4)) })
        val progress = ProgressBar(this@ViralShareActivity, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100; this.progress = item.progress; isIndeterminate = false
        }
        addView(progress, LinearLayout.LayoutParams(-1, dp(7)).apply { bottomMargin = dp(7) })
        addView(label("${stageFa(item.stage)} • ${fa(item.progress)}٪ • Lane ${item.lane + 1}", 11.7f, muted, false))

        val result = item.resultJson?.let { runCatching { JSONObject(it) }.getOrNull() }
        if (result != null) {
            val metrics = result.optJSONObject("public_metrics") ?: result.optJSONObject("public_metrics_json")
            val analysis = result.optJSONObject("analysis") ?: result
            val origin = analysis.optJSONObject("media_origin") ?: result.optJSONObject("media_origin")
            val hook = analysis.optJSONObject("hook_intelligence") ?: result.optJSONObject("hook")
            val promo = result.optString("promotion_status").ifBlank { analysis.optString("promotion_status") }
            if (metrics != null) addView(label(metricsLine(metrics), 12.5f, Color.WHITE, true).apply { setPadding(0, dp(9), 0, 0) })
            if (origin != null) {
                val classification = origin.optString("classification", "UNKNOWN")
                val confidence = origin.optDouble("confidence", Double.NaN)
                addView(label("منشأ: ${originFa(classification)}${if (confidence.isFinite()) " • ${fa((confidence * 100).toInt())}٪ اطمینان" else ""}", 12.3f, blue, true).apply { setPadding(0, dp(6), 0, 0) })
            }
            val hookText = compactHook(hook)
            if (hookText.isNotBlank()) addView(label("قلاب: $hookText", 12.3f, muted, false).apply { setPadding(0, dp(5), 0, 0) })
            if (promo.isNotBlank()) addView(label("یادگیری V4: ${promotionFa(promo)}", 12.3f, if (promo == "PROMOTE") success else warning, true).apply { setPadding(0, dp(5), 0, 0) })
        }
        item.error?.let { addView(label("خطا: $it", 11.8f, danger, false).apply { setPadding(0, dp(8), 0, 0) }) }

        val actions = LinearLayout(this@ViralShareActivity).apply {
            orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        if (item.status == "completed" && item.resultJson != null) {
            actions.addView(button("گزارش کامل", true) { showDetail(item) }, LinearLayout.LayoutParams(0, dp(46), 1f))
        } else if (item.status in setOf("failed", "dead_letter")) {
            actions.addView(button("تلاش مجدد", true) { SharedMediaWork.retry(this@ViralShareActivity, item.localId); render() }, LinearLayout.LayoutParams(0, dp(46), 1f))
        } else {
            actions.addView(label("پردازش در پس‌زمینه ادامه دارد", 11.5f, accent, true).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(0, dp(46), 1f))
        }
        addView(actions, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
    }

    private fun refreshServerProgress() {
        if (!polling.compareAndSet(false, true)) return
        val candidates = queue.all().filter { it.status in setOf("submitting", "processing", "failed") && !it.jobId.isNullOrBlank() && !it.pollToken.isNullOrBlank() }.take(8)
        if (candidates.isEmpty()) { polling.set(false); return }
        val endpoint = PayloadClient.viralEndpoint(getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE).getString("endpoint", "").orEmpty())
        val companionKey = SecretStore(this).get("api_key").orEmpty()
        if (companionKey.isBlank()) { polling.set(false); return }
        Thread {
            try {
                candidates.forEach { item ->
                    val response = runCatching { SharedMediaClient.status(endpoint, item.jobId!!, item.pollToken!!, companionKey) }.getOrNull() ?: return@forEach
                    if (response.code in 200..299 && response.body != null) queue.updateServerState(item.localId, response.body)
                }
            } finally {
                polling.set(false)
                runOnUiThread { if (resumed) render() }
            }
        }.start()
    }

    private fun showDetail(item: SharedMediaQueue.Item) {
        val root = item.resultJson?.let { runCatching { JSONObject(it) }.getOrNull() } ?: return
        val analysis = root.optJSONObject("analysis") ?: root
        val metrics = root.optJSONObject("public_metrics") ?: root.optJSONObject("public_metrics_json")
        val text = buildString {
            section("نوع محتوا", kindFa(item.mediaKind))
            if (metrics != null) section("آمار عمومی", pretty(metrics))
            section("تشخیص منشأ AI / فیلمبرداری", pretty(analysis.opt("media_origin") ?: root.opt("media_origin")))
            section("هدف و مخاطب", pretty(analysis.opt("goal_and_audience") ?: root.opt("audience_intent")))
            section("کالبدشکافی قلاب", pretty(analysis.opt("hook_intelligence") ?: root.opt("hook")))
            section("بازسازی سناریو", pretty(analysis.opt("scenario_reconstruction") ?: root.opt("scenario")))
            section("تایم‌لاین صحنه / اسلاید", pretty(analysis.opt("slide_or_scene_timeline")))
            section("دیالوگ و متن", pretty(analysis.opt("dialogue_and_text")))
            section("گرامر بصری", pretty(analysis.opt("visual_grammar")))
            section("دوربین و تدوین", pretty(analysis.opt("camera_and_editing")))
            section("صدا و موسیقی", pretty(analysis.opt("audio_music")))
            section("مکانیزم‌های رفتاری", pretty(analysis.opt("behavioral_mechanisms")))
            section("فرضیه ریتنشن", pretty(analysis.opt("retention_hypotheses")))
            section("چرا Share/Save/Comment", pretty(analysis.opt("share_save_comment_hypotheses")))
            section("مکانیزم‌های قابل یادگیری", pretty(analysis.opt("reusable_mechanisms")))
            section("عدم قطعیت‌ها", pretty(analysis.opt("uncertainties")))
            section("وضعیت ورود به V4", promotionFa(root.optString("promotion_status")))
        }
        AlertDialog.Builder(this)
            .setTitle("گزارش V4 • ${item.shortcode}")
            .setMessage(text)
            .setPositiveButton("بستن", null)
            .show()
    }

    private fun StringBuilder.section(title: String, value: String) {
        if (value.isBlank() || value == "null") return
        if (isNotEmpty()) append("\n\n")
        append(title).append("\n").append(value)
    }

    private fun pretty(value: Any?): String = when (value) {
        null, JSONObject.NULL -> ""
        is String -> value
        is JSONObject -> buildString {
            val keys = value.keys()
            while (keys.hasNext()) {
                val key = keys.next(); append("• ").append(key).append(": ").append(pretty(value.opt(key))).append('\n')
            }
        }.trim()
        is JSONArray -> buildString {
            for (i in 0 until value.length()) append("• ").append(pretty(value.opt(i))).append('\n')
        }.trim()
        else -> value.toString()
    }

    private fun compactHook(hook: JSONObject?): String {
        if (hook == null) return ""
        for (key in listOf("exact_hook", "hook", "opening", "promise", "visual_hook")) {
            val value = hook.optString(key).trim()
            if (value.isNotBlank()) return value.take(150)
        }
        return hook.toString().take(150)
    }

    private fun metricsLine(metrics: JSONObject): String {
        fun value(key: String): String {
            if (!metrics.has(key) || metrics.isNull(key)) return "—"
            return compact(metrics.optDouble(key, Double.NaN))
        }
        return "بازدید ${value("views")} • لایک ${value("likes")} • اشتراک ${value("shares")} • نظر ${value("comments")}"
    }

    private fun compact(value: Double): String = when {
        !value.isFinite() -> "—"
        value >= 1_000_000_000 -> DecimalFormat("0.0B").format(value / 1_000_000_000)
        value >= 1_000_000 -> DecimalFormat("0.0M").format(value / 1_000_000)
        value >= 1_000 -> DecimalFormat("0.0K").format(value / 1_000)
        else -> DecimalFormat("0").format(value)
    }

    private fun stateFa(item: SharedMediaQueue.Item): String = when (item.status) {
        "queued" -> "در صف امن"
        "submitting" -> "در حال ساخت Job پایدار"
        "processing" -> "در حال تحلیل عمیق"
        "completed" -> "تحلیل و یادگیری کامل شد"
        "failed" -> "موقتاً متوقف؛ قابل تلاش مجدد"
        "dead_letter" -> "نیازمند بررسی"
        else -> item.status
    }

    private fun stageFa(stage: String) = when (stage) {
        "queued" -> "صف"
        "submitting" -> "ارسال"
        "resolve" -> "دریافت داده و آمار"
        "download" -> "دریافت رسانه"
        "upload" -> "ارسال به موتور چندوجهی"
        "deep_analysis", "server_processing" -> "هوش چندوجهی + Forensics"
        "learning" -> "ثبت مستقیم در مغز V4"
        "completed" -> "تکمیل"
        "failed" -> "متوقف"
        else -> stage
    }

    private fun kindFa(kind: String) = when (kind) {
        "reel" -> "ریلز"
        "video_post" -> "پست ویدیویی"
        "photo" -> "عکس"
        "carousel" -> "اسلایدی"
        "mixed" -> "اسلایدی ترکیبی"
        else -> "در حال تشخیص"
    }

    private fun kindColor(kind: String) = when (kind) {
        "photo" -> Color.rgb(255, 177, 103)
        "carousel", "mixed" -> Color.rgb(190, 137, 255)
        "reel", "video_post" -> accent
        else -> blue
    }

    private fun stateColor(status: String) = when (status) {
        "completed" -> success
        "failed", "dead_letter" -> danger
        "queued" -> warning
        else -> accent
    }

    private fun originFa(value: String) = when (value) {
        "FILMED_LIVE_ACTION" -> "فیلمبرداری واقعی"
        "FULLY_AI_GENERATED" -> "کاملاً ساخته‌شده با AI"
        "AI_ASSISTED" -> "واقعی با کمک AI"
        "HYBRID_AI_AND_FILMED" -> "ترکیب فیلمبرداری و AI"
        "CGI_ANIMATION" -> "CGI / انیمیشن"
        "SCREEN_RECORDING" -> "ضبط صفحه"
        else -> "نامشخص"
    }

    private fun promotionFa(value: String) = when (value) {
        "PROMOTE" -> "ارتقا به حافظه فعال"
        "CONTROL" -> "نمونه کنترل"
        "REJECT" -> "رد از یادگیری فعال"
        "OBSERVE_MORE" -> "نیازمند شواهد بیشتر"
        else -> value.ifBlank { "هنوز تصمیم‌گیری نشده" }
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color); gravity = Gravity.START
        if (bold) typeface = Typeface.DEFAULT_BOLD
        setLineSpacing(0f, 1.16f)
    }

    private fun glassCard(strong: Boolean = false) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setPadding(dp(16), dp(15), dp(16), dp(15))
        background = rounded(if (strong) glassStrong else glass, 22, border)
        elevation = dp(4).toFloat()
    }

    private fun button(text: String, primary: Boolean, click: () -> Unit) = Button(this).apply {
        this.text = text; isAllCaps = false; textSize = 13f; typeface = Typeface.DEFAULT_BOLD
        setTextColor(if (primary) Color.rgb(4, 22, 23) else Color.WHITE)
        background = rounded(if (primary) accent else glassSoft, 15, if (primary) accent else border)
        setOnClickListener { click() }
    }

    private fun rounded(fill: Int, radius: Int, stroke: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE; setColor(fill); cornerRadius = dp(radius).toFloat(); setStroke(dp(1), stroke)
    }

    private fun pill(fill: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE; setColor(fill); cornerRadius = dp(20).toFloat(); setStroke(dp(1), border)
    }

    private fun margin(bottom: Int) = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = bottom }
    private fun fa(value: Int): String = value.toString().map { "۰۱۲۳۴۵۶۷۸۹"[it.digitToInt()] }.joinToString("")
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
