package ai.takeoff.insightscompanion

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import org.json.*
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

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
    private var activeTaskId: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val pollRunnable = object : Runnable {
        override fun run() {
            val taskId = activeTaskId ?: return
            val entry = StudioResultStore(this@ScenarioStudioActivity).get(taskId)
            if (entry != null) {
                if (entry.status == "completed" && !entry.resultJson.isNullOrBlank()) {
                    generate.isEnabled = true
                    runCatching { JSONObject(entry.resultJson!!) }.getOrNull()?.let {
                        packageJson = it
                        renderPackage(it)
                    }
                    return
                } else if (entry.status == "failed") {
                    generate.isEnabled = true
                    showError(entry.errorMessage ?: "فرآیند تولید سناریوها متوقف شد.")
                    return
                }
            }
            mainHandler.postDelayed(this, 2000)
        }
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        LovableUi.applyWindow(this)
        setContentView(buildUi())

        val fromTaskId = intent.getStringExtra("selected_task_id")
        if (!fromTaskId.isNullOrBlank()) {
            StudioResultStore(this).get(fromTaskId)?.resultJson?.let {
                runCatching { JSONObject(it) }.getOrNull()?.also { j -> packageJson = j; renderPackage(j) }
            }
        } else {
            state?.getString("package")?.let {
                runCatching { JSONObject(it) }.getOrNull()?.also { j -> packageJson = j; renderPackage(j) }
            }
        }
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(pollRunnable)
        super.onDestroy()
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
            val topRow = LinearLayout(this@ScenarioStudioActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                gravity = Gravity.CENTER_VERTICAL
            }
            topRow.addView(LovableUi.run { chip("✦ Scenario Studio V5", "primary") })
            topRow.addView(View(this@ScenarioStudioActivity), LinearLayout.LayoutParams(0, 1, 1f))
            topRow.addView(LovableUi.run { ghostButton("📂 آرشیو سناریوها") { showArchiveDialog() } }, LinearLayout.LayoutParams(-2, LovableUi.run { dp(36) }))
            addView(topRow)

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
                listOf(
                    "هوشمند چندسکانسه • با دیالوگ (روایت پویا)",
                    "هوشمند چندسکانسه • بدون دیالوگ (اکت تصویری و SFX)",
                    "ویدیوی کوتاه ۱۵ ثانیه‌ای • با دیالوگ (تک‌سکانسه)",
                    "ویدیوی کوتاه ۱۵ ثانیه‌ای • بدون دیالوگ (تک‌سکانسه وایرال)",
                ),
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
            addView(LovableUi.run { text("تعداد سکانس هر سناریو بر اساس نیاز روایت تعیین می‌شود. در حالت هوشمند، تعداد سکانس‌ها را روایت تعیین می‌کند. در حالت کوتاه، هر کدام از ۱۰ سناریو دقیقاً یک برداشت ۱۵ ثانیه‌ای است و تیک‌آف خودش تصمیم می‌گیرد دیالوگ لازم است یا فقط اکت، SFX و موسیقی.", 11.5f, LovableUi.muted) }.apply { setPadding(0, LovableUi.run { dp(8) }, 0, 0) })
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
        setHintTextColor(Color.rgb(107, 114, 128))
        setTextColor(Color.rgb(17, 24, 39))
        textSize = 13.5f
        gravity = Gravity.TOP or Gravity.START
        minLines = lines.coerceAtLeast(2)
        maxLines = maxOf(lines, 7)
        background = LovableUi.run { rounded(Color.WHITE, 18, LovableUi.border) }
        setPadding(LovableUi.run { dp(13) }, LovableUi.run { dp(11) }, LovableUi.run { dp(13) }, LovableUi.run { dp(11) })
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = LovableUi.run { dp(12) } }
    }

    private fun showArchiveDialog() {
        val list = StudioResultStore(this).getByType("scenario").filter { it.status == "completed" }
        if (list.isEmpty()) {
            Toast.makeText(this, "هنوز سناریوی ذخیره‌شده‌ای وجود ندارد.", Toast.LENGTH_SHORT).show()
            return
        }
        val titles = list.map { "${it.niche} (${it.mode}) • ${SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(it.createdAt))}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("آرشیو سناریوهای ذخیره‌شده")
            .setItems(titles) { _, which ->
                val selected = list[which]
                selected.resultJson?.let {
                    runCatching {
                        val obj = JSONObject(it)
                        packageJson = obj
                        renderPackage(obj)
                        Toast.makeText(this, "سناریوهای «${selected.niche}» بارگذاری شد.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("بستن", null)
            .show()
    }

    private fun requestPackage() {
        val n = niche.text.toString().trim()
        val d = description.text.toString().trim()
        if (n.length < 2 || d.length < 2) { showError("حوزه و توضیح کسب‌وکار را وارد کن."); return }
        val selectedMode = when (mode.selectedItemPosition) {
            1 -> "smart_silent"
            2 -> "short_15s"
            3 -> "short_15s_silent"
            else -> "smart"
        }
        val selectedActors = actors.selectedItemPosition + 1
        generate.isEnabled = false
        status.text = "در حال تولید در پس‌زمینه... می‌توانید از اپ خارج شوید، نتیجه ذخیره و اعلان داده خواهد شد."
        status.setTextColor(LovableUi.primary)
        results.removeAllViews()

        val taskId = UUID.randomUUID().toString()
        activeTaskId = taskId

        val entry = StudioEntry(
            id = taskId,
            type = "scenario",
            mode = selectedMode,
            title = n,
            niche = n,
            description = d,
            targetAudience = audience.text.toString().trim(),
            mainOffer = offer.text.toString().trim(),
            constraints = constraints.text.toString().trim(),
            actorCount = selectedActors,
            status = "processing",
        )
        StudioResultStore(this).save(entry)
        StudioTaskWork.enqueue(this, taskId)
        mainHandler.postDelayed(pollRunnable, 2000)

        val transportDescription = if (d.length < 10) d.padEnd(10, ' ') else d
        val body = JSONObject().apply {
            put("niche", n)
            put("business_description", transportDescription)
            put("audience", audience.text.toString().trim())
            put("offer", offer.text.toString().trim())
            put("production_constraints", constraints.text.toString().trim())
            put("actors_available", selectedActors)
            put("mode", selectedMode)
        }

        Thread {
            val response = runCatching { post(body) }.getOrElse { 0 to "" }
            runOnUiThread {
                if (response.first in 200..299) {
                    runCatching { JSONObject(response.second) }.getOrNull()?.let {
                        packageJson = it
                        StudioResultStore(this@ScenarioStudioActivity).update(taskId) { current ->
                            current.copy(status = "completed", resultJson = it.toString(), errorMessage = null)
                        }
                        generate.isEnabled = true
                        mainHandler.removeCallbacks(pollRunnable)
                        val prefs = getSharedPreferences("takeoff_scenario_stats", Context.MODE_PRIVATE)
                        prefs.edit().putInt("generated", prefs.getInt("generated", 0) + it.optJSONArray("scenarios")?.length().orZero()).apply()
                        renderPackage(it)
                    }
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
        val isShort = root.optString("mode") == "short_15s" || root.optString("mode") == "short_15s_silent"
        status.text = if (isShort)
            "بسته ${LovableUi.fa(items.length())} سناریوی تک‌سکانسه آماده است • هر سناریو ۱۵ ثانیه"
        else
            "بسته ${LovableUi.fa(items.length())} سناریویی آماده است • شواهد حافظه: ${LovableUi.fa(root.optInt("memory_evidence_count"))}"
        status.setTextColor(LovableUi.success)
        results.removeAllViews()

        val pdfActionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val exportPdfBtn = LovableUi.run { primaryButton("دانلود و باز کردن PDF فارسی") {
            exportPdfDirectly(root, shareAfter = false)
        } }
        val sharePdfBtn = LovableUi.run { ghostButton("اشتراک‌گذاری PDF") {
            exportPdfDirectly(root, shareAfter = true)
        } }
        pdfActionRow.addView(exportPdfBtn, LinearLayout.LayoutParams(0, LovableUi.run { dp(50) }, 1.3f).apply { marginEnd = LovableUi.run { dp(5) } })
        pdfActionRow.addView(sharePdfBtn, LinearLayout.LayoutParams(0, LovableUi.run { dp(50) }, 1.0f).apply { marginStart = LovableUi.run { dp(5) } })
        results.addView(pdfActionRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = LovableUi.run { dp(8) }; bottomMargin = LovableUi.run { dp(14) } })

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
        AlertDialog.Builder(this).setTitle(s.optString("title")).setView(scroll).setPositiveButton("بستن", null).show()
    }

    private fun exportPdfDirectly(root: JSONObject, shareAfter: Boolean) {
        Toast.makeText(this, "در حال ایجاد PDF فارسی با کیفیت بالا…", Toast.LENGTH_SHORT).show()
        Thread {
            val result = runCatching { TakeoffPdfExporter.exportScenarioStudio(this, root) }.getOrNull()
            runOnUiThread {
                val cacheFile = result?.second
                if (cacheFile != null && cacheFile.exists()) {
                    Toast.makeText(this, "PDF در پوشه Downloads/TakeOff ذخیره شد.", Toast.LENGTH_LONG).show()
                    TakeoffPdfExporter.shareOrViewPdf(this, cacheFile, "۱۰ سناریوی تیک‌آف")
                } else {
                    Toast.makeText(this, "ذخیره PDF انجام نشد.", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun showError(value: String) {
        status.text = value
        status.setTextColor(LovableUi.danger)
    }

    companion object {
        private const val PDF_REQUEST = 901
        private const val DEFAULT_PDF_NAME = "TakeOff-Scenario-Studio.pdf"
    }
}
