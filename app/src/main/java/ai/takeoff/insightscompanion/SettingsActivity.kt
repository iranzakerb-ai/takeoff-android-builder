package ai.takeoff.insightscompanion

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView

class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LovableUi.applyWindow(this)
        setContentView(buildUi())
    }

    private fun buildUi(): View {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(LovableUi.background)
        }
        page.addView(LovableUi.run { topBar("تنظیمات", "کنترل دستگاه، پیج و اتصال TakeOff") })
        val scroll = ScrollView(this).apply { isFillViewport = true; overScrollMode = View.OVER_SCROLL_NEVER }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(LovableUi.run { dp(16) }, LovableUi.run { dp(16) }, LovableUi.run { dp(16) }, LovableUi.run { dp(28) })
        }
        val account = runCatching { ManagedAccountStore(this).selected() }.getOrNull()
        root.addView(LovableUi.run { card() }.apply {
            addView(LovableUi.run { chip("TakeOff Insights ${BuildConfig.VERSION_NAME}", "secondary") })
            addView(LovableUi.run { text("Agent 4.1 • Learning Engine 5.1 • AI Video Studio 1.0", 10.8f, LovableUi.primary, true) }.apply { setPadding(0, LovableUi.run { dp(8) }, 0, 0) })
            addView(LovableUi.run { text("پیج فعال", 11f, LovableUi.muted) }.apply { setPadding(0, LovableUi.run { dp(12) }, 0, 0) })
            addView(LovableUi.run { text(account?.let { "@${it.normalizedHandle}" } ?: "هنوز پیجی انتخاب نشده", 14f, LovableUi.foreground, true) })
        }, LovableUi.run { margin(bottom = 22) })

        root.addView(LovableUi.run { sectionTitle("کنترل‌های اصلی") }, LovableUi.run { margin(bottom = 10) })
        root.addView(settingRow("پیج‌ها و Owner Insights", "مدیریت پیج، آمار مالک و اتصال امن") { startActivity(Intent(this, MainActivity::class.java)) }, LovableUi.run { margin(bottom = 8) })
        root.addView(settingRow("صف تحلیل", "مشاهده وضعیت، Retry و گزارش کامل") { startActivity(Intent(this, ViralShareActivity::class.java)) }, LovableUi.run { margin(bottom = 8) })
        root.addView(settingRow("استودیوی سناریو", "۱۰ سناریوی آماده ضبط واقعی از حافظه V5") { startActivity(Intent(this, ScenarioStudioActivity::class.java)) }, LovableUi.run { margin(bottom = 8) })
        root.addView(settingRow("استودیوی ویدیوی AI", "Character Sheet + پرامپت کامل Omni برای هر سکانس") { startActivity(Intent(this, AiVideoStudioActivity::class.java)) }, LovableUi.run { margin(bottom = 8) })

        root.addView(LovableUi.run { sectionTitle("درباره این نسخه") }, LovableUi.run { margin(bottom = 10, top = 14) })
        root.addView(LovableUi.run { card() }.apply {
            addView(LovableUi.run { text("TakeOff Insights ${BuildConfig.VERSION_NAME}", 12f, LovableUi.foreground, true) })
            addView(LovableUi.run { text("این نسخه اعتبارسنجی توضیح کوتاه و بازیابی اتصال امن AI Video Studio را اصلاح می‌کند. Scenario Studio بدون تغییر باقی مانده است.", 11.5f, LovableUi.muted) }.apply { setPadding(0, LovableUi.run { dp(8) }, 0, 0) })
        })
        scroll.addView(root)
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        page.addView(LovableUi.run { bottomNav("settings") })
        return page
    }

    private fun settingRow(title: String, subtitle: String, click: () -> Unit): View = LovableUi.run { card() }.apply {
        val row = LinearLayout(this@SettingsActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(LinearLayout(this@SettingsActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(LovableUi.run { text(title, 12.5f, LovableUi.foreground, true) })
            addView(LovableUi.run { text(subtitle, 10.5f, LovableUi.muted) })
        }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(LovableUi.run { text("›", 24f, LovableUi.primary, true) })
        addView(row)
        setOnClickListener { click() }
    }
}
