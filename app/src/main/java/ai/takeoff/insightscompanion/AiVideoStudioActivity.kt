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

class AiVideoStudioActivity : Activity() {
    private val bg = Color.rgb(250, 252, 255)
    private val ink = Color.rgb(15, 23, 35)
    private val muted = Color.rgb(94, 108, 126)
    private val orange = Color.rgb(255, 122, 26)
    private val teal = Color.rgb(16, 202, 205)
    private val lightGray = Color.rgb(240, 244, 248)

    private lateinit var niche: EditText
    private lateinit var description: EditText
    private lateinit var audience: EditText
    private lateinit var offer: EditText
    private lateinit var constraints: EditText
    private lateinit var status: TextView
    private lateinit var results: LinearLayout
    private lateinit var generate: Button
    private lateinit var tabCinematic: TextView
    private lateinit var tabViral10s: TextView
    private lateinit var infoTitle: TextView
    private lateinit var infoBody: TextView
    private var selectedMode = "cinematic"

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
            text = "?  ???? ?????? AI"
            textSize = 27f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink)
            setOnClickListener { finish() }
        })

        root.addView(TextView(this).apply {
            text = "???????? ??? ?????? ??????? ? Character Sheet + ?????????? Omni"
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

        tabCinematic = TextView(this).apply {
            text = "???? ?????? AI ???????"
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setOnClickListener { setMode("cinematic") }
        }

        tabViral10s = TextView(this).apply {
            text = "?????? AI ?????? ?? ????????"
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setOnClickListener { setMode("viral_10s") }
        }

        tabsContainer.addView(tabCinematic, LinearLayout.LayoutParams(0, -2, 1f))
        tabsContainer.addView(tabViral10s, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(tabsContainer, margin(10))

        root.addView(infoCard())

        niche = field("???? ???? *", "????? ?????? ?? ??????? ?????", 2)
        description = field("????? ???????? *", "??? ????? ????? ??? ?????? ???? ?? ????????", 3)
        audience = field("????? ???", "???????", 2)
        offer = field("????? ?? ??????? ????", "???????", 2)
        constraints = field("??????? ?? ???? ???", "???????", 2)
        listOf(niche, description, audience, offer, constraints).forEach { root.addView(it, margin(10)) }

        generate = Button(this).apply {
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

        setMode("cinematic")
        scroll.addView(root)
        return scroll
    }

    private fun setMode(mode: String) {
        selectedMode = mode
        if (mode == "cinematic") {
            tabCinematic.background = rounded(Color.WHITE, 12, teal)
            tabCinematic.setTextColor(teal)
            tabViral10s.background = null
            tabViral10s.setTextColor(muted)
            infoTitle.text = "?????? ??????? Flow / Omni"
            infoBody.text = "???????? ?????? ???? ?? ????? ??????? ?? ?? ?? ? ? ?? ?????. ???? ?????? ?????."
            generate.text = "???? ?????? AI ??????? ?? Omni"
        } else {
            tabViral10s.background = rounded(Color.WHITE, 12, teal)
            tabViral10s.setTextColor(teal)
            tabCinematic.background = null
            tabCinematic.setTextColor(muted)
            infoTitle.text = "???? Visual Micro-Spectacle (?? ?????)"
            infoBody.text = "??????? ?????? ?? ???????? ?? ?? ????? ??????? ??????? ???? ??????? ??? ???? ? ??? ??????."
            generate.text = "???? ?????? AI ?????? ?? ???????? (Micro-Spectacle)"
        }
    }

    private fun infoCard() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = rounded(Color.WHITE, 22, Color.rgb(228, 233, 240))
        elevation = dp(3).toFloat()
        infoTitle = TextView(this@AiVideoStudioActivity).apply {
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(teal)
        }
        infoBody = TextView(this@AiVideoStudioActivity).apply {
            textSize = 11.5f
            setTextColor(muted)
            setPadding(0, dp(6), 0, 0)
        }
        addView(infoTitle)
        addView(infoBody)
    }.also { it.layoutParams = margin(12) }

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
            toast("???? ???? ? ????? ???????? ?? ???? ??")
            return
        }
        val body = JSONObject()
            .put("niche", n)
            .put("business_description", d)
            .put("audience", audience.text.toString().trim())
            .put("offer", offer.text.toString().trim())
            .put("production_constraints", constraints.text.toString().trim())
            .put("mode", selectedMode)
            .put("creative_preference", if (selectedMode == "viral_10s") "Visual Micro-Spectacle 10s" else "Cinematic Multi-Scene")

        generate.isEnabled = false
        results.removeAllViews()
        status.text = if (selectedMode == "viral_10s") "?? ??? ????? ?? ???? Micro-Spectacle? ?????? ?????? ?? ???????? ? Character Sheet?" else "???? ???????? ????? ???? ? Retention? ????? ??????? ? Omni Prompt?"
        status.setTextColor(orange)

        Thread {
            val response = runCatching { post(body) }.getOrElse { 0 to it.message.orEmpty() }
            runOnUiThread {
                generate.isEnabled = true
                if (response.first in 200..299) {
                    runCatching { JSONObject(response.second) }.getOrNull()?.let(::render) ?: showError("???? ???? ???? ?????? ????")
                } else if (response.first == 401) {
                    showError("????? ??? ?????? ????? ????? ???? ????? ?? ?? ?????? ???? ????? ??")
                } else {
                    showError("???? ????? ?????? ??? (?? ${response.first})")
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
        val video = root.optJSONObject("video") ?: root.optJSONArray("videos")?.optJSONObject(0) ?: root.optJSONObject("result")?.optJSONObject("video") ?: root
        val isSpectacle = video.optString("mode") == "viral_10s" || video.optString("content_type") == "AI Visual Micro-Spectacle" || selectedMode == "viral_10s"
        status.text = if (isSpectacle) "?????? AI ?????? ?? ???????? ????? ???" else "???? ????? ????? ???"
        status.setTextColor(teal)

        val title = video.optString("title").ifBlank { "?????? ????? TakeOff" }

        if (isSpectacle) {
            val archetype = video.optString("archetype")
            val anchor = video.optString("reality_anchor")
            val brokenRule = video.optString("broken_impossible_rule")
            val hook1s = video.optString("visual_hook_1s")
            val audioMode = video.optString("audio_mode")
            val loop = video.optString("loop_ending")
            val desc = listOf(
                "??????? ?????: $archetype",
                "???? ??????: $anchor",
                "????? ??????: $brokenRule",
                "???? ????? ???: $hook1s",
                "?? ????: $audioMode",
                "??? ??????: $loop"
            ).joinToString("\n")
            results.addView(card("? $title (???? ?? ????????)", desc))

            // Two primary copy cards for 10s spectacle
            val charPrompt = video.optString("character_sheet_prompt").ifBlank {
                video.optJSONArray("characters")?.optJSONObject(0)?.optString("character_sheet_prompt").orEmpty()
            }
            if (charPrompt.isNotBlank()) {
                results.addView(copyCard("Character Sheet Prompt (?????? ???? ???? ? ???? ???????)", charPrompt, "??? Character Sheet Prompt"))
            }

            val omniPrompt = video.optString("omni_prompt").ifBlank {
                video.optJSONArray("scenes")?.optJSONObject(0)?.optString("omni_prompt").orEmpty()
            }
            if (omniPrompt.isNotBlank()) {
                results.addView(copyCard("10s Omni Prompt (????? ???? ?? Flow / Omni)", omniPrompt, "??? 10s Omni Prompt"))
            }
            return
        }

        // Cinematic multi-scene rendering
        results.addView(card("??????? ?????", title + "\n" + video.optString("summary").ifBlank { video.optString("concept") }))
        val chars = video.optJSONArray("characters") ?: root.optJSONArray("characters") ?: JSONArray()
        for (i in 0 until chars.length()) {
            val c = chars.optJSONObject(i) ?: continue
            val name = c.optString("name").ifBlank { "??????? ${i + 1}" }
            val prompt = c.optString("character_sheet_prompt").ifBlank { c.optString("prompt") }
            if (prompt.isNotBlank()) results.addView(copyCard("Character Sheet ? $name", prompt, "??? Character Sheet Prompt"))
        }
        val scenes = video.optJSONArray("scenes") ?: root.optJSONArray("scenes") ?: JSONArray()
        for (i in 0 until scenes.length()) {
            val s = scenes.optJSONObject(i) ?: continue
            val duration = s.optInt("duration_seconds", s.optInt("duration", 0))
            val dialogue = s.optString("dialogue").ifBlank { s.optString("spoken_dialogue") }
            val prompt = s.optString("omni_prompt").ifBlank { s.optString("prompt") }
            val header = "????? ${i + 1}${if (duration > 0) " ? ${duration} ?????" else ""}"
            val meta = listOf(s.optString("purpose"), dialogue).filter { it.isNotBlank() }.joinToString("\n")
            if (meta.isNotBlank()) results.addView(card(header, meta))
            if (prompt.isNotBlank()) results.addView(copyCard("Omni Prompt ? ????? ${i + 1}", prompt, "??? Omni Prompt ??? ?????"))
        }
        if (scenes.length() == 0) results.addView(card("????? ???", video.toString(2).take(7000)))
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
        toast("??? ??")
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
