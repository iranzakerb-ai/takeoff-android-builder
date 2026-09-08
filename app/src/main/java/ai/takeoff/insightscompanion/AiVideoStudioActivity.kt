package ai.takeoff.insightscompanion

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
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
import java.util.UUID

class AiVideoStudioActivity : Activity() {
    private lateinit var niche: EditText
    private lateinit var description: EditText
    private lateinit var audience: EditText
    private lateinit var offer: EditText
    private lateinit var constraints: EditText
    private lateinit var mode: Spinner
    private lateinit var status: TextView
    private lateinit var results: LinearLayout
    private lateinit var generate: android.widget.Button
    private var packageJson: JSONObject? = null
    private var pendingBody: JSONObject? = null

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        LovableUi.applyWindow(this)
        setContentView(buildUi())
        state?.getString("ai_video_package")?.let {
            runCatching { JSONObject(it) }.getOrNull()?.also { root ->
                packageJson = root
                renderPackage(root)
            }
        }
    }

    override fun onSaveInstanceState(out: Bundle) {
        packageJson?.let { out.putString("ai_video_package", it.toString()) }
        super.onSaveInstanceState(out)
    }

    private fun buildUi(): View {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(LovableUi.background)
        }
        page.addView(LovableUi.run {
            topBar("ساخت ویدیوی AI", "یک سناریوی نهایی + Character Sheet + Promptهای Omni", back = { finish() })
        })
        val scroll = ScrollView(this).apply { isFillViewport = true; overScrollMode = View.OVER_SCROLL_NEVER }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(LovableUi.run { dp(16) }, LovableUi.run { dp(16) }, LovableUi.run { dp(16) }, LovableUi.run { dp(32) })
        }
        root.addView(LovableUi.run { card(true) }.apply {
            addView(LovableUi.run { chip("✦ TakeOff AI Video Studio", "primary") })
            addView(LovableUi.run { text("یک ویدیوی AI کامل و آماده تولید", 17f, LovableUi.foreground, true) }.apply { setPadding(0, LovableUi.run { dp(10) }, 0, 0) })
            addView(LovableUi.run { text("در حالت سینمایی، تیک‌آف تعداد سکانس و ریتم را خودش انتخاب می‌کند. در حالت وایرال ۱۰ ثانیه‌ای، یک Visual Micro-Spectacle تک‌سکانسه می‌سازد.", 12f, LovableUi.muted) }.apply { setPadding(0, LovableUi.run { dp(7) }, 0, 0) })
        }, LovableUi.run { margin(bottom = 20) })

        root.addView(LovableUi.run { sectionTitle("اطلاعات پروژه") }, LovableUi.run { margin(bottom = 10) })
        niche = field("حوزه کاری *", "مثلاً باربری، تعمیرات خودرو یا کلینیک زیبایی")
        description = field("توضیح کسب‌وکار *", "حتی توضیح کوتاه مثل «اسباب‌کشی» قابل قبول است", 4)
        audience = field("مخاطب هدف", "اگر خالی باشد تیک‌آف تشخیص می‌دهد")
        offer = field("محصول یا پیشنهاد اصلی", "اختیاری")
        constraints = field("محدودیت یا نکته مهم", "اختیاری", 3)
        listOf(niche, description, audience, offer, constraints).forEach { root.addView(it) }

        root.addView(LovableUi.run { sectionTitle("حالت تولید ویدیوی AI") }, LovableUi.run { margin(bottom = 8, top = 4) })
        mode = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@AiVideoStudioActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf(
                    "ویدیوی AI سینمایی • چندسکانسه با دیالوگ",
                    "ویدیوی AI سینمایی • چندسکانسه بدون دیالوگ",
                    "ویدیوی AI وایرال ۱۰ ثانیه‌ای • تک‌سکانسه با دیالوگ",
                    "ویدیوی AI وایرال ۱۰ ثانیه‌ای • تک‌سکانسه بدون دیالوگ",
                ),
            )
            setSelection(0)
            background = LovableUi.run { rounded(Color.WHITE, 18, LovableUi.border) }
            setPadding(LovableUi.run { dp(12) }, LovableUi.run { dp(8) }, LovableUi.run { dp(12) }, LovableUi.run { dp(8) })
        }
        root.addView(mode, LinearLayout.LayoutParams(-1, LovableUi.run { dp(50) }).apply { bottomMargin = LovableUi.run { dp(14) } })

        root.addView(LovableUi.run { card() }.apply {
            val row = LinearLayout(this@AiVideoStudioActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                gravity = Gravity.CENTER_VERTICAL
            }
            row.addView(LovableUi.run { text("قفل Flow / Omni", 12.5f, LovableUi.foreground, true) }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(LovableUi.run { chip("۴ / ۶ / ۸ / ۱۰", "secondary") })
            addView(row)
            addView(LovableUi.run { text("این چهار عدد فقط preset زمان ساخت هر سکانس در Flow/Omni هستند. تعداد سکانس‌ها مستقل است، زمان‌ها می‌توانند تکرار شوند و ترتیب اجباری ندارند. در مود ۱۰ ثانیه‌ای دقیقاً یک سکانس ۱۰ ثانیه‌ای ساخته می‌شود.", 11.5f, LovableUi.muted) }.apply { setPadding(0, LovableUi.run { dp(8) }, 0, 0) })
        }, LovableUi.run { margin(bottom = 16) })

        generate = LovableUi.run { primaryButton("ساخت ویدیوی AI با Omni") { requestPackage() } }
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
        when {
            n.isBlank() -> { showError("فیلد «حوزه کاری» خالی است."); return }
            d.isBlank() -> { showError("فیلد «توضیح کسب‌وکار» خالی است."); return }
            n.length < 2 -> { showError("حوزه کاری را کمی واضح‌تر بنویس."); return }
            d.length < 2 -> { showError("توضیح کسب‌وکار را کمی واضح‌تر بنویس."); return }
        }
        val selectedMode = when (mode.selectedItemPosition) {
            1 -> "cinematic_silent"
            2 -> "viral_10s"
            3 -> "viral_10s_silent"
            else -> "cinematic"
        }
        val transportDescription = if (d.length < 10) d.padEnd(10, ' ') else d
        val body = JSONObject().apply {
            put("niche", n)
            put("business_description", transportDescription)
            put("audience", audience.text.toString().trim())
            put("offer", offer.text.toString().trim())
            put("production_constraints", constraints.text.toString().trim())
            put("creative_preference", if (selectedMode.startsWith("viral_10s"))
                "Visual Micro-Spectacle تک‌سکانسه، دقیقاً ۱۰ ثانیه؛ بدون دیالوگ و متمرکز بر تصویر شگفت‌انگیز؛ فوولی و صداگذاری طبیعی گوشی موبایل"
            else
                "خودکار؛ بیشینه‌سازی Retention؛ تعداد سکانس مستقل از presetهای ۴/۶/۸/۱۰ Flow/Omni است؛ هر زمان می‌تواند تکرار شود و ترتیب اجباری ندارد")
            put("mode", selectedMode)
        }
        pendingBody = body
        sendGeneration(body)
    }

    private fun sendGeneration(body: JSONObject) {
        val selectedMode = body.optString("mode", "cinematic")
        generate.isEnabled = false
        results.removeAllViews()
        status.text = if (selectedMode == "viral_10s")
            "حافظه V5 ← طراحی Visual Micro-Spectacle ← داوری قلاب ثانیه اول ← طراحی صدا/دیالوگ ← Omni Prompt ۱۰ ثانیه‌ای…"
        else
            "حافظه V5 ← ساخت ایده‌ها ← طراحی تعداد سکانس و زمان هرکدام ← داوری قلاب و Retention ← طراحی کاراکتر ← Omni Prompt ← کنترل Continuity…"
        status.setTextColor(LovableUi.primary)
        Thread {
            val response = runCatching { post(body) }.getOrElse { 0 to it.message.orEmpty() }
            runOnUiThread {
                generate.isEnabled = true
                when {
                    response.first in 200..299 -> {
                        runCatching { JSONObject(response.second) }.getOrNull()?.let {
                            packageJson = it
                            renderPackage(it)
                        } ?: showError("پاسخ سرور قابل خواندن نبود.")
                    }
                    response.first == 401 -> showPairingDialog()
                    response.first == 422 -> showError("اطلاعات ورودی با قرارداد سرور هماهنگ نبود؛ دوباره تلاش کن.")
                    response.first == 0 -> showError("ارتباط با سرور برقرار نشد. اینترنت یا آدرس سرور را بررسی کن.")
                    else -> showError("ساخت ویدیو ناموفق بود (کد ${response.first}). دوباره تلاش کن.")
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
                        pendingBody?.let { sendGeneration(it) }
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

    private fun renderPackage(root: JSONObject) {
        val video = root.optJSONObject("video") ?: root.optJSONArray("videos")?.optJSONObject(0)
        if (video == null) { showError("سناریوی نهایی در پاسخ پیدا نشد."); return }
        val scenes = video.optJSONArray("scenes") ?: JSONArray()
        val characters = video.optJSONArray("characters") ?: JSONArray()
        val isViral10 = root.optString("mode") == "viral_10s" || video.optInt("total_duration_seconds") == 10 && scenes.length() == 1
        status.text = if (isViral10)
            "ویدیوی وایرال ۱۰ ثانیه‌ای آماده • ۱ سکانس • Omni Prompt آماده کپی"
        else
            "ویدیوی نهایی آماده • ${LovableUi.fa(root.optInt("candidate_count_considered", 0))} ایده داوری شد • ${LovableUi.fa(scenes.length())} سکانس"
        status.setTextColor(LovableUi.success)
        results.removeAllViews()
        results.addView(LovableUi.run { card(true) }.apply {
            addView(LovableUi.run { chip(if (isViral10) "Visual Micro-Spectacle • ۱۰s" else "Omni • ۹:۱۶", "secondary") })
            addView(LovableUi.run { text(video.optString("title"), 15f, LovableUi.foreground, true) }.apply { setPadding(0, LovableUi.run { dp(9) }, 0, 0) })
            addView(LovableUi.run { text("${LovableUi.fa(video.optInt("total_duration_seconds"))} ثانیه • ${LovableUi.fa(scenes.length())} سکانس • ${LovableUi.fa(characters.length())} کاراکتر\n${video.optString("core_idea")}", 12f, LovableUi.foreground) }.apply { setPadding(0, LovableUi.run { dp(7) }, 0, LovableUi.run { dp(10) }) })
            addView(LovableUi.run { primaryButton("نمایش پرامپت‌های آماده تولید") { showVideo(video) } }, LinearLayout.LayoutParams(-1, LovableUi.run { dp(50) }))
            addView(LovableUi.run { ghostButton("دانلود و اشتراک‌گذاری PDF پرامپت‌ها") { exportAiVideoPdf(root) } }, LinearLayout.LayoutParams(-1, LovableUi.run { dp(48) }).apply { topMargin = LovableUi.run { dp(8) } })
        }, LovableUi.run { margin(bottom = 12, top = 6) })
    }

    private fun exportAiVideoPdf(root: JSONObject) {
        Toast.makeText(this, "در حال ایجاد PDF پرامپت‌های هوش مصنوعی…", Toast.LENGTH_SHORT).show()
        Thread {
            val result = runCatching { TakeoffPdfExporter.exportAiVideoStudio(this, root) }.getOrNull()
            runOnUiThread {
                val cacheFile = result?.second
                if (cacheFile != null && cacheFile.exists()) {
                    Toast.makeText(this, "PDF در پوشه Downloads/TakeOff ذخیره شد.", Toast.LENGTH_LONG).show()
                    TakeoffPdfExporter.shareOrViewPdf(this, cacheFile, "پرامپت‌های ویدیوی AI تیک‌آف")
                } else {
                    Toast.makeText(this, "ذخیره PDF انجام نشد.", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun showVideo(video: JSONObject) {
        val host = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(LovableUi.run { dp(14) }, LovableUi.run { dp(8) }, LovableUi.run { dp(14) }, LovableUi.run { dp(16) }) }
        host.addView(LovableUi.run { text(video.optString("title"), 16f, LovableUi.foreground, true) })
        val hook = video.optJSONObject("hook_stack")
        host.addView(LovableUi.run { text("قلاب تصویری: ${hook?.optString("visual").orEmpty()}\nقلاب گفتاری/متنی: ${hook?.optString("spoken").orEmpty()}\nقلاب صوتی: ${hook?.optString("audio").orEmpty()}", 12f, LovableUi.foreground) }.apply { setPadding(0, LovableUi.run { dp(8) }, 0, LovableUi.run { dp(12) }) })
        host.addView(LovableUi.run { sectionTitle("Character Sheet مرجع") }, LovableUi.run { margin(bottom = 8) })
        val characters = video.optJSONArray("characters") ?: JSONArray()
        for (i in 0 until characters.length()) {
            val char = characters.optJSONObject(i) ?: continue
            val prompt = char.optString("character_sheet_prompt")
            host.addView(LovableUi.run { card() }.apply {
                addView(LovableUi.run { chip(char.optString("character_id"), "secondary") })
                addView(LovableUi.run { text(char.optString("role"), 12.5f, LovableUi.foreground, true) }.apply { setPadding(0, LovableUi.run { dp(7) }, 0, LovableUi.run { dp(8) }) })
                addView(LovableUi.run { ghostButton("کپی Character Sheet Prompt") { copyText(prompt, "پرامپت کاراکتر کپی شد") } }, LinearLayout.LayoutParams(-1, LovableUi.run { dp(44) }))
            }, LovableUi.run { margin(bottom = 8) })
        }
        host.addView(LovableUi.run { sectionTitle("سکانس‌ها") }, LovableUi.run { margin(bottom = 8, top = 6) })
        val scenes = video.optJSONArray("scenes") ?: JSONArray()
        for (i in 0 until scenes.length()) {
            val scene = scenes.optJSONObject(i) ?: continue
            val prompt = scene.optString("omni_prompt").ifBlank { video.optString("omni_prompt_10s").ifBlank { video.optString("omni_prompt") } }
            host.addView(LovableUi.run { card() }.apply {
                addView(LovableUi.run { text("سکانس ${LovableUi.fa(scene.optInt("number", i + 1))} • ${LovableUi.fa(scene.optInt("duration_seconds"))} ثانیه", 12.5f, LovableUi.foreground, true) })
                addView(LovableUi.run { text(scene.optString("scene_summary").ifBlank { scene.optString("action") }, 11.5f, LovableUi.muted) }.apply { setPadding(0, LovableUi.run { dp(6) }, 0, LovableUi.run { dp(8) }) })
                addView(LovableUi.run { ghostButton("کپی Omni Prompt این سکانس") { copyText(prompt, "پرامپت سکانس کپی شد") } }, LinearLayout.LayoutParams(-1, LovableUi.run { dp(44) }))
            }, LovableUi.run { margin(bottom = 8) })
        }
        AlertDialog.Builder(this).setView(ScrollView(this).apply { addView(host) }).setPositiveButton("بستن", null).show()
    }

    private fun copyText(text: String, message: String) {
        if (text.isBlank()) { Toast.makeText(this, "پرامپت خالی است.", Toast.LENGTH_SHORT).show(); return }
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("TakeOff prompt", text))
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) { status.text = message; status.setTextColor(LovableUi.danger) }
}
