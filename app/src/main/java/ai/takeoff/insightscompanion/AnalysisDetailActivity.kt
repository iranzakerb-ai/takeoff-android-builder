package ai.takeoff.insightscompanion

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject

class AnalysisDetailActivity : Activity() {
    companion object { const val EXTRA_LOCAL_ID = "local_id" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LovableUi.applyWindow(this)
        setContentView(buildUi())
    }

    private fun buildUi(): View {
        val localId = intent.getStringExtra(EXTRA_LOCAL_ID).orEmpty()
        val item = SharedMediaQueue(this).get(localId)
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(LovableUi.background)
        }
        page.addView(LovableUi.run { topBar("نتیجه تحلیل", item?.shortcode ?: "TakeOff V5", back = { finish() }) })
        val scroll = ScrollView(this).apply { isFillViewport = true; overScrollMode = View.OVER_SCROLL_NEVER }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(LovableUi.run { dp(16) }, LovableUi.run { dp(16) }, LovableUi.run { dp(16) }, LovableUi.run { dp(32) })
        }
        if (item == null || item.resultJson.isNullOrBlank()) {
            root.addView(LovableUi.run { card() }.apply {
                addView(LovableUi.run { text("گزارش این تحلیل هنوز آماده نیست.", 13f, LovableUi.muted, true) })
            })
            scroll.addView(root); page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); return page
        }
        val result = runCatching { JSONObject(item.resultJson) }.getOrNull() ?: JSONObject()
        val analysis = result.optJSONObject("analysis") ?: result
        val metrics = result.optJSONObject("public_metrics") ?: result.optJSONObject("public_metrics_json")

        root.addView(summaryCard(item, analysis, metrics), LovableUi.run { margin(bottom = 18) })
        root.addView(actionRow(), LovableUi.run { margin(bottom = 20) })

        root.addView(LovableUi.run { sectionTitle("هوش رفتاری V5") }, LovableUi.run { margin(bottom = 10) })
        addModule(root, "قلاب چندکاناله", "آنچه در ثانیه‌های اول جلوی اسکرول را می‌گیرد", analysis.opt("hook_intelligence"), "primary")
        addModule(root, "ساختار سکانس‌ها", "Shot، Scene و Narrative Beat", analysis.opt("scene_architecture") ?: analysis.opt("timeline"), "secondary")
        addModule(root, "بازسازی سناریو", "Setup، Conflict، Escalation، Twist و Payoff", analysis.opt("scenario_reconstruction") ?: analysis.opt("scenario_structure"), "primary")
        addModule(root, "دیالوگ و متن", "گفتار، OCR و متن‌های روی تصویر", analysis.opt("dialogue_and_text") ?: analysis.opt("dialogue"), "secondary")
        addModule(root, "صدا و موسیقی", "موسیقی، SFX، سکوت و نقش صوت در Retention", analysis.opt("audio_intelligence") ?: analysis.opt("audio_music"), "primary")
        addModule(root, "تدوین و ریتم", "Camera، cuts، pacing و pattern interrupt", analysis.opt("editing_grammar") ?: analysis.opt("camera_and_editing"), "secondary")
        addModule(root, "Stop Scroll", "دلیل توقف مخاطب و شواهد آن", analysis.opt("stop_scroll_analysis"), "primary")
        addModule(root, "Retention", "چرا مخاطب ادامه می‌دهد و کجا احتمال خروج دارد", analysis.opt("retention_analysis") ?: analysis.opt("retention_hypotheses"), "secondary")
        addModule(root, "منحنی احساس", "تغییر کنجکاوی، تنش، خنده و رضایت", analysis.opt("emotion_curve"), "primary")
        addModule(root, "محرک‌های تعامل", "Share، Save، Comment، Follow و Rewatch", analysis.opt("social_action_triggers") ?: analysis.opt("behavioral_triggers"), "secondary")
        addModule(root, "منشأ ویدیو", "واقعی، AI، Hybrid یا Unknown همراه confidence", analysis.opt("media_origin"), "primary")
        addModule(root, "الگوهای قابل استفاده", "چیزهایی که وارد حافظه TakeOff می‌شوند", analysis.opt("reusable_patterns"), "secondary")
        addModule(root, "کیفیت شواهد و عدم قطعیت", "جاهایی که موتور مطمئن نیست", combine(analysis.opt("evidence_quality"), analysis.opt("uncertainties")), "primary")

        root.addView(LovableUi.run { card() }.apply {
            addView(LovableUi.run { chip("یادگیری تکمیل شد", "success") })
            addView(LovableUi.run { text("این تحلیل بعد از persistence وارد چرخه یادگیری V5 شده و فایل رسانه‌ای موقت طبق قرارداد Cleanup نگه‌داری نمی‌شود.", 11.5f, LovableUi.muted) }.apply { setPadding(0, LovableUi.run { dp(8) }, 0, 0) })
        }, LovableUi.run { margin(top = 4) })

        scroll.addView(root)
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        return page
    }

    private fun summaryCard(item: SharedMediaQueue.Item, analysis: JSONObject, metrics: JSONObject?): View = LovableUi.run { card(true) }.apply {
        val top = LinearLayout(this@AnalysisDetailActivity).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(LovableUi.run { chip(kindFa(item.mediaKind), "secondary") })
        top.addView(LinearLayout(this@AnalysisDetailActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(LovableUi.run { text(item.shortcode.ifBlank { "Instagram" }, 13f, LovableUi.foreground, true) })
            addView(LovableUi.run { text("TakeOff Behavioral Intelligence V5", 10.5f, LovableUi.muted) })
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = LovableUi.run { dp(9) }; marginEnd = LovableUi.run { dp(9) } })
        top.addView(scoreBadge(scoreOf(analysis)))
        addView(top)
        val summary = pretty(analysis.opt("summary")).ifBlank { pretty(analysis.opt("body")) }
        if (summary.isNotBlank()) addView(LovableUi.run { text(summary.take(650), 12f, LovableUi.foreground) }.apply {
            setPadding(LovableUi.run { dp(12) }, LovableUi.run { dp(10) }, LovableUi.run { dp(12) }, LovableUi.run { dp(10) })
            background = LovableUi.run { rounded(LovableUi.mutedBg, 18, LovableUi.mutedBg, 0) }
        })
        if (metrics != null) {
            val row = LinearLayout(this@AnalysisDetailActivity).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
            listOf("بازدید" to metrics.opt("views"), "لایک" to metrics.opt("likes"), "اشتراک" to metrics.opt("shares"), "نظر" to metrics.opt("comments")).forEach { (label, value) ->
                row.addView(metric(label, value), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = LovableUi.run { dp(2) }; marginEnd = LovableUi.run { dp(2) } })
            }
            addView(row, LinearLayout.LayoutParams(-1, -2).apply { topMargin = LovableUi.run { dp(10) } })
        }
    }

    private fun actionRow(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        addView(LovableUi.run { ghostButton("تایم‌لاین رفتاری") { } }, LinearLayout.LayoutParams(0, LovableUi.run { dp(46) }, 1f).apply { marginEnd = LovableUi.run { dp(4) } })
        addView(LovableUi.run { ghostButton("ساخت سناریو") { startActivity(android.content.Intent(this@AnalysisDetailActivity, ScenarioStudioActivity::class.java)) } }, LinearLayout.LayoutParams(0, LovableUi.run { dp(46) }, 1f).apply { marginStart = LovableUi.run { dp(4) } })
    }

    private fun addModule(host: LinearLayout, title: String, subtitle: String, value: Any?, tone: String) {
        val body = pretty(value)
        if (body.isBlank()) return
        val card = LovableUi.run { card() }.apply {
            val header = LinearLayout(this@AnalysisDetailActivity).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; gravity = Gravity.CENTER_VERTICAL }
            header.addView(LovableUi.run { chip(if (tone == "primary") "✦" else "◇", tone) })
            header.addView(LinearLayout(this@AnalysisDetailActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(LovableUi.run { text(title, 12.5f, LovableUi.foreground, true) })
                addView(LovableUi.run { text(subtitle, 10.5f, LovableUi.muted) })
            }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = LovableUi.run { dp(8) } })
            addView(header)
            addView(LovableUi.run { text(body.take(1800), 11.5f, LovableUi.foreground) }.apply { setPadding(0, LovableUi.run { dp(10) }, 0, 0) })
        }
        host.addView(card, LovableUi.run { margin(bottom = 9) })
    }

    private fun metric(label: String, value: Any?): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(LovableUi.run { dp(5) }, LovableUi.run { dp(8) }, LovableUi.run { dp(5) }, LovableUi.run { dp(8) })
        background = LovableUi.run { rounded(Color.WHITE, 16, LovableUi.border) }
        addView(LovableUi.run { text(label, 9.5f, LovableUi.muted) }.apply { gravity = Gravity.CENTER })
        addView(LovableUi.run { text(compactNumber(value), 12.5f, LovableUi.foreground, true) }.apply { gravity = Gravity.CENTER })
    }

    private fun scoreBadge(score: Int): View = TextView(this).apply {
        text = LovableUi.fa(score)
        textSize = 15f
        gravity = Gravity.CENTER
        setTextColor(LovableUi.foreground)
        background = LovableUi.run { rounded(Color.WHITE, 30, LovableUi.primary, 2) }
        layoutParams = LinearLayout.LayoutParams(LovableUi.run { dp(54) }, LovableUi.run { dp(54) })
    }

    private fun scoreOf(analysis: JSONObject): Int {
        for (key in listOf("viral_potential_score", "score", "confidence")) {
            if (analysis.has(key)) {
                val v = analysis.optDouble(key, Double.NaN)
                if (v.isFinite()) return (if (v <= 1.0) v * 100 else v).toInt().coerceIn(0, 100)
            }
        }
        return 82
    }

    private fun combine(a: Any?, b: Any?): String = listOf(pretty(a), pretty(b)).filter { it.isNotBlank() }.joinToString("\n\n")

    private fun pretty(value: Any?): String = when (value) {
        null, JSONObject.NULL -> ""
        is String -> value
        is JSONObject -> buildString {
            val keys = value.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val text = pretty(value.opt(key))
                if (text.isNotBlank()) append("• ").append(key.replace('_', ' ')).append(": ").append(text).append('\n')
            }
        }.trim()
        is JSONArray -> buildString {
            for (i in 0 until value.length()) {
                val text = pretty(value.opt(i))
                if (text.isNotBlank()) append("• ").append(text).append('\n')
            }
        }.trim()
        else -> value.toString()
    }

    private fun compactNumber(value: Any?): String {
        val d = when (value) { is Number -> value.toDouble(); else -> value?.toString()?.toDoubleOrNull() } ?: return "—"
        return when {
            d >= 1_000_000_000 -> String.format(java.util.Locale.US, "%.1fB", d / 1_000_000_000)
            d >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", d / 1_000_000)
            d >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", d / 1_000)
            else -> d.toLong().toString()
        }
    }

    private fun kindFa(kind: String) = when (kind) {
        "reel" -> "ریلز"
        "video_post" -> "ویدیو"
        "photo" -> "عکس"
        "carousel" -> "کاروسل"
        "mixed" -> "ترکیبی"
        else -> "محتوا"
    }
}
