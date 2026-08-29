package ai.takeoff.insightscompanion

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject

class ViralShareActivity : Activity() {
    private val bg = Color.rgb(6, 9, 14)
    private val panel = Color.rgb(14, 20, 28)
    private val panel2 = Color.rgb(18, 27, 37)
    private val border = Color.rgb(38, 52, 68)
    private val accent = Color.rgb(0, 228, 208)
    private val muted = Color.rgb(173, 185, 199)
    private val danger = Color.rgb(255, 102, 118)

    private data class StageUi(val title: String, val value: TextView, val bar: ProgressBar)

    private val order = listOf(
        "server_prepare" to "آماده‌سازی سرور",
        "media_resolve" to "یافتن فایل اصلی ریلز",
        "media_download" to "دریافت واقعی ویدیو",
        "ai_upload" to "ارسال رسانه برای تحلیل",
        "audio_visual_analysis" to "تحلیل صدا و تصویر",
        "behavioral_analysis" to "تحلیل رفتاری و Retention",
        "learning_persist" to "ثبت در حافظه یادگیری",
    )
    private val stageViews = linkedMapOf<String, StageUi>()

    private lateinit var status: TextView
    private lateinit var overallText: TextView
    private lateinit var overallBar: ProgressBar
    private lateinit var detailText: TextView
    private lateinit var urlView: TextView
    private lateinit var reportView: TextView
    private lateinit var retry: Button

    private var reelUrl = ""
    @Volatile private var generation = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
        consume(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consume(intent)
    }

    override fun onDestroy() {
        generation++
        super.onDestroy()
    }

    private fun consume(source: Intent) {
        generation++
        val shared = source.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        reelUrl = Regex("https?://(?:www\\.)?instagram\\.com/(?:reel|reels|p)/[A-Za-z0-9_-]+(?:/[^\\s]*)?")
            .find(shared)?.value.orEmpty()
        reportView.text = ""
        retry.visibility = View.GONE
        resetProgress()
        if (reelUrl.isBlank()) {
            urlView.text = "از Instagram روی Share بزن و «تیک‌آف» را انتخاب کن."
            status.text = "لینک معتبر Instagram پیدا نشد"
            status.setTextColor(danger)
            return
        }
        urlView.text = reelUrl
        startAnalysis()
    }

