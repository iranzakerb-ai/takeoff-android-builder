package ai.takeoff.insightscompanion

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject

class ViralShareActivity : Activity() {
    companion object {
        internal fun safeServerErrorBody(body: String): String {
            if (body.isBlank()) return "جزئیات امنی برای نمایش نیست."
            val root = runCatching { JSONObject(body) }.getOrNull() ?: return "جزئیات امنی برای نمایش نیست."
            val errorCode = root.optString("error_code").trim()
            val safe = errorCode.takeIf { value ->
                value.length in 1..96 && value.all { ch -> ch.isLetterOrDigit() || ch in "_-.:" }
            }
            return safe?.let { "کد خطا: $it" } ?: "جزئیات امنی برای نمایش نیست."
        }
    }

    private val bg = Color.rgb(6, 9, 14)
    private val panel = Color.rgb(14, 20, 28)
    private val panel2 = Color.rgb(18, 27, 37)
    private val accent = Color.rgb(0, 228, 208)
    private val muted = Color.rgb(173, 185, 199)
    private val danger = Color.rgb(255, 102, 118)

    private lateinit var status: TextView
    private lateinit var progressView: TextView
    private lateinit var urlView: TextView
    private lateinit var reportView: TextView
    private lateinit var retryButton: Button
    private var reelUrl = ""
    private var legacyRepairRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureProductionEndpoint()
        setContentView(buildUi())
        consumeSharedReel(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeSharedReel(intent)
    }

    private fun extractReelUrl(text: String): String =
        Regex("https?://(?:www\\.)?instagram\\.com/(?:reel|reels|p)/[A-Za-z0-9_-]+(?:/[^\\s]*)?")
            .find(text)?.value.orEmpty()

    private fun consumeSharedReel(source: Intent) {
        reelUrl = extractReelUrl(source.getStringExtra(Intent.EXTRA_TEXT).orEmpty()).ifBlank { reelUrl }
        reportView.text = ""
        retryButton.visibility = View.GONE
        legacyRepairRunning = false
        if (reelUrl.isBlank()) {
            status.text = "لینک معتبر Instagram پیدا نشد"
            status.setTextColor(danger)
            progressView.text = "✕ دریافت لینک\n○ دریافت رسانه روی سرور\n○ Whisper + تحلیل تصویر\n○ تحلیل رفتاری\n○ حافظه یادگیری"
            urlView.text = "از داخل Instagram روی Share بزن و «تیک‌آف» را انتخاب کن."
            return
        }
        urlView.text = reelUrl
        setProgress(0, "لینک دریافت شد؛ تحلیل سریع روی سرور شروع می‌شود…")
        analyzeAutomatically()
    }

