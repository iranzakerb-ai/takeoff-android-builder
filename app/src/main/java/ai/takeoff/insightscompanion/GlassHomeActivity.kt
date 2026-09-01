package ai.takeoff.insightscompanion

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject

class GlassHomeActivity : Activity() {
    private val white = Color.rgb(250, 252, 255)
    private val ink = Color.rgb(15, 23, 35)
    private val muted = Color.rgb(94, 108, 126)
    private val orange = Color.rgb(255, 122, 26)
    private val teal = Color.rgb(16, 202, 205)
    private val navy = Color.rgb(9, 24, 43)
    private val glass = Color.argb(210, 255, 255, 255)
    private val glassStrong = Color.argb(238, 255, 255, 255)
    private val border = Color.argb(115, 255, 255, 255)
    private val success = Color.rgb(0, 154, 137)
    private val warning = Color.rgb(226, 112, 18)

    private lateinit var statusText: TextView
    private lateinit var ownerCount: TextView
    private lateinit var viralCount: TextView
    private lateinit var motionRoot: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = white
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        setContentView(buildUi())
        playEntrance()
        ensureNotificationPermission()
        recoverOldPublicShareFailures()
        refreshLocal()
        refreshServer()
    }

    override fun onResume() {
        super.onResume()
        recoverOldPublicShareFailures()
        refreshLocal()
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1201)
        }
    }

    private fun recoverOldPublicShareFailures() {
        SharedMediaQueue(this).recoverLegacyCredentialFailures().forEach { SharedMediaWork.enqueue(this, it) }
    }

    private fun buildUi(): View {
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(white, Color.rgb(243, 252, 253), Color.rgb(255, 246, 237)))
        }
        motionRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(18), dp(30), dp(18), dp(30))
        }
        motionRoot.addView(TextView(this).apply {
            text = "TakeOff Insights"
            textSize = 31f
            setTextColor(ink)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.START
        })
        motionRoot.addView(TextView(this).apply {
            text = "هوشمندتر تحلیل کن، قوی‌تر محتوا بساز"
            textSize = 13.5f
            setTextColor(muted)
            gravity = Gravity.START
            setPadding(0, dp(5), 0, dp(22))
        })
        motionRoot.addView(actionCard("۱  •  آمار واقعی پیج‌ها", "ثبت آمار ریلزهای خودمان", "Retention، Watch Time، Share/Save و Outcome سناریوها را ثبت کن تا ایجنت از نتیجه واقعی یاد بگیرد.", "ثبت یا مشاهده آمار", orange) {
            launchWithFade(Intent(this, MainActivity::class.java))
        }.also { ownerCount = it.findViewWithTag("count") }, margin(dp(14)))
        motionRoot.addView(actionCard("۲  •  یادگیری از ریلزهای وایرال", "ریلز میلیونی را Share کن", "از Instagram به تیک‌آف Share کن؛ هوک، دیالوگ، سناریو، CTA، تصویر و مکانیزم‌های وایرال تحلیل می‌شوند.", "صف ریلزهای وایرال", teal) {
            launchWithFade(Intent(this, ViralShareActivity::class.java))
        }.also { viralCount = it.findViewWithTag("count") }, margin(dp(18)))

        val status = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(glass, 20, Color.argb(75, 16, 202, 205))
            elevation = dp(3).toFloat()
        }
        statusText = TextView(this).apply { text = "در حال بررسی اتصال…"; textSize = 12.5f; setTextColor(muted); gravity = Gravity.START }
        status.addView(statusText, LinearLayout.LayoutParams(0, -2, 1f))
        status.addView(Button(this).apply {
            text = "بررسی"; isAllCaps = false; textSize = 11.5f; setTextColor(navy)
            backgroundTintList = ColorStateList.valueOf(Color.argb(38, 16, 202, 205))
            setOnClickListener { tapMotion(this); refreshServer() }
        }, LinearLayout.LayoutParams(dp(88), dp(42)))
        motionRoot.addView(status)
        motionRoot.addView(TextView(this).apply {
            text = "دو مسیر اصلی؛ جزئیات فنی پشت صحنه می‌ماند."
            textSize = 11.5f; setTextColor(muted); gravity = Gravity.CENTER; setPadding(dp(10), dp(18), dp(10), 0)
        })
        scroll.addView(motionRoot)
        return scroll
    }

    private fun actionCard(eyebrow: String, title: String, body: String, buttonText: String, accent: Int, onClick: () -> Unit): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(18), dp(18), dp(18), dp(18))
        background = rounded(glassStrong, 27, border); elevation = dp(7).toFloat()
        addView(TextView(this@GlassHomeActivity).apply { text = eyebrow; textSize = 11.5f; setTextColor(accent); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.START })
        addView(TextView(this@GlassHomeActivity).apply { text = title; textSize = 23f; setTextColor(ink); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.START; setPadding(0, dp(7), 0, 0) })
        addView(TextView(this@GlassHomeActivity).apply { text = body; textSize = 13f; setTextColor(muted); gravity = Gravity.START; setLineSpacing(0f, 1.2f); setPadding(0, dp(10), 0, dp(12)) })
        addView(TextView(this@GlassHomeActivity).apply { tag = "count"; textSize = 12f; setTextColor(navy); gravity = Gravity.START; setPadding(0, 0, 0, dp(12)) })
        addView(Button(this@GlassHomeActivity).apply {
            text = buttonText; isAllCaps = false; textSize = 14f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); background = rounded(accent, 18, accent)
            setOnClickListener { tapMotion(this); postDelayed({ onClick() }, 110) }
        }, LinearLayout.LayoutParams(-1, dp(56)))
    }

    private fun playEntrance() {
        motionRoot.alpha = 0f; motionRoot.translationY = dp(24).toFloat(); motionRoot.scaleX = .985f; motionRoot.scaleY = .985f
        motionRoot.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f).setDuration(620).setInterpolator(DecelerateInterpolator()).start()
    }

    private fun tapMotion(view: View) {
        view.animate().scaleX(.97f).scaleY(.97f).alpha(.82f).setDuration(80).withEndAction { view.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(150).start() }.start()
    }

    private fun launchWithFade(intent: Intent) {
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun refreshLocal() {
        if (!::ownerCount.isInitialized || !::viralCount.isInitialized) return
        val ownerQueue = PayloadQueue(this).size()
        val account = runCatching { ManagedAccountStore(this).selected()?.normalizedHandle }.getOrNull().orEmpty()
        ownerCount.text = "پیج فعال: @${account.ifBlank { "—" }}  •  ثبت‌های در صف: $ownerQueue"
        val items = SharedMediaQueue(this).all(); val active = items.count { it.status in setOf("queued", "submitting", "processing", "failed") }; val done = items.count { it.status == "completed" }; val errors = items.count { it.status == "dead_letter" }
        viralCount.text = "در حال تحلیل: $active  •  یادگرفته‌شده: $done${if (errors > 0) "  •  نیاز به تلاش مجدد: $errors" else ""}"
    }

    private fun refreshServer() {
        if (!::statusText.isInitialized) return
        statusText.text = "در حال بررسی اتصال…"; statusText.setTextColor(muted)
        val endpoint = PayloadClient.VIRAL_PRODUCTION_ENDPOINT
        Thread {
            val health = runCatching {
                val conn = java.net.URL(endpoint.trimEnd('/') + "/v4/media-jobs/health").openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 15_000; conn.readTimeout = 20_000; conn.requestMethod = "GET"
                try { val code = conn.responseCode; val body = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty(); code to body } finally { conn.disconnect() }
            }.getOrNull()
            runOnUiThread {
                val h = health?.second?.let { runCatching { JSONObject(it) }.getOrNull() }; val ok = health?.first in 200..299 && h?.optBoolean("ok", false) == true
                statusText.animate().alpha(0f).setDuration(110).withEndAction { statusText.text = if (ok) "● سیستم آماده و متصل است" else "● اتصال کامل نیست؛ دوباره بررسی کن"; statusText.setTextColor(if (ok) success else warning); statusText.animate().alpha(1f).setDuration(220).start() }.start()
            }
        }.start()
    }

    private fun rounded(fill: Int, radius: Int, stroke: Int) = GradientDrawable().apply { shape = GradientDrawable.RECTANGLE; setColor(fill); cornerRadius = dp(radius).toFloat(); setStroke(dp(1), stroke) }
    private fun margin(bottom: Int) = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = bottom }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
