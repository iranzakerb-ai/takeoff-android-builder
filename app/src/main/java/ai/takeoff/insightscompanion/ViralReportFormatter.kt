package ai.takeoff.insightscompanion

import org.json.JSONArray
import org.json.JSONObject

/**
 * Human-facing formatter for viral Reel analysis.
 *
 * The backend has shipped more than one evidence schema (V4 jobs and the public
 * resilient endpoint). This formatter deliberately understands both and also
 * walks nested JSON/stringified JSON so a valid saved analysis is never hidden
 * just because a wrapper/key name changed.
 */
object ViralReportFormatter {
    private data class Section(val title: String, val aliases: List<String>)

    private val sections = listOf(
        Section("خلاصه ویدیو", listOf("summary", "video_summary", "content_summary")),
        Section("سبک محتوا", listOf("content_style", "format", "content_format")),
        Section("هدف و مخاطب", listOf("goal_and_audience", "audience_intent", "target_viewer", "content_goal")),
        Section("هوک / قلاب", listOf("hook_intelligence", "hook", "opening_hook", "visual_hook")),
        Section("سه ثانیه اول", listOf("hook_seconds", "first_three_seconds", "opening_seconds")),
        Section("قول یا وعده ابتدای ویدیو", listOf("promise", "opening_promise")),
        Section("داستان و ساختار روایی", listOf("story_structure", "narrative_structure")),
        Section("سناریوی بازسازی‌شده", listOf("scenario_reconstruction", "scenario", "script_reconstruction")),
        Section("تایم‌لاین صحنه‌ها", listOf("slide_or_scene_timeline", "timeline", "scene_timeline")),
        Section("دیالوگ و متن", listOf("dialogue_and_text", "dialogue", "transcript")),
        Section("متن روی تصویر", listOf("on_screen_text", "onscreen_text", "screen_text")),
        Section("کال تو اکشن", listOf("cta", "call_to_action")),
        Section("پرداخت / نتیجه نهایی", listOf("payoff", "reveal", "ending")),
        Section("کنجکاوی و حلقه‌های باز", listOf("curiosity_and_open_loops", "open_loops", "curiosity_gaps")),
        Section("شکستن الگو", listOf("pattern_interrupts", "pattern_interrupt")),
        Section("گرامر بصری", listOf("visual_grammar", "visual_analysis")),
        Section("دوربین و تدوین", listOf("camera_and_editing", "editing", "camera")),
        Section("ریتم و سرعت", listOf("pacing", "pace")),
        Section("صدا و موسیقی", listOf("audio_music", "audio", "music")),
        Section("لحن احساسی", listOf("emotional_tone", "emotion")),
        Section("مکانیزم طنز", listOf("humor_mechanism", "humor")),
        Section("مکانیزم‌های رفتاری", listOf("behavioral_mechanisms", "behavioral_triggers", "triggers")),
        Section("فرضیه‌های ریتنشن", listOf("retention_hypotheses", "retention")),
        Section("چرا احتمالاً وایرال شده", listOf("likely_virality_explanations", "virality_hypotheses", "why_viral")),
        Section("چرا Share / Save / Comment گرفته", listOf("share_save_comment_hypotheses", "engagement_hypotheses")),
        Section("الگوهای قابل استفاده دوباره", listOf("reusable_mechanisms", "reusable_patterns", "candidate_patterns")),
        Section("الگوهای منفی / چیزهایی که نباید کپی شوند", listOf("anti_patterns", "anti_pattern")),
        Section("برآورد شاخص‌های خصوصی ـ فقط فرضیه", listOf("estimated_private_metrics")),
        Section("آمار عمومی مشاهده‌شده", listOf("public_metrics", "public_metrics_json", "metrics")),
        Section("تشخیص منشأ تصویر", listOf("media_origin")),
        Section("کیفیت شواهد", listOf("evidence_quality")),
        Section("عدم قطعیت‌ها", listOf("uncertainties", "uncertainty")),
        Section("آنچه وارد حافظه یادگیری می‌شود", listOf("learning_candidate", "learning_payload")),
    )

    private val wrapperKeys = listOf(
        "analysis", "report", "result", "evidence", "semantic_analysis",
        "multimodal_analysis", "audio_visual_analysis", "behavioral_analysis", "payload",
    )

