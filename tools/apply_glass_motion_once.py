from pathlib import Path

files = {
    'app/src/main/java/ai/takeoff/insightscompanion/MainActivity.kt': None,
    'app/src/main/java/ai/takeoff/insightscompanion/ViralShareActivity.kt': None,
    'app/src/main/java/ai/takeoff/insightscompanion/CaptureReviewActivity.kt': None,
}

# MainActivity: light glass system + orange primary + teal secondary + navy ink.
p = Path(next(iter(files)))
s = p.read_text()
s = s.replace('import android.view.Gravity\n', 'import android.view.Gravity\nimport android.view.animation.DecelerateInterpolator\n')
s = s.replace('''    private val bg = Color.rgb(8, 11, 16)\n    private val panel = Color.rgb(13, 18, 25)\n    private val panel2 = Color.rgb(18, 24, 32)\n    private val border = Color.rgb(40, 51, 64)\n    private val accent = Color.rgb(0, 228, 208)\n    private val muted = Color.rgb(174, 183, 197)''', '''    private val bg = Color.rgb(250, 252, 255)\n    private val panel = Color.argb(236, 255, 255, 255)\n    private val panel2 = Color.argb(214, 255, 255, 255)\n    private val border = Color.argb(82, 9, 24, 43)\n    private val accent = Color.rgb(16, 202, 205)\n    private val primary = Color.rgb(255, 122, 26)\n    private val ink = Color.rgb(15, 23, 35)\n    private val muted = Color.rgb(94, 108, 126)''')
s = s.replace('window.statusBarColor = bg\n        window.navigationBarColor = bg', 'window.statusBarColor = Color.TRANSPARENT\n        window.navigationBarColor = bg\n        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR')
s = s.replace('setTextColor(Color.WHITE)', 'setTextColor(ink)')
s = s.replace('big(if (queueSize == 0) "آماده" else "$queueSize ثبت در صف امن", if (queueSize == 0) accent else Color.rgb(255, 190, 70))', 'big(if (queueSize == 0) "آماده" else "$queueSize ثبت در صف امن", if (queueSize == 0) accent else primary)')
s = s.replace('big(if (shortcode.isBlank()) "هنوز انتخاب نشده" else shortcode, Color.WHITE)', 'big(if (shortcode.isBlank()) "هنوز انتخاب نشده" else shortcode, ink)')
s = s.replace('big("${account.label}  @${account.normalizedHandle}", Color.WHITE)', 'big("${account.label}  @${account.normalizedHandle}", ink)')
s = s.replace('setHintTextColor(Color.rgb(105, 116, 132))', 'setHintTextColor(Color.rgb(140, 150, 164))')
s = s.replace('setTextColor(if (primary) Color.rgb(5, 15, 18) else Color.WHITE)\n        background = rounded(if (primary) accent else panel2, 14f, if (primary) accent else border)\n        setOnClickListener { click() }', 'setTextColor(if (primary) Color.WHITE else ink)\n        background = rounded(if (primary) this@MainActivity.primary else panel2, 14f, if (primary) this@MainActivity.primary else border)\n        setOnClickListener { v ->\n            v.animate().scaleX(.97f).scaleY(.97f).alpha(.82f).setDuration(80).withEndAction {\n                v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(150).start()\n                click()\n            }.start()\n        }')
s = s.replace('content.addView(view)\n    }', 'content.addView(view)\n        view.alpha = 0f\n        view.translationY = dp(16).toFloat()\n        view.animate().alpha(1f).translationY(0f).setDuration(360).setInterpolator(DecelerateInterpolator()).start()\n    }', 1)
s = s.replace('background = rounded(panel, 18f, border)\n    }', 'background = rounded(panel, 22f, border)\n        elevation = dp(5).toFloat()\n    }', 1)
s = s.replace('background = rounded(panel, 18f, border)', 'background = rounded(panel, 22f, border)')
s = s.replace('background = rounded(panel2, 13f, border)', 'background = rounded(panel2, 15f, border)')
s = s.replace('setTextColor(if (i == index) Color.rgb(5, 15, 18) else muted)\n                background = rounded(if (i == index) accent else Color.TRANSPARENT, 13f, Color.TRANSPARENT)', 'setTextColor(if (i == index) Color.WHITE else muted)\n                background = rounded(if (i == index) primary else Color.TRANSPARENT, 13f, Color.TRANSPARENT)')
p.write_text(s)

