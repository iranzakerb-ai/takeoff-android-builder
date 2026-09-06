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

class ScenarioStudioActivity : Activity() {
    private val bg = Color.rgb(250, 252, 255)
    private val ink = Color.rgb(15, 23, 35)
    private val muted = Color.rgb(94, 108, 126)
    private val orange = Color.rgb(255, 122, 26)
    private val teal = Color.rgb(16, 202, 205)

    private lateinit var niche: EditText
    private lateinit var description: EditText
    private lateinit var audience: EditText
    private lateinit var modeSpinner: Spinner
    private lateinit var status: TextView
    private lateinit var results: LinearLayout
    private lateinit var button: Button

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
            text = "‹  استودیو سناریو"
            textSize = 27f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink)
            setOnClickListener { finish() }
        })
        root.addView(TextView(this).apply {
            text = "ساخت ۱۰ سناریوی آماده ضبط با مغز TakeOff"
            textSize = 12.5f
            setTextColor(muted)
            setPadding(0, dp(6), 0, dp(18))
        })

        niche = field("حوزه کاری *", "مثلاً باربری، خدمات خودرو، کلینیک", 2)
        description = field("توضیح کسب‌وکار *", "محصول، خدمات، مزیت‌ها و هر چیزی که می‌دانی", 4)
        audience = field("مخاطب هدف", "اختیاری؛ اگر خالی باشد تیک‌آف تشخیص می‌دهد", 2)
        listOf(niche, description, audience).forEach { root.addView(it, margin(10)) }

        root.addView(TextView(this).apply {
            text = "حالت ساخت سناریو:"
            textSize = 12.5f
            setTextColor(ink)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(4), dp(4), dp(4), dp(4))
        })
        modeSpinner = Spinner(this).apply {
            val modes = listOf("ساخت هوشمند سناریوهای وایرال", "ساخت ویدیوی کوتاه ۱۵ ثانیه‌ای")
            adapter = ArrayAdapter(this@ScenarioStudioActivity, android.R.layout.simple_spinner_dropdown_item, modes)
            setSelection(0)
            background = rounded(Color.WHITE, 16, Color.rgb(225, 231, 238))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        root.addView(modeSpinner, LinearLayout.LayoutParams(-1, dp(48)).apply { bottomMargin = dp(12) })

        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(15), dp(13), dp(15), dp(13))
            background = rounded(Color.WHITE, 20, Color.rgb(225, 232, 239))
            addView(TextView(this@ScenarioStudioActivity).apply {
                text = "قفل این بخش"
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(teal)
            })
            addView(TextView(this@ScenarioStudioActivity).apply {
                text = "این همان مسیر ۱۰ سناریوی انسانی/آماده ضبط است و از AI Video Studio جداست."
                textSize = 11.5f
                setTextColor(muted)
                setPadding(0, dp(6), 0, 0)
            })
        }, margin(12))

        button = Button(this).apply {
            text = "ساخت ۱۰ سناریوی آماده ضبط"
            isAllCaps = false
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = rounded(orange, 18, orange)
            setOnClickListener { request() }
        }
        root.addView(button, LinearLayout.LayoutParams(-1, dp(56)))

        status = TextView(this).apply {
            textSize = 12.5f
            setTextColor(muted)
            setPadding(0, dp(10), 0, dp(10))
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

    private fun field(title: String, hint: String, lines: Int) = EditText(this).apply {
        this.hint = "$title\n$hint"
        minLines = lines
        maxLines = 8
        textSize = 13f
        setTextColor(ink)
        setHintTextColor(muted)
        gravity = Gravity.TOP or Gravity.START
        background = rounded(Color.WHITE, 17, Color.rgb(225, 231, 238))
        setPadding(dp(13), dp(11), dp(13), dp(11))
    }

    private fun request() {
        val n = niche.text.toString().trim()
        val d = description.text.toString().trim()
        if (n.length < 2 || d.isBlank()) {
            toast("حوزه کاری و توضیحات را وارد کن")
            return
        }
        val selectedMode = if (modeSpinner.selectedItemPosition == 1) "short_15s" else "smart"
        val body = JSONObject()
            .put("niche", n)
            .put("business_description", d)
            .put("audience", audience.text.toString().trim())
            .put("count", 10)
            .put("mode", selectedMode)

        button.isEnabled = false
        results.removeAllViews()
        status.text = "مغز TakeOff در حال ساخت و داوری ۱۰ سناریو…"
        status.setTextColor(orange)

        Thread {
            val res = runCatching { post(body) }.getOrElse { 0 to it.message.orEmpty() }
            runOnUiThread {
                button.isEnabled = true
                if (res.first in 200..299) {
                    runCatching { JSONObject(res.second) }.getOrNull()?.let(::render)
                        ?: error("پاسخ قابل خواندن نبود")
                } else {
                    error("ساخت سناریو ناموفق بود (کد ${res.first})")
                }
            }
        }.start()
    }

    private fun post(body: JSONObject): Pair<Int, String> {
        val prefs = getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE)
        val endpoint = PayloadClient.viralEndpoint(prefs.getString("endpoint", "").orEmpty()).trimEnd('/')
        val key = SecretStore(this).get("api_key").orEmpty()
        val conn = URL("$endpoint/v4/scenario-studio/generate").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = 20_000
        conn.readTimeout = 260_000
        conn.instanceFollowRedirects = false
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.setRequestProperty("Accept", "application/json")
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
        val isShort15 = root.optString("mode") == "short_15s"
        status.text = if (isShort15) "۱۰ سناریوی تک‌صحنه ۱۵ ثانیه‌ای آماده است" else "۱۰ سناریو آماده است"
        status.setTextColor(teal)
        val arr = findArray(root)
        if (arr.length() == 0) {
            results.addView(card("خروجی", root.toString(2).take(10000), root.toString(2)))
            return
        }
        for (i in 0 until arr.length()) {
            val s = arr.optJSONObject(i) ?: continue
            val title = s.optString("title").ifBlank { s.optString("name").ifBlank { "سناریو ${i + 1}" } }
            val hook = s.optString("hook")
            val body = s.optString("scenario").ifBlank { s.optString("script").ifBlank { s.toString(2) } }
            val display = listOf(hook.takeIf { it.isNotBlank() }?.let { "قلاب: $it" }, body).filterNotNull().joinToString("\n\n")
            results.addView(card("${i + 1}. $title", display, s.toString(2)))
        }
    }

    private fun findArray(root: JSONObject): JSONArray =
        root.optJSONArray("scenarios") ?: root.optJSONObject("result")?.optJSONArray("scenarios") ?: root.optJSONObject("package")?.optJSONArray("scenarios") ?: JSONArray()

    private fun card(title: String, body: String, copyValue: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(15), dp(14), dp(15), dp(14))
        background = rounded(Color.WHITE, 20, Color.rgb(230, 234, 240))
        elevation = dp(2).toFloat()
        addView(TextView(this@ScenarioStudioActivity).apply {
            text = title
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink)
        })
        addView(TextView(this@ScenarioStudioActivity).apply {
            text = body.take(7000)
            textSize = 11.5f
            setTextColor(muted)
            setPadding(0, dp(7), 0, dp(8))
            setTextIsSelectable(true)
        })
        addView(Button(this@ScenarioStudioActivity).apply {
            text = "کپی سناریو"
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = rounded(orange, 14, orange)
            setOnClickListener { copy(copyValue) }
        }, LinearLayout.LayoutParams(-1, dp(46)))
    }.also { it.layoutParams = margin(10) }

    private fun copy(v: String) {
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("TakeOff Scenario", v))
        toast("کپی شد")
    }

    private fun error(v: String) {
        status.text = v
        status.setTextColor(Color.rgb(200, 68, 61))
    }

    private fun toast(v: String) = Toast.makeText(this, v, Toast.LENGTH_SHORT).show()
    private fun rounded(fill: Int, radius: Int, stroke: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(fill)
        cornerRadius = dp(radius).toFloat()
        setStroke(dp(1), stroke)
    }
    private fun margin(bottom: Int) = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(bottom) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