    fun quick(root: JSONObject): String {
        val hook = compact(find(root, listOf("hook_intelligence", "hook", "opening_hook", "visual_hook")))
        val scenario = compact(find(root, listOf("scenario_reconstruction", "story_structure", "scenario", "narrative_structure")))
        val summary = compact(find(root, listOf("summary", "video_summary", "content_summary")))
        return buildString {
            if (hook.isNotBlank()) append("هوک: ").append(hook.take(180))
            if (scenario.isNotBlank()) {
                if (isNotEmpty()) append('\n')
                append("سناریو: ").append(scenario.take(220))
            }
            if (isEmpty() && summary.isNotBlank()) append("خلاصه: ").append(summary.take(240))
        }
    }

    fun full(root: JSONObject): String {
        val out = StringBuilder()
        val emitted = mutableSetOf<String>()
        for (section in sections) {
            val value = find(root, section.aliases) ?: continue
            val rendered = pretty(value).trim()
            if (rendered.isBlank() || rendered == "null") continue
            val fingerprint = rendered.take(800)
            if (!emitted.add(fingerprint)) continue
            if (out.isNotEmpty()) out.append("\n\n")
            out.append(section.title).append('\n').append(rendered.take(7000))
        }

        if (out.isNotEmpty()) return out.toString()

        // Last-resort compatibility path: if the server stored a valid object under
        // an unforeseen schema, show the useful saved evidence rather than claiming
        // there is no report. Technical transport fields are removed here.
        val fallback = usefulObject(root)
        val rendered = pretty(fallback).trim()
        return if (rendered.isNotBlank() && rendered != "{}") {
            "جزئیات تحلیل ذخیره‌شده\n$rendered"
        } else {
            "برای این مورد جزئیات معنایی قابل نمایش ذخیره نشده است. از گزینه «تحلیل کامل دوباره» استفاده کن."
        }
    }

    fun hasSemanticDetail(root: JSONObject): Boolean = sections.any { find(root, it.aliases) != null }

    private fun find(root: JSONObject, aliases: List<String>): Any? = findRecursive(root, aliases.toSet(), 0)

