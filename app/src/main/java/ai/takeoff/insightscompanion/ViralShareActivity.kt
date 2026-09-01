package ai.takeoff.insightscompanion

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

class ViralShareActivity : Activity() {
    companion object {
        const val EXTRA_ADDED_COUNT = "added_count"
        private const val REQ_ATTACH_VIDEO = 7312
    }

    private val bg0 = Color.rgb(250, 252, 255)
    private val bg1 = Color.rgb(243, 252, 253)
    private val glass = Color.argb(214, 255, 255, 255)
    private val glassStrong = Color.argb(240, 255, 255, 255)
    private val border = Color.argb(82, 9, 24, 43)
    private val accent = Color.rgb(16, 202, 205)
    private val orange = Color.rgb(255, 122, 26)
    private val ink = Color.rgb(15, 23, 35)
    private val muted = Color.rgb(94, 108, 126)
    private val success = Color.rgb(0, 154, 137)
    private val warning = Color.rgb(226, 112, 18)
    private val danger = Color.rgb(210, 63, 76)

    private lateinit var queue: SharedMediaQueue
    private lateinit var listHost: LinearLayout
    private lateinit var reportsHost: LinearLayout
    private lateinit var summary: TextView
    private lateinit var empty: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val polling = AtomicBoolean(false)
    private var resumed = false
    private var attachTargetId: String? = null

