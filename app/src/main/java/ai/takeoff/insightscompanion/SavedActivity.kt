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
    private var currentFilter: String = "all" // "all", "scenario", "ai_video", "media"
    private lateinit var contentContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LovableUi.applyWindow(this)
        val initialFilter = intent.getStringExtra("filter")
        if (initialFilter in listOf("all", "scenario", "ai_video", "media")) {
            currentFilter = initialFilter!!
        } else if (initialFilter == "studios") {
            currentFilter = "scenario"
        }
        setContentView(buildUi())
    }

    override fun onResume() {
        super.onResume()
        refreshContent()
    }

    private fun buildUi(): View {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(LovableUi.background)
        }
        page.addView(LovableUi.run { topBar("ذخیره‌ها", "آرشیو کامل سناریوها، ویدیوهای AI و تحلیل‌ها") })

        // Filter chips bar
        val filterBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(LovableUi.run { dp(14) }, LovableUi.run { dp(10) }, LovableUi.run { dp(14) }, LovableUi.run { dp(4) })
        }
        filterBar.addView(createFilterChip("همه", "all"), LinearLayout.LayoutParams(0, LovableUi.run { dp(38) }, 1f).apply { marginEnd = LovableUi.run { dp(4) } })
        filterBar.addView(createFilterChip("سناریوی واقعی", "scenario"), LinearLayout.LayoutParams(0, LovableUi.run { dp(38) }, 1.3f).apply { marginEnd = LovableUi.run { dp(4) } })
        filterBar.addView(createFilterChip("استودیو AI", "ai_video"), LinearLayout.LayoutParams(0, LovableUi.run { dp(38) }, 1.2f).apply { marginEnd = LovableUi.run { dp(4) } })
        filterBar.addView(createFilterChip("آنالیز ریلز", "media"), LinearLayout.LayoutParams(0, LovableUi.run { dp(38) }, 1.2f))
        page.addView(filterBar)

        val scroll = ScrollView(this).apply { isFillViewport = true; overScrollMode = View.OVER_SCROLL_NEVER }
        contentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(LovableUi.run { dp(16) }, LovableUi.run { dp(12) }, LovableUi.run { dp(16) }, LovableUi.run { dp(28) })
        }
        scroll.addView(contentContainer)
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        page.addView(LovableUi.run { bottomNav("saved") })

        refreshContent()
        return page
    }

    private fun createFilterChip(title: String, tag: String): View {
        val selected = (currentFilter == tag)
        return TextView(this).apply {
            text = title
            gravity = Gravity.CENTER
            textSize = 11.5f
            setTextColor(if (selected) Color.WHITE else LovableUi.muted)
            background = LovableUi.run {
                if (selected) rounded(LovableUi.primary, 14, LovableUi.primaryDeep)
                else rounded(LovableUi.card, 14, LovableUi.border)
            }
            setOnClickListener {
                currentFilter = tag
                recreateUi()
            }
        }
    }

    private fun recreateUi() {
        setContentView(buildUi())
    }

    private fun refreshContent() {
        contentContainer.removeAllViews()
        val studioStore = StudioResultStore(this)
        val studioItems = studioStore.getAll()
        val mediaItems = SharedMediaQueue(this).all().filter { it.status == "completed" }

        // Top Summary Card
        contentContainer.addView(LovableUi.run { card() }.apply {
            val row = LinearLayout(this@SavedActivity).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; gravity = Gravity.CENTER_VERTICAL }
            row.addView(LovableUi.run { chip("آرشیو محلی امن", "primary") })
            val totalCount = studioItems.size + mediaItems.size
            row.addView(LovableUi.run { text("${LovableUi.fa(totalCount)} مورد ذخیره‌شده", 12f, LovableUi.foreground, true) }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(row)
            addView(LovableUi.run { text("تمام سناریوها، پرامپت‌های استودیوی هوش مصنوعی و فایل‌های PDF برای دسترسی آفلاین روی دستگاه ذخیره شده‌اند.", 11.5f, LovableUi.muted) }.apply { setPadding(0, LovableUi.run { dp(8) }, 0, 0) })
        }, LovableUi.run { margin(bottom = 16) })

        // Render Scenario Items
        if (currentFilter in listOf("all", "scenario")) {
            val scenarios = studioItems.filter { it.type == "scenario" }
            if (scenarios.isNotEmpty()) {
                contentContainer.addView(LovableUi.run { sectionTitle("سناریوهای واقعی آماده ضبط") }, LovableUi.run { margin(bottom = 8, top = 6) })
                for (item in scenarios) {
                    contentContainer.addView(buildScenarioCard(item), LovableUi.run { margin(bottom = 10) })
                }
            }
        }

        // Render AI Video Items
        if (currentFilter in listOf("all", "ai_video")) {
            val aiVideos = studioItems.filter { it.type == "ai_video" }
            if (aiVideos.isNotEmpty()) {
                contentContainer.addView(LovableUi.run { sectionTitle("ویدیوهای هوش مصنوعی و پرامپت‌ها") }, LovableUi.run { margin(bottom = 8, top = 10) })
                for (item in aiVideos) {
                    contentContainer.addView(buildAiVideoCard(item), LovableUi.run { margin(bottom = 10) })
                }
            }
        }

        // Render Instagram Media Analysis Items
        if (currentFilter in listOf("all", "media")) {
            if (mediaItems.isNotEmpty()) {
                contentContainer.addView(LovableUi.run { sectionTitle("تحلیل‌های ذخیره‌شده ریلز") }, LovableUi.run { margin(bottom = 8, top = 10) })
                for (item in mediaItems) {
                    contentContainer.addView(buildMediaCard(item), LovableUi.run { margin(bottom = 8) })
                }
            }
        }

        if (contentContainer.childCount <= 1) {
            contentContainer.addView(LovableUi.run { card() }.apply {
                addView(LovableUi.run { text("هنوز موردی در این بخش ذخیره نشده است.", 12f, LovableUi.muted) })
            })
        }
    }

    private fun buildScenarioCard(item: StudioEntry): View = LovableUi.run { card() }.apply {
        val top = LinearLayout(this@SavedActivity).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; gravity = Gravity.CENTER_VERTICAL }
        val isShort = item.mode.startsWith("short_15s")
        top.addView(LovableUi.run { chip(if (isShort) "۱۵ ثانیه‌ای تک‌سکانسه" else "۱۰ سناریوی هوشمند", "primary") })
        val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(item.createdAt))
        top.addView(LovableUi.run { text(dateStr, 11f, LovableUi.muted) }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = LovableUi.run { dp(8) } })
        addView(top)

        addView(LovableUi.run { text("حوزه: ${item.niche}", 14f, LovableUi.foreground, true) }.apply { setPadding(0, LovableUi.run { dp(6) }, 0, 0) })
        if (item.description.isNotBlank()) {
            addView(LovableUi.run { text(item.description.take(100) + if (item.description.length > 100) "…" else "", 11.5f, LovableUi.muted) }.apply { setPadding(0, LovableUi.run { dp(4) }, 0, LovableUi.run { dp(8) }) })
        }

        val actionRow = LinearLayout(this@SavedActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
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
        addView(actionRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = LovableUi.run { dp(4) } })
    }

    private fun buildAiVideoCard(item: StudioEntry): View = LovableUi.run { card() }.apply {
        val top = LinearLayout(this@SavedActivity).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; gravity = Gravity.CENTER_VERTICAL }
        val isViral = item.mode.startsWith("viral_10s")
        top.addView(LovableUi.run { chip(if (isViral) "وایرال ۱۰s • Omni" else "سینمایی ۹:۱۶", "secondary") })
        val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(item.createdAt))
        top.addView(LovableUi.run { text(dateStr, 11f, LovableUi.muted) }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = LovableUi.run { dp(8) } })
        addView(top)

        addView(LovableUi.run { text(item.title.ifBlank { item.niche }, 14f, LovableUi.foreground, true) }.apply { setPadding(0, LovableUi.run { dp(6) }, 0, 0) })
        if (item.description.isNotBlank()) {
            addView(LovableUi.run { text(item.description.take(100) + if (item.description.length > 100) "…" else "", 11.5f, LovableUi.muted) }.apply { setPadding(0, LovableUi.run { dp(4) }, 0, LovableUi.run { dp(8) }) })
        }

        val actionRow = LinearLayout(this@SavedActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val copyPromptBtn = LovableUi.run { primaryButton("📋 کپی پرامپت‌ها") {
            startActivity(Intent(this@SavedActivity, AiVideoStudioActivity::class.java).apply {
                putExtra("selected_task_id", item.id)
            })
        } }
        val pdfBtn = LovableUi.run { ghostButton("📄 فایل PDF") {
            openOrExportPdf(item)
        } }
        actionRow.addView(copyPromptBtn, LinearLayout.LayoutParams(0, LovableUi.run { dp(44) }, 1.2f).apply { marginEnd = LovableUi.run { dp(4) } })
        actionRow.addView(pdfBtn, LinearLayout.LayoutParams(0, LovableUi.run { dp(44) }, 1f).apply { marginStart = LovableUi.run { dp(4) } })
        addView(actionRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = LovableUi.run { dp(4) } })
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
                val pair = if (item.type == "scenario") TakeoffPdfExporter.exportScenarioStudio(this, root)
                else TakeoffPdfExporter.exportAiVideoStudio(this, root)
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
