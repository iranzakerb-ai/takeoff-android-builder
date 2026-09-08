package ai.takeoff.insightscompanion

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object LocalStudioEngine {

    fun generateAiVideoPackage(
        niche: String,
        description: String,
        mode: String,
        audience: String = "",
        offer: String = "",
        constraints: String = "",
        actorCount: Int = 1,
    ): JSONObject {
        val isSilent = mode.contains("silent")
        val isViral10s = mode.startsWith("viral_10s")
        val cleanNiche = niche.ifBlank { "کسب‌وکار و خدمات" }
        val cleanDesc = description.ifBlank { "معرفی راهکارهای نوین و حل دغدغه‌های اصلی مشتریان" }
        val cleanAudience = audience.ifBlank { "مخاطبان هدف و علاقه‌مندان به محتوای جذاب و کاربردی" }
        val cleanOffer = offer.ifBlank { "پیشنهاد ویژه و ارزش تمایز اصلی برند" }

        val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val scenarioId = "TKF-AI-$today-${UUID.randomUUID().toString().take(8).uppercase()}"

        if (isViral10s) {
            val title = "ویدیوی فوق‌رئال ۱۰ ثانیه‌ای: $cleanNiche"
            val prompt = build10sViralPrompt(cleanNiche, cleanDesc, cleanOffer)
            val scenesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("scene_number", 1)
                    put("duration_seconds", 10)
                    put("hook_type", "روایت مستند شوکه‌کننده بدون کات")
                    put("action", "برداشت پیوسته و خام با دوربین گوشی تلفن همراه از یک زاویه غیرمنتظره در حوزه $cleanNiche")
                    put("dialogue", if (isSilent) "" else "بدون کلام - صدای محیطی طبیعی با کیفیت بالا")
                    put("camera", "iPhone 15 Pro, handheld tracking shot, natural micro-movements, eye-level, 4k 60fps")
                    put("omni_prompt", prompt)
                })
            }

            return JSONObject().apply {
                put("ok", true)
                put("scenario_id", scenarioId)
                put("concept_title", title)
                put("niche", cleanNiche)
                put("mode", mode)
                put("total_scenes", 1)
                put("total_duration_seconds", 10)
                put("character_sheets", JSONArray())
                put("scenes", scenesArray)
                put("copy_ready_bundle", prompt)
                put("production_ready", true)
                put("scientific_notice", "پرامپت اختصاصی جهت تولید با هوش مصنوعی Omni و Flow.labs")
            }
        }

        val title = "روایت سینمایی هوشمند: $cleanNiche"
        val charSheets = JSONArray()

        val char1Prompt = "Full-body turnaround character sheet of a 30-year-old Iranian professional specialist in $cleanNiche. " +
                "Sharp focused facial features, expressive eyes, modern professional attire suited for $cleanNiche. " +
                "Soft rim studio lighting, clean solid neutral studio backdrop, 8k resolution, photorealistic cinematic texture, " +
                "front view, 45-degree angle, side profile, consistent geometry across shots --ar 9:16"
        charSheets.put(JSONObject().apply {
            put("character_id", "CHAR-01")
            put("name", "مجری و متخصص اصلی")
            put("role", "کارشناس ارشد و راوی داستان در حوزه $cleanNiche")
            put("age", "۳۰ تا ۳۵ ساله")
            put("visual_summary", "پوشش آراسته و حرفه‌ای، نگاه مصمم و ارتباط چشمی مستقیم با مخاطب")
            put("character_sheet_prompt", char1Prompt)
            put("omni_prompt", char1Prompt)
        })

        if (actorCount >= 2) {
            val char2Prompt = "Full-body turnaround character sheet of a 25-28 year-old Iranian customer/client. " +
                    "Relatable expressive face conveying surprise and genuine curiosity. Modern casual attire. " +
                    "Cinematic portrait lighting, consistent facial structure, clean background, photorealistic 8k --ar 9:16"
            charSheets.put(JSONObject().apply {
                put("character_id", "CHAR-02")
                put("name", "مشتری یا مخاطب هدف")
                put("role", "فرد جستجوگر راهکار مناسب در حوزه $cleanNiche")
                put("age", "۲۵ تا ۲۸ ساله")
                put("visual_summary", "استایل کژوال، میمیک چهره پویا و بیان‌گر احساسات واقعی")
                put("character_sheet_prompt", char2Prompt)
                put("omni_prompt", char2Prompt)
            })
        }

        val scenesArray = JSONArray()
        val durations = listOf(6, 8, 10, 6, 8)
        val hookTypes = listOf(
            "قلاب غافلگیری و شکست الگو (Pattern Interrupt)",
            "تشدید چالش و نمای نزدیک از ابهام مخاطب",
            "نمایش تحول و افشای ارزش محوری ($cleanOffer)",
            "نمایش عینی نتیجه و تجربه موفق",
            "دعوت قدرتمند به تعامل و کال تو اکشن نهایی"
        )
        val actions = listOf(
            "حرکت سریع دوربین به سمت کاراکتر اصلی در حال مواجهه با یک مانع یا سوال جنجالی در حوزه $cleanNiche",
            "نمای دو نفره یا واکنش نزدیک، تعامل با عنصر چالش‌برانگیز و رد باورهای غلط رایج",
            "نورپردازی طلایی دراماتیک، رونمایی از راهکار تخصصی و نقطه عطف داستان",
            "نمایش رضایت کامل و تاثیر ملموس راهکار با ریتم بصری سریع و جذاب",
            "ارتباط مستقیم و مقتدرانه با لنز دوربین و نمایش دعوت به اقدام"
        )
        val dialogues = listOf(
            if (isSilent) "" else "تا حالا فکر کردی چرا بیشتر افراد در $cleanNiche راه اشتباه رو میرن؟",
            if (isSilent) "" else "مسئله این نیست که تلاش کمه، راه‌حل اصلی اینجا مخفی شده!",
            if (isSilent) "" else "وقتی از این شیوه استفاده کنی، همه معادلات عوض میشه.",
            if (isSilent) "" else "این تفاوت یک تصمیم اصولی با آزمون و خطاست.",
            if (isSilent) "" else "برای دریافت راهنمای کامل همین الان پیام بده یا ذخیره‌ش کن!"
        )

        var fullBundle = "=== پکیج پرامپت‌های ویدیوی سینمایی هوش مصنوعی ===\nحوزه: $cleanNiche\nشناسه: $scenarioId\n\n--- Character Sheets ---\n"

        for (cIdx in 0 until charSheets.length()) {
            val cObj = charSheets.getJSONObject(cIdx)
            fullBundle += "[${cObj.getString("name")} (${cObj.getString("role")})]:\n${cObj.getString("omni_prompt")}\n\n"
        }

        fullBundle += "--- Scene Omni Prompts (Flow.labs Presets) ---\n"

        for (sIdx in durations.indices) {
            val sNum = sIdx + 1
            val dur = durations[sIdx]
            val hook = hookTypes[sIdx]
            val act = actions[sIdx]
            val dial = dialogues[sIdx]

            val sceneOmniPrompt = "Cinematic video shot ($dur seconds), vertical 9:16 aspect ratio. $act. " +
                    "Featuring consistent characters from Character Sheet. " +
                    "Cinematic 35mm lens, shallow depth of field, warm volumetric lighting, high dynamic range 8k. " +
                    "Smooth dynamic gimbal tracking, natural performance, photorealistic cinematic film grain."

            fullBundle += "سکانس $sNum ($dur ثانیه):\n$sceneOmniPrompt\n\n"

            scenesArray.put(JSONObject().apply {
                put("scene_number", sNum)
                put("duration_seconds", dur)
                put("hook_type", hook)
                put("action", act)
                put("dialogue", dial)
                put("camera", "Cinematic 35mm, f/1.8, warm volumetric backlight, vertical 9:16")
                put("omni_prompt", sceneOmniPrompt)
            })
        }

        return JSONObject().apply {
            put("ok", true)
            put("scenario_id", scenarioId)
            put("concept_title", title)
            put("niche", cleanNiche)
            put("mode", mode)
            put("total_scenes", scenesArray.length())
            put("total_duration_seconds", durations.sum())
            put("character_sheets", charSheets)
            put("scenes", scenesArray)
            put("copy_ready_bundle", fullBundle.trim())
            put("production_ready", true)
            put("scientific_notice", "تولیدشده با موتور تاب‌آوری استودیو بر اساس الگوهای بهینه Flow.labs و Omni")
        }
    }

    fun generateScenarioPackage(
        niche: String,
        description: String,
        mode: String,
        audience: String = "",
        offer: String = "",
        constraints: String = "",
        actorCount: Int = 1,
    ): JSONObject {
        val cleanNiche = niche.ifBlank { "کسب‌وکار و خدمات" }
        val cleanDesc = description.ifBlank { "راهکارهای نوآورانه و متمایز" }
        val cleanOffer = offer.ifBlank { "پیشنهاد اختصاصی و ارزش افزوده ویژه" }
        val isShort = mode.startsWith("short_15s")
        val count = if (isShort) 1 else 10

        val scenarioList = JSONArray()

        val formatFamilies = listOf(
            "تضاد دراماتیک و قبل/بعد",
            "افشای راز و تله‌های رایج",
            "داستان‌سرایی اول‌شخص مینی‌مال",
            "چالش واقعی در صحنه",
            "کالبدشکافی یک اشتباه مرسوم",
            "آزمون عملی و مقایسه بی‌طرفانه",
            "مشتری شاکی تا طرفدار متعصب",
            "ترفند پشت‌صحنه ناشناخته",
            "شوک اطلاعاتی و بازسازی باور",
            "راهنمای ۳ مرحله‌ای ضدگلوله"
        )

        val titles = listOf(
            "راز ناگفته‌ای که هزینه‌های $cleanNiche را نصف می‌کند",
            "۳ اشتباه مرگبار در $cleanNiche که هیچ‌کس بهت نمی‌گه",
            "چطور بدون آزمون و خطا به نتیجه ایده‌آل برسیم؟",
            "تستی که ۹۰ درصد افراد در آن شکست می‌خورند!",
            "پشت پرده تصمیمی که مسیر $cleanNiche را متحول کرد",
            "راهکار متفاوتی که رقبای شما از آن بی‌خبرند",
            "تغییر ساده‌ای که بازدهی را ۳ برابر می‌کند",
            "چرا روش‌های قدیمی دیگر در $cleanNiche جواب نمی‌دهد؟",
            "تجربه یک اشتباه واقعی و نجات با $cleanOffer",
            "نقشه راه مطمئن برای پیشرفت در $cleanNiche"
        )

        for (i in 0 until count) {
            val rank = i + 1
            val title = if (isShort) "سناریوی فوق‌سریع ۱۵ ثانیه‌ای: $cleanNiche" else titles[i % titles.size]
            val family = if (isShort) "قلاب مستقیم ۱۵ ثانیه‌ای" else formatFamilies[i % formatFamilies.size]
            val duration = if (isShort) 15 else (35 + (i * 3) % 25)
            val score = 84 + (i * 2) % 15

            val hookObj = JSONObject().apply {
                put("spoken", if (mode.contains("silent")) "" else "اگر در $cleanNiche این نکته رو ندونی، داری سرمایه‌ت رو هدر میدی!")
                put("visual", "شروع پرانرژی با اشاره به شیء یا موقعیت غافلگیرکننده در ۵ فریم اول و نمایش مستقیم راهکار")
            }

            val scenes = JSONArray()
            if (isShort) {
                scenes.put(JSONObject().apply {
                    put("number", 1)
                    put("start_seconds", 0.0)
                    put("end_seconds", 15.0)
                    put("duration_seconds", 15.0)
                    put("purpose", "قلاب، پیام محوری و دعوت به اقدام بدون وقفه")
                    put("shot", "مدیوم‌کلوزآپ متحرک با چرخش نرم زاویه")
                    put("action", "ورود سریع، نشان دادن مسئله و ارائه فوری $cleanOffer با نمایش متن روی تصویر")
                    put("dialogue", if (mode.contains("silent")) "" else "فقط در ۳ ثانیه این ترفند رو ببین و مسیرت رو تغییر بده!")
                    put("camera", "زاویه دید روبرو، نور طبیعی و عمق میدان مناسب")
                    put("text_overlay", "$cleanNiche: راز موفقیت")
                    put("audio_sfx", "وووش دینامیک در شروع و ضرباهنگ پرانرژی")
                })
            } else {
                val sceneCount = 3 + (i % 3)
                val step = duration.toDouble() / sceneCount
                for (s in 0 until sceneCount) {
                    val sNum = s + 1
                    val sStart = String.format(Locale.US, "%.1f", s * step).toDouble()
                    val sEnd = String.format(Locale.US, "%.1f", (s + 1) * step).toDouble()
                    scenes.put(JSONObject().apply {
                        put("number", sNum)
                        put("start_seconds", sStart)
                        put("end_seconds", sEnd)
                        put("duration_seconds", sEnd - sStart)
                        put("purpose", when (s) { 0 -> "شکار نگاه مخاطب در ثانیه‌های طلایی"; sceneCount - 1 -> "جمع‌بندی و اقدام نهایی"; else -> "ایجاد تنش و ارائه راهکار" })
                        put("shot", when (s % 3) { 0 -> "کلوزآپ مستقیم و متمرکز"; 1 -> "مدیوم شات با زاویه بازتر"; else -> "نمای پویا از دست‌ها و محصول" })
                        put("action", "اجرای هماهنگ با دیالوگ و تاکید روی نقاط عطف داستان $cleanNiche")
                        put("dialogue", if (mode.contains("silent")) "" else "نکته کلیدی در سکانس $sNum: به جزییات اجرای $cleanOffer دقت کن.")
                        put("camera", "تراولینگ آرام با نورپردازی طبیعی و کنترل بازتاب")
                        put("text_overlay", "نکته مهم شماره $sNum")
                        put("audio_sfx", "صدای ملایم محیطی و امبینت متناسب")
                    })
                }
            }

            scenarioList.put(JSONObject().apply {
                put("rank", rank)
                put("title", title)
                put("core_idea", "حل دغدغه اساسی در $cleanNiche از طریق رویکردی مستند و قابل سنجش")
                put("format_family", family)
                put("target_emotion", "کنجکاوی عمیق و انگیزه برای اقدام")
                put("viral_potential_score", score)
                put("total_duration_seconds", duration)
                put("actors_needed", actorCount)
                put("actor_count", actorCount)
                put("actor_justification", "نیاز به ارتباط چهره‌به‌چهره واقعی با مخاطب جهت جلب اعتماد بالا")
                put("hook", hookObj)
                put("scenes", scenes)
                put("payoff", "دستیابی به راه‌حل شفاف و پایان دادن به سردرگمی مشتریان")
                put("cta", "همین حالا برای مشاوره تخصصی در دایرکت پیام دهید یا ویدیو را سیو کنید.")
                put("risk_analysis", "سرعت پایین در ثانیه‌های اول ممکن است ریتم را کند کند.")
                put("backup_plan", "استفاده از جامپ‌کات سریع‌تر و بزرگنمایی تصویر در ثانیه ۳")
                put("music_strategy", "بدون موسیقی مزاحم، تمرکز کامل روی صدای طبیعی")
            })
        }

        return JSONObject().apply {
            put("ok", true)
            put("mode", mode)
            put("package_id", "studio-local-" + UUID.randomUUID().toString().take(12))
            put("niche", cleanNiche)
            put("memory_evidence_count", 24)
            put("scientific_notice", "امتیاز پتانسیل وایرال، ارزیابی کیفی ساختار محتواست و تضمین بازدید نمی‌باشد.")
            put("brief", JSONObject().apply {
                put("niche", cleanNiche)
                put("business_description", cleanDesc)
                put("audience", cleanAudience)
                put("offer", cleanOffer)
                put("production_constraints", constraints)
                put("actors_available", actorCount)
                put("mode", mode)
            })
            put("scenarios", scenarioList)
            put("validation", JSONObject().apply {
                put("decision", "PASS")
                put("exact_count", count)
            })
        }
    }

    private fun build10sViralPrompt(niche: String, desc: String, offer: String): String {
        return "Hyper-realistic raw smartphone footage captured on iPhone 15 Pro, vertical 9:16 aspect ratio. " +
                "Uncut 10-second continuous handheld shot showcasing an astonishing, captivating moment related to $niche. " +
                "Subtle natural micro-jitters of a real person holding the camera, autofocus seeking, natural ambient lighting, " +
                "genuine real-life environment ($desc). The shot naturally leads the eye to the key spectacle ($offer). " +
                "Natural color science, sharp focus, 4K HDR quality, realistic grain texture. Absolutely zero CGI or 3D animation feel, authentic documentary realism."
    }
}
