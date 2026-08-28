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
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject

class ViralShareActivity : Activity() {
    companion object {
        internal fun safeServerErrorBody(body: String): String {
            if (body.isBlank()) return "جزئیات امنی برای نمایش نیست."
            val root = runCatching { JSONObject(body) }.getOrNull() ?: return "جزئیات امنی برای نمایش نیست."
            val code = root.optString("error_code").trim()
            val safe = code.takeIf { it.length in 1..160 && it.all { ch -> ch.isLetterOrDigit() || ch in "_-.:," } }
            return safe?.let { "کد خطا: $it" } ?: "جزئیات امنی برای نمایش نیست."
        }
    }

    private val bg = Color.rgb(6, 9, 14); private val panel = Color.rgb(14, 20, 28); private val panel2 = Color.rgb(18, 27, 37); private val border = Color.rgb(38, 52, 68); private val accent = Color.rgb(0, 228, 208); private val muted = Color.rgb(173, 185, 199); private val danger = Color.rgb(255, 102, 118)
    private lateinit var status: TextView; private lateinit var stages: TextView; private lateinit var percent: TextView; private lateinit var mediaBar: ProgressBar; private lateinit var urlView: TextView; private lateinit var reportView: TextView; private lateinit var retry: Button
    private var reelUrl = ""; @Volatile private var generation = 0

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); ensureEndpoint(); setContentView(buildUi()); consume(intent) }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); consume(intent) }
    override fun onDestroy() { generation++; super.onDestroy() }

    private fun consume(intent: Intent) {
        generation++
        val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        val found = Regex("https?://(?:www\\.)?instagram\\.com/(?:reel|reels|p)/[A-Za-z0-9_-]+(?:/[^\\s]*)?").find(text)?.value.orEmpty()
        if (found.isNotBlank()) reelUrl = found
        reportView.text = ""; retry.visibility = View.GONE; resetProgress()
        if (reelUrl.isBlank()) { status.text = "لینک معتبر Instagram پیدا نشد"; status.setTextColor(danger); stages.text = "✕ دریافت لینک\n○ دریافت رسانه روی سرور\n○ Whisper + تحلیل تصویر\n○ تحلیل رفتاری\n○ حافظه یادگیری"; urlView.text = "از Instagram روی Share بزن و «تیک‌آف» را انتخاب کن."; return }
        urlView.text = reelUrl; status.text = "لینک دریافت شد؛ سرور آماده می‌شود…"; status.setTextColor(Color.WHITE); stages.text = "✓ دریافت لینک\n○ دریافت رسانه روی سرور\n○ Whisper + تحلیل تصویر\n○ تحلیل رفتاری\n○ حافظه یادگیری"; startAnalysis()
    }

    private fun ensureEndpoint() { val prefs = getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE); prefs.edit().putString("endpoint", PayloadClient.viralEndpoint(prefs.getString("endpoint", "").orEmpty())).apply() }

    private fun buildUi(): View {
        val scroll = ScrollView(this).apply { setBackgroundColor(bg); isFillViewport = true }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(18), dp(22), dp(18), dp(30)) }
        root.addView(LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(20), dp(18), dp(20), dp(18)); background = rounded(Color.rgb(6, 29, 31), 24, Color.rgb(0, 92, 86)); addView(label("TAKEOFF • SERVER VIRAL AUTOPSY", 11f, accent, true)); addView(label("کالبدشکافی سریع ریلز", 27f, Color.WHITE, true).apply { setPadding(0, dp(5), 0, 0) }); addView(label("v${BuildConfig.VERSION_NAME} • بدون کلید • گوشی فقط لینک را می‌فرستد", 12f, muted, false).apply { setPadding(0, dp(7), 0, 0) }) }, lp(dp(14)))
        root.addView(card().apply { addView(label("ریلز ورودی", 12f, accent, true)); urlView = label("در انتظار لینک…", 13f, Color.WHITE, false).apply { setTextIsSelectable(true); setPadding(0, dp(8), 0, 0) }; addView(urlView) }, lp(dp(12)))
        root.addView(card().apply {
            addView(label("موتور سرور", 12f, accent, true)); status = label("آماده…", 17f, Color.WHITE, true).apply { setPadding(0, dp(8), 0, dp(10)) }; addView(status)
            percent = label("", 13f, accent, true).apply { visibility = View.GONE; setPadding(0, 0, 0, dp(5)) }; addView(percent)
            mediaBar = ProgressBar(this@ViralShareActivity, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100; progress = 0; isIndeterminate = false; visibility = View.GONE }; addView(mediaBar, LinearLayout.LayoutParams(-1, dp(9)).apply { bottomMargin = dp(10) })
            stages = label("○ دریافت لینک\n○ دریافت رسانه روی سرور\n○ Whisper + تحلیل تصویر\n○ تحلیل رفتاری\n○ حافظه یادگیری", 13f, muted, false).apply { setLineSpacing(dp(3).toFloat(), 1.12f); background = rounded(panel2, 16); setPadding(dp(14), dp(12), dp(14), dp(12)) }; addView(stages)
            retry = Button(this@ViralShareActivity).apply { text = "تلاش مجدد روی سرور"; isAllCaps = false; visibility = View.GONE; setTextColor(Color.WHITE); background = rounded(Color.rgb(29, 41, 54), 14, Color.rgb(53, 69, 86)); setOnClickListener { startAnalysis() } }; addView(retry, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(9) })
        }, lp(dp(12)))
        root.addView(card().apply { addView(label("گزارش هوشمند", 12f, accent, true)); addView(label("ویدیو، صدا، دیالوگ، Hook، داستان، تدوین، Retention و الگوها روی سرور تحلیل می‌شوند.", 12f, muted, false).apply { setPadding(0, dp(6), 0, dp(12)) }); reportView = label("", 13.5f, Color.WHITE, false).apply { setLineSpacing(0f, 1.22f); setTextIsSelectable(true) }; addView(reportView) })
        root.addView(Button(this).apply { text = "بستن"; isAllCaps = false; setTextColor(Color.WHITE); background = rounded(panel2, 14, border); setOnClickListener { finish() } }, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(14) }); scroll.addView(root); return scroll
    }

    private fun startAnalysis() {
        if (reelUrl.isBlank()) return
        val myGeneration = ++generation; resetProgress(); retry.visibility = View.GONE; reportView.text = ""; status.text = "در حال ایجاد پردازش امن و بدون کلید…"; status.setTextColor(Color.WHITE); stages.text = "✓ دریافت لینک\n● آماده‌سازی سرور\n○ Whisper + تحلیل تصویر\n○ تحلیل رفتاری\n○ حافظه یادگیری"
        val prefs = getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE); val endpoint = PayloadClient.viralEndpoint(prefs.getString("endpoint", "").orEmpty()); val niche = prefs.getString("viral_analysis_niche", "عمومی").orEmpty().ifBlank { "عمومی" }
        Thread {
            val started = runCatching { ViralJobClient.start(endpoint, reelUrl, niche) }.getOrNull(); if (generation != myGeneration) return@Thread
            if (started == null) { runOnUiThread { showError("ارتباط اولیه با سرور برقرار نشد", "اینترنت را بررسی کن و دوباره تلاش کن.") }; return@Thread }
            if (started.first !in 200..299) { val detail = if (started.first == 401) "این مسیر نباید کلید بخواهد. هیچ کدی وارد نکن؛ سرور باید به نسخه بدون کلید ارتقا پیدا کند." else "HTTP ${started.first} • ${safeServerErrorBody(started.second)}"; runOnUiThread { showError("شروع پردازش سرور ناموفق بود", detail) }; return@Thread }
            val root = runCatching { JSONObject(started.second) }.getOrNull(); val jobId = root?.optString("job_id").orEmpty(); val token = root?.optString("poll_token").orEmpty()
            if (jobId.isBlank() || token.isBlank()) { runOnUiThread { showError("پاسخ شروع پردازش معتبر نبود") }; return@Thread }
            poll(endpoint, jobId, token, myGeneration)
        }.start()
    }

    private fun poll(endpoint: String, jobId: String, token: String, myGeneration: Int) {
        var failures = 0; val deadline = System.currentTimeMillis() + 12 * 60_000L
        while (generation == myGeneration && System.currentTimeMillis() < deadline) {
            val pair = runCatching { ViralJobClient.poll(endpoint, jobId, token) }.getOrNull()
            if (pair == null) { failures++; if (failures >= 8) { runOnUiThread { if (generation == myGeneration) showError("ارتباط با وضعیت پردازش قطع شد", "پردازش هیچ کلیدی نمی‌خواهد. اینترنت را بررسی کن و دوباره تلاش کن.") }; return }; Thread.sleep(1000); continue }
            failures = 0
            if (pair.first !in 200..299) { runOnUiThread { if (generation == myGeneration) showError("وضعیت پردازش قابل دریافت نیست", "HTTP ${pair.first} • ${safeServerErrorBody(pair.second)}") }; return }
            val root = runCatching { JSONObject(pair.second) }.getOrNull()
            if (root == null) { Thread.sleep(800); continue }
            runOnUiThread { if (generation == myGeneration) renderProgress(root) }
            when (root.optString("status")) {
                "completed" -> { val result = root.optJSONObject("result") ?: JSONObject(); runOnUiThread { if (generation == myGeneration) showCompleted(result) }; return }
                "partial" -> { runOnUiThread { if (generation == myGeneration) showError("سرور فایل اصلی ویدیو را پیدا نکرد", root.optString("error_code", "server_side_media_unavailable")) }; return }
                "failed" -> { runOnUiThread { if (generation == myGeneration) showError("دریافت یا تحلیل سرور ناموفق بود", root.optString("error_code", "server_processing_failed")) }; return }
            }
            Thread.sleep(800)
        }
        if (generation == myGeneration) runOnUiThread { showError("زمان پردازش بیش از حد طول کشید", "دوباره تلاش کن؛ هیچ کد اتصال لازم نیست.") }
    }

    private fun renderProgress(root: JSONObject) {
        val stage = root.optString("stage", "queued"); val pct = if (root.has("media_progress_percent") && !root.isNull("media_progress_percent")) root.optInt("media_progress_percent", -1) else -1; val downloaded = root.optLong("media_downloaded_bytes", 0L); val total = if (root.has("media_total_bytes") && !root.isNull("media_total_bytes")) root.optLong("media_total_bytes", 0L) else 0L
        if (stage in setOf("media", "preprocess", "analysis", "behavior", "learning", "completed") || pct >= 0) { mediaBar.visibility = View.VISIBLE; percent.visibility = View.VISIBLE; if (pct in 1..100) { mediaBar.isIndeterminate = false; mediaBar.progress = pct; percent.text = if (total > 0) "دریافت واقعی رسانه: $pct٪ • ${bytes(downloaded)} از ${bytes(total)}" else "دریافت واقعی رسانه: $pct٪ • ${bytes(downloaded)}" } else { mediaBar.isIndeterminate = true; percent.text = if (downloaded > 0) "دریافت واقعی رسانه: ${bytes(downloaded)} • حجم کل از CDN اعلام نشده" else "در حال تعیین حجم واقعی رسانه…" } }
        when (stage) {
            "queued", "resolving" -> { status.text = "سرور در حال پیدا کردن فایل اصلی ویدیو است…"; stages.text = "✓ دریافت لینک\n● آماده‌سازی دریافت رسانه\n○ Whisper + تحلیل تصویر\n○ تحلیل رفتاری\n○ حافظه یادگیری" }
            "media" -> { status.text = if (pct in 1..99) "سرور در حال دریافت مستقیم ویدیو است — $pct٪" else "سرور در حال دریافت مستقیم ویدیو است…"; stages.text = "✓ دریافت لینک\n● دریافت رسانه روی سرور${if (pct in 1..99) " — $pct٪" else ""}\n○ Whisper + تحلیل تصویر\n○ تحلیل رفتاری\n○ حافظه یادگیری" }
            "preprocess" -> { status.text = "رسانه کامل دریافت شد؛ آماده‌سازی صدا و فریم‌ها…"; stages.text = "✓ دریافت لینک\n✓ دریافت رسانه روی سرور — 100٪\n● آماده‌سازی Whisper + تصویر\n○ تحلیل رفتاری\n○ حافظه یادگیری" }
            "analysis" -> { status.text = "Whisper و تحلیل تصویر/صدا روی سرور در حال اجراست…"; stages.text = "✓ دریافت لینک\n✓ دریافت رسانه روی سرور — 100٪\n● Whisper + تحلیل تصویر\n○ تحلیل رفتاری\n○ حافظه یادگیری" }
            "behavior" -> { status.text = "تحلیل رفتاری در حال نهایی‌شدن است…"; stages.text = "✓ دریافت لینک\n✓ دریافت رسانه روی سرور — 100٪\n✓ Whisper + تحلیل تصویر\n● تحلیل رفتاری\n○ حافظه یادگیری" }
            "learning" -> { status.text = "نتیجه در حافظه یادگیری ثبت می‌شود…"; stages.text = "✓ دریافت لینک\n✓ دریافت رسانه روی سرور — 100٪\n✓ Whisper + تحلیل تصویر\n✓ تحلیل رفتاری\n● حافظه یادگیری" }
        }; status.setTextColor(Color.WHITE)
    }

    private fun showCompleted(root: JSONObject) { mediaBar.visibility = View.VISIBLE; mediaBar.isIndeterminate = false; mediaBar.progress = 100; percent.visibility = View.VISIBLE; percent.text = "دریافت واقعی رسانه: 100٪ ✓"; stages.text = "✓ دریافت لینک\n✓ دریافت رسانه روی سرور — 100٪\n✓ Whisper + تحلیل تصویر\n✓ تحلیل رفتاری\n✓ حافظه یادگیری"; status.setTextColor(accent); status.text = if (root.optBoolean("deduplicated", false)) "گزارش قبلی معتبر باز شد" else "تحلیل سروری کامل شد و وارد حافظه یادگیری شد"; reportView.text = formatReport(root.optJSONObject("report")); retry.visibility = View.GONE; TakeoffSound.play(TakeoffSound.Cue.SUCCESS) }
    private fun showError(message: String, detail: String = "") { status.text = message; status.setTextColor(danger); if (detail.isNotBlank()) reportView.text = detail; retry.visibility = View.VISIBLE; TakeoffSound.play(TakeoffSound.Cue.WARNING) }
    private fun resetProgress() { if (!::mediaBar.isInitialized) return; mediaBar.progress = 0; mediaBar.isIndeterminate = false; mediaBar.visibility = View.GONE; percent.text = ""; percent.visibility = View.GONE }

    private fun formatReport(report: JSONObject?): String { if (report == null) return "گزارش تحلیلی موجود نیست."; val out = StringBuilder(); fun section(title: String, value: Any?) { val text = render(value); if (text.isNotBlank() && text != "null") out.append("\n$title\n").append(text).append("\n") }; section("خلاصه", report.opt("summary")); section("قلاب", report.opt("hook")); section("تایم‌لاین", report.opt("timeline")); section("دیالوگ / Whisper", report.opt("dialogue")); section("صدا و موسیقی", report.opt("audio_music")); section("گرامر بصری", report.opt("visual_grammar")); section("دوربین و تدوین", report.opt("camera_and_editing")); section("ریتم", report.opt("pacing")); section("ساختار داستان", report.opt("story_structure")); section("Open Loop", report.opt("curiosity_and_open_loops")); section("Pattern Interrupt", report.opt("pattern_interrupts")); section("Payoff", report.opt("payoff")); section("فرضیه‌های ریتنشن", report.opt("retention_hypotheses")); section("الگوهای قابل استفاده", report.opt("reusable_patterns")); section("کیفیت شواهد", report.opt("evidence_quality")); section("آمار عمومی", report.opt("public_metrics")); section("روش", report.opt("method_note")); return out.toString().trim().ifBlank { "گزارش خالی است." } }
    private fun render(value: Any?): String = when (value) { null, JSONObject.NULL -> ""; is String -> value; is JSONArray -> buildString { for (i in 0 until value.length()) append("• ").append(render(value.opt(i))).append('\n') }.trim(); is JSONObject -> buildString { val keys = value.keys(); while (keys.hasNext()) { val key = keys.next(); append("• ").append(key).append(": ").append(render(value.opt(key))).append('\n') } }.trim(); else -> value.toString() }
    private fun bytes(value: Long): String = when { value >= 1024L * 1024L -> String.format(java.util.Locale.US, "%.1f MB", value / (1024.0 * 1024.0)); value >= 1024L -> String.format(java.util.Locale.US, "%.0f KB", value / 1024.0); else -> "$value B" }
    private fun card() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(18), dp(16), dp(18), dp(16)); background = rounded(panel, 20, border) }
    private fun label(textValue: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply { text = textValue; textSize = size; setTextColor(color); if (bold) typeface = Typeface.DEFAULT_BOLD }
    private fun rounded(fill: Int, radius: Int, stroke: Int? = null) = GradientDrawable().apply { shape = GradientDrawable.RECTANGLE; setColor(fill); cornerRadius = dp(radius).toFloat(); if (stroke != null) setStroke(dp(1), stroke) }
    private fun lp(bottom: Int) = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = bottom }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
