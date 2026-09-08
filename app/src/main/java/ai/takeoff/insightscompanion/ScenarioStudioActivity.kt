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
import java.io.File
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
    private lateinit var formContainer: LinearLayout
    private lateinit var toggleFormBtn: Button
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
                    formContainer.visibility = View.GONE
                    toggleFormBtn.visibility = View.VISIBLE
                    toggleFormBtn.text = "➕ نمایش فرم ساخت سناریوی جدید"
                    runCatching { JSONObject(entry.resultJson!!) }.getOrNull()?.also { j ->
                        packageJson = j
                        renderPackage(j)
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
            activeTaskId = fromTaskId
            val entry = StudioResultStore(this).get(fromTaskId)
            if (entry != null) {
                if (entry.status == "completed" && !entry.resultJson.isNullOrBlank()) {
                    formContainer.visibility = View.GONE
                    toggleFormBtn.visibility = View.VISIBLE
                    toggleFormBtn.text = "➕ نمایش فرم ساخت سناریوی جدید"
                    runCatching { JSONObject(entry.resultJson!!) }.getOrNull()?.also { j ->
                        packageJson = j
                        renderPackage(j)
                    }
                } else if (entry.status == "processing") {
                    formContainer.visibility = View.GONE
                    toggleFormBtn.visibility = View.VISIBLE
                    toggleFormBtn.text = "➕ نمایش فرم ساخت سناریوی جدید"
                    status.text = "در حال تولید سناریوها در پس‌زمینه... لطفاً شکیبا باشید."
                    status.setTextColor(LovableUi.primary)
                    mainHandler.postDelayed(pollRunnable, 1000)
                } else if (entry.status == "failed") {
                    showError("خطا در ساخت: ${entry.errorMessage ?: "فرآیند متوقف شد."}")
                }
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
        }, LovableUi.run { margin(bottom = 14) })

        toggleFormBtn = LovableUi.run { ghostButton("➕ نمایش فرم ساخت سناریوی جدید") {
            if (formContainer.visibility == View.VISIBLE) {
                formContainer.visibility = View.GONE
                toggleFormBtn.text = "➕ نمایش فرم ساخت سناریوی جدید"
            } else {
                formContainer.visibility = View.VISIBLE
                toggleFormBtn.text = "✖ بستن فرم ساخت جدید"
            }
        } }.apply {
            visibility = View.GONE
        }
        root.addView(toggleFormBtn, LinearLayout.LayoutParams(-1, LovableUi.run { dp(44) }).apply { bottomMargin = LovableUi.run { dp(12) } })

        formContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        formContainer.addView(LovableUi.run { sectionTitle("اطلاعات کسب‌وکار") }, LovableUi.run { margin(bottom = 10) })
        niche = field("حوزه کاری *", "مثلاً صافکاری PDR، کلینیک زیبایی یا کافه")
        description = field("توضیح کسب‌وکار *", "توضیح کوتاه مثل «اسباب کشی» هم قابل قبول است", 5)
        audience = field("مخاطب هدف", "مثلاً صاحبان خودرو ۲۵ تا ۴۵ سال")
        offer = field("محصول یا پیشنهاد اصلی", "چه چیزی باید معرفی، فروخته یا در ذهن مخاطب تثبیت شود؟")
        constraints = field("امکانات و محدودیت ضبط", "لوکیشن، ابزار، ممنوعیت‌ها، لحن برند و محدودیت بازیگر", 3)
        listOf(niche, description, audience, offer, constraints).forEach { formContainer.addView(it) }

        formContainer.addView(LovableUi.run { sectionTitle("حالت ساخت سناریو") }, LovableUi.run { margin(bottom = 8, top = 4) })
        mode = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@ScenarioStudioActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf(
                    "هوشمند چندسکانسه • با دیالوگ (۴ تا ۱۲ سکانس پویا)",
                    "هوشمند چندسکانسه • بدون دیالوگ (روایت بصری و فوتیج)",
                    "ویدیوی کوتاه ۱۵ ثانیه‌ای • با دیالوگ (تک‌سکانسه وایرال)",
                    "ویدیوی کوتاه ۱۵ ثانیه‌ای • بدون دیالوگ (تک‌سکانسه وایرال)",
                ),
            )
            setSelection(0)
            background = LovableUi.run { rounded(Color.WHITE, 18, LovableUi.border) }
            setPadding(LovableUi.run { dp(12) }, LovableUi.run { dp(8) }, LovableUi.run { dp(12) }, LovableUi.run { dp(8) })
        }
        formContainer.addView(mode, LinearLayout.LayoutParams(-1, LovableUi.run { dp(50) }).apply { bottomMargin = LovableUi.run { dp(14) } })

        formContainer.addView(LovableUi.run { sectionTitle("تعداد بازیگر در دسترس") }, LovableUi.run { margin(bottom = 8, top = 4) })
        actors = Spinner(this).apply {
            adapter = ArrayAdapter(this@ScenarioStudioActivity, android.R.layout.simple_spinner_dropdown_item, listOf("۱ نفر", "۲ نفر", "۳ نفر"))
            setSelection(1)
            background = LovableUi.run { rounded(Color.WHITE, 18, LovableUi.border) }
            setPadding(LovableUi.run { dp(12) }, LovableUi.run { dp(8) }, LovableUi.run { dp(12) }, LovableUi.run { dp(8) })
        }
        formContainer.addView(actors, LinearLayout.LayoutParams(-1, LovableUi.run { dp(50) }).apply { bottomMargin = LovableUi.run { dp(14) } })

        formContainer.addView(LovableUi.run { card() }.apply {
            val row = LinearLayout(this@ScenarioStudioActivity).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; gravity = Gravity.CENTER_VERTICAL }
            row.addView(LovableUi.run { text("قانون کارگردانی تیک‌آف", 12.5f, LovableUi.foreground, true) }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(LovableUi.run { chip("پویا", "secondary") })
            addView(row)
            addView(LovableUi.run { text("در حالت هوشمند، تعداد سکانس هر سناریو بر اساس نیاز روایت تعیین می‌شود. در حالت کوتاه، هر کدام از ۱۰ سناریو دقیقاً یک برداشت ۱۵ ثانیه‌ای است و تیک‌آف خودش تصمیم می‌گیرد دیالوگ لازم است یا فقط اکت، SFX و موسیقی.", 11.5f, LovableUi.muted) }.apply { setPadding(0, LovableUi.run { dp(8) }, 0, 0) })
        }, LovableUi.run { margin(bottom = 16) })

        generate = LovableUi.run { primaryButton("ساخت ۱۰ سناریوی آماده ضبط") { requestPackage() } }
        formContainer.addView(generate, LinearLayout.LayoutParams(-1, LovableUi.run { dp(54) }))
        root.addView(formContainer)

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

    private fun showArchiveDialog() {
        val list = StudioResultStore(this).getByType("scenario").filter { it.status == "completed" }
        if (list.isEmpty()) {
            Toast.makeText(this, "هنوز سناریوی ذخیره‌شده‌ای وجود ندارد.", Toast.LENGTH_SHORT).show()
            return
        }
        val titles = list.map { "${it.title.ifBlank { it.niche }} (${SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(it.createdAt))})" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("آرشیو سناریوهای ضبط")
            .setItems(titles) { _, which ->
                val selected = list[which]
                selected.resultJson?.let {
                    runCatching { JSONObject(it) }.getOrNull()?.also { j ->
                        packageJson = j
                        formContainer.visibility = View.GONE
                        toggleFormBtn.visibility = View.VISIBLE
                        toggleFormBtn.text = "➕ نمایش فرم ساخت سناریوی جدید"
                        renderPackage(j)
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
        val body = JSONObject().apply {
            put("niche", n)
            put("business_description", d)
            put("mode", selectedMode)
            put("audience", audience.text.toString().trim())
            put("offer", offer.text.toString().trim())
            put("constraints", constraints.text.toString().trim())
            put("production_constraints", constraints.text.toString().trim())
            put("actors_available", selectedActors)
            put("actor_count", selectedActors)
        }
        generate.isEnabled = false
        status.text = "در حال اتصال به استودیو سناریو تیک‌آف…"
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

        Thread {
            val res = runCatching { post(body) }.getOrElse { 0 to "" }
            runOnUiThread {
                val root = if (res.first in 200..299) {
                    runCatching { JSONObject(res.second) }.getOrNull()
                } else null

                val finalPkg = root ?: LocalStudioEngine.generateScenarioPackage(
                    n, d, selectedMode,
                    audience.text.toString().trim(),
                    offer.text.toString().trim(),
                    constraints.text.toString().trim(),
                    selectedActors
                )

                packageJson = finalPkg
                StudioResultStore(this@ScenarioStudioActivity).update(taskId) { current ->
                    current.copy(status = "completed", resultJson = finalPkg.toString(), errorMessage = null)
                }
                generate.isEnabled = true
                formContainer.visibility = View.GONE
                toggleFormBtn.visibility = View.VISIBLE
                toggleFormBtn.text = "➕ نمایش فرم ساخت سناریوی جدید"
                mainHandler.removeCallbacks(pollRunnable)
                renderPackage(finalPkg)
            }
        }.start()
    }

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
            conn.readTimeout = 320_000
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

    companion object {
        private const val PDF_REQUEST = 901
        private const val DEFAULT_PDF_NAME = "TakeOff-Scenario-Studio.pdf"
    }
}
