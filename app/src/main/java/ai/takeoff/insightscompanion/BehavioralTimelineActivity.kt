package ai.takeoff.insightscompanion

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject

class BehavioralTimelineActivity : Activity() {
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
        page.addView(LovableUi.run { topBar("تایم‌لاین رفتاری", item?.shortcode ?: "TakeOff V5", back = { finish() }) })
        val scroll = ScrollView(this).apply { isFillViewport = true; overScrollMode = View.OVER_SCROLL_NEVER }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(LovableUi.run { dp(16) }, LovableUi.run { dp(16) }, LovableUi.run { dp(16) }, LovableUi.run { dp(32) })
        }
        val result = item?.resultJson?.let { runCatching { JSONObject(it) }.getOrNull() }
        val analysis = result?.optJSONObject("analysis") ?: result
        if (analysis == null) {
            root.addView(LovableUi.run { card() }.apply { addView(LovableUi.run { text("تایم‌لاین هنوز آماده نیست.", 12.5f, LovableUi.muted) }) })
        } else {
            root.addView(LovableUi.run { card(true) }.apply {
                addView(LovableUi.run { chip("Behavioral Video Timeline", "primary") })
                addView(LovableUi.run { text("ویدیو را به‌جای یک متن بلند، به زنجیره‌ای از اتفاق‌های رفتاری می‌بینیم: قلاب، حفظ توجه، تغییر احساس، re-hook، payoff و CTA.", 12f, LovableUi.foreground) }.apply { setPadding(0, LovableUi.run { dp(10) }, 0, 0) })
            }, LovableUi.run { margin(bottom = 18) })

            val scenes = extractScenes(analysis)
            if (scenes.length() == 0) {
                root.addView(LovableUi.run { card() }.apply { addView(LovableUi.run { text("برای این گزارش سکانس زمان‌دار قابل نمایش پیدا نشد؛ گزارش کامل V5 همچنان در صفحه نتیجه موجود است.", 12f, LovableUi.muted) }) })
            } else {
                for (i in 0 until scenes.length()) {
                    val scene = scenes.optJSONObject(i) ?: continue
                    root.addView(sceneCard(scene, i, scenes.length()), LovableUi.run { margin(bottom = 8) })
                }
            }
            addBehaviorSection(root, "نقاط توقف اسکرول", analysis.opt("stop_scroll_analysis"), "primary")
            addBehaviorSection(root, "زنجیره Retention", analysis.opt("behavioral_timeline") ?: analysis.opt("retention_analysis"), "secondary")
            addBehaviorSection(root, "منحنی احساس", analysis.opt("emotion_curve"), "primary")
            addBehaviorSection(root, "محرک‌های تعامل", analysis.opt("social_action_triggers"), "secondary")
        }
        scroll.addView(root)
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        return page
    }

    private fun sceneCard(scene: JSONObject, index: Int, total: Int): View = LovableUi.run { card() }.apply {
        val start = first(scene, "start", "start_seconds", "from")
        val end = first(scene, "end", "end_seconds", "to")
        val time = if (start.isNotBlank() || end.isNotBlank()) "$start ← $end" else first(scene, "timeframe", "time", "timestamp")
        val top = LinearLayout(this@BehavioralTimelineActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(TextView(this@BehavioralTimelineActivity).apply {
            text = LovableUi.fa(index + 1)
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = LovableUi.run { rounded(if (index == 0) LovableUi.primary else LovableUi.navy, 18, Color.TRANSPARENT, 0) }
        }, LinearLayout.LayoutParams(LovableUi.run { dp(36) }, LovableUi.run { dp(36) }))
        top.addView(LinearLayout(this@BehavioralTimelineActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(LovableUi.run { text(first(scene, "purpose", "label", "title").ifBlank { "سکانس ${LovableUi.fa(index + 1)}" }, 12.5f, LovableUi.foreground, true) })
            if (time.isNotBlank()) addView(LovableUi.run { text(time, 10.5f, LovableUi.primary, true) })
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = LovableUi.run { dp(9) }; marginEnd = LovableUi.run { dp(9) } })
        top.addView(LovableUi.run { chip(if (index == 0) "HOOK" else if (index == total - 1) "PAYOFF" else "BEAT", if (index == 0) "primary" else "secondary") })
        addView(top)
        val action = first(scene, "action", "description", "visual", "event")
        val dialogue = first(scene, "dialogue", "spoken", "text")
        val role = first(scene, "behavioral_role", "retention_role", "purpose")
        val camera = first(scene, "camera", "shot", "edit")
        val lines = listOf(
            "اتفاق" to action,
            "دیالوگ/متن" to dialogue,
            "نقش رفتاری" to role,
            "دوربین/تدوین" to camera,
        ).filter { it.second.isNotBlank() }
        lines.forEach { (label, value) ->
            addView(LovableUi.run { text("$label: ${value.take(500)}", 11.2f, LovableUi.foreground) }.apply { setPadding(0, LovableUi.run { dp(7) }, 0, 0) })
        }
    }

    private fun addBehaviorSection(root: LinearLayout, title: String, raw: Any?, tone: String) {
        val text = pretty(raw)
        if (text.isBlank()) return
        root.addView(LovableUi.run { card() }.apply {
            addView(LovableUi.run { chip(title, tone) })
            addView(LovableUi.run { text(text.take(1900), 11.4f, LovableUi.foreground) }.apply { setPadding(0, LovableUi.run { dp(9) }, 0, 0) })
        }, LovableUi.run { margin(bottom = 9, top = 4) })
    }

    private fun extractScenes(analysis: JSONObject): JSONArray {
        val architecture = analysis.optJSONObject("scene_architecture")
        val fromArchitecture = architecture?.optJSONArray("scenes")
        if (fromArchitecture != null && fromArchitecture.length() > 0) return fromArchitecture
        val timeline = analysis.optJSONArray("timeline")
        if (timeline != null) return timeline
        return JSONArray()
    }

    private fun first(obj: JSONObject, vararg keys: String): String {
        for (key in keys) {
            val raw = obj.opt(key)
            if (raw != null && raw != JSONObject.NULL) {
                val value = raw.toString().trim()
                if (value.isNotBlank() && value != "0.0") return value
            }
        }
        return ""
    }

    private fun pretty(value: Any?): String = when (value) {
        null, JSONObject.NULL -> ""
        is String -> value
        is JSONObject -> buildString {
            val keys = value.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val child = pretty(value.opt(key))
                if (child.isNotBlank()) append("• ").append(key.replace('_', ' ')).append(": ").append(child).append('\n')
            }
        }.trim()
        is JSONArray -> buildString {
            for (i in 0 until value.length()) {
                val child = pretty(value.opt(i))
                if (child.isNotBlank()) append("• ").append(child).append('\n')
            }
        }.trim()
        else -> value.toString()
    }
}