    private fun findRecursive(node: Any?, aliases: Set<String>, depth: Int): Any? {
        if (node == null || node === JSONObject.NULL || depth > 6) return null
        val normalized = normalizeJson(node)
        when (normalized) {
            is JSONObject -> {
                for (alias in aliases) {
                    if (!normalized.has(alias) || normalized.isNull(alias)) continue
                    val value = normalizeJson(normalized.opt(alias))
                    if (isMeaningful(value)) return value
                }
                // Search known semantic wrappers before walking arbitrary fields.
                for (key in wrapperKeys) {
                    if (!normalized.has(key) || normalized.isNull(key)) continue
                    val found = findRecursive(normalized.opt(key), aliases, depth + 1)
                    if (found != null) return found
                }
                val keys = normalized.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (key in wrapperKeys) continue
                    val child = normalizeJson(normalized.opt(key))
                    if (child is JSONObject || child is JSONArray) {
                        val found = findRecursive(child, aliases, depth + 1)
                        if (found != null) return found
                    }
                }
            }
            is JSONArray -> {
                for (i in 0 until normalized.length()) {
                    val found = findRecursive(normalized.opt(i), aliases, depth + 1)
                    if (found != null) return found
                }
            }
        }
        return null
    }

    private fun isMeaningful(value: Any?): Boolean = when (value) {
        null, JSONObject.NULL -> false
        is String -> value.trim().isNotBlank() && value.trim() != "null"
        is JSONObject -> value.length() > 0
        is JSONArray -> value.length() > 0
        else -> true
    }

    private fun compact(value: Any?): String = when (val v = normalizeJson(value)) {
        null, JSONObject.NULL -> ""
        is String -> v.trim()
        is JSONObject -> {
            val preferred = listOf(
                "exact_hook", "hook", "opening", "promise", "visual_hook", "summary",
                "exact_reconstruction", "concise_reconstruction", "setup", "structure",
            )
            preferred.firstNotNullOfOrNull { key ->
                normalizeJson(v.opt(key)).let { child -> if (child is String && child.isNotBlank()) child.trim() else null }
            } ?: pretty(v).replace('\n', ' ').take(320)
        }
        is JSONArray -> pretty(v).replace('\n', ' ').take(320)
        else -> v.toString()
    }

    private fun normalizeJson(value: Any?): Any? {
        if (value !is String) return value
        val text = value.trim()
        if (text.length < 2) return text
        return when {
            text.startsWith("{") && text.endsWith("}") -> runCatching { JSONObject(text) }.getOrDefault(text)
            text.startsWith("[") && text.endsWith("]") -> runCatching { JSONArray(text) }.getOrDefault(text)
            else -> text
        }
    }

    private fun pretty(value: Any?, depth: Int = 0): String = when (val v = normalizeJson(value)) {
        null, JSONObject.NULL -> ""
        is String -> v.trim()
        is Boolean, is Number -> v.toString()
        is JSONObject -> buildString {
            val keys = v.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key in technicalKeys) continue
                val child = normalizeJson(v.opt(key))
                val rendered = pretty(child, depth + 1).trim()
                if (rendered.isBlank()) continue
                if (isNotEmpty()) append('\n')
                if (child is JSONObject || child is JSONArray) {
                    append("• ").append(keyFa(key)).append(":\n")
                    append(indent(rendered))
                } else {
                    append("• ").append(keyFa(key)).append(": ").append(rendered)
                }
            }
        }
        is JSONArray -> buildString {
            for (i in 0 until v.length()) {
                val rendered = pretty(v.opt(i), depth + 1).trim()
                if (rendered.isBlank()) continue
                if (isNotEmpty()) append('\n')
                append("• ").append(i + 1).append(") ")
                if (rendered.contains('\n')) append('\n').append(indent(rendered)) else append(rendered)
            }
        }
        else -> v.toString()
    }

    private fun indent(text: String): String = text.lines().joinToString("\n") { "   $it" }

    private fun usefulObject(root: JSONObject): JSONObject {
        val out = JSONObject()
        val keys = root.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key in technicalKeys) continue
            val value = normalizeJson(root.opt(key))
            if (!isMeaningful(value)) continue
            out.put(key, value)
        }
        if (out.length() == 0) {
            for (key in wrapperKeys) {
                val value = normalizeJson(root.opt(key))
                if (isMeaningful(value)) out.put(key, value)
            }
        }
        return out
    }

    private val technicalKeys = setOf(
        "job_id", "poll_token", "token", "source_url", "evidence_id", "created_at", "updated_at",
        "available_at", "lease_until", "attempts", "source_bytes", "video_bytes", "frame_count",
        "analysis_provider", "analysis_model", "metric_provenance", "metric_provenance_json",
        "collection_policy", "method_note", "deduplicated", "learning_persisted", "version",
    )

    private fun keyFa(key: String): String = mapOf(
        "summary" to "خلاصه",
        "classification" to "طبقه‌بندی",
        "confidence" to "اطمینان",
        "observed_signals" to "نشانه‌های مشاهده‌شده",
        "counter_signals" to "نشانه‌های مخالف",
        "content_credentials" to "اعتبارنامه محتوایی",
        "segment_map" to "نقشه بخش‌ها",
        "content_goal" to "هدف محتوا",
        "target_viewer" to "مخاطب هدف",
        "setup" to "شروع",
        "conflict_or_problem" to "مسئله / تعارض",
        "escalation_or_information_sequence" to "روند پیشروی",
        "turn_or_reveal" to "چرخش / افشا",
        "payoff" to "پرداخت نهایی",
        "cta" to "کال تو اکشن",
        "loop" to "حلقه",
        "narrative_archetype" to "الگوی روایی",
        "exact_reconstruction" to "بازسازی دقیق",
        "concise_reconstruction" to "بازسازی کوتاه",
        "promise" to "وعده",
        "curiosity_gap" to "شکاف کنجکاوی",
        "pattern_interrupt" to "شکستن الگو",
        "emotional_trigger" to "محرک احساسی",
        "audience_qualification" to "فیلتر مخاطب",
        "likely_scroll_stop_mechanism" to "مکانیزم احتمالی توقف اسکرول",
        "visual" to "تصویر",
        "spoken" to "گفتار",
        "audio" to "صدا",
        "on_screen" to "متن روی تصویر",
        "start" to "شروع",
        "end" to "پایان",
        "start_seconds" to "شروع (ثانیه)",
        "end_seconds" to "پایان (ثانیه)",
        "function" to "نقش",
        "observed_action" to "اتفاق مشاهده‌شده",
        "dialogue" to "دیالوگ",
        "shot" to "نما",
        "camera" to "دوربین",
        "edit" to "تدوین",
        "emotion" to "احساس",
        "basis" to "مبنای برآورد",
        "status" to "وضعیت",
        "views" to "بازدید",
        "likes" to "لایک",
        "comments" to "کامنت",
        "shares" to "اشتراک‌گذاری",
        "saves" to "ذخیره",
        "reposts" to "بازنشر",
        "followers" to "فالوئر",
        "candidate_patterns" to "الگوهای کاندید",
    )[key] ?: key.replace('_', ' ')
}