    private fun buildUi(): View {
        val scroll = ScrollView(this).apply { setBackgroundColor(bg); isFillViewport = true }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(18), dp(22), dp(18), dp(30))
        }

        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(18))
            background = rounded(Color.rgb(6, 29, 31), 24, Color.rgb(0, 92, 86))
            addView(label("TAKEOFF • LIVE SERVER PROGRESS", 11f, accent, true))
            addView(label("کالبدشکافی زنده ریلز", 27f, Color.WHITE, true).apply { setPadding(0, dp(5), 0, 0) })
            addView(label("v${BuildConfig.VERSION_NAME} • Vercel • Gemini • Supabase", 12f, muted, false).apply { setPadding(0, dp(7), 0, 0) })
        }, lp(dp(14)))

        root.addView(card().apply {
            addView(label("ریلز ورودی", 12f, accent, true))
            urlView = label("در انتظار لینک…", 13f, Color.WHITE, false).apply {
                setTextIsSelectable(true); setPadding(0, dp(8), 0, 0)
            }
            addView(urlView)
        }, lp(dp(12)))

        root.addView(card().apply {
            addView(label("موتور سرور", 12f, accent, true))
            status = label("آماده…", 17f, Color.WHITE, true).apply { setPadding(0, dp(8), 0, dp(5)) }
            addView(status)
            overallText = label("پیشرفت کل: ۰٪", 14f, accent, true)
            addView(overallText)
            overallBar = ProgressBar(this@ViralShareActivity, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100; progress = 0; isIndeterminate = false
            }
            addView(overallBar, LinearLayout.LayoutParams(-1, dp(10)).apply { topMargin = dp(6); bottomMargin = dp(8) })
            detailText = label("در انتظار اولین پاسخ واقعی سرور…", 12f, muted, false)
            addView(detailText)
            addView(stagePanel(), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
            retry = Button(this@ViralShareActivity).apply {
                text = "تلاش مجدد"; isAllCaps = false; visibility = View.GONE; setTextColor(Color.WHITE)
                background = rounded(Color.rgb(29, 41, 54), 14, Color.rgb(53, 69, 86))
                setOnClickListener { startAnalysis() }
            }
            addView(retry, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(10) })
        }, lp(dp(12)))

        root.addView(card().apply {
            addView(label("گزارش هوشمند", 12f, accent, true))
            addView(label("صدا، تصویر، Hook، داستان، تدوین، Retention و الگوهای رفتاری روی سرور تحلیل و در حافظه یادگیری ثبت می‌شوند.", 12f, muted, false).apply { setPadding(0, dp(6), 0, dp(12)) })
            reportView = label("", 13.5f, Color.WHITE, false).apply { setLineSpacing(0f, 1.22f); setTextIsSelectable(true) }
            addView(reportView)
        })

        root.addView(Button(this).apply {
            text = "بستن"; isAllCaps = false; setTextColor(Color.WHITE); background = rounded(panel2, 14, border); setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(14) })

        scroll.addView(root)
        return scroll
    }

    private fun stagePanel(): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; background = rounded(panel2, 16); setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        order.forEach { (key, title) ->
            val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
            val titleView = label(title, 12.5f, muted, false)
            val valueView = label("۰٪", 12.5f, muted, true).apply { gravity = Gravity.END }
            header.addView(titleView, LinearLayout.LayoutParams(0, -2, 1f)); header.addView(valueView, LinearLayout.LayoutParams(dp(56), -2)); box.addView(header)
            val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100; progress = 0; isIndeterminate = false }
            box.addView(bar, LinearLayout.LayoutParams(-1, dp(6)).apply { topMargin = dp(3); bottomMargin = dp(7) })
            stageViews[key] = StageUi(title, valueView, bar)
        }
        return box
    }

    private fun startAnalysis() {
        if (reelUrl.isBlank()) return
        val mine = ++generation
        retry.visibility = View.GONE; reportView.text = ""; resetProgress(); status.text = "در حال اتصال به موتور Vercel…"; status.setTextColor(Color.WHITE)
        val niche = getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE).getString("viral_analysis_niche", "عمومی").orEmpty().ifBlank { "عمومی" }
        Thread {
            var terminal = false
            val result = runCatching {
                ViralJobClient.analyze(PayloadClient.PRODUCTION_ENDPOINT, reelUrl, niche) { event ->
                    if (generation != mine) return@analyze
                    if (event.optString("type") == "error" || event.optString("stage") == "completed") terminal = true
                    runOnUiThread { if (generation == mine) renderEvent(event) }
                }
            }.getOrElse {
                if (generation == mine) runOnUiThread { showError("ارتباط با سرور قطع شد", it.javaClass.simpleName) }
                return@Thread
            }
            if (generation != mine) return@Thread
            if (result.httpCode !in 200..299) runOnUiThread { showError("سرور پاسخ معتبر نداد", "HTTP ${result.httpCode} • ${result.errorBody.take(100)}") }
            else if (!terminal) runOnUiThread { showError("فرآیند بدون پاسخ نهایی پایان یافت", result.errorBody.ifBlank { "server_job_incomplete" }) }
        }.start()
    }

    private fun renderEvent(root: JSONObject) {
        if (root.optString("type") == "error") {
            showError("پردازش روی سرور متوقف شد", root.optString("error_code", "server_processing_failed")); return
        }
        val stage = root.optString("stage")
        val stagePct = root.optInt("stage_progress_percent", 0).coerceIn(0, 100)
        val overallPct = root.optInt("overall_progress_percent", 0).coerceIn(0, 100)
        val idx = order.indexOfFirst { it.first == stage }
        if (idx >= 0) {
            order.take(idx).forEach { setStage(it.first, 100, true) }
            setStage(stage, stagePct, stagePct >= 100)
        } else if (stage == "completed") order.forEach { setStage(it.first, 100, true) }
        overallBar.progress = overallPct
        overallText.text = "پیشرفت کل: ${fa(overallPct)}٪ • مرحله جاری: ${fa(stagePct)}٪"
        val serverLabel = root.optString("stage_label").ifBlank { stage }
        status.text = if (stage == "completed") "تحلیل کامل شد" else "$serverLabel — ${fa(stagePct)}٪"
        status.setTextColor(if (stage == "completed") accent else Color.WHITE)

        val elapsed = root.optLong("elapsed_ms", 0L); val downloaded = root.optLong("downloaded_bytes", 0L); val uploaded = root.optLong("uploaded_bytes", 0L); val total = root.optLong("total_bytes", 0L)
        detailText.text = when {
            downloaded > 0L -> "دریافت: ${bytes(downloaded)}${if (total > 0) " از ${bytes(total)}" else ""}${elapsedText(elapsed)}"
            uploaded > 0L -> "ارسال برای تحلیل: ${bytes(uploaded)}${if (total > 0) " از ${bytes(total)}" else ""}${elapsedText(elapsed)}"
            root.optBoolean("retryable", false) -> "سرویس تحلیل موقتاً شلوغ است؛ تیک‌آف خودکار دوباره تلاش می‌کند.${elapsedText(elapsed)}"
            elapsed > 0L -> "زمان سپری‌شده: ${formatSeconds(elapsed)}"
            else -> "پیشرفت از خود سرور دریافت می‌شود؛ درصد ساختگی نمایش داده نمی‌شود."
        }
        if (stage == "completed") showCompleted(root.optJSONObject("result") ?: JSONObject())
    }

    private fun setStage(key: String, pct: Int, done: Boolean) {
        val ui = stageViews[key] ?: return; val p = pct.coerceIn(0, 100); ui.bar.progress = p; ui.value.text = (if (done) "✓ " else "") + fa(p) + "٪"; ui.value.setTextColor(if (done) accent else if (p > 0) Color.WHITE else muted)
    }

    private fun showCompleted(result: JSONObject) {
        overallBar.progress = 100; overallText.text = "پیشرفت کل: ۱۰۰٪ • همه مراحل تکمیل شد"; retry.visibility = View.GONE
        val report = result.optJSONObject("report") ?: JSONObject()
        reportView.text = buildString {
            append("خلاصه\n").append(report.optString("summary", "—")).append("\n\n")
            append("Hook\n").append(pretty(report.opt("hook")))
            report.opt("hook_seconds")?.let { append("\nزمان Hook: ").append(pretty(it)) }
            append("\n\n")
            append("دیالوگ\n").append(pretty(report.opt("dialogue"))).append("\n\n")
            append("تایم‌لاین و ساختار صحنه‌ها\n").append(pretty(report.opt("timeline"))).append("\n\n")
            append("گرامر بصری\n").append(pretty(report.opt("visual_grammar"))).append("\n\n")
            append("دوربین و تدوین\n").append(pretty(report.opt("camera_and_editing"))).append("\n\n")
            append("صدا و موسیقی\n").append(pretty(report.opt("audio_music"))).append("\n\n")
            append("ریتم، Payoff و CTA\n")
                .append("ریتم: ").append(pretty(report.opt("pacing"))).append("\n")
                .append("Payoff: ").append(pretty(report.opt("payoff"))).append("\n")
                .append("CTA: ").append(pretty(report.opt("cta"))).append("\n\n")
            append("کنجکاوی و Open Loop\n").append(pretty(report.opt("curiosity_and_open_loops"))).append("\n\n")
            append("Pattern Interrupts\n").append(pretty(report.opt("pattern_interrupts"))).append("\n\n")
            append("محرک‌های رفتاری\n").append(pretty(report.opt("behavioral_triggers"))).append("\n\n")
            append("فرضیه‌های Retention\n").append(pretty(report.opt("retention_hypotheses"))).append("\n\n")
            append("فرضیه‌های Share / Save / Comment\n").append(pretty(report.opt("share_save_comment_hypotheses"))).append("\n\n")
            append("الگوهای قابل استفاده\n").append(pretty(report.opt("reusable_patterns"))).append("\n\n")
            append("Anti-patterns\n").append(pretty(report.opt("anti_patterns"))).append("\n\n")
            append("کیفیت شواهد و عدم قطعیت\n")
                .append("کیفیت: ").append(pretty(report.opt("evidence_quality"))).append("\n")
                .append("عدم قطعیت‌ها: ").append(pretty(report.opt("uncertainties"))).append("\n\n")
            append("مدل تحلیل: ").append(report.optString("analysis_model", "—")).append("\n")
            append("ثبت در حافظه یادگیری: ").append(if (result.optBoolean("learning_persisted", false)) "انجام شد ✓" else "تأیید نشد")
        }
    }

    private fun pretty(value: Any?): String = when (value) { null -> "—"; is JSONObject -> value.toString(2); else -> value.toString() }
    private fun showError(title: String, detail: String = "") { status.text = title; status.setTextColor(danger); detailText.text = if (detail.isBlank()) "جزئیات امنی برای نمایش نیست." else "کد: $detail"; retry.visibility = View.VISIBLE }
    private fun resetProgress() { if (::overallBar.isInitialized) overallBar.progress = 0; if (::overallText.isInitialized) overallText.text = "پیشرفت کل: ۰٪"; if (::detailText.isInitialized) detailText.text = "در انتظار اولین پاسخ واقعی سرور…"; stageViews.keys.forEach { setStage(it, 0, false) } }
    private fun elapsedText(ms: Long): String = if (ms > 0L) " • ${formatSeconds(ms)}" else ""
    private fun formatSeconds(ms: Long): String = "${fa((ms / 1000L).toInt())} ثانیه"
    private fun bytes(value: Long): String { val mb = value / (1024.0 * 1024.0); return if (mb >= 1.0) String.format(java.util.Locale.US, "%.1f MB", mb) else "${value / 1024} KB" }
    private fun fa(value: Int): String = value.toString().map { when (it) {'0'->'۰';'1'->'۱';'2'->'۲';'3'->'۳';'4'->'۴';'5'->'۵';'6'->'۶';'7'->'۷';'8'->'۸';'9'->'۹';else->it} }.joinToString("")
    private fun card() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(18), dp(16), dp(18), dp(16)); background = rounded(panel, 20, border) }
    private fun label(text: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply { this.text = text; textSize = size; setTextColor(color); typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT; layoutDirection = View.LAYOUT_DIRECTION_RTL; textDirection = View.TEXT_DIRECTION_RTL }
    private fun rounded(color: Int, radiusDp: Int, stroke: Int? = null) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radiusDp).toFloat(); stroke?.let { setStroke(dp(1), it) } }
    private fun lp(bottom: Int) = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = bottom }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
