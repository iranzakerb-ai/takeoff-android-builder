package ai.takeoff.insightscompanion

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SavedActivity : Activity() {
    private var selectedTab = 0 // 0=all, 1=scenario, 2=ai_video, 3=media
    private lateinit var contentContainer: LinearLayout

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        LovableUi.applyWindow(this)
        setContentView(buildUi())
    }

    override fun onResume() {
        super.onResume()
        rebuildList()
    }

    private fun buildUi(): View {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(LovableUi.background)
        }
        page.addView(LovableUi.run { topBar("ذخیره‌ها و آرشیو", "سناریوها، ویدیوهای هوش مصنوعی و آنالیزهای ریلز", back = { finish() }) })

        val tabsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(LovableUi.run { dp(16) }, LovableUi.run { dp(8) }, LovableUi.run { dp(16) }, LovableUi.run { dp(8) })
        }
        val tabs = listOf("همه", "سناریوی واقعی", "استودیو AI", "آنالیز ریلز")
        tabs.forEachIndexed { idx, label ->
            val tabBtn = LovableUi.run {
                if (idx == selectedTab) primaryButton(label) { selectTab(idx) }
                else ghostButton(label) { selectTab(idx) }
            }
            tabsRow.addView(tabBtn, LinearLayout.LayoutParams(0, LovableUi.run { dp(38) }, 1f).apply {
                if (idx > 0) marginStart = LovableUi.run { dp(4) }
            })
        }
        page.addView(tabsRow)

        val scroll = ScrollView(this).apply { isFillViewport = true; overScrollMode = View.OVER_SCROLL_NEVER }
        contentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(LovableUi.run { dp(16) }, LovableUi.run { dp(8) }, LovableUi.run { dp(16) }, LovableUi.run { dp(32) })
        }
        scroll.addView(contentContainer)
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        rebuildList()
        return page
    }

    private fun selectTab(index: Int) {
        selectedTab = index
        setContentView(buildUi())
    }

    private fun rebuildList() {
        if (!::contentContainer.isInitialized) return
        contentContainer.removeAllViews()

        val studioStore = StudioResultStore(this)
        val mediaQueue = SharedMediaQueue(this)

        val studioItems = studioStore.getAll()
        val mediaItems = mediaQueue.getAll()

        when (selectedTab) {
            0 -> {
                studioItems.forEach { item ->
                    if (item.type == "scenario") contentContainer.addView(buildScenarioCard(item), LovableUi.run { margin(bottom = 12) })
                    else contentContainer.addView(buildAiVideoCard(item), LovableUi.run { margin(bottom = 12) })
                }
                mediaItems.forEach { item ->
                    contentContainer.addView(buildMediaCard(item), LovableUi.run { margin(bottom = 12) })
                }
            }
            1 -> {
                studioItems.filter { it.type == "scenario" }.forEach { item ->
                    contentContainer.addView(buildScenarioCard(item), LovableUi.run { margin(bottom = 12) })
                }
            }
            2 -> {
                studioItems.filter { it.type == "ai_video" }.forEach { item ->
                    contentContainer.addView(buildAiVideoCard(item), LovableUi.run { margin(bottom = 12) })
                }
            }
            3 -> {
                mediaItems.forEach { item ->
                    contentContainer.addView(buildMediaCard(item), LovableUi.run { margin(bottom = 12) })
                }
            }
        }

        if (contentContainer.childCount == 0) {
            contentContainer.addView(LovableUi.run { card() }.apply {
                addView(LovableUi.run { text("هنوز موردی در این بخش ذخیره نشده است.", 12.5f, LovableUi.muted) })
            })
        }
    }

    private fun buildScenarioCard(item: StudioEntry): View = LovableUi.run { card() }.apply {
        val top = LinearLayout(this@SavedActivity).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; gravity = Gravity.CENTER_VERTICAL }
        val isShort = item.mode.startsWith("short_15s")
        val statusFa = when (item.status) {
            "processing" -> "⏳ در حال ساخت در پس‌زمینه"
            "failed" -> "❌ خطا در ساخت"
            else -> if (isShort) "۱۵ ثانیه‌ای تک‌سکانسه" else "۱۰ سناریوی هوشمند"
        }
        val chipTone = when (item.status) {
            "processing" -> "secondary"
            "failed" -> "danger"
            else -> "primary"
        }
        top.addView(LovableUi.run { chip(statusFa, chipTone) })
        val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(item.createdAt))
        top.addView(LovableUi.run { text(dateStr, 11f, LovableUi.muted) }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = LovableUi.run { dp(8) } })
        addView(top)

        addView(LovableUi.run { text("حوزه: ${item.niche}", 14f, LovableUi.foreground, true) }.apply { setPadding(0, LovableUi.run { dp(6) }, 0, 0) })
        if (item.description.isNotBlank()) {
            addView(LovableUi.run { text(item.description.take(100) + if (item.description.length > 100) "…" else "", 11.5f, LovableUi.muted) }.apply { setPadding(0, LovableUi.run { dp(4) }, 0, LovableUi.run { dp(8) }) })
        }

        if (item.status == "failed" && !item.errorMessage.isNullOrBlank()) {
            addView(LovableUi.run { text(item.errorMessage!!, 11f, LovableUi.danger) }.apply { setPadding(0, 0, 0, LovableUi.run { dp(6) }) })
        }

        val actionRow = LinearLayout(this@SavedActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        if (item.status == "completed") {
            val viewBtn = LovableUi.run { primaryButton("👁️ مشاهده ۱۰ سناریو") {
                startActivity(Intent(this@SavedActivity, ScenarioStudioActivity::class.java).apply {
                    putExtra("selected_task_id", item.id)
                })
            } }
            val pdfBtn = LovableUi.run { ghostButton("📄 فایل PDF") {
                openOrExportPdf(item)
            } }
            actionRow.addView(viewBtn, LinearLayout.LayoutParams(0, LovableUi.run { dp(44) }, 1.2f).apply { marginEnd = LovableUi.run { dp(4) } })
            actionRow.addView(pdfBtn, LinearLayout.LayoutParams(0, LovableUi.run { dp(44) }, 1f).apply { marginStart = LovableUi.run { dp(4) } })
        } else if (item.status == "processing") {
            val watchBtn = LovableUi.run { ghostButton("⏳ در حال پردازش (مشاهده وضعیت)") {
                startActivity(Intent(this@SavedActivity, ScenarioStudioActivity::class.java).apply {
                    putExtra("selected_task_id", item.id)
                })
            } }
            actionRow.addView(watchBtn, LinearLayout.LayoutParams(-1, LovableUi.run { dp(44) }))
        } else {
            val retryBtn = LovableUi.run { primaryButton("🔄 تلاش مجدد برای ساخت") {
                retryStudioEntry(item)
            } }
            actionRow.addView(retryBtn, LinearLayout.LayoutParams(-1, LovableUi.run { dp(44) }))
        }
        addView(actionRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = LovableUi.run { dp(4) } })
    }

    private fun buildAiVideoCard(item: StudioEntry): View = LovableUi.run { card() }.apply {
        val top = LinearLayout(this@SavedActivity).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; gravity = Gravity.CENTER_VERTICAL }
        val isViral = item.mode.startsWith("viral_10s")
        val statusFa = when (item.status) {
            "processing" -> "⏳ در حال ساخت در پس‌زمینه"
            "failed" -> "❌ خطا در ساخت"
            else -> if (isViral) "وایرال ۱۰s • Omni" else "سینمایی ۹:۱۶"
        }
        val chipTone = when (item.status) {
            "processing" -> "secondary"
            "failed" -> "danger"
            else -> "primary"
        }
        top.addView(LovableUi.run { chip(statusFa, chipTone) })
        val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(item.createdAt))
        top.addView(LovableUi.run { text(dateStr, 11f, LovableUi.muted) }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = LovableUi.run { dp(8) } })
        addView(top)

        addView(LovableUi.run { text(item.title.ifBlank { item.niche }, 14f, LovableUi.foreground, true) }.apply { setPadding(0, LovableUi.run { dp(6) }, 0, 0) })
        if (item.description.isNotBlank()) {
            addView(LovableUi.run { text(item.description.take(100) + if (item.description.length > 100) "…" else "", 11.5f, LovableUi.muted) }.apply { setPadding(0, LovableUi.run { dp(4) }, 0, LovableUi.run { dp(8) }) })
        }

        if (item.status == "failed" && !item.errorMessage.isNullOrBlank()) {
            addView(LovableUi.run { text(item.errorMessage!!, 11f, LovableUi.danger) }.apply { setPadding(0, 0, 0, LovableUi.run { dp(6) }) })
        }

        val actionRow = LinearLayout(this@SavedActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        if (item.status == "completed") {
            val copyPromptBtn = LovableUi.run { primaryButton("📋 کپی پرامپت‌ها") {
                startActivity(Intent(this@SavedActivity, AiVideoStudioActivity::class.java).apply {
                    putExtra("selected_task_id", item.id)
                })
            } }
            actionRow.addView(copyPromptBtn, LinearLayout.LayoutParams(-1, LovableUi.run { dp(44) }))
        } else if (item.status == "processing") {
            val watchBtn = LovableUi.run { ghostButton("⏳ در حال تولید و داوری (مشاهده وضعیت)") {
                startActivity(Intent(this@SavedActivity, AiVideoStudioActivity::class.java).apply {
                    putExtra("selected_task_id", item.id)
                })
            } }
            actionRow.addView(watchBtn, LinearLayout.LayoutParams(-1, LovableUi.run { dp(44) }))
        } else {
            val retryBtn = LovableUi.run { primaryButton("🔄 تلاش مجدد برای ساخت") {
                retryStudioEntry(item)
            } }
            actionRow.addView(retryBtn, LinearLayout.LayoutParams(-1, LovableUi.run { dp(44) }))
        }
        addView(actionRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = LovableUi.run { dp(4) } })
    }

    private fun retryStudioEntry(item: StudioEntry) {
        StudioResultStore(this).update(item.id) { current ->
            current.copy(status = "processing", errorMessage = null)
        }
        StudioTaskWork.enqueue(this, item.id)
        Toast.makeText(this, "تلاش مجدد در پس‌زمینه آغاز شد.", Toast.LENGTH_SHORT).show()
        rebuildList()
    }

    private fun buildMediaCard(item: SharedMediaQueue.Item): View = LovableUi.run { card() }.apply {
        val row = LinearLayout(this@SavedActivity).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(LovableUi.run { chip(kindFa(item.mediaKind), "secondary") })
        row.addView(LinearLayout(this@SavedActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(LovableUi.run { text(item.shortcode.ifBlank { "Instagram" }, 12.5f, LovableUi.foreground, true) })
            addView(LovableUi.run { text("تحلیل کامل • آماده بازبینی", 10.5f, LovableUi.muted) })
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = LovableUi.run { dp(8) }; marginEnd = LovableUi.run { dp(8) } })
        row.addView(LovableUi.run { text("›", 24f, LovableUi.primary, true) })
        addView(row)
        setOnClickListener { startActivity(Intent(this@SavedActivity, ViralShareActivity::class.java)) }
    }

    private fun openOrExportPdf(item: StudioEntry) {
        if (item.type != "scenario") return
        if (!item.pdfPath.isNullOrBlank()) {
            val file = File(item.pdfPath!!)
            if (file.exists()) {
                TakeoffPdfExporter.shareOrViewPdf(this, file, item.title)
                return
            }
        }
        val jsonStr = item.resultJson
        if (jsonStr.isNullOrBlank()) {
            Toast.makeText(this, "داده سناریو در دسترس نیست.", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "در حال ایجاد خروجی PDF…", Toast.LENGTH_SHORT).show()
        Thread {
            val root = runCatching { JSONObject(jsonStr) }.getOrNull()
            if (root != null) {
                val pair = TakeoffPdfExporter.exportScenarioStudio(this, root)
                runOnUiThread {
                    pair.second?.let {
                        StudioResultStore(this).update(item.id) { current -> current.copy(pdfPath = it.absolutePath) }
                        TakeoffPdfExporter.shareOrViewPdf(this, it, item.title)
                    } ?: Toast.makeText(this, "ساخت PDF انجام نشد.", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
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
