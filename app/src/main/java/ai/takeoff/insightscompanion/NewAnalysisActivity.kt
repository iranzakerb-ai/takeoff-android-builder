package ai.takeoff.insightscompanion

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class NewAnalysisActivity : Activity() {
    private lateinit var url: EditText
    private var goal = "افزایش بازدید"
    private var depth = "عمیق + V5"
    private lateinit var goalHost: LinearLayout
    private lateinit var depthHost: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LovableUi.applyWindow(this)
        setContentView(buildUi())
    }

    private fun buildUi(): View {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(LovableUi.background)
        }
        page.addView(LovableUi.run { topBar("تحلیل جدید", "ویدیو را بده؛ بینش بگیر", back = { finish() }) })
        val scroll = ScrollView(this).apply { isFillViewport = true; overScrollMode = View.OVER_SCROLL_NEVER }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(LovableUi.run { dp(16) }, LovableUi.run { dp(16) }, LovableUi.run { dp(16) }, LovableUi.run { dp(28) })
        }

        root.addView(LovableUi.run { card() }.apply {
            addView(LovableUi.run { sectionTitle("لینک ریلز یا پست") })
            url = EditText(this@NewAnalysisActivity).apply {
                hint = "https://instagram.com/reel/..."
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
                textDirection = View.TEXT_DIRECTION_LTR
                layoutDirection = View.LAYOUT_DIRECTION_LTR
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                setTextColor(LovableUi.foreground)
                setHintTextColor(LovableUi.muted)
                textSize = 12.5f
                setPadding(LovableUi.run { dp(13) }, LovableUi.run { dp(12) }, LovableUi.run { dp(13) }, LovableUi.run { dp(12) })
                background = LovableUi.run { rounded(LovableUi.mutedBg, 18, LovableUi.border) }
            }
            addView(url, LinearLayout.LayoutParams(-1, LovableUi.run { dp(52) }).apply { topMargin = LovableUi.run { dp(12) } })
            addView(LovableUi.run { text("لینک عمومی Reel، Post یا Carousel را وارد کن. برای Share مستقیم از Instagram هم همان مسیر قبلی فعال است.", 11f, LovableUi.muted) }.apply {
                setPadding(0, LovableUi.run { dp(9) }, 0, 0)
            })
            val share = LovableUi.run { ghostButton("باز کردن صف Share اینستاگرام") { startActivity(Intent(this@NewAnalysisActivity, ViralShareActivity::class.java)) } }
            addView(share, LinearLayout.LayoutParams(-1, LovableUi.run { dp(48) }).apply { topMargin = LovableUi.run { dp(12) } })
        }, LovableUi.run { margin(bottom = 22) })

        root.addView(LovableUi.run { sectionTitle("هدف این محتوا") }, LovableUi.run { margin(bottom = 10) })
        goalHost = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.CENTER_VERTICAL
        }
        root.addView(goalHost, LovableUi.run { margin(bottom = 22) })
        renderGoals()

        root.addView(LovableUi.run { sectionTitle("عمق تحلیل") }, LovableUi.run { margin(bottom = 10) })
        depthHost = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        root.addView(depthHost, LovableUi.run { margin(bottom = 22) })
        renderDepths()

        root.addView(LovableUi.run { card() }.apply {
            val row = LinearLayout(this@NewAnalysisActivity).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; gravity = Gravity.CENTER_VERTICAL }
            row.addView(LovableUi.run { text("تنظیمات پیشرفته", 12.5f, LovableUi.foreground, true) }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(LovableUi.run { chip("V5 • حافظه فعال", "secondary") })
            addView(row)
            addView(LovableUi.run { text("تحلیل کامل فعلی همیشه قلاب چندکاناله، سناریو، AI provenance، Retention و یادگیری Supabase را اجرا می‌کند.", 11f, LovableUi.muted) }.apply { setPadding(0, LovableUi.run { dp(8) }, 0, 0) })
        }, LovableUi.run { margin(bottom = 18) })

        root.addView(LovableUi.run { primaryButton("✦ شروع تحلیل") { submit() } }, LinearLayout.LayoutParams(-1, LovableUi.run { dp(54) }))
        root.addView(LovableUi.run { text("تحلیل با حافظه رفتاری TakeOff V5 ترکیب می‌شود", 10.5f, LovableUi.muted) }.apply {
            gravity = Gravity.CENTER
            setPadding(0, LovableUi.run { dp(10) }, 0, 0)
        })
        scroll.addView(root)
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        page.addView(LovableUi.run { bottomNav("new") })
        return page
    }

    private fun renderGoals() {
        goalHost.removeAllViews()
        listOf("افزایش بازدید", "افزایش ذخیره", "جذب مشتری").forEach { item ->
            val chip = LovableUi.run { chip(item, if (goal == item) "primary" else "muted") }
            chip.setOnClickListener { goal = item; renderGoals() }
            goalHost.addView(chip, LinearLayout.LayoutParams(0, LovableUi.run { dp(38) }, 1f).apply { marginStart = LovableUi.run { dp(3) }; marginEnd = LovableUi.run { dp(3) } })
        }
    }

    private fun renderDepths() {
        depthHost.removeAllViews()
        val options = listOf(
            "سریع" to "ورود سریع به صف؛ موتور پردازش اصلی ثابت می‌ماند",
            "کامل" to "تحلیل کامل Golden + V5",
            "عمیق + V5" to "تحلیل کامل + استخراج الگوی یادگیری",
        )
        options.forEach { (title, detail) ->
            val selected = depth == title
            val card = LovableUi.run { card() }.apply {
                background = LovableUi.run { rounded(if (selected) LovableUi.primarySoft else Color.WHITE, 22, if (selected) LovableUi.primary else LovableUi.border) }
                val row = LinearLayout(this@NewAnalysisActivity).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; gravity = Gravity.CENTER_VERTICAL }
                row.addView(TextView(this@NewAnalysisActivity).apply {
                    text = if (selected) "●" else "○"
                    textSize = 18f
                    setTextColor(if (selected) LovableUi.primary else LovableUi.muted)
                    gravity = Gravity.CENTER
                }, LinearLayout.LayoutParams(LovableUi.run { dp(28) }, LovableUi.run { dp(32) }))
                row.addView(LinearLayout(this@NewAnalysisActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(LovableUi.run { text(title, 13f, LovableUi.foreground, true) })
                    addView(LovableUi.run { text(detail, 10.5f, LovableUi.muted) })
                }, LinearLayout.LayoutParams(0, -2, 1f))
                if (title == "عمیق + V5") row.addView(LovableUi.run { chip("پیشنهاد ما", "secondary") })
                addView(row)
                setOnClickListener { depth = title; renderDepths() }
            }
            depthHost.addView(card, LovableUi.run { margin(bottom = 8) })
        }
    }

    private fun submit() {
        val raw = url.text.toString().trim()
        val canonical = SharedMediaQueue.extractUrls(raw).firstOrNull()
        if (canonical == null) {
            url.background = LovableUi.run { rounded(Color.rgb(255, 244, 242), 18, LovableUi.danger) }
            Toast.makeText(this, "لینک معتبر Instagram را وارد کن", Toast.LENGTH_SHORT).show()
            return
        }
        val account = runCatching { ManagedAccountStore(this).selected()?.normalizedHandle }.getOrNull()
        val prefs = getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE)
        val niche = account?.let { prefs.getString("niche_$it", null) }
            ?.takeIf { !it.isNullOrBlank() }
            ?: prefs.getString("viral_analysis_niche", "عمومی").orEmpty().ifBlank { "عمومی" }
        val item = runCatching { SharedMediaQueue(this).enqueue(canonical, niche, account) }.getOrElse {
            Toast.makeText(this, "لینک قابل ثبت نبود", Toast.LENGTH_SHORT).show(); return
        }
        SharedMediaWork.enqueue(this, item)
        startActivity(Intent(this, ViralShareActivity::class.java).putExtra(ViralShareActivity.EXTRA_ADDED_COUNT, 1))
        finish()
    }
}
