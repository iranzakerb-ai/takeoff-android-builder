package ai.takeoff.insightscompanion

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

object LovableUi {
    // 3D Spatial Dark-Glass Palette
    val legacyBackground = Color.rgb(252, 250, 247)
    val legacyPrimary = Color.rgb(245, 104, 43)
    val legacySecondary = Color.rgb(111, 218, 204)
    val background = Color.rgb(11, 15, 24)       // #0B0F18 Deep Midnight Slate
    val foreground = Color.rgb(243, 246, 252)    // #F3F6FC Radiant high-contrast Persian text
    val card = Color.rgb(19, 26, 42)             // #131A2A Frosted Obsidian Glass
    val cardSpecular = Color.argb(55, 255, 255, 255) // Specular top light reflection
    val cardBevel = Color.rgb(10, 14, 22)        // #0A0E16 Bottom 3D extrusion shade
    val primary = Color.rgb(255, 107, 43)        // #FF6B2B Radiant Cyber Orange
    val primaryDeep = Color.rgb(180, 52, 10)     // #B4340A 3D Keycap extrusion shadow
    val primarySoft = Color.argb(45, 255, 107, 43)
    val secondary = Color.rgb(0, 229, 255)       // #00E5FF Electric Cyan Neon
    val secondarySoft = Color.argb(40, 0, 229, 255)
    val secondaryText = Color.rgb(0, 229, 255)
    val muted = Color.rgb(150, 166, 188)         // #96A6BC Legible cool grey-blue
    val mutedBg = Color.rgb(24, 33, 52)          // #182134 Inset 3D surface
    val border = Color.rgb(36, 48, 76)           // #24304C Structural bevel border
    val borderHighlight = Color.argb(80, 255, 255, 255)
    val success = Color.rgb(46, 213, 115)        // #2ED573 Neon Emerald
    val warning = Color.rgb(255, 171, 0)         // #FFAB00 Neon Amber
    val danger = Color.rgb(255, 71, 87)          // #FF4757 Neon Ruby
    val navy = Color.rgb(8, 12, 18)              // #080C12 Pure deep navy
    val darkCard = Color.rgb(15, 21, 34)

    fun applyWindow(activity: Activity) {
        activity.window.statusBarColor = background
        activity.window.navigationBarColor = navy
        // Light icons on dark status & navigation bars
        activity.window.decorView.systemUiVisibility = 0
    }

    fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    fun Context.dp(value: Float): Float = value * resources.displayMetrics.density

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

