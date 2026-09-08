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
        setPadding(LovableUi.run { dp(18) }, LovableUi.run { dp(24) }, LovableUi.run { dp(18) }, LovableUi.run { dp(68) })

        val heroGradient = android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.rgb(255, 96, 32),
                Color.rgb(150, 24, 76),
                Color.rgb(24, 18, 38)
            )
        ).apply {
            val r = LovableUi.run { dp(30).toFloat() }
            cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, r, r, r, r)
        }
        val heroBevel = android.graphics.drawable.GradientDrawable().apply {
            val r = LovableUi.run { dp(30).toFloat() }
            cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, r, r, r, r)
            setColor(Color.rgb(10, 14, 22))
        }
        background = android.graphics.drawable.LayerDrawable(arrayOf(heroBevel, heroGradient)).apply {
            setLayerInset(1, 0, 0, 0, LovableUi.run { dp(4) })
        }
        elevation = LovableUi.run { dp(10).toFloat() }
        translationZ = LovableUi.run { dp(4).toFloat() }

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
            val iconBase = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = LovableUi.run { dp(18).toFloat() }
                setColor(Color.argb(80, 10, 14, 22))
            }
            val iconFace = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = LovableUi.run { dp(18).toFloat() }
                setColor(Color.argb(60, 255, 255, 255))
                setStroke(LovableUi.run { dp(1) }, Color.argb(120, 255, 255, 255))
            }
            background = android.graphics.drawable.LayerDrawable(arrayOf(iconBase, iconFace)).apply {
                setLayerInset(1, 0, 0, 0, LovableUi.run { dp(3) })
            }
            elevation = LovableUi.run { dp(4).toFloat() }
        }, LinearLayout.LayoutParams(LovableUi.run { dp(42) }, LovableUi.run { dp(42) }).apply { marginEnd = LovableUi.run { dp(10) } })

        titleRow.addView(LinearLayout(this@GlassHomeActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(LovableUi.run { text("سلام محمدحسین ✦", 11.5f, Color.argb(235, 255, 255, 255)) })
            addView(LovableUi.run { text("مرکز فرماندهی تیک‌آف", 16.5f, Color.WHITE, true) })
        }, LinearLayout.LayoutParams(0, -2, 1f))

        titleRow.addView(TextView(this@GlassHomeActivity).apply {
            text = "◉"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(LovableUi.secondary)
            val liveBase = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = LovableUi.run { dp(18).toFloat() }
                setColor(Color.rgb(8, 12, 18))
            }
            val liveFace = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = LovableUi.run { dp(18).toFloat() }
                setColor(Color.rgb(20, 28, 44))
                setStroke(LovableUi.run { dp(1) }, Color.argb(100, 0, 229, 255))
            }
            background = android.graphics.drawable.LayerDrawable(arrayOf(liveBase, liveFace)).apply {
                setLayerInset(1, 0, 0, 0, LovableUi.run { dp(2) })
            }
            elevation = LovableUi.run { dp(4).toFloat() }
            translationZ = LovableUi.run { dp(1).toFloat() }
        }, LinearLayout.LayoutParams(LovableUi.run { dp(42) }, LovableUi.run { dp(42) }))
        addView(titleRow)

        val stats = LinearLayout(this@GlassHomeActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(0, LovableUi.run { dp(22) }, 0, 0)
        }
        statAnalyses = statBox("تحلیل این ماه", "✦", LovableUi.secondary, stats)
        statScore = statBox("میانگین امتیاز", "⚡", LovableUi.warning, stats)
        statScenarios = statBox("سناریوی ساخته‌شده", "★", LovableUi.success, stats)
        addView(stats)
    }

    private fun statBox(label: String, icon: String, iconColor: Int, parent: LinearLayout): TextView {
        val value = LovableUi.run { text("۰", 21f, Color.WHITE, true) }.apply { gravity = Gravity.CENTER }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(LovableUi.run { dp(6) }, LovableUi.run { dp(12) }, LovableUi.run { dp(6) }, LovableUi.run { dp(12) })

            val podBase = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = LovableUi.run { dp(20).toFloat() }
                setColor(Color.argb(80, 10, 14, 22))
            }
            val podFace = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.argb(70, 255, 255, 255), Color.argb(25, 255, 255, 255))
            ).apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = LovableUi.run { dp(20).toFloat() }
                setStroke(LovableUi.run { dp(1) }, Color.argb(100, 255, 255, 255))
            }
            background = android.graphics.drawable.LayerDrawable(arrayOf(podBase, podFace)).apply {
                setLayerInset(1, 0, 0, 0, LovableUi.run { dp(3) })
            }
            elevation = LovableUi.run { dp(5).toFloat() }
            translationZ = LovableUi.run { dp(2).toFloat() }

            val topBadge = LinearLayout(this@GlassHomeActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            topBadge.addView(TextView(this@GlassHomeActivity).apply {
                text = icon
                textSize = 11f
                setTextColor(iconColor)
            })
            addView(topBadge)
            addView(value)
            addView(LovableUi.run { text(label, 10f, Color.argb(230, 255, 255, 255)) }.apply { gravity = Gravity.CENTER })

            setOnTouchListener { v, ev ->
                when (ev.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.translationY = LovableUi.run { dp(3).toFloat() }
                        v.scaleX = 0.96f
                        v.scaleY = 0.96f
                        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.animate().translationY(0f).scaleX(1f).scaleY(1f).setDuration(120).start()
                        true
                    }
                    else -> false
                }
            }
        }
        parent.addView(box, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = LovableUi.run { dp(4) }; marginEnd = LovableUi.run { dp(4) } })
        return value
    }

    private fun quickActions(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        fun add(label: String, icon: String, toneBase: Int, toneFaceStart: Int, toneFaceEnd: Int, click: () -> Unit) {
            val cell = LinearLayout(this@GlassHomeActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(LovableUi.run { dp(4) }, LovableUi.run { dp(12) }, LovableUi.run { dp(4) }, LovableUi.run { dp(12) })

                background = LovableUi.run { card3dDrawable(Color.rgb(22, 30, 48), 22, 4) }
                elevation = LovableUi.run { dp(5).toFloat() }
                translationZ = LovableUi.run { dp(2).toFloat() }

                val iconBase = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = LovableUi.run { dp(16).toFloat() }
                    setColor(toneBase)
                }
                val iconFace = android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                    intArrayOf(toneFaceStart, toneFaceEnd)
                ).apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = LovableUi.run { dp(16).toFloat() }
                    setStroke(LovableUi.run { dp(1) }, Color.argb(120, 255, 255, 255))
                }
                val iconDrawable = android.graphics.drawable.LayerDrawable(arrayOf(iconBase, iconFace)).apply {
                    setLayerInset(1, 0, 0, 0, LovableUi.run { dp(3) })
                }
                addView(TextView(this@GlassHomeActivity).apply {
                    text = icon
                    textSize = 19f
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    background = iconDrawable
                    elevation = LovableUi.run { dp(3).toFloat() }
                }, LinearLayout.LayoutParams(LovableUi.run { dp(44) }, LovableUi.run { dp(44) }))

                addView(LovableUi.run { text(label, 11.5f, LovableUi.foreground, true) }.apply {
                    gravity = Gravity.CENTER
                    setPadding(0, LovableUi.run { dp(7) }, 0, 0)
                })

                setOnTouchListener { v, ev ->
                    when (ev.action) {
                        MotionEvent.ACTION_DOWN -> {
                            v.translationY = LovableUi.run { dp(3).toFloat() }
                            v.scaleX = 0.96f
                            v.scaleY = 0.96f
                            v.elevation = LovableUi.run { dp(1).toFloat() }
                            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            v.animate().translationY(0f).scaleX(1f).scaleY(1f).setDuration(130).withEndAction { click() }.start()
                            v.elevation = LovableUi.run { dp(5).toFloat() }
                            true
                        }
                        MotionEvent.ACTION_CANCEL -> {
                            v.animate().translationY(0f).scaleX(1f).scaleY(1f).setDuration(100).start()
                            v.elevation = LovableUi.run { dp(5).toFloat() }
                            true
                        }
                        else -> false
                    }
                }
            }
            addView(cell, LinearLayout.LayoutParams(0, LovableUi.run { dp(96) }, 1f).apply { marginStart = LovableUi.run { dp(4) }; marginEnd = LovableUi.run { dp(4) } })
        }
        add("تحلیل", "▷", Color.rgb(180, 52, 10), Color.rgb(255, 130, 52), Color.rgb(245, 96, 32)) { startActivity(Intent(this@GlassHomeActivity, NewAnalysisActivity::class.java)) }
        add("صف", "▱", Color.rgb(12, 44, 56), Color.rgb(0, 229, 255), Color.rgb(0, 160, 190)) { startActivity(Intent(this@GlassHomeActivity, ViralShareActivity::class.java)) }
        add("سناریو", "✦", Color.rgb(52, 36, 12), Color.rgb(255, 185, 45), Color.rgb(230, 140, 20)) { startActivity(Intent(this@GlassHomeActivity, ScenarioStudioActivity::class.java)) }
        add("حافظه", "↗", Color.rgb(36, 20, 60), Color.rgb(168, 85, 247), Color.rgb(126, 34, 206)) { startActivity(Intent(this@GlassHomeActivity, MemoryActivity::class.java)) }
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