# Viral share queue: same visual language and fade/slide motion.
p = Path('app/src/main/java/ai/takeoff/insightscompanion/ViralShareActivity.kt')
s = p.read_text()
s = s.replace('import android.view.Gravity\n', 'import android.view.Gravity\nimport android.view.animation.DecelerateInterpolator\n')
s = s.replace('''    private val bg0 = Color.rgb(5, 8, 15)\n    private val bg1 = Color.rgb(12, 26, 39)\n    private val glass = Color.argb(80, 255, 255, 255)\n    private val glassStrong = Color.argb(108, 255, 255, 255)\n    private val border = Color.argb(76, 255, 255, 255)\n    private val accent = Color.rgb(79, 235, 216)\n    private val muted = Color.rgb(188, 200, 214)\n    private val success = Color.rgb(102, 235, 173)\n    private val warning = Color.rgb(255, 194, 92)\n    private val danger = Color.rgb(255, 125, 145)''', '''    private val bg0 = Color.rgb(250, 252, 255)\n    private val bg1 = Color.rgb(243, 252, 253)\n    private val glass = Color.argb(214, 255, 255, 255)\n    private val glassStrong = Color.argb(240, 255, 255, 255)\n    private val border = Color.argb(82, 9, 24, 43)\n    private val accent = Color.rgb(16, 202, 205)\n    private val orange = Color.rgb(255, 122, 26)\n    private val ink = Color.rgb(15, 23, 35)\n    private val muted = Color.rgb(94, 108, 126)\n    private val success = Color.rgb(0, 154, 137)\n    private val warning = Color.rgb(226, 112, 18)\n    private val danger = Color.rgb(210, 63, 76)''')
s = s.replace('window.navigationBarColor = bg0', 'window.navigationBarColor = bg0\n        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR')
s = s.replace('setContentView(buildUi())', 'setContentView(buildUi())\n        window.decorView.rootView.alpha = 0f\n        window.decorView.rootView.translationY = dp(18).toFloat()\n        window.decorView.rootView.animate().alpha(1f).translationY(0f).setDuration(520).setInterpolator(DecelerateInterpolator()).start()', 1)
s = s.replace('intArrayOf(bg0, bg1, Color.rgb(6, 13, 25))', 'intArrayOf(bg0, bg1, Color.rgb(255, 246, 237))')
s = s.replace('label("یادگیری از ریلزهای وایرال", 25f, Color.WHITE, true)', 'label("یادگیری از ریلزهای وایرال", 25f, ink, true)')
s = s.replace('setTextColor(Color.WHITE)', 'setTextColor(ink)')
s = s.replace('label(item.shortcode.ifBlank { "Instagram Reel" }, 17f, Color.WHITE, true)', 'label(item.shortcode.ifBlank { "Instagram Reel" }, 17f, ink, true)')
s = s.replace('label(quick, 12.5f, Color.WHITE, false)', 'label(quick, 12.5f, ink, false)')
s = s.replace('val body = label(report, 14f, Color.WHITE, false)', 'val body = label(report, 14f, ink, false)')
s = s.replace('rounded(Color.rgb(13, 23, 34), 24, border)', 'rounded(Color.rgb(250, 252, 255), 24, border)')
s = s.replace('setTextColor(Color.rgb(4, 24, 24))\n        background = rounded(accent, 17, accent)\n        setOnClickListener { click() }', 'setTextColor(Color.WHITE)\n        background = rounded(orange, 17, orange)\n        setOnClickListener { v ->\n            v.animate().scaleX(.97f).scaleY(.97f).alpha(.82f).setDuration(80).withEndAction {\n                v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(150).start()\n                click()\n            }.start()\n        }')
s = s.replace('elevation = dp(4).toFloat()', 'elevation = dp(6).toFloat()')
p.write_text(s)

# Capture review: light glass, orange confirmation, teal labels, entry motion.
p = Path('app/src/main/java/ai/takeoff/insightscompanion/CaptureReviewActivity.kt')
s = p.read_text()
s = s.replace('import android.view.Gravity\n', 'import android.view.Gravity\nimport android.view.animation.DecelerateInterpolator\n')
s = s.replace('private val accent = Color.rgb(0, 228, 208)', 'private val accent = Color.rgb(16, 202, 205)\n    private val primary = Color.rgb(255, 122, 26)\n    private val ink = Color.rgb(15, 23, 35)\n    private val muted = Color.rgb(94, 108, 126)')
s = s.replace('setContentView(buildUi())', 'setContentView(buildUi())\n        window.statusBarColor = Color.TRANSPARENT\n        window.navigationBarColor = Color.rgb(250,252,255)\n        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR\n        window.decorView.rootView.alpha = 0f\n        window.decorView.rootView.translationY = dp(18).toFloat()\n        window.decorView.rootView.animate().alpha(1f).translationY(0f).setDuration(500).setInterpolator(DecelerateInterpolator()).start()')
s = s.replace('setBackgroundColor(Color.rgb(8,11,16))', 'background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.rgb(250,252,255), Color.rgb(243,252,253), Color.rgb(255,246,237)))')
s = s.replace('setTextColor(Color.WHITE)', 'setTextColor(ink)')
s = s.replace('setTextColor(Color.rgb(174,183,197))', 'setTextColor(muted)')
s = s.replace('setTextColor(Color.rgb(190,200,212))', 'setTextColor(muted)')
s = s.replace('background = rounded(Color.rgb(18,24,32), 12f, Color.rgb(48,60,74))', 'background = rounded(Color.argb(222,255,255,255), 14f, Color.argb(80,9,24,43))')
s = s.replace('setTextColor(if(primary) Color.rgb(5,15,18) else Color.WHITE); background=rounded(if(primary)accent else Color.rgb(18,24,32),15f,if(primary)accent else Color.rgb(48,60,74)); setOnClickListener{click()}', 'setTextColor(if(primary) Color.WHITE else ink); background=rounded(if(primary)this@CaptureReviewActivity.primary else Color.argb(222,255,255,255),15f,if(primary)this@CaptureReviewActivity.primary else Color.argb(80,9,24,43)); setOnClickListener{ v -> v.animate().scaleX(.97f).scaleY(.97f).setDuration(80).withEndAction { v.animate().scaleX(1f).scaleY(1f).setDuration(140).start(); click() }.start() }')
s = s.replace('background = rounded(Color.rgb(13,18,25),18f,Color.rgb(32,43,55))', 'background = rounded(Color.argb(238,255,255,255),22f,Color.argb(80,9,24,43)); elevation=dp(5).toFloat()')
p.write_text(s)

# Remove one-shot automation after it has done its work.
Path('.github/workflows/apply-glass-motion-once.yml').unlink(missing_ok=True)
Path('tools/apply_glass_motion_once.py').unlink(missing_ok=True)
