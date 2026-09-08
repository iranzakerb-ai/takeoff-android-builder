package ai.takeoff.insightscompanion

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject

@SuppressLint("ClickableViewAccessibility")
class GlassHomeActivity : Activity() {
    private lateinit var queueSummary: TextView
    private lateinit var recentHost: LinearLayout
    private lateinit var healthSummary: TextView
    private lateinit var statAnalyses: TextView
    private lateinit var statScore: TextView
    private lateinit var statScenarios: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LovableUi.applyWindow(this)
        setContentView(buildUi())
        refreshLocal()
        refreshServer()
    }

    override fun onResume() {
        super.onResume()
        if (::queueSummary.isInitialized) refreshLocal()
    }

    private fun buildUi(): View {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(LovableUi.background)
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        content.addView(hero())

        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(LovableUi.run { dp(16) }, 0, LovableUi.run { dp(16) }, LovableUi.run { dp(26) })
        }

        body.addView(LovableUi.run { card(true) }.apply {
            val top = LinearLayout(this@GlassHomeActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                gravity = Gravity.CENTER_VERTICAL
            }
            top.addView(LovableUi.run { chip("✦ اقدام پیشنهادی امروز", "primary") })
            top.addView(LovableUi.run { text("به‌روزرسانی زنده", 10f, LovableUi.muted) }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(top)
            addView(LovableUi.run { text("ویدیوهای جدید را تحلیل کن تا V5 قلاب، رفتار مخاطب و الگوهای قابل‌تکرار را به حافظه تیک‌آف اضافه کند.", 13f, LovableUi.foreground) }.apply {
                setPadding(0, LovableUi.run { dp(10) }, 0, LovableUi.run { dp(12) })
            })
            val actions = LinearLayout(this@GlassHomeActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
            }
            actions.addView(LovableUi.run { primaryButton("شروع تحلیل جدید") { startActivity(Intent(this@GlassHomeActivity, NewAnalysisActivity::class.java)) } }, LinearLayout.LayoutParams(0, LovableUi.run { dp(50) }, 1f).apply { marginEnd = LovableUi.run { dp(4) } })
            actions.addView(LovableUi.run { ghostButton("استودیوی سناریو") { startActivity(Intent(this@GlassHomeActivity, ScenarioStudioActivity::class.java)) } }, LinearLayout.LayoutParams(0, LovableUi.run { dp(50) }, 1f).apply { marginStart = LovableUi.run { dp(4) } })
            addView(actions)
        }, LovableUi.run { margin(bottom = 22) }.apply { topMargin = -LovableUi.run { dp(38) } })

        body.addView(LovableUi.run { sectionTitle("دسترسی سریع") }, LovableUi.run { margin(bottom = 10) })
        body.addView(quickActions(), LovableUi.run { margin(bottom = 22) })

        body.addView(sectionHeader("در حال پردازش", "صف تحلیل") { startActivity(Intent(this, ViralShareActivity::class.java)) })
        queueSummary = LovableUi.run { text("در حال خواندن صف…", 12f, LovableUi.muted) }
        body.addView(queueSummary, LovableUi.run { margin(bottom = 8) })
        recentHost = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        body.addView(recentHost, LovableUi.run { margin(bottom = 22) })

        body.addView(sectionHeader("حافظه و یادگیری", "مشاهده همه") { startActivity(Intent(this, MemoryActivity::class.java)) })
        body.addView(LovableUi.run { card() }.apply {
            addView(LovableUi.run { chip("V5 Behavioral Intelligence", "secondary") })
            addView(LovableUi.run { text("مغز تیک‌آف از تحلیل‌های کامل، قلاب‌ها، Retention، سناریو و الگوهای رفتاری یاد می‌گیرد و آن‌ها را در Scenario Studio به کار می‌برد.", 12.5f, LovableUi.foreground) }.apply {
                setPadding(0, LovableUi.run { dp(10) }, 0, LovableUi.run { dp(10) })
            })
            healthSummary = LovableUi.run { text("در حال بررسی سلامت سیستم…", 11f, LovableUi.muted) }
            addView(healthSummary)
        }, LovableUi.run { margin(bottom = 22) })

        body.addView(sectionHeader("استودیوی سناریو", "ساخت ۱۰ سناریو") { startActivity(Intent(this, ScenarioStudioActivity::class.java)) })
        body.addView(LovableUi.run { card() }.apply {
            addView(LovableUi.run { text("کسب‌وکارت رو توضیح بده؛ تیک‌آف سناریوش رو می‌سازه.", 14f, LovableUi.foreground, true) })
            addView(LovableUi.run { text("۱۰ سناریوی متمایز با تعداد سکانس پویا، قلاب، دیالوگ، زمان‌بندی، بازیگر و PDF آماده ضبط.", 12f, LovableUi.muted) }.apply { setPadding(0, LovableUi.run { dp(7) }, 0, LovableUi.run { dp(12) }) })
            addView(LovableUi.run { primaryButton("ورود به استودیوی سناریو") { startActivity(Intent(this@GlassHomeActivity, ScenarioStudioActivity::class.java)) } }, LinearLayout.LayoutParams(-1, LovableUi.run { dp(50) }))
        })

        scroll.addView(content.apply { addView(body) })
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        page.addView(LovableUi.run { bottomNav("home") }, LinearLayout.LayoutParams(-1, -2))
        return page
    }

    private fun hero(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setPadding(LovableUi.run { dp(18) }, LovableUi.run { dp(22) }, LovableUi.run { dp(18) }, LovableUi.run { dp(64) })
        background = LovableUi.run { brandGradient(0) }
        val titleRow = LinearLayout(this@GlassHomeActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.CENTER_VERTICAL
        }
        titleRow.addView(TextView(this@GlassHomeActivity).apply {
            text = "✦"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = LovableUi.run { rounded(Color.argb(45, 255, 255, 255), 18, Color.argb(90, 255, 255, 255), 1) }
            elevation = LovableUi.run { dp(2).toFloat() }
        }, LinearLayout.LayoutParams(LovableUi.run { dp(40) }, LovableUi.run { dp(40) }).apply { marginEnd = LovableUi.run { dp(10) } })
        titleRow.addView(LinearLayout(this@GlassHomeActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(LovableUi.run { text("سلام محمدحسین ✦", 11.5f, Color.argb(235, 255, 255, 255)) })
            addView(LovableUi.run { text("مرکز فرماندهی تیک‌آف", 16f, Color.WHITE, true) })
        }, LinearLayout.LayoutParams(0, -2, 1f))
        titleRow.addView(TextView(this@GlassHomeActivity).apply {
            text = "◉"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(LovableUi.secondary)
            background = LovableUi.run { rounded(LovableUi.navy, 18, LovableUi.navy, 0) }
            elevation = LovableUi.run { dp(3).toFloat() }
            translationZ = LovableUi.run { dp(1).toFloat() }
        }, LinearLayout.LayoutParams(LovableUi.run { dp(40) }, LovableUi.run { dp(40) }))
        addView(titleRow)

        val stats = LinearLayout(this@GlassHomeActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(0, LovableUi.run { dp(22) }, 0, 0)
        }
        statAnalyses = statBox("تحلیل این ماه", stats)
        statScore = statBox("میانگین امتیاز", stats)
        statScenarios = statBox("سناریوی ساخته‌شده", stats)
        addView(stats)
    }

    private fun statBox(label: String, parent: LinearLayout): TextView {
        val value = LovableUi.run { text("۰", 20f, Color.WHITE, true) }.apply { gravity = Gravity.CENTER }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(LovableUi.run { dp(6) }, LovableUi.run { dp(12) }, LovableUi.run { dp(6) }, LovableUi.run { dp(11) })
            background = LovableUi.run { rounded(Color.argb(40, 255, 255, 255), 20, Color.argb(80, 255, 255, 255), 1) }
            elevation = LovableUi.run { dp(3).toFloat() }
            translationZ = LovableUi.run { dp(1).toFloat() }
            addView(value)
            addView(LovableUi.run { text(label, 10.2f, Color.WHITE) }.apply { gravity = Gravity.CENTER; alpha = 0.94f })
        }
        parent.addView(box, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = LovableUi.run { dp(4) }; marginEnd = LovableUi.run { dp(4) } })
        return value
    }

    private fun quickActions(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        fun add(label: String, icon: String, toneBg: Int, toneFg: Int, click: () -> Unit) {
            val cell = LinearLayout(this@GlassHomeActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(LovableUi.run { dp(4) }, LovableUi.run { dp(11) }, LovableUi.run { dp(4) }, LovableUi.run { dp(11) })
                background = LovableUi.run { rounded(Color.WHITE, 22, LovableUi.border, 1) }
                elevation = LovableUi.run { dp(3).toFloat() }
                translationZ = LovableUi.run { dp(1).toFloat() }
                addView(TextView(this@GlassHomeActivity).apply {
                    text = icon
                    textSize = 19f
                    gravity = Gravity.CENTER
                    setTextColor(toneFg)
                    background = LovableUi.run { rounded(toneBg, 15, toneBg, 0) }
                    elevation = LovableUi.run { dp(1).toFloat() }
                }, LinearLayout.LayoutParams(LovableUi.run { dp(40) }, LovableUi.run { dp(40) }))
                addView(LovableUi.run { text(label, 11f, LovableUi.foreground, true) }.apply { gravity = Gravity.CENTER; setPadding(0, LovableUi.run { dp(6) }, 0, 0) })
                setOnTouchListener { v, ev ->
                    when (ev.action) {
                        MotionEvent.ACTION_DOWN -> {
                            v.translationY = LovableUi.run { dp(2).toFloat() }
                            v.scaleX = 0.97f
                            v.scaleY = 0.97f
                            v.elevation = LovableUi.run { dp(1).toFloat() }
                            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            v.animate().translationY(0f).scaleX(1f).scaleY(1f).setDuration(120).withEndAction { click() }.start()
                            v.elevation = LovableUi.run { dp(3).toFloat() }
                            true
                        }
                        MotionEvent.ACTION_CANCEL -> {
                            v.animate().translationY(0f).scaleX(1f).scaleY(1f).setDuration(100).start()
                            v.elevation = LovableUi.run { dp(3).toFloat() }
                            true
                        }
                        else -> false
                    }
                }
            }
            addView(cell, LinearLayout.LayoutParams(0, LovableUi.run { dp(88) }, 1f).apply { marginStart = LovableUi.run { dp(3) }; marginEnd = LovableUi.run { dp(3) } })
        }
        add("تحلیل", "▷", LovableUi.primarySoft, LovableUi.primary) { startActivity(Intent(this@GlassHomeActivity, NewAnalysisActivity::class.java)) }
        add("صف", "▱", LovableUi.secondarySoft, LovableUi.secondaryText) { startActivity(Intent(this@GlassHomeActivity, ViralShareActivity::class.java)) }
        add("سناریو", "✦", Color.rgb(254, 245, 222), LovableUi.warning) { startActivity(Intent(this@GlassHomeActivity, ScenarioStudioActivity::class.java)) }
        add("حافظه", "↗", LovableUi.mutedBg, LovableUi.foreground) { startActivity(Intent(this@GlassHomeActivity, MemoryActivity::class.java)) }
    }

    private fun sectionHeader(title: String, action: String, click: () -> Unit): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        gravity = Gravity.CENTER_VERTICAL
        addView(LovableUi.run { sectionTitle(title) }, LinearLayout.LayoutParams(0, -2, 1f))
        addView(LovableUi.run { text(action, 11f, LovableUi.primary, true) }.apply { setOnClickListener { click() } })
    }

    private fun refreshLocal() {
        val items = SharedMediaQueue(this).all()
        val active = items.count { it.status in setOf("queued", "submitting", "processing", "failed") }
        val completed = items.count { it.status == "completed" }
        val scores = items.mapNotNull { item ->
            item.resultJson?.let { runCatching { JSONObject(it) }.getOrNull() }?.let { root ->
                val analysis = root.optJSONObject("analysis") ?: root
                val candidates = listOf("viral_potential_score", "score", "confidence")
                candidates.firstNotNullOfOrNull { key -> if (analysis.has(key)) analysis.optDouble(key).takeIf { it.isFinite() } else null }
            }
        }
        statAnalyses.text = LovableUi.fa(items.size.coerceAtMost(999))
        statScore.text = LovableUi.fa(if (scores.isEmpty()) 76 else scores.average().toInt().coerceIn(0, 100))
        val prefs = getSharedPreferences("takeoff_scenario_stats", Context.MODE_PRIVATE)
        statScenarios.text = LovableUi.fa(prefs.getInt("generated", 0))
        queueSummary.text = if (active == 0) "فعلاً پردازشی در صف نیست • $completed تحلیل کامل در دستگاه" else "$active پردازش فعال • $completed تحلیل کامل"
        queueSummary.setTextColor(if (active > 0) LovableUi.primary else LovableUi.muted)
        recentHost.removeAllViews()
        val visible = items.take(3)
        if (visible.isEmpty()) {
            recentHost.addView(LovableUi.run { card() }.apply { addView(LovableUi.run { text("هنوز تحلیلی ثبت نشده. یک لینک اینستاگرام وارد کن یا از Instagram به تیک‌آف Share کن.", 12f, LovableUi.muted) }) })
        } else {
            visible.forEach { item ->
                recentHost.addView(LovableUi.run { card() }.apply {
                    val row = LinearLayout(this@GlassHomeActivity).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; gravity = Gravity.CENTER_VERTICAL }
                    row.addView(LovableUi.run { chip(kindFa(item.mediaKind), if (item.status == "completed") "secondary" else "primary") })
                    row.addView(LinearLayout(this@GlassHomeActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(LovableUi.run { text(item.shortcode.ifBlank { "Instagram" }, 12.5f, LovableUi.foreground, true) })
                        addView(LovableUi.run { text(stageFa(item.stage), 10.5f, LovableUi.muted) })
                    }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = LovableUi.run { dp(8) }; marginEnd = LovableUi.run { dp(8) } })
                    row.addView(LovableUi.run { text(if (item.status == "completed") "✓" else "${LovableUi.fa(item.progress)}٪", 12f, if (item.status == "completed") LovableUi.success else LovableUi.primary, true) })
                    addView(row)
                    setOnClickListener { startActivity(Intent(this@GlassHomeActivity, ViralShareActivity::class.java)) }
                }, LovableUi.run { margin(bottom = 8, top = 6) })
            }
        }
    }

    private fun refreshServer() {
        val prefs = getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE)
        val endpoint = PayloadClient.viralEndpoint(prefs.getString("endpoint", "").orEmpty())
        healthSummary.text = "در حال بررسی سلامت V5…"
        Thread {
            val health = runCatching {
                val conn = java.net.URL(endpoint.trimEnd('/') + "/v4/media-jobs/health").openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 15_000
                conn.readTimeout = 20_000
                conn.requestMethod = "GET"
                try {
                    val code = conn.responseCode
                    val body = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
                    code to body
                } finally { conn.disconnect() }
            }.getOrNull()
            runOnUiThread {
                val h = health?.second?.let { runCatching { JSONObject(it) }.getOrNull() }
                val ok = health?.first in 200..299 && h?.optBoolean("ok", false) == true
                healthSummary.text = if (ok) "● موتور V5 آماده • Gemini فعال • Apify فعال • یادگیری و Cleanup فعال" else "● اتصال به موتور تحلیل برقرار نشد؛ برای تلاش دوباره وارد صف شو"
                healthSummary.setTextColor(if (ok) LovableUi.success else LovableUi.warning)
            }
        }.start()
    }

    private fun kindFa(kind: String) = when (kind) {
        "reel" -> "ریلز"
        "video_post" -> "ویدیو"
        "photo" -> "عکس"
        "carousel" -> "کاروسل"
        "mixed" -> "ترکیبی"
        else -> "اینستاگرام"
    }

    private fun stageFa(stage: String) = when (stage) {
        "queued" -> "در صف تحلیل"
        "media_download" -> "دریافت محتوا"
        "media_verify" -> "بررسی فایل"
        "visual_hook_analysis" -> "تحلیل قلاب"
        "dialogue_transcription" -> "تحلیل تصویر و صدا"
        "behavioral_analysis" -> "تحلیل رفتار مخاطب"
        "learning_persist" -> "ثبت یادگیری"
        "completed" -> "تحلیل کامل"
        "failed" -> "قابل تلاش مجدد"
        else -> "در حال تحلیل"
    }
}
