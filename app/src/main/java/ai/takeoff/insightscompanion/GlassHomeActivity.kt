package ai.takeoff.insightscompanion

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject

class GlassHomeActivity : Activity() {
    private val bg0 = Color.rgb(3, 6, 12)
    private val bg1 = Color.rgb(9, 23, 34)
    private val glass = Color.argb(166, 18, 29, 42)
    private val glassStrong = Color.argb(210, 13, 25, 37)
    private val border = Color.argb(110, 102, 229, 217)
    private val accent = Color.rgb(55, 239, 219)
    private val muted = Color.rgb(178, 191, 207)
    private val warning = Color.rgb(255, 196, 92)
    private val success = Color.rgb(91, 236, 171)

    private lateinit var queueSummary: TextView
    private lateinit var evidenceSummary: TextView
    private lateinit var healthSummary: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = bg0
        setContentView(buildUi())
        refreshLocal()
        refreshServer()
    }

    override fun onResume() {
        super.onResume()
        refreshLocal()
    }

    private fun buildUi(): android.view.View {
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(bg0, bg1, Color.rgb(5, 10, 20)))
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
            setPadding(dp(16), dp(22), dp(16), dp(30))
        }
        root.addView(card(true).apply {
            addView(text("TAKEOFF • INSIGHTS V4", 11f, accent, true))
            addView(text("مرکز فرمان یادگیری", 29f, Color.WHITE, true).apply { setPadding(0, dp(5), 0, 0) })
            addView(text("سناریو ← اجرا ← انتشار ← Outcome ← یادگیری", 12.5f, muted, false).apply { setPadding(0, dp(6), 0, 0) })
        }, margin(dp(12)))

        root.addView(card().apply {
            addView(section("صف یادگیری چندرسانه‌ای"))
            queueSummary = text("در حال خواندن…", 18f, Color.WHITE, true)
            addView(queueSummary)
            addView(text("ریلز، پست ویدیویی، عکس و Carousel را پشت‌سرهم Share کن. پردازش‌های قبلی متوقف نمی‌شوند.", 12.5f, muted, false).apply { setPadding(0, dp(7), 0, dp(10)) })
            addView(primary("باز کردن صف یادگیری") {
                startActivity(Intent(this@GlassHomeActivity, ViralShareActivity::class.java))
            })
        }, margin(dp(12)))

        root.addView(card().apply {
            addView(section("مغز V4"))
            evidenceSummary = text("در حال خواندن Evidence امن دستگاه…", 13f, muted, false)
            addView(evidenceSummary)
            addView(secondary("تازه‌سازی") { refreshLocal(); refreshServer() })
        }, margin(dp(12)))

        root.addView(card().apply {
            addView(section("Owner Insights و Outcome واقعی"))
            val account = runCatching { ManagedAccountStore(this@GlassHomeActivity).selected()?.normalizedHandle }.getOrNull().orEmpty()
            val pending = PayloadQueue(this@GlassHomeActivity).size()
            addView(text("پیج فعال: @${account.ifBlank { "—" }}\nثبت‌های Owner در صف امن: $pending", 13f, Color.WHITE, true))
            addView(text("Retention، Share/Save، Watch Time و اجرای سناریو از این مسیر به Outcome Loop می‌رسند.", 12f, muted, false).apply { setPadding(0, dp(7), 0, dp(10)) })
            addView(secondary("ورود به کنسول ثبت آمار") {
                startActivity(Intent(this@GlassHomeActivity, MainActivity::class.java))
            })
        }, margin(dp(12)))

        root.addView(card().apply {
            addView(section("سلامت سیستم"))
            healthSummary = text("در حال بررسی…", 12.5f, muted, false)
            addView(healthSummary)
            addView(secondary("بررسی دوباره") { refreshServer() })
        }, margin(dp(12)))

        root.addView(card().apply {
            addView(section("روش استفاده"))
            addView(text("۱) داخل Instagram هر Reel یا Post را Share کن.\n۲) تیک‌آف را انتخاب کن.\n۳) فوراً برگرد و مورد بعدی را Share کن.\n۴) هر تعداد مورد، ردیفی در صف باقی می‌ماند و سه Lane همزمان تحلیل می‌شوند.\n۵) فقط Evidence تأییدشده به حافظه فعال V4 ارتقا پیدا می‌کند.", 13f, Color.WHITE, false))
        }, margin(0))

        scroll.addView(root)
        return scroll
    }

    private fun refreshLocal() {
        if (!::queueSummary.isInitialized) return
        val items = SharedMediaQueue(this).all()
        val active = items.count { it.status in setOf("queued", "submitting", "processing", "failed") }
        val done = items.count { it.status == "completed" }
        var promoted = 0; var reel = 0; var carousel = 0; var photo = 0
        items.filter { it.status == "completed" }.forEach { item ->
            when (item.mediaKind) {
                "reel", "video_post" -> reel++
                "carousel", "mixed" -> carousel++
                "photo" -> photo++
            }
            val root = item.resultJson?.let { runCatching { JSONObject(it) }.getOrNull() }
            if (root?.optString("promotion_status") == "PROMOTE") promoted++
        }
        queueSummary.text = "فعال $active  •  تکمیل $done  •  ارتقا به V4 $promoted"
        queueSummary.setTextColor(if (active > 0) warning else success)
        if (::evidenceSummary.isInitialized) {
            evidenceSummary.text = "Evidence تکمیل‌شده روی این دستگاه: $done\nویدیو $reel • اسلایدی $carousel • عکس $photo • Promoted $promoted"
            evidenceSummary.setTextColor(if (done > 0) Color.WHITE else muted)
        }
    }

    private fun refreshServer() {
        val prefs = getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE)
        val endpoint = PayloadClient.viralEndpoint(prefs.getString("endpoint", "").orEmpty())
        healthSummary.text = "در حال بررسی Backend V4…"
        Thread {
            val health = runCatching {
                val url = java.net.URL(endpoint.trimEnd('/') + "/v4/media-jobs/health")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 15_000; conn.readTimeout = 20_000; conn.requestMethod = "GET"
                try {
                    val code = conn.responseCode
                    val body = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
                    code to body
                } finally { conn.disconnect() }
            }.getOrNull()
            runOnUiThread {
                val h = health?.second?.let { runCatching { JSONObject(it) }.getOrNull() }
                val ok = health?.first in 200..299 && h?.optBoolean("ok", false) == true
                healthSummary.text = if (ok) {
                    "● V4 آماده • Queue پایدار • Gemini ${if (h?.optBoolean("gemini") == true) "فعال" else "غیرفعال"} • Apify ${if (h?.optBoolean("apify") == true) "فعال" else "غیرفعال"}"
                } else "● Backend V4 هنوز آماده نیست یا اتصال برقرار نشد"
                healthSummary.setTextColor(if (ok) success else warning)
            }
        }.start()
    }

    private fun card(strong: Boolean = false) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
        setPadding(dp(16), dp(15), dp(16), dp(15))
        background = rounded(if (strong) glassStrong else glass, 22, border)
        elevation = dp(4).toFloat()
    }

    private fun section(value: String) = text(value, 12f, accent, true).apply { setPadding(0, 0, 0, dp(8)) }
    private fun text(value: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color); gravity = Gravity.START
        if (bold) typeface = Typeface.DEFAULT_BOLD
        setLineSpacing(0f, 1.18f)
    }
    private fun primary(value: String, click: () -> Unit) = button(value, true, click)
    private fun secondary(value: String, click: () -> Unit) = button(value, false, click)
    private fun button(value: String, primary: Boolean, click: () -> Unit) = Button(this).apply {
        text = value; isAllCaps = false; textSize = 13.5f; typeface = Typeface.DEFAULT_BOLD
        setTextColor(if (primary) Color.rgb(4, 22, 23) else Color.WHITE)
        background = rounded(if (primary) accent else Color.argb(95, 36, 53, 70), 15, if (primary) accent else border)
        setOnClickListener { click() }
    }
    private fun rounded(fill: Int, radius: Int, stroke: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE; setColor(fill); cornerRadius = dp(radius).toFloat(); setStroke(dp(1), stroke)
    }
    private fun margin(bottom: Int) = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = bottom }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
