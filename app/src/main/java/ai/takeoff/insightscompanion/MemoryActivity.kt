package ai.takeoff.insightscompanion

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import org.json.JSONObject

class MemoryActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LovableUi.applyWindow(this)
        setContentView(buildUi())
    }

    override fun onResume() {
        super.onResume()
        setContentView(buildUi())
    }

    private fun buildUi(): View {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(LovableUi.background)
        }
        page.addView(LovableUi.run { topBar("حافظه تیک‌آف", "الگوهایی که V5 از محتوا یاد گرفته") })
        val scroll = ScrollView(this).apply { isFillViewport = true; overScrollMode = View.OVER_SCROLL_NEVER }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(LovableUi.run { dp(16) }, LovableUi.run { dp(16) }, LovableUi.run { dp(16) }, LovableUi.run { dp(28) })
        }
        val completed = SharedMediaQueue(this).all().filter { it.status == "completed" }
        root.addView(LovableUi.run { card() }.apply {
            addView(LovableUi.run { chip("V5 Behavioral Memory", "secondary") })
            addView(LovableUi.run { text("تیک‌آف از هر تحلیل کامل، قلاب، ساختار سکانس، Retention، محرک تعامل و الگوهای قابل تعمیم را نگه می‌دارد؛ خود فایل ویدیو بعد از یادگیری حذف می‌شود.", 12.5f, LovableUi.foreground) }.apply {
                setPadding(0, LovableUi.run { dp(10) }, 0, LovableUi.run { dp(10) })
            })
            addView(LovableUi.run { text("${LovableUi.fa(completed.size)} تحلیل کامل روی این دستگاه", 12f, LovableUi.primary, true) })
        }, LovableUi.run { margin(bottom = 22) })

        root.addView(LovableUi.run { sectionTitle("الگوهای اخیر") }, LovableUi.run { margin(bottom = 10) })
        if (completed.isEmpty()) {
            root.addView(LovableUi.run { card() }.apply {
                addView(LovableUi.run { text("هنوز حافظه محلی قابل نمایش نیست. یک تحلیل کامل انجام بده تا الگوهای V5 اینجا ظاهر شوند.", 12f, LovableUi.muted) })
            })
        } else {
            completed.take(30).forEach { item -> root.addView(memoryCard(item), LovableUi.run { margin(bottom = 9) }) }
        }
        scroll.addView(root)
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        page.addView(LovableUi.run { bottomNav("memory") })
        return page
    }

    private fun memoryCard(item: SharedMediaQueue.Item): View = LovableUi.run { card() }.apply {
        val root = item.resultJson?.let { runCatching { JSONObject(it) }.getOrNull() }
        val analysis = root?.optJSONObject("analysis") ?: root
        val hook = analysis?.optJSONObject("hook_intelligence")
        val origin = analysis?.optJSONObject("media_origin")?.optString("classification").orEmpty()
        val reusable = analysis?.optJSONArray("reusable_patterns") ?: analysis?.optJSONArray("reusable_mechanisms")
        val titleRow = LinearLayout(this@MemoryActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.CENTER_VERTICAL
        }
        titleRow.addView(LovableUi.run { chip(kindFa(item.mediaKind), "secondary") })
        titleRow.addView(LovableUi.run { text(item.shortcode.ifBlank { "Instagram" }, 13f, LovableUi.foreground, true) }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = LovableUi.run { dp(8) }; marginEnd = LovableUi.run { dp(8) } })
        titleRow.addView(LovableUi.run { chip("یادگرفته شد", "success") })
        addView(titleRow)
        val dominant = hook?.opt("dominant_hook")?.toString().orEmpty().ifBlank { hook?.optString("visual_hook").orEmpty() }
        if (dominant.isNotBlank()) addView(LovableUi.run { text("قلاب غالب: ${dominant.take(170)}", 11.5f, LovableUi.foreground) }.apply { setPadding(0, LovableUi.run { dp(9) }, 0, 0) })
        if (origin.isNotBlank()) addView(LovableUi.run { text("منشأ: ${originFa(origin)}", 11f, LovableUi.secondaryText, true) }.apply { setPadding(0, LovableUi.run { dp(6) }, 0, 0) })
        if (reusable != null && reusable.length() > 0) addView(LovableUi.run { text("الگوی قابل‌تکرار: ${reusable.opt(0).toString().take(190)}", 11f, LovableUi.muted) }.apply { setPadding(0, LovableUi.run { dp(6) }, 0, 0) })
        setOnClickListener { startActivity(Intent(this@MemoryActivity, ViralShareActivity::class.java)) }
    }

    private fun kindFa(kind: String) = when (kind) {
        "reel" -> "ریلز"
        "video_post" -> "ویدیو"
        "photo" -> "عکس"
        "carousel" -> "کاروسل"
        "mixed" -> "ترکیبی"
        else -> "محتوا"
    }

    private fun originFa(value: String) = when (value) {
        "FILMED_LIVE_ACTION" -> "فیلمبرداری واقعی"
        "FULLY_AI_GENERATED" -> "کاملاً AI"
        "AI_ASSISTED" -> "واقعی با کمک AI"
        "HYBRID_AI_AND_FILMED" -> "ترکیبی AI + واقعی"
        "CGI_ANIMATION" -> "CGI / انیمیشن"
        "SCREEN_RECORDING" -> "ضبط صفحه"
        else -> "نامشخص"
    }
}