    fun Context.card3dDrawable(
        fill: Int = LovableUi.card,
        radius: Int = 22,
        depth: Int = 4,
    ): LayerDrawable {
        val cornerPx = dp(radius).toFloat()
        val depthPx = dp(depth)
        val base = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerPx
            setColor(cardBevel)
        }
        val face = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.rgb(28, 38, 60), fill)
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerPx
            setStroke(dp(1), cardSpecular)
        }
        return LayerDrawable(arrayOf(base, face)).apply {
            setLayerInset(1, 0, 0, 0, depthPx)
        }
    }

    fun Context.brandGradient(radius: Int = 28): GradientDrawable = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(Color.rgb(255, 130, 52), Color.rgb(245, 96, 32), LovableUi.primaryDeep),
    ).apply {
        cornerRadius = dp(radius).toFloat()
        setStroke(dp(1), Color.argb(120, 255, 255, 255))
    }

    fun Context.card(strong: Boolean = false): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        background = card3dDrawable(if (strong) Color.rgb(22, 30, 48) else LovableUi.card, 22, if (strong) 5 else 3)
        elevation = dp(if (strong) 8 else 4).toFloat()
        translationZ = if (strong) dp(3f) else dp(1.5f)
    }

    fun Context.card3d(fill: Int = Color.rgb(19, 26, 42), radius: Int = 22, depth: Int = 4): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        background = card3dDrawable(fill, radius, depth)
        elevation = dp(depth * 2).toFloat()
        translationZ = dp(depth).toFloat()
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

    fun Context.sectionTitle(value: String): TextView = text(value, 14f, LovableUi.foreground, true)

    fun Context.chip(value: String, tone: String = "muted"): TextView {
        val fill: Int
        val fg: Int
        val strokeColor: Int
        when (tone) {
            "primary" -> {
                fill = Color.rgb(56, 26, 15)
                fg = Color.rgb(255, 145, 80)
                strokeColor = Color.argb(180, 255, 107, 43)
            }
            "secondary" -> {
                fill = Color.rgb(12, 44, 56)
                fg = LovableUi.secondary
                strokeColor = Color.argb(180, 0, 229, 255)
            }
            "success" -> {
                fill = Color.rgb(14, 46, 28)
                fg = LovableUi.success
                strokeColor = Color.argb(180, 46, 213, 115)
            }
            "danger" -> {
                fill = Color.rgb(54, 18, 22)
                fg = LovableUi.danger
                strokeColor = Color.argb(180, 255, 71, 87)
            }
            "warning" -> {
                fill = Color.rgb(52, 36, 12)
                fg = LovableUi.warning
                strokeColor = Color.argb(180, 255, 171, 0)
            }
            else -> {
                fill = Color.rgb(26, 36, 56)
                fg = LovableUi.muted
                strokeColor = LovableUi.border
            }
        }
        return text(value, 11f, fg, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(5), dp(12), dp(5))
            background = rounded(fill, 16, strokeColor, 1)
            elevation = dp(2).toFloat()
            translationZ = dp(1).toFloat()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun Context.primaryButton(value: String, onClick: () -> Unit): Button = Button(this).apply {
        text = value
        isAllCaps = false
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.WHITE)
        val depthPx = dp(4)

        val bottomLedge = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(20).toFloat()
            setColor(LovableUi.primaryDeep)
        }
        val topFace = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.rgb(255, 140, 66), LovableUi.primary, LovableUi.primaryDeep)
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(20).toFloat()
            setStroke(dp(1), Color.argb(140, 255, 255, 255))
        }
        background = LayerDrawable(arrayOf(bottomLedge, topFace)).apply {
            setLayerInset(1, 0, 0, 0, depthPx)
        }
        elevation = dp(6).toFloat()
        translationZ = dp(2).toFloat()

        setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    view.translationY = depthPx.toFloat()
                    view.scaleX = 0.985f
                    view.scaleY = 0.985f
                    view.elevation = dp(1).toFloat()
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    view.animate()
                        .translationY(0f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(130)
                        .setInterpolator(OvershootInterpolator(1.6f))
                        .withEndAction { onClick() }
                        .start()
                    view.elevation = dp(6).toFloat()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    view.animate()
                        .translationY(0f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                    view.elevation = dp(6).toFloat()
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
        textSize = 13.5f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(LovableUi.foreground)
        val depthPx = dp(3)

        val bottomLedge = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(20).toFloat()
            setColor(Color.rgb(12, 17, 26))
        }
        val topFace = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.rgb(36, 48, 72), Color.rgb(24, 32, 50))
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(20).toFloat()
            setStroke(dp(1), Color.argb(65, 255, 255, 255))
        }
        background = LayerDrawable(arrayOf(bottomLedge, topFace)).apply {
            setLayerInset(1, 0, 0, 0, depthPx)
        }
        elevation = dp(4).toFloat()
        translationZ = dp(1.5f)

        setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    view.translationY = depthPx.toFloat()
                    view.scaleX = 0.985f
                    view.scaleY = 0.985f
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
                        .setInterpolator(OvershootInterpolator(1.5f))
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

    fun Context.topBar(title: String, subtitle: String? = null, back: (() -> Unit)? = null): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(16), dp(12), dp(16), dp(12))

        val barBase = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.rgb(10, 14, 22))
        }
        val barFace = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.argb(245, 18, 25, 40))
            setStroke(dp(1), Color.argb(45, 255, 255, 255))
        }
        background = LayerDrawable(arrayOf(barBase, barFace)).apply {
            setLayerInset(1, 0, 0, 0, dp(2))
        }
        elevation = dp(6).toFloat()
        translationZ = dp(2).toFloat()

        if (back != null) {
            addView(TextView(this@topBar).apply {
                text = "‹"
                textSize = 28f
                gravity = Gravity.CENTER
                setTextColor(LovableUi.foreground)
                background = rounded(Color.rgb(28, 38, 60), 18, Color.argb(60, 255, 255, 255), 1)
                elevation = dp(3).toFloat()
                setOnClickListener { back() }
            }, LinearLayout.LayoutParams(dp(38), dp(38)).apply { marginEnd = dp(10) })
        } else {
            addView(ImageView(this@topBar).apply {
                setImageResource(R.drawable.ic_takeoff_logo)
                setPadding(dp(7), dp(7), dp(7), dp(7))
                background = rounded(Color.rgb(26, 36, 56), 14, Color.argb(70, 255, 255, 255), 1)
                elevation = dp(3).toFloat()
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
        setPadding(dp(10), dp(8), dp(10), dp(10))

        val islandBase = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(28).toFloat()
            setColor(Color.rgb(10, 14, 22))
        }
        val islandFace = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.rgb(28, 38, 60), Color.rgb(18, 24, 38))
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(28).toFloat()
            setStroke(dp(1), Color.argb(60, 255, 255, 255))
        }
        background = LayerDrawable(arrayOf(islandBase, islandFace)).apply {
            setLayerInset(1, 0, 0, 0, dp(3))
        }
        elevation = dp(14).toFloat()
        translationZ = dp(6).toFloat()

        fun item(label: String, key: String, target: Class<out Activity>?, primaryAction: Boolean = false) {
            val host = LinearLayout(this@bottomNav).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(2), 0, dp(2), 0)
                if (primaryAction) {
                    val fab = TextView(this@bottomNav).apply {
                        text = "+"
                        textSize = 28f
                        gravity = Gravity.CENTER
                        setTextColor(Color.WHITE)

                        val fabBase = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(LovableUi.primaryDeep)
                        }
                        val fabFace = GradientDrawable(
                            GradientDrawable.Orientation.TL_BR,
                            intArrayOf(Color.rgb(255, 140, 66), LovableUi.primary, LovableUi.primaryDeep)
                        ).apply {
                            shape = GradientDrawable.OVAL
                            setStroke(dp(1), Color.argb(140, 255, 255, 255))
                        }
                        background = LayerDrawable(arrayOf(fabBase, fabFace)).apply {
                            setLayerInset(1, 0, 0, 0, dp(4))
                        }
                        elevation = dp(8).toFloat()
                        translationZ = dp(3).toFloat()
                    }
                    addView(fab, LinearLayout.LayoutParams(dp(52), dp(52)).apply {
                        bottomMargin = dp(2)
                        topMargin = -dp(8)
                    })
                } else {
                    val iconView = TextView(this@bottomNav).apply {
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
                    }
                    addView(iconView, LinearLayout.LayoutParams(dp(32), dp(28)))
                }
                addView(text(label, 10.2f, if (active == key || primaryAction) LovableUi.primary else LovableUi.muted, active == key || primaryAction).apply { gravity = Gravity.CENTER })
                setOnClickListener {
                    animate().scaleX(0.90f).scaleY(0.90f).setDuration(60).withEndAction {
                        animate().scaleX(1f).scaleY(1f).setDuration(120).setInterpolator(OvershootInterpolator(1.4f)).start()
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
