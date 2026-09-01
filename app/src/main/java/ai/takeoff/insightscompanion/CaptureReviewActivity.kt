package ai.takeoff.insightscompanion

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.animation.DecelerateInterpolator
import android.view.View
import android.widget.*
import org.json.JSONObject

class CaptureReviewActivity : Activity() {
    private val accent = Color.rgb(16, 202, 205)
    private val primary = Color.rgb(255, 122, 26)
    private val ink = Color.rgb(15, 23, 35)
    private val muted = Color.rgb(94, 108, 126)
    private lateinit var payload: JSONObject
    private val metricFields = linkedMapOf<String, EditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        payload = runCatching { JSONObject(intent.getStringExtra("payload") ?: "{}") }.getOrElse { JSONObject() }
        if (payload.optJSONObject("metrics") == null) return finish()
        setContentView(buildUi())
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.rgb(250,252,255)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        window.decorView.rootView.alpha = 0f
        window.decorView.rootView.translationY = dp(18).toFloat()
        window.decorView.rootView.animate().alpha(1f).translationY(0f).setDuration(500).setInterpolator(DecelerateInterpolator()).start()
        TakeoffSound.play(TakeoffSound.Cue.CAPTURE)
    }

    private fun buildUi(): View {
        val scroll = ScrollView(this).apply { background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.rgb(250,252,255), Color.rgb(243,252,253), Color.rgb(255,246,237))); isFillViewport = true }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(20), dp(26), dp(20), dp(30))
        }
        scroll.addView(root)
        root.addView(TextView(this).apply {
            text = "بررسی قبل از ثبت"
            textSize = 25f
            setTextColor(ink)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.START
        })
        root.addView(TextView(this).apply {
            text = "اعداد را یک نگاه سریع بررسی کن. اگر OCR اشتباه خوانده، همین‌جا اصلاحش کن؛ بعد از تأیید وارد صف امن می‌شود."
            textSize = 13.5f
            setTextColor(muted)
            setPadding(0,dp(8),0,dp(18))
            gravity = Gravity.START
        })

        root.addView(infoCard("پیج", "@${payload.optString("account_id")}"))
        root.addView(infoCard("ریلز", payload.optString("shortcode")))
        root.addView(infoCard("بخش شناسایی‌شده", payload.optJSONObject("ocr")?.optString("page_hint") ?: "Insights"))

        val metricCard = card()
        metricCard.addView(title("آمار شناسایی‌شده"))
        val metrics = payload.getJSONObject("metrics")
        val keys = metrics.keys().asSequence().toList().sorted()
        keys.forEach { key ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val label = TextView(this).apply {
                text = faLabel(key)
                textSize = 13.5f
                setTextColor(muted)
                gravity = Gravity.START
            }
            val edit = EditText(this).apply {
                val value = metrics.optDouble(key)
                setText(if (value % 1.0 == 0.0) value.toLong().toString() else value.toString())
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                setTextColor(ink)
                textSize = 16f
                gravity = Gravity.CENTER
                background = rounded(Color.argb(222,255,255,255), 14f, Color.argb(80,9,24,43))
            }
            metricFields[key] = edit
            row.addView(label, LinearLayout.LayoutParams(0, dp(52), 1f))
            row.addView(edit, LinearLayout.LayoutParams(dp(130), dp(48)))
            metricCard.addView(row)
        }
        root.addView(metricCard, margin(dp(12)))

        val scenarioCard = card()
        scenarioCard.addView(title("اتصال به سناریو"))
        val store = LocalInsightStore(this)
        val account = payload.optString("account_id")
        val shortcode = payload.optString("shortcode")
        val planned = store.scenarios(account)
        val existing = store.reels(account).firstOrNull { it.shortcode == shortcode }?.scenarioId
        val options = mutableListOf("بدون سناریوی مشخص")
        options += planned.map { "${it.scenarioId} — ${it.title}" }
        val scenarioSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@CaptureReviewActivity, android.R.layout.simple_spinner_dropdown_item, options)
        }
        val initial = planned.indexOfFirst { it.scenarioId == existing }.let { if (it >= 0) it + 1 else 0 }
        scenarioSpinner.setSelection(initial)
        scenarioCard.addView(scenarioSpinner, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52)))
        root.addView(scenarioCard, margin(dp(12)))

        val fidelityCard = card()
        fidelityCard.addView(title("تطابق اجرا با سناریو"))
        val fidelity = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        val values = listOf(
            "complete" to "کامل طبق سناریو",
            "minor_changes" to "با تغییر جزئی",
            "major_changes" to "با تغییر زیاد",
            "unknown" to "سناریوی مشخصی نداشت",
        )
        val fidelityIds = values.map { View.generateViewId() }
        values.forEachIndexed { index, (_, label) ->
            fidelity.addView(RadioButton(this).apply {
                text = label
                setTextColor(ink)
                id = fidelityIds[index]
            })
        }
        fidelity.check(if (initial > 0) fidelityIds[0] else fidelityIds[3])
        scenarioSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    fidelity.check(fidelityIds[3])
                } else if (fidelity.checkedRadioButtonId == fidelityIds[3]) {
                    fidelity.check(fidelityIds[0])
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        fidelityCard.addView(fidelity)
        root.addView(fidelityCard, margin(dp(14)))

        val confirm = button("تأیید و ثبت امن", true) {
            val updated = JSONObject()
            metricFields.forEach { (key, field) ->
                field.text.toString().trim().replace(",", "").toDoubleOrNull()?.let { value ->
                    updated.put(key, if (value % 1.0 == 0.0) value.toLong() else value)
                }
            }
            if (updated.length() == 0) {
                Toast.makeText(this, "حداقل یک عدد معتبر لازم است", Toast.LENGTH_SHORT).show()
                return@button
            }
            payload.put("metrics", updated)
            val selectedScenario = planned.getOrNull(scenarioSpinner.selectedItemPosition - 1)?.scenarioId
            if (selectedScenario.isNullOrBlank()) payload.remove("scenario_id") else payload.put("scenario_id", selectedScenario)
            val fidelityIndex = fidelityIds.indexOf(fidelity.checkedRadioButtonId).let { if (it >= 0) it else values.lastIndex }
            payload.put("execution_fidelity", values[fidelityIndex].first)
            payload.put("operator_reviewed", true)
            PayloadQueue(this).enqueue(payload)
            store.bindScenario(account, shortcode, selectedScenario)
            store.markCapture(payload, "queued")
            TakeoffSound.play(TakeoffSound.Cue.SUCCESS)
            Toast.makeText(this, "ثبت شد؛ داده در صف امن است", Toast.LENGTH_LONG).show()
            finish()
        }
        root.addView(confirm, margin(dp(8)))

        val reject = button("رد کردن این ثبت", false) {
            TakeoffSound.play(TakeoffSound.Cue.WARNING)
            finish()
        }
        root.addView(reject, margin(0))
        return scroll
    }

    private fun faLabel(key: String) = mapOf(
        "views" to "بازدید", "viewers" to "بیننده یکتا", "reach" to "حساب‌های دسترسی‌یافته", "likes" to "لایک",
        "comments" to "نظر", "shares" to "اشتراک‌گذاری", "reposts" to "بازنشر", "saves" to "ذخیره", "follows" to "فالو از ریلز",
        "profile_visits" to "بازدید پروفایل", "watch_time_seconds" to "زمان تماشای کل", "total_watch_time_seconds" to "زمان تماشای کل",
        "average_watch_time_seconds" to "میانگین زمان تماشا", "avg_watch_time_seconds" to "میانگین زمان تماشا", "completion_rate" to "نرخ تکمیل ویدیو",
        "hold_3s_rate" to "نگهداشت ۳ ثانیه اول", "skip_rate" to "نرخ رد کردن / اسکیپ", "replay_rate" to "نرخ بازپخش",
        "share_rate" to "نرخ اشتراک‌گذاری", "save_rate" to "نرخ ذخیره", "like_rate" to "نرخ لایک", "comment_rate" to "نرخ نظر",
        "repost_rate" to "نرخ بازنشر", "follower_rate" to "سهم دنبال‌کننده‌ها", "nonfollower_rate" to "سهم غیردنبال‌کننده‌ها",
    )[key] ?: key.replace('_', ' ')

    private fun infoCard(label: String, value: String): View = card().apply {
        addView(title(label))
        addView(TextView(this@CaptureReviewActivity).apply { text = value; textSize = 16f; setTextColor(ink); gravity = Gravity.START })
    }.also { it.layoutParams = margin(dp(8)) }
    private fun card() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(16),dp(15),dp(16),dp(15)); background = rounded(Color.argb(238,255,255,255),22f,Color.argb(80,9,24,43)); elevation=dp(5).toFloat() }
    private fun title(value: String) = TextView(this).apply { text=value; textSize=12.5f; setTextColor(accent); typeface=Typeface.DEFAULT_BOLD; gravity=Gravity.START; setPadding(0,0,0,dp(8)) }
    private fun button(label: String, primary: Boolean, click: () -> Unit) = Button(this).apply { text=label; isAllCaps=false; textSize=14f; typeface=Typeface.DEFAULT_BOLD; setTextColor(if(primary) Color.WHITE else ink); background=rounded(if(primary)this@CaptureReviewActivity.primary else Color.argb(222,255,255,255),15f,if(primary)this@CaptureReviewActivity.primary else Color.argb(80,9,24,43)); setOnClickListener{ v -> v.animate().scaleX(.97f).scaleY(.97f).setDuration(80).withEndAction { v.animate().scaleX(1f).scaleY(1f).setDuration(140).start(); click() }.start() } }
    private fun rounded(fill:Int,radius:Float,stroke:Int)=GradientDrawable().apply{shape=GradientDrawable.RECTANGLE;setColor(fill);cornerRadius=dp(radius.toInt()).toFloat();setStroke(dp(1),stroke)}
    private fun margin(bottom:Int)=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT).apply{bottomMargin=bottom}
    private fun dp(value:Int)=(value*resources.displayMetrics.density).toInt()
}
