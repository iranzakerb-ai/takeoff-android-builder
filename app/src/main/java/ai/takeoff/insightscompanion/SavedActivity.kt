package ai.takeoff.insightscompanion

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView

class SavedActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LovableUi.applyWindow(this)
        setContentView(buildUi())
    }

    override fun onResume() {
        super.onResume()
        setContentView(buildUi())
    }

    private fun buildUi(): View {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(LovableUi.background)
        }
        page.addView(LovableUi.run { topBar("ذخیره‌ها", "تحلیل‌ها و خروجی‌های آماده بازبینی") })
        val scroll = ScrollView(this).apply { isFillViewport = true; overScrollMode = View.OVER_SCROLL_NEVER }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(LovableUi.run { dp(16) }, LovableUi.run { dp(16) }, LovableUi.run { dp(16) }, LovableUi.run { dp(28) })
        }
        val completed = SharedMediaQueue(this).all().filter { it.status == "completed" }
        root.addView(LovableUi.run { card() }.apply {
            val row = LinearLayout(this@SavedActivity).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; gravity = Gravity.CENTER_VERTICAL }
            row.addView(LovableUi.run { chip("ذخیره محلی امن", "primary") })
            row.addView(LovableUi.run { text("${LovableUi.fa(completed.size)} مورد", 12f, LovableUi.foreground, true) }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(row)
            addView(LovableUi.run { text("گزارش‌های تکمیل‌شده برای دسترسی سریع روی دستگاه باقی می‌مانند؛ رسانه اصلی پس از یادگیری روی سرور حذف می‌شود.", 11.5f, LovableUi.muted) }.apply { setPadding(0, LovableUi.run { dp(9) }, 0, 0) })
        }, LovableUi.run { margin(bottom = 20) })

        root.addView(LovableUi.run { sectionTitle("تحلیل‌های ذخیره‌شده") }, LovableUi.run { margin(bottom = 10) })
        if (completed.isEmpty()) {
            root.addView(LovableUi.run { card() }.apply { addView(LovableUi.run { text("هنوز موردی ذخیره نشده.", 12f, LovableUi.muted) }) })
        } else {
            completed.forEach { item ->
                root.addView(LovableUi.run { card() }.apply {
                    val row = LinearLayout(this@SavedActivity).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; gravity = Gravity.CENTER_VERTICAL }
                    row.addView(LovableUi.run { chip(kindFa(item.mediaKind), "secondary") })
                    row.addView(LinearLayout(this@SavedActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(LovableUi.run { text(item.shortcode.ifBlank { "Instagram" }, 12.5f, LovableUi.foreground, true) })
                        addView(LovableUi.run { text("تحلیل کامل • آماده مشاهده", 10.5f, LovableUi.muted) })
                    }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = LovableUi.run { dp(8) }; marginEnd = LovableUi.run { dp(8) } })
                    row.addView(LovableUi.run { text("›", 24f, LovableUi.primary, true) })
                    addView(row)
                    setOnClickListener { startActivity(Intent(this@SavedActivity, ViralShareActivity::class.java)) }
                }, LovableUi.run { margin(bottom = 8) })
            }
        }
        scroll.addView(root)
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        page.addView(LovableUi.run { bottomNav("saved") })
        return page
    }

    private fun kindFa(kind: String) = when (kind) {
        "reel" -> "ریلز"
        "video_post" -> "ویدیو"
        "photo" -> "عکس"
        "carousel" -> "کاروسل"
        "mixed" -> "ترکیبی"
        else -> "محتوا"
    }
}
