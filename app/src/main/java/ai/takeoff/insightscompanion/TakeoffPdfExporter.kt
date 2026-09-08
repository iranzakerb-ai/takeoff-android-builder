package ai.takeoff.insightscompanion

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TakeoffPdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2)

    fun exportScenarioStudio(context: Context, root: JSONObject): Pair<Uri?, File?> {
        val pdf = PdfDocument()
        var pageNumber = 1
        var currentPage: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var cursorY = 0f

        val items = root.optJSONArray("scenarios") ?: JSONArray()
        val brief = root.optJSONObject("brief")
        val niche = brief?.optString("niche").orEmpty()
        val mode = root.optString("mode", "smart")

        fun startNewPage(headerTitle: String, headerSubtitle: String): Canvas {
            currentPage?.let {
                pdf.finishPage(it)
            }
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = pdf.startPage(pageInfo)
            currentPage = page
            val c = page.canvas
            c.drawColor(Color.rgb(250, 250, 252))

            // Top Header Bar
            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, PAGE_WIDTH.toFloat(), 68f,
                    intArrayOf(Color.rgb(15, 23, 42), Color.rgb(30, 41, 59)),
                    null, Shader.TileMode.CLAMP
                )
            }
            c.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 68f, headerPaint)

            // Accent Coral Top Line
            val accentLine = Paint().apply { color = Color.rgb(255, 96, 32) }
            c.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 3.5f, accentLine)

            // Header Title RTL
            drawRtlText(c, headerTitle, PAGE_WIDTH - MARGIN, 26f, CONTENT_WIDTH, 14f, Color.WHITE, bold = true)
            drawRtlText(c, headerSubtitle, PAGE_WIDTH - MARGIN, 46f, CONTENT_WIDTH, 9.5f, Color.rgb(203, 213, 225))

            // Footer
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(148, 163, 184)
                textSize = 8.5f
                textAlign = Paint.Align.CENTER
            }
            c.drawText("TakeOff Scenario Studio • صفحه $pageNumber", PAGE_WIDTH / 2f, PAGE_HEIGHT - 18f, footerPaint)

            pageNumber++
            cursorY = 82f
            return c
        }

        fun ensureSpace(neededHeight: Float, headerTitle: String, headerSubtitle: String): Canvas {
            val currentCanvas = canvas
            return if (currentCanvas == null || cursorY + neededHeight > PAGE_HEIGHT - 36f) {
                val newC = startNewPage(headerTitle, headerSubtitle)
                canvas = newC
                newC
            } else {
                currentCanvas
            }
        }

        // --- PAGE 1: Overview ---
        val c1 = ensureSpace(300f, "استودیو سناریو تیک‌آف", "بسته جامع آماده ضبط • حوزه: $niche")
        val modeTitle = when (mode) {
            "smart_silent" -> "هوشمند چندسکانسه • بدون دیالوگ (اکت و SFX)"
            "short_15s" -> "کوتاه ۱۵ ثانیه‌ای • تک‌سکانسه با دیالوگ"
            "short_15s_silent" -> "کوتاه ۱۵ ثانیه‌ای • تک‌سکانسه بدون دیالوگ"
            else -> "هوشمند چندسکانسه • با دیالوگ"
        }

        // Summary Card
        drawCardBox(c1, MARGIN, cursorY, CONTENT_WIDTH, 110f, Color.WHITE, Color.rgb(226, 232, 240))
        drawRtlText(c1, "مشخصات پکیج سناریوها", PAGE_WIDTH - MARGIN - 14f, cursorY + 14f, CONTENT_WIDTH - 28f, 13f, Color.rgb(15, 23, 42), bold = true)
        val summaryText = "حوزه کسب‌وکار: $niche\n" +
                "حالت کارگردانی: $modeTitle\n" +
                "تعداد سناریو: ${LovableUi.fa(items.length())} سناریوی کامل\n" +
                "شواهد یادگیری تحلیل‌شده: ${LovableUi.fa(root.optInt("memory_evidence_count"))} الگو"
        drawRtlText(c1, summaryText, PAGE_WIDTH - MARGIN - 14f, cursorY + 36f, CONTENT_WIDTH - 28f, 10f, Color.rgb(51, 65, 85))
        cursorY += 122f

        // Scientific Notice
        val notice = root.optString("scientific_notice", "امتیاز پتانسیل وایرال، امتیاز کیفی خلاقه است و به منزله تضمین بازدید نیست.")
        drawCardBox(c1, MARGIN, cursorY, CONTENT_WIDTH, 44f, Color.rgb(254, 243, 199), Color.rgb(251, 191, 36))
        drawRtlText(c1, "نکته علمی: $notice", PAGE_WIDTH - MARGIN - 12f, cursorY + 12f, CONTENT_WIDTH - 24f, 9.5f, Color.rgb(146, 64, 14))
        cursorY += 56f

        // Render each Scenario
        for (i in 0 until items.length()) {
            val s = items.optJSONObject(i) ?: continue
            val rank = s.optInt("rank", i + 1)
            val title = s.optString("title", "سناریو $rank")
            val score = s.optInt("viral_potential_score", 0)
            val duration = s.optInt("total_duration_seconds", 15)
            val actorCount = s.optInt("actor_count", 1)
            val formatFamily = s.optString("format_family")
            val scenes = s.optJSONArray("scenes") ?: JSONArray()
            val hook = s.optJSONObject("hook")

            val cardTitle = "سناریو #${LovableUi.fa(rank)} • $title"
            val pageSubtitle = "مدت: ${LovableUi.fa(duration)}s | سکانس: ${LovableUi.fa(scenes.length())} | بازیگر: ${LovableUi.fa(actorCount)}"

            // Header for Scenario
            val c = ensureSpace(90f, "سناریو #${LovableUi.fa(rank)} • $niche", pageSubtitle)
            drawCardBox(c, MARGIN, cursorY, CONTENT_WIDTH, 70f, Color.WHITE, Color.rgb(203, 213, 225))

            // Score Chip
            val scoreBoxX = MARGIN + 12f
            drawCardBox(c, scoreBoxX, cursorY + 10f, 74f, 26f, Color.rgb(255, 96, 32), Color.rgb(234, 88, 12))
            drawRtlText(c, "${LovableUi.fa(score)}/۱۰۰", scoreBoxX + 68f, cursorY + 16f, 62f, 10f, Color.WHITE, bold = true)

            // Title & Meta
            drawRtlText(c, "$rank. $title", PAGE_WIDTH - MARGIN - 12f, cursorY + 12f, CONTENT_WIDTH - 110f, 13f, Color.rgb(15, 23, 42), bold = true)
            val metaStr = "فرمت: $formatFamily | مدت: ${LovableUi.fa(duration)} ثانیه | سکانس: ${LovableUi.fa(scenes.length())} | بازیگر: ${LovableUi.fa(actorCount)}"
            drawRtlText(c, metaStr, PAGE_WIDTH - MARGIN - 12f, cursorY + 34f, CONTENT_WIDTH - 110f, 9.5f, Color.rgb(71, 85, 105))
            drawRtlText(c, "ایده محوری: ${s.optString("core_idea")}", PAGE_WIDTH - MARGIN - 12f, cursorY + 50f, CONTENT_WIDTH - 24f, 9.5f, Color.rgb(30, 41, 59))
            cursorY += 80f

            // Hook Box
            val spokenHook = hook?.optString("spoken").orEmpty()
            val visualHook = hook?.optString("visual").orEmpty()
            val hookText = if (spokenHook.isNotBlank()) "قلاب گفتاری: $spokenHook\nقلاب تصویری: $visualHook" else "قلاب تصویری: $visualHook"
            val hookH = measureTextHeight(hookText, CONTENT_WIDTH - 24f, 9.5f) + 24f
            val cH = ensureSpace(hookH, cardTitle, pageSubtitle)
            drawCardBox(cH, MARGIN, cursorY, CONTENT_WIDTH, hookH, Color.rgb(241, 245, 249), Color.rgb(203, 213, 225))
            drawRtlText(cH, "⚡ قلاب شروع (Scroll-Stopper)", PAGE_WIDTH - MARGIN - 12f, cursorY + 8f, CONTENT_WIDTH - 24f, 10.5f, Color.rgb(234, 88, 12), bold = true)
            drawRtlText(cH, hookText, PAGE_WIDTH - MARGIN - 12f, cursorY + 24f, CONTENT_WIDTH - 24f, 9.5f, Color.rgb(30, 41, 59))
            cursorY += hookH + 10f

            // Scenes
            for (j in 0 until scenes.length()) {
                val sc = scenes.optJSONObject(j) ?: continue
                val scNum = sc.optInt("number", j + 1)
                val start = sc.optDouble("start_seconds", 0.0)
                val end = sc.optDouble("end_seconds", 0.0)
                val purpose = sc.optString("purpose")
                val action = sc.optString("action")
                val dialogue = sc.optString("dialogue")
                val camera = sc.optString("camera")
                val sfx = sc.optString("audio_sfx")

                val sceneBody = "اکشن/تصویر: $action\n" +
                        (if (dialogue.isNotBlank()) "دیالوگ: «$dialogue»\n" else "صدا/کلام: بدون دیالوگ\n") +
                        "دوربین و شات: $camera | فولی/SFX: $sfx"

                val bodyH = measureTextHeight(sceneBody, CONTENT_WIDTH - 24f, 9.5f)
                val sceneBoxH = bodyH + 34f
                val cSc = ensureSpace(sceneBoxH + 8f, cardTitle, pageSubtitle)

                drawCardBox(cSc, MARGIN, cursorY, CONTENT_WIDTH, sceneBoxH, Color.WHITE, Color.rgb(226, 232, 240))
                val scHeader = "سکانس ${LovableUi.fa(scNum)} • (${start} تا ${end} ثانیه) — $purpose"
                drawRtlText(cSc, scHeader, PAGE_WIDTH - MARGIN - 12f, cursorY + 8f, CONTENT_WIDTH - 24f, 10.5f, Color.rgb(30, 58, 138), bold = true)
                drawRtlText(cSc, sceneBody, PAGE_WIDTH - MARGIN - 12f, cursorY + 26f, CONTENT_WIDTH - 24f, 9.5f, Color.rgb(51, 65, 85))
                cursorY += sceneBoxH + 8f
            }

            // Payoff & CTA
            val payoffStr = "نتیجه و Payoff: ${s.optString("payoff")}\nدعوت به اقدام (CTA): ${s.optString("cta")}"
            val payH = measureTextHeight(payoffStr, CONTENT_WIDTH - 24f, 9.5f) + 20f
            val cP = ensureSpace(payH + 16f, cardTitle, pageSubtitle)
            drawCardBox(cP, MARGIN, cursorY, CONTENT_WIDTH, payH, Color.rgb(240, 253, 244), Color.rgb(187, 247, 208))
            drawRtlText(cP, payoffStr, PAGE_WIDTH - MARGIN - 12f, cursorY + 10f, CONTENT_WIDTH - 24f, 9.5f, Color.rgb(22, 101, 52))
            cursorY += payH + 20f
        }

        currentPage?.let { pdf.finishPage(it) }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val cleanNiche = niche.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF]"), "_").trim()
        val fileName = "TakeOff_Scenarios_${cleanNiche}_$timeStamp.pdf"

        val cacheFile = File(context.cacheDir, fileName)
        FileOutputStream(cacheFile).use { pdf.writeTo(it) }

        val downloadUri = saveToDownloads(context, pdf, fileName)
        pdf.close()

        return downloadUri to cacheFile
    }

    fun exportAiVideoStudio(context: Context, root: JSONObject): Pair<Uri?, File?> {
        val pdf = PdfDocument()
        var pageNumber = 1
        var currentPage: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var cursorY = 0f

        val video = root.optJSONObject("video") ?: root.optJSONArray("videos")?.optJSONObject(0) ?: JSONObject()
        val brief = root.optJSONObject("brief")
        val niche = brief?.optString("niche").orEmpty()
        val title = video.optString("title", "ویدیوی هوش مصنوعی")
        val characters = video.optJSONArray("characters") ?: JSONArray()
        val scenes = video.optJSONArray("scenes") ?: JSONArray()
        val totalSec = video.optInt("total_duration_seconds", 10)

        fun startNewPage(headerTitle: String, headerSubtitle: String): Canvas {
            currentPage?.let { pdf.finishPage(it) }
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = pdf.startPage(pageInfo)
            currentPage = page
            val c = page.canvas
            c.drawColor(Color.rgb(250, 250, 252))

            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, PAGE_WIDTH.toFloat(), 68f,
                    intArrayOf(Color.rgb(24, 18, 48), Color.rgb(45, 28, 88)),
                    null, Shader.TileMode.CLAMP
                )
            }
            c.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 68f, headerPaint)

            val accentLine = Paint().apply { color = Color.rgb(168, 85, 247) }
            c.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 3.5f, accentLine)

            drawRtlText(c, headerTitle, PAGE_WIDTH - MARGIN, 26f, CONTENT_WIDTH, 14f, Color.WHITE, bold = true)
            drawRtlText(c, headerSubtitle, PAGE_WIDTH - MARGIN, 46f, CONTENT_WIDTH, 9.5f, Color.rgb(233, 213, 255))

            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(148, 163, 184)
                textSize = 8.5f
                textAlign = Paint.Align.CENTER
            }
            c.drawText("TakeOff AI Video Studio • صفحه $pageNumber", PAGE_WIDTH / 2f, PAGE_HEIGHT - 18f, footerPaint)

            pageNumber++
            cursorY = 82f
            return c
        }

        fun ensureSpace(neededHeight: Float, headerTitle: String, headerSubtitle: String): Canvas {
            val currentCanvas = canvas
            return if (currentCanvas == null || cursorY + neededHeight > PAGE_HEIGHT - 36f) {
                val newC = startNewPage(headerTitle, headerSubtitle)
                canvas = newC
                newC
            } else {
                currentCanvas
            }
        }

        // Overview Card
        val c1 = ensureSpace(220f, "استودیو تولید ویدیوی هوش مصنوعی (Omni / Flow)", "$title • $niche")
        drawCardBox(c1, MARGIN, cursorY, CONTENT_WIDTH, 90f, Color.WHITE, Color.rgb(226, 232, 240))
        drawRtlText(c1, title, PAGE_WIDTH - MARGIN - 14f, cursorY + 14f, CONTENT_WIDTH - 28f, 13.5f, Color.rgb(15, 23, 42), bold = true)
        val overview = "حوزه: $niche | مدت: ${LovableUi.fa(totalSec)} ثانیه | سکانس: ${LovableUi.fa(scenes.length())} | کاراکتر: ${LovableUi.fa(characters.length())}\n" +
                "ایده محوری: ${video.optString("core_idea")}\n" +
                "مدل رندر: Google Omni / Flow.labs (فرمت عمودی ۹:۱۶)"
        drawRtlText(c1, overview, PAGE_WIDTH - MARGIN - 14f, cursorY + 36f, CONTENT_WIDTH - 28f, 9.5f, Color.rgb(51, 65, 85))
        cursorY += 100f

        // Characters Section
        for (i in 0 until characters.length()) {
            val ch = characters.optJSONObject(i) ?: continue
            val charId = ch.optString("character_id", "@CHAR_01")
            val role = ch.optString("role")
            val prompt = ch.optString("character_sheet_prompt")
            val charText = "نقش: $role\nقفل هویت: ${ch.optString("identity_lock")}\nپرامپت پرتره چندزاویه‌ای (Character Sheet Prompt):\n$prompt"
            val h = measureTextHeight(charText, CONTENT_WIDTH - 24f, 9.2f) + 32f
            val cCh = ensureSpace(h + 10f, "کاراکترهای ویدیوی AI", charId)
            drawCardBox(cCh, MARGIN, cursorY, CONTENT_WIDTH, h, Color.WHITE, Color.rgb(216, 180, 254))
            drawRtlText(cCh, "✦ مشخصات و پرامپت کاراکتر: $charId", PAGE_WIDTH - MARGIN - 12f, cursorY + 10f, CONTENT_WIDTH - 24f, 11f, Color.rgb(126, 34, 206), bold = true)
            drawRtlText(cCh, charText, PAGE_WIDTH - MARGIN - 12f, cursorY + 28f, CONTENT_WIDTH - 24f, 9.2f, Color.rgb(51, 65, 85))
            cursorY += h + 10f
        }

        // Scenes & Omni Prompts
        for (i in 0 until scenes.length()) {
            val sc = scenes.optJSONObject(i) ?: continue
            val scNum = sc.optInt("number", i + 1)
            val sec = sc.optInt("duration_seconds", 4)
            val summary = sc.optString("scene_summary").ifBlank { sc.optString("action") }
            val omni = sc.optString("omni_prompt").ifBlank { video.optString("omni_prompt") }

            val scText = "هدف سکانس: $summary\n\nپرامپت کامل Omni برای تولید این کلیپ:\n$omni"
            val h = measureTextHeight(scText, CONTENT_WIDTH - 24f, 9f) + 34f
            val cSc = ensureSpace(h + 10f, "سکانس‌های ویدیوی AI", "سکانس $scNum (${sec} ثانیه)")
            drawCardBox(cSc, MARGIN, cursorY, CONTENT_WIDTH, h, Color.WHITE, Color.rgb(226, 232, 240))
            drawRtlText(cSc, "سکانس ${LovableUi.fa(scNum)} • مدت: ${LovableUi.fa(sec)} ثانیه (Preset Flow/Omni)", PAGE_WIDTH - MARGIN - 12f, cursorY + 10f, CONTENT_WIDTH - 24f, 11f, Color.rgb(30, 41, 59), bold = true)
            drawRtlText(cSc, scText, PAGE_WIDTH - MARGIN - 12f, cursorY + 28f, CONTENT_WIDTH - 24f, 9f, Color.rgb(71, 85, 105))
            cursorY += h + 10f
        }

        currentPage?.let { pdf.finishPage(it) }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val cleanTitle = title.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF]"), "_").trim()
        val fileName = "TakeOff_AIVideo_${cleanTitle}_$timeStamp.pdf"

        val cacheFile = File(context.cacheDir, fileName)
        FileOutputStream(cacheFile).use { pdf.writeTo(it) }

        val downloadUri = saveToDownloads(context, pdf, fileName)
        pdf.close()

        return downloadUri to cacheFile
    }

    private fun saveToDownloads(context: Context, pdf: PdfDocument, fileName: String): Uri? {
        return runCatching {
            val resolver = context.contentResolver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/TakeOff")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { pdf.writeTo(it) }
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
                uri
            } else {
                @Suppress("DEPRECATION")
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "TakeOff")
                if (!dir.exists()) dir.mkdirs()
                val target = File(dir, fileName)
                FileOutputStream(target).use { pdf.writeTo(it) }
                Uri.fromFile(target)
            }
        }.getOrNull()
    }

    fun shareOrViewPdf(context: Context, cacheFile: File, title: String) {
        runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "مشاهده یا ارسال PDF $title")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }

    private fun drawCardBox(canvas: Canvas, x: Float, y: Float, width: Float, height: Float, fill: Int, stroke: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fill
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stroke
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val r = RectF(x, y, x + width, y + height)
        canvas.drawRoundRect(r, 6f, 6f, paint)
        canvas.drawRoundRect(r, 6f, 6f, strokePaint)
    }

    private fun drawRtlText(
        canvas: Canvas,
        text: String,
        rightX: Float,
        topY: Float,
        width: Float,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): Float {
        if (text.isBlank()) return 0f
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        }
        val layout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(TextDirectionHeuristics.RTL)
                .setLineSpacing(0f, 1.2f)
                .setIncludePad(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(text, paint, width.toInt(), Layout.Alignment.ALIGN_NORMAL, 1.2f, 0f, false)
        }
        canvas.save()
        canvas.translate(rightX - width, topY)
        layout.draw(canvas)
        canvas.restore()
        return layout.height.toFloat()
    }

    private fun measureTextHeight(text: String, width: Float, size: Float): Float {
        if (text.isBlank()) return 0f
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        val layout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(TextDirectionHeuristics.RTL)
                .setLineSpacing(0f, 1.2f)
                .setIncludePad(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(text, paint, width.toInt(), Layout.Alignment.ALIGN_NORMAL, 1.2f, 0f, false)
        }
        return layout.height.toFloat()
    }
}
