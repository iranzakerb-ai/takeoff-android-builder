package ai.takeoff.insightscompanion

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

object LovableUi {
    val background = Color.rgb(252, 250, 247)
    val foreground = Color.rgb(43, 36, 31)
    val card = Color.WHITE
    val primary = Color.rgb(245, 104, 43)
    val primaryDeep = Color.rgb(234, 80, 34)
    val primarySoft = Color.rgb(255, 241, 232)
    val secondary = Color.rgb(111, 218, 204)
    val secondarySoft = Color.rgb(229, 250, 247)
    val secondaryText = Color.rgb(42, 99, 96)
    val muted = Color.rgb(113, 105, 98)
    val mutedBg = Color.rgb(246, 243, 239)
    val border = Color.rgb(234, 228, 221)
    val success = Color.rgb(69, 174, 119)
    val warning = Color.rgb(202, 145, 38)
    val danger = Color.rgb(204, 72, 64)
    val navy = Color.rgb(31, 38, 46)

    fun applyWindow(activity: Activity) {
        activity.window.statusBarColor = LovableUi.background
        activity.window.navigationBarColor = LovableUi.background
        activity.window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
    }

    fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    fun Context.rounded(fill: Int, radius: Int = 18, stroke: Int = LovableUi.border, strokeWidth: Int = 1): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fill)
            cornerRadius = dp(radius).toFloat()
            if (strokeWidth > 0) setStroke(dp(strokeWidth), stroke)
        }

    fun Context.brandGradient(radius: Int = 28): GradientDrawable = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(Color.rgb(255, 126, 46), LovableUi.primaryDeep),
    ).apply { cornerRadius = dp(radius).toFloat() }

    fun Context.card(strong: Boolean = false): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setPadding(dp(16), dp(15), dp(16), dp(15))
        background = rounded(if (strong) Color.WHITE else LovableUi.card, 24, LovableUi.border)
        elevation = dp(if (strong) 4 else 2).toFloat()
    }

    fun Context.text(value: String, size: Float, color: Int = LovableUi.foreground, bold: Boolean = false): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        gravity = Gravity.START
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        textDirection = View.TEXT_DIRECTION_RTL
        typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        setLineSpacing(0f, 1.22f)
    }

    fun Context.sectionTitle(value: String): TextView = text(value, 13f, LovableUi.foreground, true)

    fun Context.chip(value: String, tone: String = "muted"): TextView {
        val fill: Int
        val fg: Int
        when (tone) {
            "primary" -> { fill = LovableUi.primarySoft; fg = LovableUi.primary }
            "secondary" -> { fill = LovableUi.secondarySoft; fg = LovableUi.secondaryText }
            "success" -> { fill = Color.rgb(235, 249, 241); fg = LovableUi.success }
            "danger" -> { fill = Color.rgb(254, 238, 236); fg = LovableUi.danger }
            "warning" -> { fill = Color.rgb(253, 246, 225); fg = LovableUi.warning }
            else -> { fill = LovableUi.mutedBg; fg = LovableUi.muted }
        }
        return text(value, 11f, fg, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(5), dp(10), dp(5))
            background = rounded(fill, 20, fill, 0)
        }
    }

    fun Context.primaryButton(value: String, onClick: () -> Unit): Button = Button(this).apply {
        text = value
        isAllCaps = false
        textSize = 13.5f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.WHITE)
        background = brandGradient(18)
        elevation = dp(3).toFloat()
        setOnClickListener {
            animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).withEndAction {
                animate().scaleX(1f).scaleY(1f).setDuration(160).start()
                onClick()
            }.start()
        }
    }

    fun Context.ghostButton(value: String, onClick: () -> Unit): Button = Button(this).apply {
        text = value
        isAllCaps = false
        textSize = 13f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(LovableUi.foreground)
        background = rounded(Color.WHITE, 18, LovableUi.border)
        setOnClickListener { onClick() }
    }

    fun Context.topBar(title: String, subtitle: String? = null, back: (() -> Unit)? = null): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(16), dp(11), dp(16), dp(11))
        background = rounded(Color.argb(248, 252, 250, 247), 0, LovableUi.border)
        if (back != null) {
            addView(TextView(this@topBar).apply {
                text = "‹"
                textSize = 28f
                gravity = Gravity.CENTER
                setTextColor(LovableUi.foreground)
                background = rounded(LovableUi.mutedBg, 18, LovableUi.mutedBg, 0)
                setOnClickListener { back() }
            }, LinearLayout.LayoutParams(dp(38), dp(38)).apply { marginEnd = dp(10) })
        } else {
            addView(ImageView(this@topBar).apply {
                setImageResource(R.drawable.ic_takeoff_logo)
                setPadding(dp(7), dp(7), dp(7), dp(7))
                background = rounded(LovableUi.navy, 14, LovableUi.navy, 0)
            }, LinearLayout.LayoutParams(dp(38), dp(38)).apply { marginEnd = dp(10) })
        }
        addView(LinearLayout(this@topBar).apply {
            orientation = LinearLayout.VERTICAL
            addView(text(title, 15f, LovableUi.foreground, true))
            subtitle?.let { addView(text(it, 11f, LovableUi.muted, false)) }
        }, LinearLayout.LayoutParams(0, -2, 1f))
    }

    fun Activity.bottomNav(active: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        gravity = Gravity.CENTER
        setPadding(dp(5), dp(7), dp(5), dp(9))
        background = rounded(Color.argb(252, 255, 255, 255), 0, LovableUi.border)
        fun item(label: String, key: String, target: Class<out Activity>?, primaryAction: Boolean = false) {
            val host = LinearLayout(this@bottomNav).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(2), 0, dp(2), 0)
                if (primaryAction) {
                    addView(TextView(this@bottomNav).apply {
                        text = "+"
                        textSize = 28f
                        gravity = Gravity.CENTER
                        setTextColor(Color.WHITE)
                        background = brandGradient(18)
                    }, LinearLayout.LayoutParams(dp(50), dp(50)))
                } else {
                    addView(TextView(this@bottomNav).apply {
                        text = when (key) {
                            "home" -> "⌂"
                            "memory" -> "◇"
                            "saved" -> "▣"
                            "settings" -> "⚙"
                            else -> "•"
                        }
                        textSize = 20f
                        gravity = Gravity.CENTER
                        setTextColor(if (active == key) LovableUi.primary else LovableUi.muted)
                    }, LinearLayout.LayoutParams(dp(32), dp(30)))
                }
                addView(text(label, 10f, if (active == key || primaryAction) LovableUi.primary else LovableUi.muted, active == key || primaryAction).apply { gravity = Gravity.CENTER })
                setOnClickListener {
                    if (target != null && this@bottomNav::class.java != target) {
                        startActivity(Intent(this@bottomNav, target).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
                    }
                }
            }
            addView(host, LinearLayout.LayoutParams(0, -2, 1f))
        }
        item("خانه", "home", GlassHomeActivity::class.java)
        item("حافظه", "memory", MemoryActivity::class.java)
        item("تحلیل جدید", "new", NewAnalysisActivity::class.java, true)
        item("ذخیره‌ها", "saved", SavedActivity::class.java)
        item("تنظیمات", "settings", SettingsActivity::class.java)
    }

    fun Context.margin(bottom: Int = 0, top: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(bottom)
            topMargin = dp(top)
        }

    fun fa(value: Int): String = value.toString().map { ch ->
        if (ch.isDigit()) "۰۱۲۳۴۵۶۷۸۹"[ch.digitToInt()] else ch
    }.joinToString("")
}
