package ai.takeoff.insightscompanion

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import org.json.*
import java.net.HttpURLConnection
import java.net.URL

class ScenarioStudioActivity : Activity() {
    private lateinit var niche: EditText
    private lateinit var description: EditText
    private lateinit var audience: EditText
    private lateinit var offer: EditText
    private lateinit var constraints: EditText
    private lateinit var actors: Spinner
    private lateinit var mode: Spinner
    private lateinit var status: TextView
    private lateinit var results: LinearLayout
    private lateinit var generate: Button
    private var packageJson: JSONObject? = null

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        LovableUi.applyWindow(this)
        setContentView(buildUi())
        state?.getString("package")?.let {
            runCatching { JSONObject(it) }.getOrNull()?.also { j -> packageJson = j; renderPackage(j) }
        }
    }

    override fun onSaveInstanceState(out: Bundle) {
        packageJson?.let { out.putString("package", it.toString()) }
        super.onSaveInstanceState(out)
    }

    private fun buildUi(): View {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(LovableUi.background)
        }
        page.addView(LovableUi.run { topBar("استودیوی سناریو", "کسب‌وکارت رو توضیح بده؛ تیک‌آف سناریوش رو می‌سازه", back = { finish() }) })
        val scroll = ScrollView(this).apply { isFillViewport = true; overScrollMode = View.OVER_SCROLL_NEVER }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(LovableUi.run { dp(16) }, LovableUi.run { dp(16) }, LovableUi.run { dp(16) }, LovableUi.run { dp(32) })
        }

        root.addView(LovableUi.run { card(true) }.apply {
            addView(LovableUi.run { chip("✦ Scenario Studio V5", "primary") })
            addView(LovableUi.run { text("۱۰ سناریوی کامل، متمایز و آماده ضبط", 17f, LovableUi.foreground, true) }.apply { setPadding(0, LovableUi.run { dp(10) }, 0, 0) })
            addView(LovableUi.run { text("دو حالت داری: هوشمند چندسکانسه یا کوتاه ۱۵ ثانیه‌ای تک‌سکانسه. تیک‌آف قبل از ایده‌پردازی حافظه رفتاری همان حوزه را می‌خواند.", 12f, LovableUi.muted) }.apply { setPadding(0, LovableUi.run { dp(7) }, 0, 0) })
        }, LovableUi.run { margin(bottom = 20) })

        root.addView(LovableUi.run { sectionTitle("اطلاعات کسب‌وکار") }, LovableUi.run { margin(bottom = 10) })
        niche = field("حوزه کاری *", "مثلاً صافکاری PDR، کلینیک زیبایی یا کافه")
        description = field("توضیح کسب‌وکار *", "توضیح کوتاه مثل «اسباب کشی» هم قابل قبول است", 5)
        audience = field("مخاطب هدف", "مثلاً صاحبان خودرو ۲۵ تا ۴۵ سال")
        offer = field("محصول یا پیشنهاد اصلی", "چه چیزی باید معرفی، فروخته یا در ذهن مخاطب تثبیت شود؟")
        constraints = field("امکانات و محدودیت ضبط", "لوکیشن، ابزار، ممنوعیت‌ها، لحن برند و محدودیت بازیگر", 3)
        listOf(niche, description, audience, offer, constraints).forEach { root.addView(it) }

        root.addView(LovableUi.run { sectionTitle("حالت ساخت سناریو") }, LovableUi.run { margin(bottom = 8, top = 4) })
        mode = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@ScenarioStudioActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("ساخت هوشمند سناریوهای وایرال", "ویدیوی کوتاه ۱۵ ثانیه‌ای • تک‌سکانسه"),
            )
            setSelection(0)
            background = LovableUi.run { rounded(Color.WHITE, 18, LovableUi.border) }
            setPadding(LovableUi.run { dp(12) }, LovableUi.run { dp(8) }, LovableUi.run { dp(12) }, LovableUi.run { dp(8) })
        }
        root.addView(mode, LinearLayout.LayoutParams(-1, LovableUi.run { dp(50) }).apply { bottomMargin = LovableUi.run { dp(14) } })

        root.addView(LovableUi.run { sectionTitle("تعداد بازیگر در دسترس") }, LovableUi.run { margin(bottom = 8, top = 4) })
        actors = Spinner(this).apply {
            adapter = ArrayAdapter(this@ScenarioStudioActivity, android.R.layout.simple_spinner_dropdown_item, listOf("۱ نفر", "۲ نفر", "۳ نفر"))
            setSelection(1)
            background = LovableUi.run { rounded(Color.WHITE, 18, LovableUi.border) }
            setPadding(LovableUi.run { dp(12) }, LovableUi.run { dp(8) }, LovableUi.run { dp(12) }, LovableUi.run { dp(8) })
        }
        root.addView(actors, LinearLayout.LayoutParams(-1, LovableUi.run { dp(50) }).apply { bottomMargin = LovableUi.run { dp(14) } })

        root.addView(LovableUi.run { card() }.apply {
            val row = LinearLayout(this@ScenarioStudioActivity).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; gravity = Gravity.CENTER_VERTICAL }
            row.addView(LovableUi.run { text("قانون کارگردانی تیک‌آف", 12.5f, LovableUi.foreground, true) }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(LovableUi.run { chip("پویا", "secondary") })
            addView(row)
            addView(LovableUi.run { text("در حالت هوشمند، تعداد سکانس‌ها را روایت تعیین می‌کند. در حالت کوتاه، هر کدام از ۱۰ سناریو دقیقاً یک برداشت ۱۵ ثانیه‌ای است و تیک‌آف خودش تصمیم می‌گیرد دیالوگ لازم است یا فقط اکت، SFX و موسیقی.", 11.5f, LovableUi.muted) }.apply { setPadding(0, LovableUi.run { dp(8) }, 0, 0) })
        }, LovableUi.run { margin(bottom = 16) })

        generate = LovableUi.run { primaryButton("ساخت ۱۰ سناریوی آماده ضبط") { requestPackage() } }
        root.addView(generate, LinearLayout.LayoutParams(-1, LovableUi.run { dp(54) }))
        status = LovableUi.run { text("", 12.5f, LovableUi.muted) }.apply { setPadding(0, LovableUi.run { dp(12) }, 0, LovableUi.run { dp(4) }) }
        root.addView(status)
        results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        root.addView(results)

        scroll.addView(root)
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        return page
    }

    private fun field(title: String, hint: String, lines: Int = 1) = EditText(this).apply {
        this.hint = "$title\n$hint"
        setHintTextColor(LovableUi.muted)
        setTextColor(LovableUi.foreground)
        textSize = 13f
        gravity = Gravity.TOP or Gravity.START
        minLines = lines.coerceAtLeast(2)
        maxLines = maxOf(lines, 7)
        background = LovableUi.run { rounded(Color.WHITE, 18, LovableUi.border) }
        setPadding(LovableUi.run { dp(13) }, LovableUi.run { dp(11) }, LovableUi.run { dp(13) }, LovableUi.run { dp(11) })
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = LovableUi.run { dp(12) } }
    }

    private fun requestPackage() {
        val n = niche.text.toString().trim()
        val d = description.text.toString().trim()
        if (n.length < 2 || d.length < 2) { showError("حوزه و توضیح کسب‌وکار را وارد کن."); return }
        val selectedMode = if (mode.selectedItemPosition == 1) "short_15s" else "smart"
        generate.isEnabled = false
        status.text = if (selectedMode == "short_15s")
            "حافظه تیک‌آف ← طراحی ۱۰ ایده تک‌سکانسه ۱۵ ثانیه‌ای ← انتخاب دیالوگ/اکت و صدا ← داوری Retention…"
        else
            "شناخت کسب‌وکار ← بررسی حافظه تیک‌آف ← طراحی قلاب‌ها ← ساخت سناریوها ← داوری تنوع و Retention…"
        status.setTextColor(LovableUi.primary)
        results.removeAllViews()
        // Production backend v0.19.0 historically required 10 chars. Padding only
        // protects older deployments; the semantic text remains unchanged after trim.
        val transportDescription = if (d.length < 10) d.padEnd(10, ' ') else d
        val body = JSONObject().apply {
            put("niche", n)
            put("business_description", transportDescription)
            put("audience", audience.text.toString().trim())
            put("offer", offer.text.toString().trim())
            put("production_constraints", constraints.text.toString().trim())
            put("actors_available", actors.selectedItemPosition + 1)
            put("mode", selectedMode)
        }
        Thread {
            val response = runCatching { post(body) }.getOrElse { 0 to "" }
            runOnUiThread {
                generate.isEnabled = true
                if (response.first in 200..299) {
                    runCatching { JSONObject(response.second) }.getOrNull()?.let {
                        packageJson = it
                        val prefs = getSharedPreferences("takeoff_scenario_stats", Context.MODE_PRIVATE)
                        prefs.edit().putInt("generated", prefs.getInt("generated", 0) + it.optJSONArray("scenarios")?.length().orZero()).apply()
                        renderPackage(it)
                    } ?: showError("پاسخ سرور قابل خواندن نبود.")
                } else if (response.first == 422) {
                    showError("اطلاعات ورودی با قرارداد سرور هماهنگ نبود؛ دوباره تلاش کن.")
                } else {
                    showError(if (response.first == 401) "اتصال امن دستگاه را در کنسول تنظیم کن." else "بسته کامل تأیید نشد؛ خروجی ناقص تحویل داده نشد. (کد ${response.first})")
                }
            }
        }.start()
    }

    private fun Int?.orZero() = this ?: 0

    private fun post(body: JSONObject): Pair<Int, String> {
        val prefs = getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE)
        val endpoints = PayloadClient.candidateEndpoints(prefs.getString("endpoint", "").orEmpty())
        val key = SecretStore(this).get("api_key").orEmpty()
        var last: Pair<Int, String> = 0 to ""
        for (ep in endpoints) {
            val conn = URL(ep.trimEnd('/') + "/v4/scenario-studio/generate").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 20_000
            conn.readTimeout = 285_000
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "TakeOff-Insights/" + BuildConfig.VERSION_NAME)
            if (key.isNotBlank()) conn.setRequestProperty("X-Takeoff-Companion-Key", key)
            val res = try {
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                code to stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            } catch (_: Exception) {
                0 to ""
            } finally { conn.disconnect() }
            if (res.first in 200..299) return res
            last = res
            if (res.first != 404 && res.first !in 502..504) return res
        }
        return last
    }

    private fun renderPackage(root: JSONObject) {
        val items = root.optJSONArray("scenarios") ?: JSONArray()
        val isShort = root.optString("mode") == "short_15s"
        status.text = if (isShort)
            "بسته ${LovableUi.fa(items.length())} سناریوی تک‌سکانسه آماده است • هر سناریو ۱۵ ثانیه"
        else
            "بسته ${LovableUi.fa(items.length())} سناریویی آماده است • شواهد حافظه: ${LovableUi.fa(root.optInt("memory_evidence_count"))}"
        status.setTextColor(LovableUi.success)
        results.removeAllViews()
        results.addView(LovableUi.run { primaryButton("خروجی PDF فارسی همه سناریوها") {
            startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                type = "application/pdf"
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_TITLE, "TakeOff-Scenario-Studio.pdf")
            }, PDF_REQUEST)
        } }, LinearLayout.LayoutParams(-1, LovableUi.run { dp(52) }).apply { topMargin = LovableUi.run { dp(8) }; bottomMargin = LovableUi.run { dp(14) } })

        for (i in 0 until items.length()) {
            val s = items.optJSONObject(i) ?: continue
            val scenes = s.optJSONArray("scenes") ?: JSONArray()
            val card = LovableUi.run { card() }.apply {
                val top = LinearLayout(this@ScenarioStudioActivity).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; gravity = Gravity.CENTER_VERTICAL }
                top.addView(LovableUi.run { chip("#${LovableUi.fa(s.optInt("rank", i + 1))}", "primary") })
                top.addView(LovableUi.run { text(s.optString("title"), 14f, LovableUi.foreground, true) }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = LovableUi.run { dp(8) }; marginEnd = LovableUi.run { dp(8) } })
                top.addView(LovableUi.run { chip("${LovableUi.fa(s.optInt("viral_potential_score"))}/۱۰۰", "secondary") })
                addView(top)
                val meta = "${LovableUi.fa(s.optInt("total_duration_seconds"))} ثانیه • ${LovableUi.fa(scenes.length())} سکانس • ${LovableUi.fa(s.optInt("actor_count"))} بازیگر"
                addView(LovableUi.run { text(meta, 11f, LovableUi.primary, true) }.apply { setPadding(0, LovableUi.run { dp(8) }, 0, 0) })
                val hook = s.optJSONObject("hook")
                addView(LovableUi.run { text("قلاب: ${hook?.optString("spoken").orEmpty()}\nپایان: ${s.optString("payoff")}\nCTA: ${s.optString("cta")}", 12f, LovableUi.foreground) }.apply { setPadding(0, LovableUi.run { dp(8) }, 0, LovableUi.run { dp(10) }) })
                addView(LovableUi.run { ghostButton("نمایش جزئیات همه سکانس‌ها") { showScenario(s) } }, LinearLayout.LayoutParams(-1, LovableUi.run { dp(46) }))
            }
            results.addView(card, LovableUi.run { margin(bottom = 10) })
        }
    }

    private fun showScenario(s: JSONObject) {
        val scenes = s.optJSONArray("scenes") ?: JSONArray()
        val host = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(LovableUi.run { dp(16) }, LovableUi.run { dp(8) }, LovableUi.run { dp(16) }, LovableUi.run { dp(8) }) }
        val hook = s.optJSONObject("hook")
        host.addView(LovableUi.run { chip("قلاب سه ثانیه اول", "primary") })
        host.addView(LovableUi.run { text(hook?.optString("spoken").orEmpty(), 13f, LovableUi.foreground, true) }.apply { setPadding(0, LovableUi.run { dp(8) }, 0, LovableUi.run { dp(12) }) })
        for (i in 0 until scenes.length()) {
            val x = scenes.optJSONObject(i) ?: continue
            host.addView(LovableUi.run { card() }.apply {
                addView(LovableUi.run { text("سکانس ${LovableUi.fa(i + 1)} • ${x.optDouble("start_seconds")} تا ${x.optDouble("end_seconds")} ثانیه", 12f, LovableUi.primary, true) })
                addView(LovableUi.run { text("هدف: ${x.optString("purpose")}\nتصویر: ${x.optString("action")}\nدیالوگ: ${x.optString("dialogue").ifBlank { "بدون دیالوگ" }}\nدوربین: ${x.optString("camera")}\nتدوین: ${x.optString("edit_transition")}", 11.5f, LovableUi.foreground) }.apply { setPadding(0, LovableUi.run { dp(7) }, 0, 0) })
            }, LovableUi.run { margin(bottom = 8) })
        }
        val scroll = ScrollView(this).apply { addView(host) }
        android.app.AlertDialog.Builder(this).setTitle(s.optString("title")).setView(scroll).setPositiveButton("بستن", null).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PDF_REQUEST && resultCode == RESULT_OK) data?.data?.let { writePdf(it) }
    }

    private fun writePdf(uri: Uri) {
        val root = packageJson ?: return
        runCatching {
            val pdf = PdfDocument()
            val items = root.optJSONArray("scenarios") ?: JSONArray()
            var pageNo = 1
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(43, 36, 31); textSize = 11.5f; typeface = Typeface.create("sans-serif", Typeface.NORMAL); textAlign = Paint.Align.RIGHT }
            val strongPaint = Paint(bodyPaint).apply { typeface = Typeface.create("sans-serif", Typeface.BOLD); color = Color.rgb(31, 38, 46) }
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 19f; typeface = Typeface.create("sans-serif", Typeface.BOLD); textAlign = Paint.Align.RIGHT }
            fun newPage(title: String): Pair<PdfDocument.Page, Float> {
                val info = PdfDocument.PageInfo.Builder(595, 842, pageNo).create()
                val page = pdf.startPage(info)
                val c = page.canvas
                c.drawColor(Color.WHITE)
                val header = Paint().apply { shader = LinearGradient(0f, 0f, 595f, 72f, Color.rgb(255, 126, 46), LovableUi.primaryDeep, Shader.TileMode.CLAMP) }
                c.drawRect(0f, 0f, 595f, 76f, header)
                c.drawText(title, 555f, 45f, titlePaint)
                val footer = Paint(bodyPaint).apply { color = Color.rgb(105, 119, 136); textSize = 9f; textAlign = Paint.Align.CENTER }
                c.drawText("TakeOff Scenario Studio • صفحه $pageNo", 297.5f, 826f, footer)
                pageNo++
                return page to 104f
            }
            fun writeScenario(title: String, lines: List<Pair<String, Boolean>>) {
                var pair = newPage(title)
                var page = pair.first
                var y = pair.second
                for ((raw, bold) in lines) {
                    val wrapped = wrap(raw, 78).ifEmpty { listOf(" ") }
                    for (line in wrapped) {
                        if (y > 790f) { pdf.finishPage(page); pair = newPage("$title • ادامه"); page = pair.first; y = pair.second }
                        page.canvas.drawText(line, 555f, y, if (bold) strongPaint else bodyPaint)
                        y += if (bold) 20f else 17f
                    }
                    y += if (bold) 4f else 2f
                }
                pdf.finishPage(page)
            }
            writeScenario("استودیو سناریو تیک‌آف", listOf(
                "بسته کامل آماده ضبط" to true,
                ("حوزه: " + root.optJSONObject("brief")?.optString("niche").orEmpty()) to false,
                ("تعداد سناریو: " + items.length()) to false,
                ("شواهد حافظه استفاده‌شده: " + root.optInt("memory_evidence_count")) to false,
                root.optString("scientific_notice") to false,
            ))
            for (i in 0 until items.length()) {
                val s = items.getJSONObject(i)
                val hook = s.optJSONObject("hook") ?: JSONObject()
                val lines = mutableListOf<Pair<String, Boolean>>()
                lines += "مشخصات اجرا" to true
                lines += ("مدت: ${s.optInt("total_duration_seconds")} ثانیه | بازیگر: ${s.optInt("actor_count")} | سکانس: ${s.optInt("scene_count")} | امتیاز خلاقه: ${s.optInt("viral_potential_score")}/100") to false
                lines += ("فرمت: " + s.optString("format_family")) to false
                lines += ("نقش‌ها: " + jsonArrayText(s.optJSONArray("actor_roles"))) to false
                lines += ("دلیل تعداد بازیگر: " + s.optString("actor_justification")) to false
                lines += "ایده و قلاب" to true
                lines += ("ایده: " + s.optString("core_idea")) to false
                lines += ("قلاب گفتاری: " + hook.optString("spoken")) to false
                lines += ("قلاب تصویری: " + hook.optString("visual")) to false
                lines += ("متن روی تصویر: " + hook.optString("onscreen_text")) to false
                lines += ("معماری نگهداشت: " + s.optString("retention_architecture")) to false
                val scenes = s.optJSONArray("scenes") ?: JSONArray()
                for (j in 0 until scenes.length()) {
                    val x = scenes.getJSONObject(j)
                    lines += ("سکانس ${j + 1} • ${x.optDouble("start_seconds")} تا ${x.optDouble("end_seconds")} ثانیه") to true
                    lines += ("هدف: " + x.optString("purpose")) to false
                    lines += ("تصویر/اکشن: " + x.optString("action")) to false
                    lines += ("دیالوگ: " + x.optString("dialogue").ifBlank { "بدون دیالوگ" }) to false
                    lines += ("شات و دوربین: " + x.optString("shot") + " | " + x.optString("camera")) to false
                    lines += ("متن/صدا/کات: " + x.optString("onscreen_text") + " | " + x.optString("audio_sfx") + " | " + x.optString("edit_transition")) to false
                }
                lines += "پایان و کنترل ضبط" to true
                lines += ("Payoff: " + s.optString("payoff")) to false
                lines += ("CTA: " + s.optString("cta")) to false
                lines += ("ریسک ضبط: " + s.optString("production_risks")) to false
                lines += ("پلن جایگزین: " + s.optString("fallback_plan")) to false
                writeScenario(s.optInt("rank").toString() + " • " + s.optString("title"), lines)
            }
            contentResolver.openOutputStream(uri)?.use { pdf.writeTo(it) } ?: error("output")
            pdf.close()
        }.onSuccess { Toast.makeText(this, "PDF کامل فارسی ذخیره شد.", Toast.LENGTH_LONG).show() }
            .onFailure { Toast.makeText(this, "ذخیره PDF انجام نشد.", Toast.LENGTH_LONG).show() }
    }

    private fun jsonArrayText(array: JSONArray?): String {
        if (array == null) return ""
        val out = mutableListOf<String>()
        for (i in 0 until array.length()) out += array.optString(i)
        return out.filter { it.isNotBlank() }.joinToString("، ")
    }

    private fun wrap(value: String, max: Int): List<String> {
        val words = value.replace("\n", " ").split(Regex("\\s+"))
        val out = mutableListOf<String>()
        var line = ""
        for (word in words) {
            if ((line + " " + word).trim().length > max && line.isNotBlank()) { out += line; line = word }
            else line = (line + " " + word).trim()
        }
        if (line.isNotBlank()) out += line
        return out
    }

    private fun showError(value: String) {
        status.text = value
        status.setTextColor(LovableUi.danger)
    }

    companion object { private const val PDF_REQUEST = 901 }
}
