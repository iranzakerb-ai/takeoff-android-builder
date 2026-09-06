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
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
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
    private val lightTeal = Color.rgb(228, 246, 246)
    private val lightGray = Color.rgb(240, 244, 248)

    private lateinit var niche: EditText
    private lateinit var description: EditText
    private lateinit var audience: EditText
    private lateinit var status: TextView
    private lateinit var results: LinearLayout
    private lateinit var button: Button
    private lateinit var tabSmart: TextView
    private lateinit var tabShort15s: TextView
    private lateinit var modeDescription: TextView
    private var selectedMode = "smart"

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
            text = "?  ??????? ??????"
            textSize = 27f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink)
            setOnClickListener { finish() }
        })

        root.addView(TextView(this).apply {
            text = "???? ?? ??????? ????? ??? ?? ??? TakeOff"
            textSize = 12.5f
            setTextColor(muted)
            setPadding(0, dp(6), 0, dp(14))
        })

        // 2-Mode Selector Tab Bar
        val tabsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(4), dp(4), dp(4), dp(4))
            background = rounded(lightGray, 14, Color.rgb(225, 232, 240))
        }

        tabSmart = TextView(this).apply {
            text = "???? ?????? ????????? ??????"
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setOnClickListener { setMode("smart") }
        }

        tabShort15s = TextView(this).apply {
            text = "???? ?????? ????? ?? ????????"
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setOnClickListener { setMode("short_15s") }
        }

        tabsContainer.addView(tabSmart, LinearLayout.LayoutParams(0, -2, 1f))
        tabsContainer.addView(tabShort15s, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(tabsContainer, margin(10))

        modeDescription = TextView(this).apply {
            textSize = 11.5f
            setTextColor(muted)
            setPadding(dp(4), 0, dp(4), dp(12))
        }
        root.addView(modeDescription)

        niche = field("???? ???? *", "????? ??????? ????? ?????? ??????", 2)
        description = field("????? ???????? *", "?????? ?????? ??????? ? ?? ???? ?? ???????", 4)
        audience = field("????? ???", "???????? ??? ???? ???? ?????? ????? ??????", 2)
        listOf(niche, description, audience).forEach { root.addView(it, margin(10)) }

        button = Button(this).apply {
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

        setMode("smart")
        scroll.addView(root)
        return scroll
    }

    private fun setMode(mode: String) {
        selectedMode = mode
        if (mode == "smart") {
            tabSmart.background = rounded(Color.WHITE, 12, teal)
            tabSmart.setTextColor(teal)
            tabShort15s.background = null
            tabShort15s.setTextColor(muted)
            modeDescription.text = "???? ??????: ????????? ?????????? ???????? (? ?? ?? ????) ?? ??? ?? ?? ?? ?????."
            button.text = "???? ?? ??????? ?????? ??????"
        } else {
            tabShort15s.background = rounded(Color.WHITE, 12, teal)
            tabShort15s.setTextColor(teal)
            tabSmart.background = null
            tabSmart.setTextColor(muted)
            modeDescription.text = "???? ?? ????????: ?? ?????? ??????? ?? ???? ??? ?? ????? (?????? ?????? ?? SFX)."
            button.text = "???? ?? ?????? ????? ?? ????????"
        }
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
            toast("???? ???? ? ??????? ?? ???? ??")
            return
        }
        val body = JSONObject()
            .put("niche", n)
            .put("business_description", d)
            .put("audience", audience.text.toString().trim())
            .put("count", 10)
            .put("mode", selectedMode)

        button.isEnabled = false
        results.removeAllViews()
        status.text = if (selectedMode == "short_15s") "??? TakeOff ?? ??? ????? ?? ?????? ????? ?? ?????????" else "??? TakeOff ?? ??? ???? ? ????? ?? ???????"
        status.setTextColor(orange)

        Thread {
            val res = runCatching { post(body) }.getOrElse { 0 to it.message.orEmpty() }
            runOnUiThread {
                button.isEnabled = true
                if (res.first in 200..299) {
                    runCatching { JSONObject(res.second) }.getOrNull()?.let(::render) ?: error("???? ???? ?????? ????")
                } else {
                    error("???? ?????? ?????? ??? (?? ${res.first})")
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
        status.text = if (selectedMode == "short_15s") "?? ?????? ????? ?? ???????? ????? ???" else "?? ?????? ????? ???"
        status.setTextColor(teal)
        val arr = findArray(root)
        if (arr.length() == 0) {
            results.addView(card("?????", root.toString(2).take(10000), root.toString(2)))
            return
        }
        for (i in 0 until arr.length()) {
            val s = arr.optJSONObject(i) ?: continue
            val title = s.optString("title").ifBlank { s.optString("name").ifBlank { "?????? ${i + 1}" } }
            val hook = s.optString("hook")
            val body = s.optString("scenario").ifBlank { s.optString("script").ifBlank { s.toString(2) } }
            val display = listOf(hook.takeIf { it.isNotBlank() }?.let { "????: $it" }, body).filterNotNull().joinToString("\n\n")
            results.addView(card("${i + 1}. $title", display, s.toString(2)))
        }
    }

    private fun findArray(root: JSONObject): JSONArray =
        root.optJSONArray("scenarios")
            ?: root.optJSONObject("result")?.optJSONArray("scenarios")
            ?: root.optJSONObject("package")?.optJSONArray("scenarios")
            ?: JSONArray()

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
            text = "??? ??????"
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = rounded(orange, 14, orange)
            setOnClickListener { copy(copyValue) }
        }, LinearLayout.LayoutParams(-1, dp(46)))
    }.also { it.layoutParams = margin(10) }

    private fun copy(v: String) {
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("TakeOff Scenario", v))
        toast("??? ??")
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
