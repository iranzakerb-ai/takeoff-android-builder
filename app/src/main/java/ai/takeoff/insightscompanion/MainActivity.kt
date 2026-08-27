package ai.takeoff.insightscompanion

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private val bg = Color.rgb(8, 11, 16)
    private val panel = Color.rgb(13, 18, 25)
    private val panel2 = Color.rgb(18, 24, 32)
    private val border = Color.rgb(40, 51, 64)
    private val accent = Color.rgb(0, 228, 208)
    private val muted = Color.rgb(174, 183, 197)

    private lateinit var accountStore: ManagedAccountStore
    private lateinit var insightStore: LocalInsightStore
    private lateinit var secretStore: SecretStore
    private lateinit var content: FrameLayout
    private lateinit var nav: LinearLayout
    private val selectedForCompare = linkedSetOf<String>()

    private val projectionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val service = Intent(this, CaptureService::class.java)
                .setAction(CaptureService.ACTION_START)
                .putExtra(CaptureService.EXTRA_RESULT_CODE, result.resultCode)
                .putExtra(CaptureService.EXTRA_RESULT_DATA, result.data)
            ContextCompat.startForegroundService(this, service)
            TakeoffSound.play(TakeoffSound.Cue.SUCCESS)
            toast("جلسه ثبت آمار فعال شد")
        } else {
            TakeoffSound.play(TakeoffSound.Cue.WARNING)
            toast("مجوز ثبت صفحه داده نشد")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        accountStore = ManagedAccountStore(this)
        insightStore = LocalInsightStore(this)
        secretStore = SecretStore(this)
        if (accountStore.all().isEmpty()) accountStore.upsert("پیج تیک‌آف", "takeoff.content")
        setContentView(shell())
        handleShared(intent)
        scheduleReminders()
        showTab(0)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShared(intent)
        showTab(1)
    }

    private fun shell(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(8))
        }
        header.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_takeoff_logo)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }, LinearLayout.LayoutParams(dp(46), dp(46)))
        header.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, 0, 0)
            addView(TextView(this@MainActivity).apply {
                text = "تیک‌آف"
                textSize = 21f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.START
            })
            addView(TextView(this@MainActivity).apply {
                text = "دستیار میدانی یادگیری"
                textSize = 11.5f
                setTextColor(accent)
                gravity = Gravity.START
            })
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(header)

        content = FrameLayout(this).apply { setBackgroundColor(bg) }
        root.addView(content, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(8), dp(7), dp(8), dp(8))
            background = rounded(panel, 18f, border)
        }
        listOf("خانه", "ثبت آمار", "ریلزها", "آموخته‌ها", "پیج‌ها").forEachIndexed { index, label ->
            nav.addView(Button(this).apply {
                text = label
                isAllCaps = false
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(1, 0, 1, 0)
                setOnClickListener { showTab(index) }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        }
        root.addView(nav, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(66)).apply {
            setMargins(dp(10), 0, dp(10), dp(8))
        })
        return root
    }

    private fun showTab(index: Int) {
        content.removeAllViews()
        for (i in 0 until nav.childCount) {
            (nav.getChildAt(i) as Button).apply {
                setTextColor(if (i == index) Color.rgb(5, 15, 18) else muted)
                background = rounded(if (i == index) accent else Color.TRANSPARENT, 13f, Color.TRANSPARENT)
            }
        }
        val view = when (index) {
            0 -> homePage()
            1 -> capturePage()
            2 -> reelsPage()
            3 -> learningsPage()
            else -> accountsPage()
        }
        content.addView(view)
    }

    private fun page(title: String, subtitle: String, builder: (LinearLayout) -> Unit): View {
        val scroll = ScrollView(this).apply { setBackgroundColor(bg); isFillViewport = true }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(18), dp(8), dp(18), dp(24))
        }
        root.addView(TextView(this).apply {
            text = title
            textSize = 24f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.START
        })
        root.addView(TextView(this).apply {
            text = subtitle
            textSize = 12.8f
            setTextColor(muted)
            gravity = Gravity.START
            setPadding(0, dp(4), 0, dp(14))
        })
        builder(root)
        scroll.addView(root)
        return scroll
    }

    private fun homePage(): View = page("مرکز فرمان", "همه چیز مهم را یک‌جا ببین") { root ->
        val active = accountStore.selected()
        val queueSize = PayloadQueue(this).size()
        val reels = insightStore.reels(active?.normalizedHandle)
        root.addView(card().apply {
            addView(section("وضعیت امروز"))
            addView(big(if (queueSize == 0) "آماده" else "$queueSize ثبت در صف امن", if (queueSize == 0) accent else Color.rgb(255, 190, 70)))
            addView(body("پیج فعال: @${active?.normalizedHandle.orEmpty()}\nریلزهای ثبت‌شده: ${reels.size}"))
        }, margin(dp(12)))

        val healthText = body("سلامت اتصال و Ground Truth را بررسی کن.")
        root.addView(card().apply {
            addView(section("سلامت سیستم"))
            addView(healthText)
            addView(button("بررسی سلامت", false) { checkHealth(healthText) })
        }, margin(dp(12)))

        if (reels.isNotEmpty()) {
            root.addView(card().apply {
                addView(section("آخرین ریلزها"))
                reels.take(3).forEach { addView(reelSummary(it)) }
            }, margin(dp(12)))
        }
        root.addView(button("شروع ثبت آمار", true) { showTab(1) }, margin(0))
    }

    private fun capturePage(): View = page("ثبت آمار", "ریلز درست، پیج درست، سناریوی درست") { root ->
        val active = accountStore.selected()?.normalizedHandle.orEmpty()
        val prefs = getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE)
        val shortcode = prefs.getString("shortcode", "").orEmpty()
        val armedAccount = prefs.getString("armed_account", "").orEmpty()
        root.addView(card().apply {
            addView(section("ریلز آماده"))
            addView(big(if (shortcode.isBlank()) "هنوز انتخاب نشده" else shortcode, Color.WHITE))
            val message = when {
                shortcode.isBlank() -> "از اینستاگرام ریلز را Share کن و تیک‌آف را انتخاب کن."
                armedAccount.isNotBlank() && armedAccount != active -> "این ریلز برای @$armedAccount قفل شده. برای @$active دوباره Share کن."
                else -> "ریلز برای @$active قفل شده و آماده ثبت است."
            }
            addView(body(message))
        }, margin(dp(12)))

        val linked = insightStore.reels(active).firstOrNull { it.shortcode == shortcode }?.scenarioId
        root.addView(card().apply {
            addView(section("سناریوی متصل"))
            addView(body(linked?.let { "سناریو: $it" } ?: "اگر این ریلز خروجی ایجنت است، قبل از ثبت سناریو را وصل کن."))
            addView(button("مدیریت سناریوهای در انتظار انتشار", false) { scenarioDialog(active, shortcode) })
        }, margin(dp(12)))

        val queueSize = PayloadQueue(this).size()
        root.addView(card().apply {
            addView(section("صندوق ثبت"))
            addView(body(if (queueSize == 0) "صف امن خالی است." else "$queueSize ثبت منتظر ارسال یا تأیید پایدار سرور است."))
            addView(button("ارسال صف امن", false) {
                startService(Intent(this@MainActivity, CaptureService::class.java).setAction(CaptureService.ACTION_SYNC))
                TakeoffSound.play(TakeoffSound.Cue.SYNC)
            })
        }, margin(dp(12)))

        root.addView(button("شروع جلسه ثبت آمار", true) { startCapture() }, margin(dp(8)))
        root.addView(button("توقف جلسه", false) {
            startService(Intent(this, CaptureService::class.java).setAction(CaptureService.ACTION_STOP))
        }, margin(0))
    }

    private fun reelsPage(): View = page("ریلزها", "تاریخچه، جستجو و مقایسه عملکرد") { root ->
        val active = accountStore.selected()?.normalizedHandle
        val search = field("جستجو: ریلز یا سناریو", "")
        root.addView(search)
        val host = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        fun render(query: String) {
            host.removeAllViews()
            val q = query.trim().lowercase()
            val rows = insightStore.reels(active).filter {
                q.isBlank() || it.shortcode.lowercase().contains(q) || it.scenarioId.orEmpty().lowercase().contains(q)
            }
            if (rows.isEmpty()) host.addView(body("هنوز داده‌ای برای این پیج ثبت نشده است."))
            rows.forEach { host.addView(reelCard(it), margin(dp(9))) }
        }

        root.addView(button("جستجو", false) { render(search.text.toString()) }, margin(dp(8)))
        root.addView(button("مقایسه ریلزهای انتخاب‌شده", false) { showComparison() }, margin(dp(10)))
        root.addView(host)
        render("")
    }

    private fun learningsPage(): View = page("آموخته‌ها", "آنچه TakeOff از شواهد واقعی یاد گرفته، بدون ادعای تضمین") { root ->
        val active = accountStore.selected()?.normalizedHandle.orEmpty()
        val niche = nicheFor(active)
        val text = body("حوزه: $niche\nبرای دریافت آخرین حافظه الگوها از سرور بزن.")
        root.addView(card().apply {
            addView(section("حافظه یادگیری"))
            addView(text)
            addView(button("به‌روزرسانی آموخته‌ها", true) { loadLearnings(text, niche) })
        }, margin(dp(12)))

        val local = insightStore.reels(active).filter { it.metrics.isNotEmpty() }
        if (local.isNotEmpty()) {
            val bestShare = local.maxByOrNull { rate(it, "shares") }
            val bestSave = local.maxByOrNull { rate(it, "saves") }
            root.addView(card().apply {
                addView(section("برداشت سریع از این پیج"))
                addView(body("بیشترین نسبت اشتراک: ${bestShare?.shortcode ?: "—"}\nبیشترین نسبت ذخیره: ${bestSave?.shortcode ?: "—"}\nنمونه‌های دارای داده: ${local.size}\n\nاین مقایسه‌ها توصیفی‌اند، نه اثبات علت."))
            }, margin(0))
        }
    }

    private fun accountsPage(): View = page("پیج‌ها", "مدیریت پیج‌ها، اتصال و پشتیبان امن") { root ->
        val selected = accountStore.selected()?.normalizedHandle
        accountStore.all().forEach { account ->
            val reels = insightStore.reels(account.normalizedHandle)
            root.addView(card().apply {
                addView(section(if (account.normalizedHandle == selected) "پیج فعال" else "پیج"))
                addView(big("${account.label}  @${account.normalizedHandle}", Color.WHITE))
                addView(body("حوزه: ${nicheFor(account.normalizedHandle)} • ${reels.size} ریلز"))
                if (account.normalizedHandle != selected) {
                    addView(button("فعال کردن", false) {
                        accountStore.select(account.normalizedHandle)
                        TakeoffSound.play(TakeoffSound.Cue.SUCCESS)
                        showTab(4)
                    })
                }
            }, margin(dp(9)))
        }
        root.addView(button("+ افزودن پیج", true) { addAccountDialog() }, margin(dp(12)))

        val prefs = getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE)
        val endpoint = field("آدرس سرور تیک‌آف", prefs.getString("endpoint", "").orEmpty())
        val key = field("کلید اتصال", secretStore.get("api_key").orEmpty()).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        root.addView(card().apply {
            addView(section("تنظیمات اتصال"))
            addView(endpoint)
            addView(key)
            addView(button("ذخیره اتصال", false) {
                try {
                    PayloadClient.validateEndpoint(endpoint.text.toString())
                    prefs.edit().putString("endpoint", endpoint.text.toString().trim()).apply()
                    secretStore.put("api_key", key.text.toString())
                    TakeoffSound.play(TakeoffSound.Cue.SUCCESS)
                    toast("اتصال ذخیره شد")
                } catch (_: Exception) {
                    endpoint.error = "آدرس HTTPS معتبر لازم است"
                    TakeoffSound.play(TakeoffSound.Cue.ERROR)
                }
            })
        }, margin(dp(12)))

        root.addView(card().apply {
            addView(section("پشتیبان امن"))
            addView(body("فهرست پیج‌ها، ریلزها و اتصال سناریوها به‌صورت رمزگذاری‌شده روی همین دستگاه پشتیبان می‌شود."))
            addView(button("ساخت و بررسی پشتیبان", false) {
                val file = BackupManager.create(this@MainActivity)
                val ok = BackupManager.verify(this@MainActivity, file)
                TakeoffSound.play(if (ok) TakeoffSound.Cue.SUCCESS else TakeoffSound.Cue.ERROR)
                toast(if (ok) "پشتیبان امن ساخته شد" else "پشتیبان ناموفق بود")
            })
        }, margin(0))
    }

    private fun reelSummary(reel: LocalInsightStore.Reel): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(7), 0, dp(7))
        addView(TextView(this@MainActivity).apply {
            text = "${reel.shortcode} • ${stateFa(reel.state)}"
            textSize = 14f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.START
        })
        addView(TextView(this@MainActivity).apply {
            text = metricsLine(reel.metrics)
            textSize = 12f
            setTextColor(muted)
            gravity = Gravity.START
        })
    }

    private fun reelCard(reel: LocalInsightStore.Reel): View {
        val key = "${reel.accountId}::${reel.shortcode}"
        return card().apply {
            val check = CheckBox(this@MainActivity).apply {
                text = "${reel.shortcode} • ${stateFa(reel.state)}"
                textSize = 14.5f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                isChecked = key in selectedForCompare
                setOnCheckedChangeListener { button, checked ->
                    if (checked && selectedForCompare.size >= 5) {
                        button.isChecked = false
                        toast("حداکثر ۵ ریلز")
                    } else if (checked) {
                        selectedForCompare += key
                    } else {
                        selectedForCompare -= key
                    }
                }
            }
            addView(check)
            addView(body("${reel.scenarioId?.let { "سناریو $it • " }.orEmpty()}${metricsLine(reel.metrics)}\nتطابق اجرا: ${fidelityFa(reel.executionFidelity)}"))
            setOnClickListener { showReelDetail(reel) }
        }
    }

    private fun showReelDetail(reel: LocalInsightStore.Reel) {
        val message = buildString {
            append("پیج: @${reel.accountId}\n")
            append("سناریو: ${reel.scenarioId ?: "نامشخص"}\n")
            append("وضعیت: ${stateFa(reel.state)}\n")
            append("تطابق اجرا: ${fidelityFa(reel.executionFidelity)}\n\n")
            append(metricsLine(reel.metrics))
            append("\n\nShare/View: ${pct(rate(reel, "shares"))}")
            append("\nSave/View: ${pct(rate(reel, "saves"))}")
            append("\nComment/View: ${pct(rate(reel, "comments"))}")
        }
        AlertDialog.Builder(this).setTitle("تحلیل ${reel.shortcode}").setMessage(message).setPositiveButton("بستن", null).show()
    }

    private fun showComparison() {
        if (selectedForCompare.size < 2) {
            toast("حداقل ۲ ریلز را انتخاب کن")
            return
        }
        val map = insightStore.reels().associateBy { "${it.accountId}::${it.shortcode}" }
        val rows = selectedForCompare.mapNotNull { map[it] }
        val text = rows.joinToString("\n\n") { reel ->
            "${reel.shortcode}\n${metricsLine(reel.metrics)}\nShare/View ${pct(rate(reel, "shares"))} • Save/View ${pct(rate(reel, "saves"))}"
        }
        AlertDialog.Builder(this).setTitle("مقایسه ${rows.size} ریلز").setMessage(text).setPositiveButton("بستن", null).show()
    }

    private fun scenarioDialog(account: String, shortcode: String) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(18), dp(8), dp(18), 0)
        }
        val id = field("شناسه سناریو", "")
        val title = field("عنوان کوتاه سناریو", "")
        box.addView(id)
        box.addView(title)
        val scenarios = insightStore.scenarios(account)
        val labels = mutableListOf("بدون اتصال")
        labels += scenarios.map { "${it.scenarioId} — ${it.title}" }
        val spinner = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, labels) }
        box.addView(spinner)
        val dialog = AlertDialog.Builder(this)
            .setTitle("سناریوی در انتظار انتشار")
            .setView(box)
            .setNegativeButton("بستن", null)
            .setPositiveButton("ذخیره/اتصال", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newId = id.text.toString().trim()
                if (newId.isNotBlank()) {
                    insightStore.addScenario(account, newId, title.text.toString())
                    if (shortcode.isNotBlank()) insightStore.bindScenario(account, shortcode, newId)
                } else if (shortcode.isNotBlank()) {
                    val picked = scenarios.getOrNull(spinner.selectedItemPosition - 1)?.scenarioId
                    insightStore.bindScenario(account, shortcode, picked)
                }
                TakeoffSound.play(TakeoffSound.Cue.SUCCESS)
                dialog.dismiss()
                showTab(1)
            }
        }
        dialog.show()
    }

    private fun addAccountDialog() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(18), dp(8), dp(18), 0)
        }
        val label = field("نام کارفرما / پیج", "")
        val handle = field("آیدی اینستاگرام، مثال: client.page", "")
        val niche = field("حوزه، مثال: باربری", "")
        box.addView(label)
        box.addView(handle)
        box.addView(niche)
        val dialog = AlertDialog.Builder(this)
            .setTitle("افزودن پیج")
            .setView(box)
            .setNegativeButton("انصراف", null)
            .setPositiveButton("ذخیره", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                try {
                    val account = accountStore.upsert(label.text.toString(), handle.text.toString())
                    getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE).edit()
                        .putString("niche_${account.normalizedHandle}", niche.text.toString().trim().ifBlank { "عمومی" })
                        .apply()
                    TakeoffSound.play(TakeoffSound.Cue.SUCCESS)
                    dialog.dismiss()
                    showTab(4)
                } catch (_: IllegalArgumentException) {
                    handle.error = "آیدی معتبر نیست"
                    TakeoffSound.play(TakeoffSound.Cue.ERROR)
                }
            }
        }
        dialog.show()
    }

    private fun checkHealth(text: TextView) {
        val endpoint = getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE).getString("endpoint", "").orEmpty()
        if (endpoint.isBlank()) {
            text.text = "اول اتصال سرور را در بخش پیج‌ها تنظیم کن."
            return
        }
        text.text = "در حال بررسی…"
        Thread {
            val result = runCatching { PayloadClient.getHealth(endpoint) }
            runOnUiThread {
                val pair = result.getOrNull()
                if (pair == null || pair.first !in 200..299) {
                    text.text = "اتصال برقرار نشد."
                    TakeoffSound.play(TakeoffSound.Cue.ERROR)
                    return@runOnUiThread
                }
                val root = runCatching { JSONObject(pair.second) }.getOrNull()
                val gt = root?.optJSONObject("owner_ground_truth")
                val configured = gt?.optBoolean("configured", false) == true
                val available = gt?.optBoolean("available", false) == true
                val required = gt?.optBoolean("required", false) == true
                text.text = if (configured && available) {
                    "● همه‌چیز آماده\nGround Truth پایدار فعال است."
                } else {
                    "● نیاز به بررسی\nconfigured=$configured • available=$available • required=$required"
                }
                TakeoffSound.play(if (configured && available) TakeoffSound.Cue.SUCCESS else TakeoffSound.Cue.WARNING)
            }
        }.start()
    }

    private fun loadLearnings(text: TextView, niche: String) {
        val endpoint = getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE).getString("endpoint", "").orEmpty()
        if (endpoint.isBlank()) {
            text.text = "آدرس سرور تنظیم نشده."
            return
        }
        text.text = "در حال خواندن حافظه الگوها…"
        Thread {
            val patterns = runCatching { PayloadClient.getPatterns(endpoint, niche) }.getOrNull()
            val readiness = runCatching { PayloadClient.getReadiness(endpoint, niche) }.getOrNull()
            runOnUiThread {
                if (patterns == null || patterns.first !in 200..299) {
                    text.text = "برای حوزه «$niche» هنوز حافظه قابل نمایش نیست."
                    TakeoffSound.play(TakeoffSound.Cue.WARNING)
                    return@runOnUiThread
                }
                val arr = runCatching { JSONArray(patterns.second) }.getOrElse { JSONArray() }
                val lines = mutableListOf<String>()
                for (i in 0 until minOf(arr.length(), 5)) {
                    val item = arr.optJSONObject(i) ?: continue
                    val key = item.optString("pattern", item.optString("key", "الگو"))
                    val samples = item.optInt("samples", item.optInt("support", 0))
                    lines += "• $key — شواهد: $samples نمونه"
                }
                val readinessObj = readiness?.second?.let { runCatching { JSONObject(it) }.getOrNull() }
                val readinessText = if (readinessObj != null) {
                    "آمادگی یادگیری: ${if (readinessObj.optBoolean("ready", false)) "قابل استفاده" else "هنوز محدود"}\n\n"
                } else ""
                text.text = readinessText + (if (lines.isEmpty()) "هنوز الگوی پایداری برای نمایش نیست." else lines.joinToString("\n")) + "\n\nاین‌ها ارتباط آماری‌اند، نه تضمین نتیجه."
                TakeoffSound.play(TakeoffSound.Cue.SUCCESS)
            }
        }.start()
    }

    private fun handleShared(intent: Intent) {
        if (intent.action != Intent.ACTION_SEND || intent.type != "text/plain") return
        val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        val url = Regex("https?://(?:www\\.)?instagram\\.com/(?:reel|p)/[A-Za-z0-9_-]+/?").find(text)?.value ?: return
        val shortcode = url.trimEnd('/').substringAfterLast('/')
        val account = accountStore.selected()?.normalizedHandle.orEmpty()
        getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE).edit()
            .putString("reel_url", url)
            .putString("shortcode", shortcode)
            .putString("armed_account", account)
            .apply()
        insightStore.armReel(account, shortcode, url)
        TakeoffSound.play(TakeoffSound.Cue.CAPTURE)
    }

    private fun startCapture() {
        val prefs = getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE)
        val active = accountStore.selected()?.normalizedHandle.orEmpty()
        val armed = prefs.getString("armed_account", "").orEmpty()
        if (prefs.getString("endpoint", "").isNullOrBlank() || secretStore.get("api_key").isNullOrBlank()) {
            toast("ابتدا اتصال سرور را ذخیره کن")
            showTab(4)
            return
        }
        if (prefs.getString("shortcode", "").isNullOrBlank()) {
            toast("اول ریلز را از اینستاگرام به تیک‌آف Share کن")
            return
        }
        if (armed.isNotBlank() && armed != active) {
            TakeoffSound.play(TakeoffSound.Cue.ERROR)
            toast("ریلز برای پیج دیگری قفل شده؛ دوباره Share کن")
            return
        }
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(manager.createScreenCaptureIntent())
    }

    private fun scheduleReminders() {
        val request = PeriodicWorkRequestBuilder<PendingWorker>(1, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "takeoff-owner-insights-pending",
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    private fun nicheFor(handle: String): String = getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE)
        .getString("niche_$handle", "عمومی").orEmpty().ifBlank { "عمومی" }

    private fun rate(reel: LocalInsightStore.Reel, key: String): Double {
        val views = reel.metrics["views"] ?: return 0.0
        if (views <= 0.0) return 0.0
        return (reel.metrics[key] ?: 0.0) / views
    }

    private fun pct(value: Double) = DecimalFormat("0.00%").format(value)

    private fun metricsLine(metrics: Map<String, Double>): String {
        fun value(name: String): String = metrics[name]?.let { formatNumber(it) } ?: "—"
        return "بازدید ${value("views")} • اشتراک ${value("shares")} • ذخیره ${value("saves")}"
    }

    private fun formatNumber(value: Double): String = when {
        value >= 1_000_000 -> DecimalFormat("0.0M").format(value / 1_000_000)
        value >= 1_000 -> DecimalFormat("0.0K").format(value / 1_000)
        else -> DecimalFormat("0").format(value)
    }

    private fun stateFa(value: String) = when (value) {
        "recorded" -> "ثبت قطعی"
        "queued" -> "در صف امن"
        "review" -> "منتظر بررسی"
        "armed" -> "آماده ثبت"
        else -> value
    }

    private fun fidelityFa(value: String) = when (value) {
        "complete" -> "کامل طبق سناریو"
        "minor_changes" -> "تغییر جزئی"
        "major_changes" -> "تغییر زیاد"
        else -> "نامشخص"
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setPadding(dp(16), dp(15), dp(16), dp(15))
        background = rounded(panel, 18f, border)
    }

    private fun section(value: String) = TextView(this).apply {
        text = value
        textSize = 12.5f
        setTextColor(accent)
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.START
        setPadding(0, 0, 0, dp(8))
    }

    private fun big(value: String, color: Int) = TextView(this).apply {
        text = value
        textSize = 20f
        setTextColor(color)
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.START
        setPadding(0, 0, 0, dp(6))
    }

    private fun body(value: String) = TextView(this).apply {
        text = value
        textSize = 13.2f
        setTextColor(muted)
        gravity = Gravity.START
        setLineSpacing(0f, 1.18f)
        setPadding(0, 0, 0, dp(8))
    }

    private fun field(hintText: String, value: String) = EditText(this).apply {
        hint = hintText
        setHintTextColor(Color.rgb(105, 116, 132))
        setTextColor(Color.WHITE)
        textSize = 14.5f
        setText(value)
        gravity = Gravity.START or Gravity.CENTER_VERTICAL
        setSingleLine(true)
        setPadding(dp(14), 0, dp(14), 0)
        background = rounded(panel2, 13f, border)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52)).apply { bottomMargin = dp(9) }
    }

    private fun button(label: String, primary: Boolean, click: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 13.5f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(if (primary) Color.rgb(5, 15, 18) else Color.WHITE)
        background = rounded(if (primary) accent else panel2, 14f, if (primary) accent else border)
        setOnClickListener { click() }
    }

    private fun rounded(fill: Int, radius: Float, stroke: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(fill)
        cornerRadius = dp(radius.toInt()).toFloat()
        if (stroke != Color.TRANSPARENT) setStroke(dp(1), stroke)
    }

    private fun margin(bottom: Int) = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
        bottomMargin = bottom
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
