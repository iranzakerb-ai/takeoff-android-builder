package ai.takeoff.insightscompanion

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject

class GlassHomeActivity : Activity() {
    private val bg0 = Color.rgb(5, 8, 15)
    private val bg1 = Color.rgb(12, 26, 39)
    private val glass = Color.argb(82, 255, 255, 255)
    private val glassStrong = Color.argb(112, 255, 255, 255)
    private val border = Color.argb(78, 255, 255, 255)
    private val accent = Color.rgb(79, 235, 216)
    private val muted = Color.rgb(188, 200, 214)
    private val success = Color.rgb(102, 235, 173)
    private val warning = Color.rgb(255, 194, 92)

    private lateinit var statusText: TextView
    private lateinit var ownerCount: TextView
    private lateinit var viralCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = bg0
        setContentView(buildUi())
        recoverOldPublicShareFailures()
        refreshLocal()
        refreshServer()
    }

    override fun onResume() {
        super.onResume()
        recoverOldPublicShareFailures()
        refreshLocal()
    }

    private fun recoverOldPublicShareFailures() {
        SharedMediaQueue(this).recoverLegacyCredentialFailures().forEach {
            SharedMediaWork.enqueue(this, it)
        }
    }

    private fun buildUi(): View {
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(bg0, bg1, Color.rgb(6, 13, 25)),
            )
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(18), dp(28), dp(18), dp(30))
        }

        root.addView(TextView(this).apply {
            text = "TakeOff Insights"
            textSize = 30f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.START
        })
        root.addView(TextView(this).apply {
            text = "دو کار. یک حلقه یادگیری."
            textSize = 14f
            setTextColor(muted)
            gravity = Gravity.START
            setPadding(0, dp(5), 0, dp(24))
        })

        root.addView(actionCard(
            eyebrow = "۱  •  آمار واقعی پیج‌ها",
            title = "ثبت آمار ریلزهای خودمان",
            body = "Retention، Watch Time، Share/Save و Outcome سناریوهای آژانس و کارفرما را ثبت کن تا ایجنت از نتیجه واقعی یاد بگیرد.",
            buttonText = "ثبت یا مشاهده آمار",
            onClick = { startActivity(Intent(this, MainActivity::class.java)) },
        ).also { card ->
            ownerCount = card.findViewWithTag("count")
        }, margin(dp(14)))

        root.addView(actionCard(
            eyebrow = "۲  •  یادگیری از ریلزهای وایرال",
            title = "ریلز میلیونی را Share کن",
            body = "از Instagram به تیک‌آف Share کن؛ سرور ویدیو را می‌گیرد و هوک، دیالوگ، سناریو، CTA، تصویر و مکانیزم‌های وایرال را تحلیل می‌کند.",
            buttonText = "صف ریلزهای وایرال",
            onClick = { startActivity(Intent(this, ViralShareActivity::class.java)) },
        ).also { card ->
            viralCount = card.findViewWithTag("count")
        }, margin(dp(18)))

        val status = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(Color.argb(62, 255, 255, 255), 18, border)
        }
        statusText = TextView(this).apply {
            text = "در حال بررسی اتصال…"
            textSize = 12.5f
            setTextColor(muted)
            gravity = Gravity.START
        }
        status.addView(statusText, LinearLayout.LayoutParams(0, -2, 1f))
        status.addView(Button(this).apply {
            text = "بررسی"
            isAllCaps = false
            textSize = 11.5f
            setTextColor(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(Color.argb(45, 255, 255, 255))
            setOnClickListener { refreshServer() }
        }, LinearLayout.LayoutParams(dp(88), dp(42)))
        root.addView(status)

        root.addView(TextView(this).apply {
            text = "هدف محصول فقط همین دو مسیر است؛ جزئیات فنی پشت صحنه می‌ماند."
            textSize = 11.5f
            setTextColor(Color.argb(170, 188, 200, 214))
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(18), dp(10), 0)
        })

        scroll.addView(root)
        return scroll
    }

    private fun actionCard(
        eyebrow: String,
        title: String,
        body: String,
        buttonText: String,
        onClick: () -> Unit,
    ): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setPadding(dp(18), dp(18), dp(18), dp(18))
        background = rounded(glassStrong, 26, border)
        elevation = dp(5).toFloat()

        addView(TextView(this@GlassHomeActivity).apply {
            text = eyebrow
            textSize = 11.5f
            setTextColor(accent)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.START
        })
        addView(TextView(this@GlassHomeActivity).apply {
            text = title
            textSize = 23f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.START
            setPadding(0, dp(7), 0, 0)
        })
        addView(TextView(this@GlassHomeActivity).apply {
            text = body
            textSize = 13f
            setTextColor(muted)
            gravity = Gravity.START
            setLineSpacing(0f, 1.2f)
            setPadding(0, dp(10), 0, dp(12))
        })
        addView(TextView(this@GlassHomeActivity).apply {
            tag = "count"
            text = ""
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.START
            setPadding(0, 0, 0, dp(12))
        })
        addView(Button(this@GlassHomeActivity).apply {
            text = buttonText
            isAllCaps = false
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(4, 24, 24))
            background = rounded(accent, 18, accent)
            setOnClickListener { onClick() }
        }, LinearLayout.LayoutParams(-1, dp(56)))
    }

    private fun refreshLocal() {
        if (!::ownerCount.isInitialized || !::viralCount.isInitialized) return
        val ownerQueue = PayloadQueue(this).size()
        val account = runCatching { ManagedAccountStore(this).selected()?.normalizedHandle }.getOrNull().orEmpty()
        ownerCount.text = "پیج فعال: @${account.ifBlank { "—" }}  •  ثبت‌های در صف: $ownerQueue"

        val items = SharedMediaQueue(this).all()
        val active = items.count { it.status in setOf("queued", "submitting", "processing", "failed") }
        val done = items.count { it.status == "completed" }
        val errors = items.count { it.status == "dead_letter" }
        viralCount.text = "در حال تحلیل: $active  •  یادگرفته‌شده: $done${if (errors > 0) "  •  نیاز به تلاش مجدد: $errors" else ""}"
    }

    private fun refreshServer() {
        if (!::statusText.isInitialized) return
        statusText.text = "در حال بررسی اتصال…"
        statusText.setTextColor(muted)
        val endpoint = PayloadClient.VIRAL_PRODUCTION_ENDPOINT
        Thread {
            val health = runCatching {
                val url = java.net.URL(endpoint.trimEnd('/') + "/v4/media-jobs/health")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 15_000
                conn.readTimeout = 20_000
                conn.requestMethod = "GET"
                try {
                    val code = conn.responseCode
                    val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                        ?.bufferedReader()?.use { it.readText() }.orEmpty()
                    code to body
                } finally {
                    conn.disconnect()
                }
            }.getOrNull()
            runOnUiThread {
                val h = health?.second?.let { runCatching { JSONObject(it) }.getOrNull() }
                val ok = health?.first in 200..299 && h?.optBoolean("ok", false) == true
                statusText.text = if (ok) "● سیستم آماده و متصل است" else "● اتصال کامل نیست؛ دوباره بررسی کن"
                statusText.setTextColor(if (ok) success else warning)
            }
        }.start()
    }

    private fun rounded(fill: Int, radius: Int, stroke: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(fill)
        cornerRadius = dp(radius).toFloat()
        setStroke(dp(1), stroke)
    }

    private fun margin(bottom: Int) = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = bottom }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
