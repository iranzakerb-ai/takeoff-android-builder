package ai.takeoff.insightscompanion

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AiVideoStudioActivity : Activity() {
    private val bg = Color.rgb(250, 252, 255)
    private val ink = Color.rgb(15, 23, 35)
    private val muted = Color.rgb(94, 108, 126)
    private val orange = Color.rgb(255, 122, 26)
    private val teal = Color.rgb(16, 202, 205)

    private lateinit var niche: EditText
    private lateinit var description: EditText
    private lateinit var audience: EditText
    private lateinit var offer: EditText
    private lateinit var constraints: EditText
    private lateinit var modeSpinner: Spinner
    private lateinit var status: TextView
    private lateinit var results: LinearLayout
    private lateinit var generate: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        setContentView(buildUi())
    }

    private fun buildUi(): View {
        val scroll = ScrollView(this).apply { isFillViewport = true; setBackgroundColor(bg) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(18), dp(22), dp(18), dp(32))
        }
        root.addView(TextView(this).apply {
            text = "‹  ساخت ویدیوی AI"
            textSize = 27f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink)
            setOnClickListener { finish() }
        })
        root.addView(TextView(this).apply {
            text = "یک ویدیوی AI کامل و آماده تولید • Character Sheet + Promptهای Omni"
            textSize = 12.5f
            setTextColor(muted)
            setPadding(0, dp(6), 0, dp(18))
        })
        root.addView(infoCard())

        niche = field("حوزه کاری *", "مثلاً باربری یا تعمیرات خودرو", 2)
        description = field("توضیح کسب‌وکار *", "حتی توضیح کوتاه مثل «اسباب کشی»", 3)
        audience = field("مخاطب هدف", "اختیاری", 2)
        offer = field("محصول یا پیشنهاد اصلی", "اختیاری", 2)
        constraints = field("محدودیت یا نکته مهم", "اختیاری", 2)
        listOf(niche, description, audience, offer, constraints).forEach { root.addView(it, margin(10)) }

        root.addView(TextView(this).apply {
            text = "حالت تولید ویدیو AI:"
            textSize = 12.5f
            setTextColor(ink)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(4), dp(4), dp(4), dp(4))
        })
        modeSpinner = Spinner(this).apply {
            val modes = listOf("ساخت ویدیوی AI سینمایی (چند سکانس)", "ساخت ویدیوی AI وایرال ۱۰ ثانیه‌ای (Visual Micro-Spectacle)")
            adapter = ArrayAdapter(this@AiVideoStudioActivity, android.R.layout.simple_spinner_dropdown_item, modes)
            setSelection(0)
            background = rounded(Color.WHITE, 16, Color.rgb(225, 231, 238))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        root.addView(modeSpinner, LinearLayout.LayoutParams(-1, dp(48)).apply { bottomMargin = dp(12) })

        generate = Button(this).apply {
            text = "ساخت ویدیوی AI با Omni"
            isAllCaps = false
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = rounded(orange, 18, orange)
            setOnClickListener { generate() }
        }
        root.addView(generate, LinearLayout.LayoutParams(-1, dp(56)).apply { bottomMargin = dp(10) })

        status = TextView(this).apply {
            textSize = 12.5f
            setTextColor(muted)
            gravity = Gravity.START
            setPadding(0, dp(5), 0, dp(10))
        }
        root.addView(status)

        results = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        root.addView(results)
        scroll.addView(root)
        return scroll
    }

    private fun infoCard() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = rounded(Color.WHITE, 22, Color.rgb(228, 233, 240))
        elevation = dp(3).toFloat()
        addView(TextView(this@AiVideoStudioActivity).apply {
            text = "Flow / Omni Timing"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(teal)
        })
        addView(TextView(this@AiVideoStudioActivity).apply {
            text = "تیک‌آف خودش تعداد سکانس‌ها را انتخاب می‌کند. ۴، ۶، ۸ و ۱۰ ثانیه فقط preset زمان هر سکانس هستند و می‌توانند تکرار شوند. بدون موزیک پس‌زمینه."
            textSize = 12f
            setTextColor(muted)
            setPadding(0, dp(7), 0, 0)
        })
    }.also { it.layoutParams = margin(16) }

    private fun field(title: String, hint: String, lines: Int) = EditText(this).apply {
        this.hint = "$title\n$hint"
        minLines = lines
        maxLines = 6
        textSize = 13f
        setTextColor(ink)
        setHintTextColor(muted)
        gravity = Gravity.TOP or Gravity.START
        background = rounded(Color.WHITE, 17, Color.rgb(225, 231, 238))
        setPadding(dp(13), dp(11), dp(13), dp(11))
    }

    private fun generate() {
        val n = niche.text.toString().trim()
        val d = description.text.toString().trim()
        if (n.length < 2 || d.isBlank()) {
            toast("حوزه کاری و توضیح کسب‌وکار را وارد کن")
            return
        }
        val selectedMode = if (modeSpinner.selectedItemPosition == 1) "viral_10s" else "cinematic"
        val body = JSONObject()
            .put("niche", n)
            .put("business_description", d)
            .put("audience", audience.text.toString().trim())
            .put("offer", offer.text.toString().trim())
            .put("production_constraints", constraints.text.toString().trim())
            .put("creative_preference", "خودکار؛ بیشینه‌سازی Retention؛ بدون موزیک پس‌زمینه")
            .put("mode", selectedMode)

        generate.isEnabled = false
        results.removeAllViews()
        status.text = if (selectedMode == "viral_10s") "مغز تیک‌آف در حال طراحی Visual Micro-Spectacle ۱۰ ثانیه‌ای و داوری ویروسی…" else "ساخت ایده‌ها، داوری قلاب و Retention، طراحی کاراکتر و Omni Prompt…"
        status.setTextColor(orange)

        Thread {
            val response = runCatching { post(body) }.getOrElse { 0 to it.message.orEmpty() }
            runOnUiThread {
                generate.isEnabled = true
                if (response.first in 200..299) {
                    runCatching { JSONObject(response.second) }.getOrNull()?.let(::render)
                        ?: showError("پاسخ سرور قابل خواندن نبود")
                } else if (response.first == 401) {
                    showError("اتصال امن دستگاه معتبر نیست؛ کلید اتصال را در برنامه اصلی بررسی کن")
                } else {
                    showError("ساخت ویدیو ناموفق بود (کد ${response.first})")
                }
            }
        }.start()
    }

    private fun post(body: JSONObject): Pair<Int, String> {
        val prefs = getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE)
        val endpoint = PayloadClient.viralEndpoint(prefs.getString("endpoint", "").orEmpty()).trimEnd('/')
        val key = SecretStore(this).get("api_key").orEmpty()
        val conn = URL("$endpoint/v4/ai-video-studio/generate").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = 20_000
        conn.readTimeout = 320_000
        conn.instanceFollowRedirects = false
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", "TakeOff-Insights/${BuildConfig.VERSION_NAME}")
        if (key.isNotBlank()) conn.setRequestProperty("X-Takeoff-Companion-Key", key)
        return try {
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            code to stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        } finally {
            conn.disconnect()
        }
    }

    private fun render(root: JSONObject) {
        results.removeAllViews()
        val isViral10 = root.optString("mode") == "viral_10s"
        status.text = if (isViral10) "بسته Visual Micro-Spectacle (۱۰ ثانیه‌ای) آماده است" else "بسته نهایی آماده است"
        status.setTextColor(teal)

        val video = root.optJSONObject("video") ?: root.optJSONArray("videos")?.optJSONObject(0) ?: root.optJSONObject("result")?.optJSONObject("video") ?: root
        val title = video.optString("title").ifBlank { video.optString("concept").ifBlank { "ویدیوی نهایی TakeOff" } }
        results.addView(card("سناریوی نهایی", title + "\n" + video.optString("summary").ifBlank { video.optString("concept") }))

        // Characters
        val chars = video.optJSONArray("characters") ?: root.optJSONArray("characters") ?: JSONArray()
        var firstCharPrompt = ""
        for (i in 0 until chars.length()) {
            val c = chars.optJSONObject(i) ?: continue
            val name = c.optString("name").ifBlank { "کاراکتر ${i + 1}" }
            val prompt = c.optString("character_sheet_prompt").ifBlank { c.optString("prompt") }
            if (firstCharPrompt.isBlank()) firstCharPrompt = prompt
            if (prompt.isNotBlank()) results.addView(copyCard("Character Sheet • $name", prompt, "کپی Character Sheet Prompt"))
        }

        // Micro-Spectacle Card if present
        val spec = video.optJSONObject("spectacle") ?: root.optJSONObject("spectacle")
        val omni10 = video.optString("omni_prompt_10s").ifBlank { root.optString("omni_prompt_10s") }
        if (spec != null || isViral10) {
            val arch = spec?.optString("archetype_name").orEmpty().ifBlank { spec?.optString("archetype").orEmpty() }
            val action = spec?.optString("spectacle_action").orEmpty()
            val contrast = spec?.optString("contrast_shift").orEmpty()
            val sound = spec?.optString("sound_design").orEmpty()
            val score = spec?.optDouble("viral_score", 0.0) ?: 0.0
            val specSummary = buildString {
                if (arch.isNotBlank()) append("الگوی جاذبه بصری: $arch\n")
                if (action.isNotBlank()) append("اکشن میکرو-اسپکتاکل: $action\n")
                if (contrast.isNotBlank()) append("تغییر تضاد حسی: $contrast\n")
                if (sound.isNotBlank()) append("طراحی صدا و Foley: $sound\n")
                if (score > 0.0) append("امتیاز ویروسی (داوری ۹ گانه): ${"%.1f".format(score)} / 10.0\n")
            }.trim()
            if (specSummary.isNotBlank()) {
                results.addView(card("طراحی Visual Micro-Spectacle (۱۰ ثانیه)", specSummary))
            }
            if (firstCharPrompt.isNotBlank()) {
                results.addView(copyCard("پرامپت کاراکتر شیت (Character Sheet)", firstCharPrompt, "کپی Character Sheet Prompt"))
            }
            val promptToCopy = if (omni10.isNotBlank()) omni10 else video.optString("omni_prompt")
            if (promptToCopy.isNotBlank()) {
                results.addView(copyCard("پرامپت جامع تولید ویدیو (Omni Prompt)", promptToCopy, "کپی Omni Prompt (۱۰ ثانیه‌ای)"))
            }
        }

        // Scenes
        val scenes = video.optJSONArray("scenes") ?: root.optJSONArray("scenes") ?: JSONArray()
        for (i in 0 until scenes.length()) {
            val s = scenes.optJSONObject(i) ?: continue
            val duration = s.optInt("duration_seconds", s.optInt("duration", 0))
            val dialogue = s.optString("dialogue").ifBlank { s.optString("spoken_dialogue") }
            val prompt = s.optString("omni_prompt").ifBlank { s.optString("prompt") }
            val header = "سکانس ${i + 1}${if (duration > 0) " • ${duration} ثانیه" else ""}"
            val meta = listOf(s.optString("purpose"), dialogue).filter { it.isNotBlank() }.joinToString("\n")
            if (meta.isNotBlank()) results.addView(card(header, meta))
            if (prompt.isNotBlank()) results.addView(copyCard("Omni Prompt • سکانس ${i + 1}", prompt, "کپی Omni Prompt این سکانس"))
        }

        if (scenes.length() == 0 && (spec == null || omni10.isBlank())) {
            results.addView(card("خروجی خام", video.toString(2).take(7000)))
        }
    }

    private fun card(title: String, body: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(15), dp(14), dp(15), dp(14))
        background = rounded(Color.WHITE, 20, Color.rgb(230, 234, 240))
        elevation = dp(2).toFloat()
        addView(TextView(this@AiVideoStudioActivity).apply {
            text = title
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink)
        })
        addView(TextView(this@AiVideoStudioActivity).apply {
            text = body
            textSize = 11.5f
            setTextColor(muted)
            setPadding(0, dp(7), 0, 0)
            setTextIsSelectable(true)
        })
    }.also { it.layoutParams = margin(10) }

    private fun copyCard(title: String, body: String, buttonLabel: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(15), dp(14), dp(15), dp(14))
        background = rounded(Color.WHITE, 20, Color.rgb(218, 242, 239))
        addView(TextView(this@AiVideoStudioActivity).apply {
            text = title
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(teal)
        })
        addView(TextView(this@AiVideoStudioActivity).apply {
            text = body
            textSize = 11f
            setTextColor(ink)
            setPadding(0, dp(7), 0, dp(8))
            setTextIsSelectable(true)
        })
        addView(Button(this@AiVideoStudioActivity).apply {
            text = buttonLabel
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = rounded(orange, 15, orange)
            setOnClickListener { copy(body) }
        }, LinearLayout.LayoutParams(-1, dp(48)))
    }.also { it.layoutParams = margin(10) }

    private fun copy(value: String) {
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("TakeOff", value))
        toast("کپی شد")
    }

    private fun showError(value: String) {
        status.text = value
        status.setTextColor(Color.rgb(200, 68, 61))
    }

    private fun toast(value: String) = Toast.makeText(this, value, Toast.LENGTH_SHORT).show()
    private fun rounded(fill: Int, radius: Int, stroke: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(fill)
        cornerRadius = dp(radius).toFloat()
        setStroke(dp(1), stroke)
    }
    private fun margin(bottom: Int) = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(bottom) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
