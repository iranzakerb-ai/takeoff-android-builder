package ai.takeoff.insightscompanion

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AiVideoStudioActivity : Activity() {
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
    private var pendingBody: JSONObject? = null
    private var activeTaskId: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val pollRunnable = object : Runnable {
        override fun run() {
            val taskId = activeTaskId ?: return
            val entry = StudioResultStore(this@AiVideoStudioActivity).get(taskId)
            if (entry != null) {
                if (entry.status == "completed" && !entry.resultJson.isNullOrBlank()) {
                    generate.isEnabled = true
                    formContainer.visibility = View.GONE
                    toggleFormBtn.visibility = View.VISIBLE
                    toggleFormBtn.text = "➕ نمایش فرم ساخت ویدیوی جدید"
                    runCatching { JSONObject(entry.resultJson!!) }.getOrNull()?.let {
                        renderPackage(it)
                    }
                    return
                } else if (entry.status == "failed") {
                    generate.isEnabled = true
                    showError(entry.errorMessage ?: "فرآیند تولید ویدیوی هوش مصنوعی متوقف شد.")
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
                    toggleFormBtn.text = "➕ نمایش فرم ساخت ویدیوی جدید"
                    runCatching { JSONObject(entry.resultJson!!) }.getOrNull()?.let { j -> renderPackage(j) }
                } else if (entry.status == "processing") {
                    formContainer.visibility = View.GONE
                    toggleFormBtn.visibility = View.VISIBLE
                    toggleFormBtn.text = "➕ نمایش فرم ساخت ویدیوی جدید"
                    status.text = "در حال ساخت و داوری ویدیوی هوش مصنوعی در سرور... لطفاً شکیبا باشید."
                    status.setTextColor(LovableUi.primary)
                    mainHandler.postDelayed(pollRunnable, 1000)
                } else if (entry.status == "failed") {
                    showError("خطا در ساخت ویدیو: ${entry.errorMessage ?: "فرآیند متوقف شد."}")
                }
            }
        }
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(pollRunnable)
        super.onDestroy()
    }

    private fun buildUi(): View {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(LovableUi.background)
        }
        page.addView(LovableUi.run { topBar("استودیوی ویدیوی AI", "تولید سناریوی ویدیویی، Character Sheet و پرامپت‌های Omni", back = { finish() }) })
        val scroll = ScrollView(this).apply { isFillViewport = true; overScrollMode = View.OVER_SCROLL_NEVER }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(LovableUi.run { dp(16) }, LovableUi.run { dp(16) }, LovableUi.run { dp(16) }, LovableUi.run { dp(32) })
        }

        root.addView(LovableUi.run { card(true) }.apply {
            val topRow = LinearLayout(this@AiVideoStudioActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                gravity = Gravity.CENTER_VERTICAL
            }
            topRow.addView(LovableUi.run { chip("✦ AI Video Studio 1.0", "secondary") })
            topRow.addView(View(this@AiVideoStudioActivity), LinearLayout.LayoutParams(0, 1, 1f))
            topRow.addView(LovableUi.run { ghostButton("📂 آرشیو ویدیوها") { showArchiveDialog() } }, LinearLayout.LayoutParams(-2, LovableUi.run { dp(36) }))
            addView(topRow)

            addView(LovableUi.run { text("یک ویدیوی AI کامل و آماده تولید", 17f, LovableUi.foreground, true) }.apply { setPadding(0, LovableUi.run { dp(10) }, 0, 0) })
            addView(LovableUi.run { text("۴ حالت هوش مصنوعی: سینمایی چندسکانسه (با دیالوگ و بدون دیالوگ) و ویدیوی وایرال ۱۰ ثانیه‌ای شگفت‌انگیز با پرامپت‌های دوربین واقعی iPhone 15 Pro و تایمینگ‌های دقیق Flow.labs.", 12f, LovableUi.muted) }.apply { setPadding(0, LovableUi.run { dp(7) }, 0, 0) })
        }, LovableUi.run { margin(bottom = 14) })

        toggleFormBtn = LovableUi.run { ghostButton("➕ نمایش فرم ساخت ویدیوی جدید") {
            if (formContainer.visibility == View.VISIBLE) {
                formContainer.visibility = View.GONE
                toggleFormBtn.text = "➕ نمایش فرم ساخت ویدیوی جدید"
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

        formContainer.addView(LovableUi.run { sectionTitle("اطلاعات کسب‌وکار و ایده") }, LovableUi.run { margin(bottom = 10) })
        niche = field("حوزه کاری *", "مثلاً دندانپزشکی، بوتیک لباس یا نرم‌افزار")
        description = field("توضیح کسب‌وکار و ایده مدنظر *", "توضیح کوتاه یا کامل درباره آنچه می‌خواهید بسازید", 5)
        audience = field("مخاطب هدف", "مثلاً جوانان ۱۸ تا ۳۰ سال علاقه‌مند به تکنولوژی")
        offer = field("محصول یا پیام اصلی", "چه ارزشی یا محصولی باید در ویدیو تثبیت شود؟")
        constraints = field("محدودیت‌های سناریو", "نبایدها، لحن برند، ترجیحات بصری", 3)
        listOf(niche, description, audience, offer, constraints).forEach { formContainer.addView(it) }

        formContainer.addView(LovableUi.run { sectionTitle("حالت ویدیوی هوش مصنوعی") }, LovableUi.run { margin(bottom = 8, top = 4) })
        mode = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@AiVideoStudioActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf(
                    "سینمایی چندسکانسه • با دیالوگ (روایت داستانی)",
                    "سینمایی چندسکانسه • بدون دیالوگ (اکت بصری و فولی)",
                    "وایرال ۱۰ ثانیه‌ای شگفت‌انگیز • با دیالوگ (Omni Prompt)",
                    "وایرال ۱۰ ثانیه‌ای شگفت‌انگیز • بدون دیالوگ (اکت خالص)",
                ),
            )
            setSelection(0)
            background = LovableUi.run { rounded(Color.WHITE, 18, LovableUi.border) }
            setPadding(LovableUi.run { dp(12) }, LovableUi.run { dp(8) }, LovableUi.run { dp(12) }, LovableUi.run { dp(8) })
        }
        formContainer.addView(mode, LinearLayout.LayoutParams(-1, LovableUi.run { dp(50) }).apply { bottomMargin = LovableUi.run { dp(14) } })

        formContainer.addView(LovableUi.run { sectionTitle("تعداد کاراکترهای کلیدی") }, LovableUi.run { margin(bottom = 8, top = 4) })
        actors = Spinner(this).apply {
            adapter = ArrayAdapter(this@AiVideoStudioActivity, android.R.layout.simple_spinner_dropdown_item, listOf("۱ کاراکتر", "۲ کاراکتر", "۳ کاراکتر"))
            setSelection(0)
            background = LovableUi.run { rounded(Color.WHITE, 18, LovableUi.border) }
            setPadding(LovableUi.run { dp(12) }, LovableUi.run { dp(8) }, LovableUi.run { dp(12) }, LovableUi.run { dp(8) })
        }
        formContainer.addView(actors, LinearLayout.LayoutParams(-1, LovableUi.run { dp(50) }).apply { bottomMargin = LovableUi.run { dp(14) } })

        formContainer.addView(LovableUi.run { card() }.apply {
            val row = LinearLayout(this@AiVideoStudioActivity).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; gravity = Gravity.CENTER_VERTICAL }
            row.addView(LovableUi.run { text("مشخصات خروجی استودیو AI", 12.5f, LovableUi.foreground, true) }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(LovableUi.run { chip("Flow.labs Presets", "secondary") })
            addView(row)
            addView(LovableUi.run { text("مدت سکانس‌ها به صورت خودکار از میان مقادیر ۴ / ۶ / ۸ / ۱۰ ثانیه انتخاب می‌شود. این چهار عدد فقط preset زمان ساخت هر سکانس در Flow/Omni هستند، تعداد سکانس‌ها مستقل است، زمان‌ها می‌توانند تکرار شوند و ترتیب اجباری ندارند. برای ویدیوهای وایرال ۱۰ ثانیه‌ای، پرامپت‌ها کاملاً رئال گوشی طراحی شده و بدون کاراکتر شیت مستقیماً در تولید ویدیو قرار می‌گیرند.", 11.5f, LovableUi.muted) }.apply { setPadding(0, LovableUi.run { dp(8) }, 0, 0) })
        }, LovableUi.run { margin(bottom = 16) })

        generate = LovableUi.run { primaryButton("ساخت ویدیوی AI با Omni") { requestPackage() } }
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
        val list = StudioResultStore(this).getByType("ai_video").filter { it.status == "completed" }
        if (list.isEmpty()) {
            Toast.makeText(this, "هنوز ویدیوی هوش مصنوعی ذخیره‌شده‌ای وجود ندارد.", Toast.LENGTH_SHORT).show()
            return
        }
        val titles = list.map { "${it.title.ifBlank { it.niche }} (${SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(it.createdAt))})" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("آرشیو ویدیوهای هوش مصنوعی")
            .setItems(titles) { _, which ->
                val selected = list[which]
                selected.resultJson?.let {
                    runCatching { JSONObject(it) }.getOrNull()?.let { j ->
                        formContainer.visibility = View.GONE
                        toggleFormBtn.visibility = View.VISIBLE
                        toggleFormBtn.text = "➕ نمایش فرم ساخت ویدیوی جدید"
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
        when {
            n.isBlank() -> { showError("فیلد «حوزه کاری» خالی است."); return }
            d.isBlank() -> { showError("فیلد «توضیح کسب‌وکار» خالی است."); return }
            n.length < 2 -> { showError("حوزه کاری را کمی واضح‌تر بنویس."); return }
            d.length < 2 -> { showError("توضیح کسب‌وکار را کمی بیشتر بنویس."); return }
        }
        val selectedMode = when (mode.selectedItemPosition) {
            1 -> "cinematic_silent"
            2 -> "viral_10s"
            3 -> "viral_10s_silent"
            else -> "cinematic"
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
        pendingBody = body

        generate.isEnabled = false
        status.text = "در حال تولید در پس‌زمینه... می‌توانید از اپ خارج شوید، نتیجه ذخیره و اعلان داده خواهد شد."
        status.setTextColor(LovableUi.primary)
        results.removeAllViews()

        val taskId = UUID.randomUUID().toString()
        activeTaskId = taskId

        val entry = StudioEntry(
            id = taskId,
            type = "ai_video",
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

        sendGeneration(body, taskId)
    }

    private fun sendGeneration(body: JSONObject, taskId: String) {
        Thread {
            val response = runCatching { post(body) }.getOrElse { 0 to "" }
            runOnUiThread {
                if (response.first in 200..299) {
                    runCatching { JSONObject(response.second) }.getOrNull()?.let {
                        StudioResultStore(this@AiVideoStudioActivity).update(taskId) { current ->
                            current.copy(status = "completed", resultJson = it.toString(), errorMessage = null)
                        }
                        generate.isEnabled = true
                        formContainer.visibility = View.GONE
                        toggleFormBtn.visibility = View.VISIBLE
                        toggleFormBtn.text = "➕ نمایش فرم ساخت ویدیوی جدید"
                        mainHandler.removeCallbacks(pollRunnable)
                        renderPackage(it)
                    }
                } else if (response.first == 401) {
                    showPairingDialog()
                } else {
                    val fallback = LocalStudioEngine.generateAiVideoPackage(
                        body.optString("niche"),
                        body.optString("business_description"),
                        body.optString("mode"),
                        body.optString("audience"),
                        body.optString("offer"),
                        body.optString("constraints"),
                        body.optInt("actor_count", 1)
                    )
                    StudioResultStore(this@AiVideoStudioActivity).update(taskId) { current ->
                        current.copy(status = "completed", resultJson = fallback.toString(), errorMessage = null)
                    }
                    generate.isEnabled = true
                    formContainer.visibility = View.GONE
                    toggleFormBtn.visibility = View.VISIBLE
                    toggleFormBtn.text = "➕ نمایش فرم ساخت ویدیوی جدید"
                    mainHandler.removeCallbacks(pollRunnable)
                    renderPackage(fallback)
                }
            }
        }.start()
    }

    private fun showPairingDialog() {
        val input = EditText(this).apply {
            hint = "کد اتصال امن یا کلید فعلی"
            setSingleLine(true)
            setPadding(24, 18, 24, 18)
        }
        AlertDialog.Builder(this)
            .setTitle("اتصال امن دستگاه")
            .setMessage("این نصب هنوز توکن معتبر ندارد. کد اتصال امن یا کلید فعلی را یک‌بار وارد کن؛ اپ توکن دستگاه را امن ذخیره می‌کند و ساخت ویدیو خودکار ادامه پیدا می‌کند.")
            .setView(input)
            .setNegativeButton("انصراف", null)
            .setPositiveButton("اتصال و ادامه") { _, _ ->
                val credential = input.text.toString().trim()
                if (credential.isBlank()) { showError("کد اتصال امن وارد نشده."); return@setPositiveButton }
                pairAndRetry(credential)
            }
            .show()
    }

    private fun pairAndRetry(credential: String) {
        generate.isEnabled = false
        status.text = "در حال ساخت اتصال امن دستگاه…"
        status.setTextColor(LovableUi.primary)
        Thread {
            val result = runCatching { pairDevice(credential) }.getOrElse { 0 to "" }
            runOnUiThread {
                generate.isEnabled = true
                if (result.first in 200..299) {
                    val token = runCatching { JSONObject(result.second).optString("device_token") }.getOrDefault("")
                    if (token.isBlank()) {
                        showError("توکن دستگاه از سرور دریافت نشد.")
                    } else {
                        SecretStore(this).put("api_key", token)
                        status.text = "اتصال امن برقرار شد؛ ادامه ساخت ویدیو…"
                        pendingBody?.let {
                            val tid = activeTaskId ?: UUID.randomUUID().toString()
                            sendGeneration(it, tid)
                        }
                    }
                } else {
                    showError(if (result.first == 429) "تلاش‌های اتصال زیاد بوده؛ کمی بعد دوباره امتحان کن." else "کد اتصال امن معتبر نیست.")
                }
            }
        }.start()
    }

    private fun pairDevice(credential: String): Pair<Int, String> {
        val prefs = getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE)
        val endpoints = PayloadClient.candidateEndpoints(prefs.getString("endpoint", "").orEmpty())
        val deviceId = prefs.getString("device_id", null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("device_id", it).apply() }
        val payload = JSONObject().apply {
            put("device_id", deviceId)
            put("device_name", "TakeOff Android ${Build.MODEL}")
            put("code", credential)
        }
        fun attempt(endpoint: String, asMaster: Boolean): Pair<Int, String> {
            val conn = URL(endpoint.trimEnd('/') + "/v2/viral-evidence/pair").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 20_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Accept", "application/json")
            if (asMaster) conn.setRequestProperty("X-Takeoff-Companion-Key", credential)
            return try {
                conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                code to stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            } catch (_: Exception) {
                0 to ""
            } finally { conn.disconnect() }
        }
        var last: Pair<Int, String> = 0 to ""
        for (ep in endpoints) {
            val masterTry = attempt(ep, true)
            val res = if (masterTry.first in 200..299) masterTry else attempt(ep, false)
            if (res.first in 200..299) return res
            last = res
            if (res.first != 404 && res.first !in 502..504) return res
        }
        return last
    }

    private fun post(body: JSONObject): Pair<Int, String> {
        val prefs = getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE)
        val endpoints = PayloadClient.candidateEndpoints(prefs.getString("endpoint", "").orEmpty())
        val key = SecretStore(this).get("api_key").orEmpty()
        var last: Pair<Int, String> = 0 to ""
        for (ep in endpoints) {
            val conn = URL(ep.trimEnd('/') + "/v4/ai-video-studio/generate").openConnection() as HttpURLConnection
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

    private var selectedConceptIndex = 0

    private fun renderPackage(root: JSONObject) {
        selectedConceptIndex = 0
        renderSelectedConcept(root)
    }

    private fun renderSelectedConcept(root: JSONObject) {
        val videos = root.optJSONArray("videos")
        val video = (if (videos != null && videos.length() > selectedConceptIndex) {
            videos.optJSONObject(selectedConceptIndex)
        } else null) ?: root.optJSONObject("video") ?: videos?.optJSONObject(0)

        if (video == null) { showError("سناریوی نهایی در پاسخ پیدا نشد."); return }
        val scenes = video.optJSONArray("scenes") ?: JSONArray()
        val characters = video.optJSONArray("characters") ?: JSONArray()
        val isViral10 = root.optString("mode") == "viral_10s" || root.optString("mode") == "viral_10s_silent" ||
                (video.optInt("total_duration_seconds") == 10 && scenes.length() == 1)

        val rankDisplay = video.optInt("rank", selectedConceptIndex + 1)
        val rankLabel = if (rankDisplay == 1) "برنده #۱" else "#$rankDisplay"

        status.text = if (isViral10)
            "ویدیوی ۱۰ ثانیه‌ای ($rankLabel) • ۱ سکانس • Omni Prompt آماده کپی"
        else
            "سناریوی ویدیویی ($rankLabel) • ${LovableUi.fa(root.optInt("candidate_count_considered", 0))} ایده داوری شد • ${LovableUi.fa(scenes.length())} سکانس • Omni Cinematic"
        status.setTextColor(LovableUi.success)
        results.removeAllViews()

        // 10 Ranked Concepts Selector
        if (videos != null && videos.length() > 1) {
            results.addView(LovableUi.run { sectionTitle("انتخاب از بین ۱۰ ایده برتر داوری‌شده") }, LovableUi.run { margin(bottom = 6) })
            val hScroll = HorizontalScrollView(this).apply {
                overScrollMode = View.OVER_SCROLL_NEVER
                isHorizontalScrollBarEnabled = false
            }
            val selectorRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                setPadding(0, LovableUi.run { dp(2) }, 0, LovableUi.run { dp(8) })
            }
            for (idx in 0 until videos.length()) {
                val vItem = videos.optJSONObject(idx) ?: continue
                val rNum = vItem.optInt("rank", idx + 1)
                val isSel = idx == selectedConceptIndex
                val chipText = if (rNum == 1) "🏆 ایده #۱ (برنده)" else "ایده #$rNum"
                val chipTone = if (isSel) "primary" else "secondary"
                val chipView = LovableUi.run { chip(chipText, chipTone) }.apply {
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        if (selectedConceptIndex != idx) {
                            selectedConceptIndex = idx
                            renderSelectedConcept(root)
                        }
                    }
                }
                selectorRow.addView(chipView, LinearLayout.LayoutParams(-2, -2).apply {
                    marginStart = LovableUi.run { dp(6) }
                })
            }
            hScroll.addView(selectorRow)
            results.addView(hScroll, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = LovableUi.run { dp(8) } })
        }

        if (isViral10) {
            renderViral10Package(root, video, scenes)
        } else {
            renderCinematicPackage(root, video, scenes, characters)
        }
    }

    private fun renderViral10Package(root: JSONObject, video: JSONObject, scenes: JSONArray) {
        val omniPrompt = video.optString("omni_prompt_10s").ifBlank {
            if (scenes.length() > 0) scenes.optJSONObject(0)?.optString("omni_prompt").orEmpty() else video.optString("omni_prompt")
        }

        val rankNum = video.optInt("rank", selectedConceptIndex + 1)
        val rankBadge = if (rankNum == 1) "ایده #۱ (برنده داوری)" else "ایده #$rankNum"

        results.addView(LovableUi.run { card(true) }.apply {
            val topRow = LinearLayout(this@AiVideoStudioActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                gravity = Gravity.CENTER_VERTICAL
            }
            topRow.addView(LovableUi.run { chip("Visual Micro-Spectacle • ۱۰ ثانیه", "primary") })
            topRow.addView(View(this@AiVideoStudioActivity), LinearLayout.LayoutParams(0, 1, 1f))
            topRow.addView(LovableUi.run { chip(rankBadge, "secondary") })
            addView(topRow)

            addView(LovableUi.run { text(video.optString("title"), 16f, LovableUi.foreground, true) }.apply { setPadding(0, LovableUi.run { dp(10) }, 0, 0) })
            addView(LovableUi.run { text(video.optString("core_idea"), 12f, LovableUi.foreground) }.apply { setPadding(0, LovableUi.run { dp(6) }, 0, LovableUi.run { dp(8) }) })

            val hook = video.optJSONObject("hook_stack")
            if (hook != null) {
                val hookText = "قلاب تصویری: ${hook.optString("visual")}\nقلاب صوتی: ${hook.optString("audio")}"
                addView(LovableUi.run { text(hookText, 11f, LovableUi.muted) }.apply { setPadding(0, 0, 0, LovableUi.run { dp(10) }) })
            }

            addView(LovableUi.run { card() }.apply {
                setBackgroundColor(Color.rgb(15, 23, 42))
                addView(LovableUi.run { text("📌 توجه: این ویدیو کاملاً رئال و موبایلی طراحی شده و نیازی به طراحی کاراکتر شیت ندارد. پرامپت زیر مستقیماً به ابزارهای ویدیوساز Omni داده می‌شود.", 11f, Color.rgb(147, 197, 253)) })
            }, LovableUi.run { margin(bottom = 12) })

            addView(LovableUi.run { sectionTitle("پرامپت ویدیوی ۱۰ ثانیه‌ای Omni") }, LovableUi.run { margin(bottom = 6) })
            val promptBox = TextView(this@AiVideoStudioActivity).apply {
                text = omniPrompt
                setTextColor(Color.rgb(226, 232, 240))
                textSize = 12f
                setTextIsSelectable(true)
                background = LovableUi.run { rounded(Color.rgb(15, 21, 34), 14, LovableUi.border) }
                setPadding(LovableUi.run { dp(12) }, LovableUi.run { dp(10) }, LovableUi.run { dp(12) }, LovableUi.run { dp(10) })
            }
            addView(promptBox, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = LovableUi.run { dp(12) } })

            val copyBtn = LovableUi.run { primaryButton("📋 کپی پرامپت ویدیوی ۱۰ ثانیه‌ای Omni") {
                copyText(omniPrompt, "پرامپت ویدیوی ۱۰ ثانیه‌ای Omni کپی شد!")
            } }
            addView(copyBtn, LinearLayout.LayoutParams(-1, LovableUi.run { dp(52) }))
        }, LovableUi.run { margin(bottom = 16, top = 6) })
    }

    private fun renderCinematicPackage(root: JSONObject, video: JSONObject, scenes: JSONArray, characters: JSONArray) {
        results.addView(LovableUi.run { card(true) }.apply {
            val topRow = LinearLayout(this@AiVideoStudioActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                gravity = Gravity.CENTER_VERTICAL
            }
            topRow.addView(LovableUi.run { chip("Omni Cinematic • ۹:۱۶", "secondary") })
            topRow.addView(View(this@AiVideoStudioActivity), LinearLayout.LayoutParams(0, 1, 1f))
            topRow.addView(LovableUi.run { chip("${LovableUi.fa(scenes.length())} سکانس • ${LovableUi.fa(video.optInt("total_duration_seconds"))} ثانیه", "primary") })
            addView(topRow)

            addView(LovableUi.run { text(video.optString("title"), 16f, LovableUi.foreground, true) }.apply { setPadding(0, LovableUi.run { dp(10) }, 0, 0) })
            addView(LovableUi.run { text(video.optString("core_idea"), 12f, LovableUi.foreground) }.apply { setPadding(0, LovableUi.run { dp(6) }, 0, LovableUi.run { dp(8) }) })

            val copyAllBtn = LovableUi.run { primaryButton("📋 کپی تمام پرامپت‌ها یکجا") {
                val fullText = buildFullPromptText(video, characters, scenes)
                copyText(fullText, "تمام پرامپت‌های کاراکترها و سکانس‌ها کپی شد!")
            } }
            addView(copyAllBtn, LinearLayout.LayoutParams(-1, LovableUi.run { dp(50) }).apply { topMargin = LovableUi.run { dp(6) } })
        }, LovableUi.run { margin(bottom = 16, top = 6) })

        if (characters.length() > 0) {
            results.addView(LovableUi.run { sectionTitle("🎭 پرامپت‌های طراحی کاراکتر شیت (Character Sheets)") }, LovableUi.run { margin(bottom = 10, top = 6) })
            for (i in 0 until characters.length()) {
                val char = characters.optJSONObject(i) ?: continue
                val charPrompt = char.optString("character_sheet_prompt")
                val charId = char.optString("character_id", "کاراکتر ${i + 1}")
                val role = char.optString("role")
                results.addView(LovableUi.run { card() }.apply {
                    val row = LinearLayout(this@AiVideoStudioActivity).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; gravity = Gravity.CENTER_VERTICAL }
                    row.addView(LovableUi.run { chip(charId, "primary") })
                    row.addView(LovableUi.run { text(role, 13f, LovableUi.foreground, true) }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = LovableUi.run { dp(8) } })
                    addView(row)

                    val promptBox = TextView(this@AiVideoStudioActivity).apply {
                        text = charPrompt
                        setTextColor(Color.rgb(203, 213, 225))
                        textSize = 11.5f
                        setTextIsSelectable(true)
                        background = LovableUi.run { rounded(Color.rgb(15, 21, 34), 12, LovableUi.border) }
                        setPadding(LovableUi.run { dp(10) }, LovableUi.run { dp(8) }, LovableUi.run { dp(10) }, LovableUi.run { dp(8) })
                    }
                    addView(promptBox, LinearLayout.LayoutParams(-1, -2).apply { topMargin = LovableUi.run { dp(8) }; bottomMargin = LovableUi.run { dp(10) } })

                    val copyCharBtn = LovableUi.run { primaryButton("📋 کپی Character Sheet Prompt") {
                        copyText(charPrompt, "پرامپت کاراکتر $charId کپی شد!")
                    } }
                    addView(copyCharBtn, LinearLayout.LayoutParams(-1, LovableUi.run { dp(44) }))
                }, LovableUi.run { margin(bottom = 10) })
            }
        }

        results.addView(LovableUi.run { sectionTitle("🎬 پرامپت‌های سکانس به سکانس (Omni Scene Prompts)") }, LovableUi.run { margin(bottom = 10, top = 10) })
        for (i in 0 until scenes.length()) {
            val scene = scenes.optJSONObject(i) ?: continue
            val sceneNum = scene.optInt("number", i + 1)
            val durationSec = scene.optInt("duration_seconds", 6)
            val scenePrompt = scene.optString("omni_prompt").ifBlank { video.optString("omni_prompt") }
            val summary = scene.optString("scene_summary").ifBlank { scene.optString("action") }

            results.addView(LovableUi.run { card() }.apply {
                val row = LinearLayout(this@AiVideoStudioActivity).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; gravity = Gravity.CENTER_VERTICAL }
                row.addView(LovableUi.run { chip("سکانس ${LovableUi.fa(sceneNum)}", "secondary") })
                row.addView(LovableUi.run { text("${LovableUi.fa(durationSec)} ثانیه • Flow.labs", 12f, LovableUi.primary, true) }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = LovableUi.run { dp(8) } })
                addView(row)

                if (summary.isNotBlank()) {
                    addView(LovableUi.run { text(summary, 12f, LovableUi.muted) }.apply { setPadding(0, LovableUi.run { dp(6) }, 0, LovableUi.run { dp(6) }) })
                }

                val promptBox = TextView(this@AiVideoStudioActivity).apply {
                    text = scenePrompt
                    setTextColor(Color.rgb(203, 213, 225))
                    textSize = 11.5f
                    setTextIsSelectable(true)
                    background = LovableUi.run { rounded(Color.rgb(15, 21, 34), 12, LovableUi.border) }
                    setPadding(LovableUi.run { dp(10) }, LovableUi.run { dp(8) }, LovableUi.run { dp(10) }, LovableUi.run { dp(8) })
                }
                addView(promptBox, LinearLayout.LayoutParams(-1, -2).apply { topMargin = LovableUi.run { dp(6) }; bottomMargin = LovableUi.run { dp(10) } })

                val copySceneBtn = LovableUi.run { primaryButton("📋 کپی Omni Prompt این سکانس") {
                    copyText(scenePrompt, "پرامپت سکانس $sceneNum کپی شد!")
                } }
                addView(copySceneBtn, LinearLayout.LayoutParams(-1, LovableUi.run { dp(44) }))
            }, LovableUi.run { margin(bottom = 10) })
        }
    }

    private fun buildFullPromptText(video: JSONObject, characters: JSONArray, scenes: JSONArray): String {
        val sb = StringBuilder()
        sb.append("=== ").append(video.optString("title")).append(" ===\n\n")
        if (characters.length() > 0) {
            sb.append("--- CHARACTER SHEETS ---\n\n")
            for (i in 0 until characters.length()) {
                val c = characters.optJSONObject(i) ?: continue
                sb.append("[Character: ").append(c.optString("character_id")).append(" - ").append(c.optString("role")).append("]\n")
                sb.append(c.optString("character_sheet_prompt")).append("\n\n")
            }
        }
        sb.append("--- SCENE PROMPTS ---\n\n")
        for (i in 0 until scenes.length()) {
            val s = scenes.optJSONObject(i) ?: continue
            sb.append("[Scene ").append(s.optInt("number", i + 1)).append(" (").append(s.optInt("duration_seconds")).append("s)]\n")
            sb.append(s.optString("omni_prompt")).append("\n\n")
        }
        return sb.toString().trim()
    }

    private fun copyText(text: String, message: String) {
        if (text.isBlank()) { Toast.makeText(this, "پرامپت خالی است.", Toast.LENGTH_SHORT).show(); return }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("TakeOff prompt", text)
        clipboard.setPrimaryClip(clip)
        window?.decorView?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        status.text = message
        status.setTextColor(LovableUi.danger)
    }
}