    private val ticker = object : Runnable {
        override fun run() {
            if (!resumed) return
            render(); refreshServerProgress(); handler.postDelayed(this, 2_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        queue = SharedMediaQueue(this)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = bg0
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        recoverOldCredentialFailures()
        setContentView(buildUi())
        window.decorView.rootView.alpha = 0f
        window.decorView.rootView.translationY = dp(18).toFloat()
        window.decorView.rootView.animate().alpha(1f).translationY(0f).setDuration(520).setInterpolator(DecelerateInterpolator()).start()
        val added = intent.getIntExtra(EXTRA_ADDED_COUNT, 0)
        if (added > 0) Toast.makeText(this, "$added ریلز وارد موتور تحلیل شد", Toast.LENGTH_SHORT).show()
        render()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent); setIntent(intent); recoverOldCredentialFailures()
        val added = intent.getIntExtra(EXTRA_ADDED_COUNT, 0)
        if (added > 0) Toast.makeText(this, "$added مورد دیگر اضافه شد", Toast.LENGTH_SHORT).show()
        render()
    }

    override fun onResume() { super.onResume(); resumed = true; recoverOldCredentialFailures(); handler.removeCallbacks(ticker); handler.post(ticker) }
    override fun onPause() { resumed = false; handler.removeCallbacks(ticker); super.onPause() }

    @Deprecated("Activity result compatibility for minSdk 26")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_ATTACH_VIDEO || resultCode != RESULT_OK) return
        val target = attachTargetId ?: return
        val uri = data?.data ?: return
        val saved = SharedMediaIntake.persist(this, uri)
        if (saved == null) {
            Toast.makeText(this, "فایل ویدیو قابل خواندن نبود", Toast.LENGTH_LONG).show(); return
        }
        queue.attachLocalMedia(target, saved.path, saved.mime)?.let { SharedMediaWork.enqueue(this, it) }
        attachTargetId = null
        Toast.makeText(this, "فایل اصلی اضافه شد؛ تحلیل کامل شروع شد", Toast.LENGTH_SHORT).show()
        render()
    }

    private fun recoverOldCredentialFailures() { queue.recoverLegacyCredentialFailures().forEach { SharedMediaWork.enqueue(this, it) } }

    private fun buildUi(): View {
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(bg0, bg1, Color.rgb(255, 246, 237)))
        }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(18), dp(26), dp(18), dp(30)) }
        root.addView(glassCard(strong = true).apply {
            addView(label("موتور هوشمند تحلیل ریلز", 25f, ink, true))
            addView(label("فایل واقعی، صدا، تصویر، Hook، داستان، Retention و الگوهای رفتاری مرحله‌به‌مرحله بررسی و فقط با شواهد کافی وارد حافظه می‌شوند.", 13f, muted, false).apply { setPadding(0, dp(7), 0, dp(12)) })
            summary = label("در حال خواندن…", 13f, accent, true); addView(summary)
        }, margin(dp(14)))

        empty = label("هنوز ریلزی نفرستادی.\nInstagram → Share → TakeOff", 15f, muted, false).apply { gravity = Gravity.CENTER; setPadding(dp(12), dp(44), dp(12), dp(44)) }
        root.addView(empty)
        listHost = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        root.addView(listHost)

        // Per request, reports are rendered automatically below the Close button.
        root.addView(Button(this).apply {
            text = "بستن"; isAllCaps = false; textSize = 13f; setTextColor(ink)
            background = rounded(Color.argb(70, 255, 255, 255), 18, border)
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(8); bottomMargin = dp(14) })

        reportsHost = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        root.addView(reportsHost)
        scroll.addView(root)
        return scroll
    }

    private fun render() {
        if (!::listHost.isInitialized) return
        val items = queue.all()
        val active = items.count { it.status in setOf("queued", "submitting", "processing", "failed") }
        val completed = items.count { it.status == "completed" }
        val needsMedia = items.count { it.status in setOf("needs_media", "partial") }
        summary.text = "در حال تحلیل $active  •  یادگرفته‌شده $completed${if (needsMedia > 0) "  •  نیازمند فایل $needsMedia" else ""}"
        empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        listHost.removeAllViews(); reportsHost.removeAllViews()
        items.forEach { listHost.addView(jobCard(it), margin(dp(10))) }
        val completedItems = items.filter { it.status == "completed" && !it.resultJson.isNullOrBlank() }
        completedItems.forEach { item ->
            val result = runCatching { JSONObject(item.resultJson!!) }.getOrNull() ?: return@forEach
            reportsHost.addView(reportCard(item, result), margin(dp(12)))
        }
    }

    private fun jobCard(item: SharedMediaQueue.Item): View = glassCard().apply {
        addView(label(item.shortcode.ifBlank { "Instagram Reel" }, 18f, ink, true))
        addView(label(stateFa(item.status), 13f, stateColor(item.status), true).apply { setPadding(0, dp(5), 0, dp(8)) })
        addView(label("پیشرفت کل: ${item.progress}٪  •  مرحله جاری: ${progressFa(item)}", 12f, muted, false))
        val progress = ProgressBar(this@ViralShareActivity, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100; this.progress = item.progress
            progressTintList = ColorStateList.valueOf(accent); progressBackgroundTintList = ColorStateList.valueOf(Color.argb(35, 9, 24, 43))
        }
        addView(progress, LinearLayout.LayoutParams(-1, dp(7)).apply { topMargin = dp(8); bottomMargin = dp(12) })
        addView(stagePanel(item))

        item.error?.let { addView(label(errorFa(it), 12f, if (item.status == "needs_media") warning else danger, false).apply { setPadding(0, dp(10), 0, 0) }) }
        when (item.status) {
            "needs_media", "partial" -> {
                addView(primary("انتخاب فایل اصلی ویدیو") { chooseVideo(item.localId) }, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(12) })
                addView(label("اگر Instagram فقط لینک را Share کرده باشد، موتور ابتدا مسیرهای سروری را امتحان می‌کند. اگر همه آن‌ها مسدود باشند، فایل اصلی را انتخاب کن تا تحلیل چندوجهی از روی خود ویدیو انجام شود.", 11.5f, muted, false).apply { setPadding(0, dp(8), 0, 0) })
            }
            "failed", "dead_letter" -> addView(primary("تلاش مجدد") { SharedMediaWork.retry(this@ViralShareActivity, item.localId); render() }, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(12) })
            "completed" -> addView(label("✓ شواهد ویدیویی کافی بود؛ گزارش کامل به‌صورت خودکار پایین همین صفحه نمایش داده شده است.", 12f, success, true).apply { setPadding(0, dp(10), 0, 0) })
        }
    }

    private fun stagePanel(item: SharedMediaQueue.Item): LinearLayout {
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(12), dp(10), dp(12), dp(10)); background = rounded(Color.argb(55, 16, 202, 205), 18, Color.argb(55, 16, 202, 205)) }
        val stages = listOf(
            "آماده‌سازی موتور" to 4,
            "یافتن فایل اصلی ریلز" to 18,
            "دریافت واقعی ویدیو" to 32,
            "ارسال رسانه برای تحلیل" to 42,
            "تحلیل صدا، تصویر و متن" to 62,
            "تحلیل رفتاری و Retention" to 78,
            "تطبیق با حافظه و الگوها" to 90,
            "ثبت در حافظه یادگیری" to 100,
        )
        stages.forEach { (name, threshold) ->
            val done = item.progress >= threshold || item.status == "completed"
            val current = !done && item.progress < threshold && threshold == stages.firstOrNull { item.progress < it.second }?.second
            val prefix = when { done -> "✓"; current -> "◉"; else -> "○" }
            panel.addView(label("$prefix  $name${if (done) "  ۱۰۰٪" else if (current) "  ${item.progress}٪" else ""}", 12f, if (done || current) accent else muted, done || current).apply { setPadding(0, dp(4), 0, dp(4)) })
        }
        return panel
    }

    private fun reportCard(item: SharedMediaQueue.Item, root: JSONObject): View = glassCard(strong = true).apply {
        addView(label("گزارش هوشمند کامل — ${item.shortcode}", 20f, accent, true))
        addView(label("تحلیل از روی شواهد واقعی ویدیو؛ مشاهدات از فرضیه‌ها جدا نگه داشته می‌شوند.", 11.8f, muted, false).apply { setPadding(0, dp(6), 0, dp(12)) })
        addView(label(ViralReportFormatter.full(root), 13.5f, ink, false).apply { setTextIsSelectable(true); setPadding(0, dp(4), 0, dp(8)) })
    }

    private fun chooseVideo(localId: String) {
        attachTargetId = localId
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "video/*" }, REQ_ATTACH_VIDEO)
    }

    private fun refreshServerProgress() {
        if (!polling.compareAndSet(false, true)) return
        val candidates = queue.all().filter { it.status in setOf("submitting", "processing", "failed") && !it.jobId.isNullOrBlank() && !it.pollToken.isNullOrBlank() }.take(8)
        if (candidates.isEmpty()) { polling.set(false); return }
        val endpoint = PayloadClient.VIRAL_PRODUCTION_ENDPOINT
        val companionKey = SecretStore(this).get("api_key").orEmpty()
        if (companionKey.isBlank()) { polling.set(false); return }
        Thread {
            try {
                candidates.forEach { item ->
                    val response = runCatching { SharedMediaClient.status(endpoint, item.jobId!!, item.pollToken!!, companionKey) }.getOrNull() ?: return@forEach
                    if (response.code in 200..299 && response.body != null) queue.updateServerState(item.localId, response.body)
                }
            } finally { polling.set(false); runOnUiThread { if (resumed) render() } }
        }.start()
    }

    private fun stateFa(status: String): String = when (status) {
        "completed" -> "یادگیری کامل و تأیید شد"
        "queued" -> "در صف موتور"
        "submitting" -> "در حال آماده‌سازی سرور"
        "processing" -> "تحلیل عمیق در حال اجراست"
        "needs_media", "partial" -> "پردازش روی سرور متوقف شد — فایل اصلی لازم است"
        "failed" -> "پردازش موقتاً متوقف شد"
        "dead_letter" -> "نیاز به تلاش مجدد"
        else -> "در حال پردازش"
    }

    private fun stateColor(status: String): Int = when (status) { "completed" -> success; "failed", "dead_letter", "needs_media", "partial" -> warning; else -> accent }

    private fun progressFa(item: SharedMediaQueue.Item): String = when (item.stage) {
        "queued" -> "آماده‌سازی"
        "submitting" -> "ساخت Job"
        "media_ready" -> "فایل اصلی آماده است"
        "media_fetch", "download", "public_analysis" -> "یافتن و دریافت فایل اصلی"
        "media_upload" -> "ارسال رسانه برای تحلیل"
        "transcription" -> "فهم دیالوگ و صدا"
        "visual_analysis", "deep_analysis" -> "فهم تصویر، Hook و داستان"
        "behavioral_analysis", "retention" -> "تحلیل رفتاری و Retention"
        "matching", "pattern_matching" -> "تطبیق با حافظه"
        "learning", "learning_persist" -> "ثبت در حافظه یادگیری"
        "media_required" -> "فایل اصلی قابل دریافت نیست"
        "completed" -> "تمام شد"
        else -> "${item.progress}٪"
    }

    private fun errorFa(raw: String): String {
        val e = raw.lowercase()
        return when {
            "media_url_unavailable" in e || "insufficient" in e -> "سرور نتوانست فایل اصلی این ریلز را از Instagram دریافت کند؛ این مورد یادگرفته‌شده حساب نشده است. فایل اصلی را انتخاب کن تا تحلیل واقعی ادامه پیدا کند."
            "companion_credential_required" in e || "invalid companion key" in e || "http 401" in e -> "برای ارسال مستقیم فایل واقعی به موتور خصوصی، اتصال اپ باید معتبر باشد؛ مسیر عمومی سرور همچنان خودکار امتحان می‌شود."
            "429" in e -> "سرور موقتاً شلوغ است؛ دوباره تلاش می‌شود."
            "timeout" in e -> "زمان پردازش بیش از حد شد؛ دوباره تلاش کن."
            else -> "پردازش کامل نشد؛ دوباره تلاش کن."
        }
    }

    private fun glassCard(strong: Boolean = false) = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(17), dp(16), dp(17), dp(16)); background = rounded(if (strong) glassStrong else glass, 24, border); elevation = dp(6).toFloat() }
    private fun label(value: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply { text = value; textSize = size; setTextColor(color); gravity = Gravity.START; if (bold) typeface = Typeface.DEFAULT_BOLD; setLineSpacing(0f, 1.2f) }
    private fun primary(value: String, click: () -> Unit) = Button(this).apply { text = value; isAllCaps = false; textSize = 13.5f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); background = rounded(orange, 17, orange); setOnClickListener { v -> v.animate().scaleX(.97f).scaleY(.97f).alpha(.82f).setDuration(80).withEndAction { v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(150).start(); click() }.start() } }
    private fun rounded(fill: Int, radius: Int, stroke: Int) = GradientDrawable().apply { shape = GradientDrawable.RECTANGLE; setColor(fill); cornerRadius = dp(radius).toFloat(); setStroke(dp(1), stroke) }
    private fun margin(bottom: Int) = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = bottom }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