    private fun ensureProductionEndpoint() {
        val prefs = getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE)
        val resolved = PayloadClient.viralEndpoint(prefs.getString("endpoint", "").orEmpty())
        prefs.edit().putString("endpoint", resolved).apply()
    }

    private fun rounded(color: Int, radius: Int, stroke: Int? = null) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setPadding(dp(18), dp(16), dp(18), dp(16))
        background = rounded(panel, 20, Color.rgb(38, 52, 68))
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
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(20), dp(18), dp(20), dp(18))
            background = rounded(Color.rgb(6, 29, 31), 24, Color.rgb(0, 92, 86))
            addView(TextView(this@ViralShareActivity).apply {
                text = "TAKEOFF • SERVER VIRAL AUTOPSY"
                textSize = 11f; setTextColor(accent); typeface = Typeface.DEFAULT_BOLD
            })
            addView(TextView(this@ViralShareActivity).apply {
                text = "کالبدشکافی سریع ریلز"
                textSize = 27f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD
                setPadding(0, dp(5), 0, 0)
            })
            addView(TextView(this@ViralShareActivity).apply {
                text = "v${BuildConfig.VERSION_NAME} • گوشی فقط لینک را می‌فرستد"
                textSize = 12f; setTextColor(muted); setPadding(0, dp(7), 0, 0)
            })
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })

        root.addView(card().apply {
            addView(TextView(this@ViralShareActivity).apply {
                text = "ریلز ورودی"; textSize = 12f; setTextColor(accent); typeface = Typeface.DEFAULT_BOLD
            })
            urlView = TextView(this@ViralShareActivity).apply {
                text = "در انتظار لینک…"; textSize = 13f; setTextColor(Color.WHITE); setTextIsSelectable(true); setPadding(0, dp(8), 0, 0)
            }
            addView(urlView)
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })

        root.addView(card().apply {
            addView(TextView(this@ViralShareActivity).apply {
                text = "موتور سرور"; textSize = 12f; setTextColor(accent); typeface = Typeface.DEFAULT_BOLD
            })
            status = TextView(this@ViralShareActivity).apply {
                text = "آماده…"; textSize = 17f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(8), 0, dp(12))
            }
            addView(status)
            progressView = TextView(this@ViralShareActivity).apply {
                text = "○ دریافت لینک\n○ دریافت رسانه روی سرور\n○ Whisper + تحلیل تصویر\n○ تحلیل رفتاری\n○ حافظه یادگیری"
                textSize = 13f; setTextColor(muted); setLineSpacing(dp(3).toFloat(), 1.12f)
                background = rounded(panel2, 16); setPadding(dp(14), dp(12), dp(14), dp(12))
            }
            addView(progressView)
            retryButton = Button(this@ViralShareActivity).apply {
                text = "تلاش مجدد روی سرور"; isAllCaps = false; visibility = View.GONE; setTextColor(Color.WHITE)
                background = rounded(Color.rgb(29, 41, 54), 14, Color.rgb(53, 69, 86)); setOnClickListener { analyzeAutomatically() }
            }
            addView(retryButton, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(9) })
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })

        root.addView(card().apply {
            addView(TextView(this@ViralShareActivity).apply {
                text = "گزارش هوشمند"; textSize = 12f; setTextColor(accent); typeface = Typeface.DEFAULT_BOLD
            })
            addView(TextView(this@ViralShareActivity).apply {
                text = "ویدیو، صدا، دیالوگ، Hook، داستان، تدوین، Retention و الگوها همگی روی سرور تحلیل می‌شوند."
                textSize = 12f; setTextColor(muted); setPadding(0, dp(6), 0, dp(12))
            })
            reportView = TextView(this@ViralShareActivity).apply {
                textSize = 13.5f; setTextColor(Color.WHITE); setLineSpacing(0f, 1.22f); setTextIsSelectable(true)
            }
            addView(reportView)
        })
        root.addView(Button(this).apply {
            text = "بستن"; isAllCaps = false; setTextColor(Color.WHITE); background = rounded(panel2, 14, Color.rgb(44, 57, 72)); setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(14) })
        scroll.addView(root)
        return scroll
    }

    private fun setProgress(stage: Int, message: String) {
        status.text = message
        status.setTextColor(if (stage >= 5) accent else Color.WHITE)
        val labels = listOf("دریافت لینک", "دریافت رسانه روی سرور", "Whisper + تحلیل تصویر", "تحلیل رفتاری", "حافظه یادگیری")
        progressView.text = labels.mapIndexed { index, label ->
            when { index < stage -> "✓ $label"; index == stage && stage < labels.size -> "● $label"; else -> "○ $label" }
        }.joinToString("\n")
    }

    private fun showError(message: String, detail: String = "") {
        legacyRepairRunning = false
        status.text = message; status.setTextColor(danger)
        if (detail.isNotBlank()) reportView.text = detail
        retryButton.visibility = View.VISIBLE
        TakeoffSound.play(TakeoffSound.Cue.WARNING)
    }

    private fun analyzeAutomatically() {
        if (reelUrl.isBlank() || legacyRepairRunning) return
        val prefs = getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE)
        val endpoint = PayloadClient.viralEndpoint(prefs.getString("endpoint", "").orEmpty())
        val niche = prefs.getString("viral_analysis_niche", "عمومی").orEmpty().ifBlank { "عمومی" }
        runAnalysis(endpoint, niche)
    }

    private fun runAnalysis(endpoint: String, niche: String) {
        retryButton.visibility = View.GONE
        setProgress(1, "سرور در حال دریافت مستقیم ویدیو است…")
        Thread {
            val result = runCatching { PayloadClient.postViralEvidence(endpoint, "", reelUrl, niche) }
            runOnUiThread {
                val pair = result.getOrNull()
                if (pair == null) {
                    showError("ارتباط با سرور قطع شد", "اینترنت/VPN را بررسی کن.")
                    return@runOnUiThread
                }
                if (LegacyCompanionRepair.shouldRepair(pair.first)) {
                    repairLegacy401(endpoint, niche)
                    return@runOnUiThread
                }
                handleAnalysisResponse(pair)
            }
        }.start()
    }

    private fun repairLegacy401(endpoint: String, niche: String) {
        if (legacyRepairRunning) return
        legacyRepairRunning = true
        retryButton.visibility = View.GONE
        status.text = "سرور قدیمی شناسایی شد؛ اتصال امن در حال بازیابی است…"
        status.setTextColor(Color.WHITE)
        reportView.text = "HTTP 401 به‌صورت خودکار مدیریت شد؛ در صورت نیاز فقط یک‌بار کد اتصال امن درخواست می‌شود."
        LegacyCompanionRepair(this).recover(
            endpoint = endpoint,
            reelUrl = reelUrl,
            niche = niche,
            onFinished = { pair ->
                legacyRepairRunning = false
                handleAnalysisResponse(pair)
            },
            onFailure = { reason ->
                showError("بازیابی اتصال کامل نشد", reason)
            },
        )
    }

    private fun handleAnalysisResponse(pair: Pair<Int, String>) {
        if (pair.first !in 200..299) {
            showError("تحلیل سرور کامل نشد", "HTTP ${pair.first} • ${safeServerErrorBody(pair.second)}")
            return
        }
        val root = runCatching { JSONObject(pair.second) }.getOrNull() ?: run {
            showError("پاسخ سرور قابل خواندن نبود")
            return
        }
        when (root.optString("status")) {
            "completed" -> showCompleted(root, root.optBoolean("deduplicated", false))
            "partial" -> showError(
                "سرور هنوز فایل ویدیو را دریافت نکرد",
                "هیچ Capture از گوشی انجام نمی‌شود. فقط «تلاش مجدد روی سرور» را بزن؛ مسیر server-side egress دوباره امتحان می‌شود.",
            )
            "failed" -> showError("دریافت/تحلیل سرور ناموفق بود", safeServerErrorBody(pair.second))
            else -> showError("تحلیل کامل نشد", "جزئیات امنی برای نمایش نیست.")
        }
    }

    private fun showCompleted(root: JSONObject, dedupe: Boolean) {
        legacyRepairRunning = false
        progressView.text = "✓ دریافت لینک\n✓ دریافت رسانه روی سرور\n✓ Whisper + تحلیل تصویر\n✓ تحلیل رفتاری\n✓ حافظه یادگیری"
        status.setTextColor(accent)
        status.text = if (dedupe) "گزارش قبلی معتبر باز شد" else "تحلیل سروری کامل شد و وارد حافظه یادگیری شد"
        val mode = root.optString("analysis_mode")
        if (mode.isNotBlank()) status.append("\n$mode")
        reportView.text = formatReport(root.optJSONObject("report"))
        retryButton.visibility = View.GONE
        TakeoffSound.play(TakeoffSound.Cue.SUCCESS)
    }

    private fun formatReport(report: JSONObject?): String {
        if (report == null) return "گزارش تحلیلی موجود نیست."
        val out = StringBuilder()
        fun section(title: String, value: Any?) { val text = render(value); if (text.isNotBlank() && text != "null") out.append("\n$title\n").append(text).append("\n") }
        section("خلاصه", report.opt("summary")); section("قلاب", report.opt("hook")); section("تایم‌لاین", report.opt("timeline")); section("دیالوگ / Whisper", report.opt("dialogue"))
        section("صدا و موسیقی", report.opt("audio_music")); section("گرامر بصری", report.opt("visual_grammar")); section("دوربین و تدوین", report.opt("camera_and_editing")); section("ریتم", report.opt("pacing"))
        section("ساختار داستان", report.opt("story_structure")); section("Open Loop", report.opt("curiosity_and_open_loops")); section("Pattern Interrupt", report.opt("pattern_interrupts")); section("Payoff", report.opt("payoff"))
        section("فرضیه‌های ریتنشن", report.opt("retention_hypotheses")); section("الگوهای قابل استفاده", report.opt("reusable_patterns")); section("کیفیت شواهد", report.opt("evidence_quality")); section("آمار عمومی", report.opt("public_metrics"))
        section("زمان اجرای سرور (ms)", report.opt("server_pipeline_timings_ms")); section("روش", report.opt("method_note"))
        return out.toString().trim().ifBlank { "گزارش خالی است." }
    }

    private fun render(value: Any?): String = when (value) {
        null, JSONObject.NULL -> ""
        is String -> value
        is JSONArray -> buildString { for (i in 0 until value.length()) append("• ").append(render(value.opt(i))).append('\n') }.trim()
        is JSONObject -> buildString { val keys = value.keys(); while (keys.hasNext()) { val key = keys.next(); append("• ").append(key).append(": ").append(render(value.opt(key))).append('\n') } }.trim()
        else -> value.toString()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
