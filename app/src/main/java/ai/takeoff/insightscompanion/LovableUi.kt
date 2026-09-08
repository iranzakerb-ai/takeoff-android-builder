package ai.takeoff.insightscompanion

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

object LovableUi {
    val background = Color.rgb(249, 247, 243)
    val foreground = Color.rgb(38, 30, 24)
    val card = Color.WHITE
    val primary = Color.rgb(245, 96, 32)
    val primaryDeep = Color.rgb(222, 68, 20)
    val primarySoft = Color.rgb(255, 238, 227)
    val secondary = Color.rgb(68, 202, 184)
    val secondarySoft = Color.rgb(224, 248, 244)
    val secondaryText = Color.rgb(32, 94, 90)
    val muted = Color.rgb(120, 110, 102)
    val mutedBg = Color.rgb(244, 240, 234)
    val border = Color.rgb(230, 222, 212)
    val borderHighlight = Color.argb(140, 255, 255, 255)
    val success = Color.rgb(58, 168, 110)
    val warning = Color.rgb(208, 142, 30)
    val danger = Color.rgb(212, 60, 52)
    val navy = Color.rgb(24, 32, 40)
    val darkCard = Color.rgb(28, 36, 45)

    fun applyWindow(activity: Activity) {
        activity.window.statusBarColor = Color.rgb(249, 247, 243)
        activity.window.navigationBarColor = Color.rgb(255, 255, 255)
        activity.window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
    }

    fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    fun Context.rounded(
        fill: Int,
        radius: Int = 18,
        stroke: Int = LovableUi.border,
        strokeWidth: Int = 1,
    ): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(fill)
        cornerRadius = dp(radius).toFloat()
        if (strokeWidth > 0) setStroke(dp(strokeWidth), stroke)
    }

    fun Context.brandGradient(radius: Int = 28): GradientDrawable = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(Color.rgb(255, 122, 44), Color.rgb(245, 96, 32), LovableUi.primaryDeep),
    ).apply {
        cornerRadius = dp(radius).toFloat()
        setStroke(dp(1), Color.argb(90, 255, 255, 255))
    }

    fun Context.card(strong: Boolean = false): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        background = rounded(if (strong) Color.WHITE else LovableUi.card, 22, LovableUi.border, 1)
        elevation = dp(if (strong) 6 else 3).toFloat()
        translationZ = dp(if (strong) 3 else 1).toFloat()
    }

    fun Context.card3d(fill: Int = Color.WHITE, radius: Int = 22, depth: Int = 4): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        background = rounded(fill, radius, LovableUi.border, 1)
        elevation = dp(depth).toFloat()
        translationZ = dp(depth / 2).toFloat()
    }

    fun Context.text(value: String, size: Float, color: Int = LovableUi.foreground, bold: Boolean = false): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        gravity = Gravity.START
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        textDirection = View.TEXT_DIRECTION_RTL
        typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        setLineSpacing(0f, 1.24f)
    }

    fun Context.sectionTitle(value: String): TextView = text(value, 13.5f, LovableUi.foreground, true)

    fun Context.chip(value: String, tone: String = "muted"): TextView {
        val fill: Int
        val fg: Int
        val strokeColor: Int
        when (tone) {
            "primary" -> {
                fill = LovableUi.primarySoft
                fg = LovableUi.primary
                strokeColor = Color.argb(120, 245, 96, 32)
            }
            "secondary" -> {
                fill = LovableUi.secondarySoft
                fg = LovableUi.secondaryText
                strokeColor = Color.argb(120, 68, 202, 184)
            }
            "success" -> {
                fill = Color.rgb(232, 248, 238)
                fg = LovableUi.success
                strokeColor = Color.argb(100, 58, 168, 110)
            }
            "danger" -> {
                fill = Color.rgb(254, 235, 233)
                fg = LovableUi.danger
                strokeColor = Color.argb(100, 212, 60, 52)
            }
            "warning" -> {
                fill = Color.rgb(254, 245, 222)
                fg = LovableUi.warning
                strokeColor = Color.argb(100, 208, 142, 30)
            }
            else -> {
                fill = LovableUi.mutedBg
                fg = LovableUi.muted
                strokeColor = LovableUi.border
            }
        }
        return text(value, 11f, fg, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(11), dp(5), dp(11), dp(5))
            background = rounded(fill, 16, strokeColor, 1)
            elevation = dp(1).toFloat()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun Context.primaryButton(value: String, onClick: () -> Unit): Button = Button(this).apply {
        text = value
        isAllCaps = false
        textSize = 13.5f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.WHITE)
        background = brandGradient(20)
        elevation = dp(4).toFloat()
        translationZ = dp(2).toFloat()
        setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    view.translationY = dp(2).toFloat()
                    view.scaleX = 0.975f
                    view.scaleY = 0.975f
                    view.elevation = dp(1).toFloat()
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    view.animate()
                        .translationY(0f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120)
                        .setInterpolator(OvershootInterpolator(1.4f))
                        .withEndAction { onClick() }
                        .start()
                    view.elevation = dp(4).toFloat()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    view.animate()
                        .translationY(0f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                    view.elevation = dp(4).toFloat()
                    true
                }
                else -> false
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun Context.ghostButton(value: String, onClick: () -> Unit): Button = Button(this).apply {
        text = value
        isAllCaps = false
        textSize = 13f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(LovableUi.foreground)
        background = rounded(Color.WHITE, 20, LovableUi.border, 1)
        elevation = dp(2).toFloat()
        translationZ = dp(1).toFloat()
        setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    view.translationY = dp(2).toFloat()
                    view.scaleX = 0.98f
                    view.scaleY = 0.98f
                    view.elevation = dp(0).toFloat()
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    view.animate()
                        .translationY(0f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120)
                        .setInterpolator(OvershootInterpolator(1.3f))
                        .withEndAction { onClick() }
                        .start()
                    view.elevation = dp(2).toFloat()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    view.animate()
                        .translationY(0f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                    view.elevation = dp(2).toFloat()
                    true
                }
                else -> false
            }
        }
    }

    fun Context.topBar(title: String, subtitle: String? = null, back: (() -> Unit)? = null): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(16), dp(12), dp(16), dp(12))
        background = rounded(Color.argb(250, 252, 250, 247), 0, LovableUi.border, 1)
        elevation = dp(3).toFloat()
        translationZ = dp(1).toFloat()
        if (back != null) {
            addView(TextView(this@topBar).apply {
                text = "‹"
                textSize = 28f
                gravity = Gravity.CENTER
                setTextColor(LovableUi.foreground)
                background = rounded(LovableUi.mutedBg, 18, LovableUi.border, 1)
                elevation = dp(2).toFloat()
                setOnClickListener { back() }
            }, LinearLayout.LayoutParams(dp(38), dp(38)).apply { marginEnd = dp(10) })
        } else {
            addView(ImageView(this@topBar).apply {
                setImageResource(R.drawable.ic_takeoff_logo)
                setPadding(dp(7), dp(7), dp(7), dp(7))
                background = rounded(LovableUi.navy, 14, LovableUi.navy, 0)
                elevation = dp(2).toFloat()
            }, LinearLayout.LayoutParams(dp(38), dp(38)).apply { marginEnd = dp(10) })
        }
        addView(LinearLayout(this@topBar).apply {
            orientation = LinearLayout.VERTICAL
            addView(text(title, 15.5f, LovableUi.foreground, true))
            subtitle?.let { addView(text(it, 11f, LovableUi.muted, false)) }
        }, LinearLayout.LayoutParams(0, -2, 1f))
    }

    fun Activity.bottomNav(active: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        gravity = Gravity.CENTER
        setPadding(dp(8), dp(8), dp(8), dp(10))
        background = rounded(Color.argb(252, 255, 255, 255), 0, LovableUi.border, 1)
        elevation = dp(8).toFloat()
        translationZ = dp(4).toFloat()

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
                        background = brandGradient(22)
                        elevation = dp(5).toFloat()
                    }, LinearLayout.LayoutParams(dp(52), dp(52)).apply { bottomMargin = dp(2) })
                } else {
                    addView(TextView(this@bottomNav).apply {
                        text = when (key) {
                            "home" -> "⌂"
                            "memory" -> "◇"
                            "saved" -> "▣"
                            "settings" -> "⚙"
                            else -> "•"
                        }
                        textSize = 21f
                        gravity = Gravity.CENTER
                        setTextColor(if (active == key) LovableUi.primary else LovableUi.muted)
                    }, LinearLayout.LayoutParams(dp(32), dp(28)))
                }
                addView(text(label, 10.2f, if (active == key || primaryAction) LovableUi.primary else LovableUi.muted, active == key || primaryAction).apply { gravity = Gravity.CENTER })
                setOnClickListener {
                    animate().scaleX(0.92f).scaleY(0.92f).setDuration(60).withEndAction {
                        animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                        if (target != null && this@bottomNav::class.java != target) {
                            startActivity(Intent(this@bottomNav, target).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
                        }
                    }.start()
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
