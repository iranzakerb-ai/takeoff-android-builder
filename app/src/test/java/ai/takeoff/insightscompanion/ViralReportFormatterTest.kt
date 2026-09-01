package ai.takeoff.insightscompanion

import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ViralReportFormatterTest {
    @Test fun rendersCurrentV4AnalysisContract() {
        val root = JSONObject(
            """{
              "public_metrics":{"views":1200000,"likes":45000},
              "analysis":{
                "summary":"یک موقعیت روزمره با چرخش ناگهانی",
                "hook_intelligence":{"exact_hook":"در ثانیه اول یک اتفاق غیرمنتظره رخ می‌دهد","promise":"ببین آخرش چه می‌شود"},
                "scenario_reconstruction":{"setup":"شروع عادی","conflict_or_problem":"اتفاق خلاف انتظار","turn_or_reveal":"افشای نهایی","payoff":"پایان غافلگیرکننده"},
                "slide_or_scene_timeline":[{"start_seconds":0,"end_seconds":2,"observed_action":"شروع سریع"}],
                "dialogue_and_text":{"dialogue":"صبر کن ببین چی میشه"},
                "cta":"کامنت کن",
                "behavioral_mechanisms":["curiosity gap","reversal"],
                "retention_hypotheses":["باز ماندن سوال تا پایان"],
                "share_save_comment_hypotheses":["غافلگیری قابل ارسال برای دوست"],
                "reusable_mechanisms":["شروع در میانه اتفاق"],
                "uncertainties":["علت واقعی وایرال شدن قابل اثبات نیست"]
              }
            }"""
        )
        val report = ViralReportFormatter.full(root)
        assertTrue(report.contains("هوک / قلاب"))
        assertTrue(report.contains("در ثانیه اول یک اتفاق غیرمنتظره"))
        assertTrue(report.contains("سناریوی بازسازی‌شده"))
        assertTrue(report.contains("شروع عادی"))
        assertTrue(report.contains("دیالوگ و متن"))
        assertTrue(report.contains("چرا Share / Save / Comment گرفته"))
        assertTrue(report.contains("عدم قطعیت‌ها"))
        assertFalse(report.contains("خلاصه قابل نمایش در این نسخه پیدا نشد"))
    }

    @Test fun rendersPublicResilientContract() {
        val root = JSONObject(
            """{
              "summary":"داستان کوتاه معمایی",
              "content_style":"story",
              "hook":"یک سوال تصویری در همان فریم اول",
              "hook_seconds":{"0-1":"نمای عجیب","1-2":"سوال باز"},
              "promise":"پاسخ در پایان",
              "story_structure":{"setup":"سوال","escalation":"نشانه‌ها","payoff":"جواب"},
              "timeline":[{"start":0,"end":1.5,"observed_action":"نمای نزدیک"}],
              "dialogue":"فکر می‌کنی چی شده؟",
              "on_screen_text":"تا آخر ببین",
              "likely_virality_explanations":["کنجکاوی بالا و پرداخت روشن"],
              "estimated_private_metrics":{"classification":"ESTIMATED","confidence":"low"},
              "public_metrics":{"views":2100000},
              "learning_candidate":{"status":"candidate_only","candidate_patterns":["open loop"]}
            }"""
        )
        val report = ViralReportFormatter.full(root)
        assertTrue(report.contains("داستان و ساختار روایی"))
        assertTrue(report.contains("چرا احتمالاً وایرال شده"))
        assertTrue(report.contains("برآورد شاخص‌های خصوصی ـ فقط فرضیه"))
        assertTrue(report.contains("آمار عمومی مشاهده‌شده"))
        assertTrue(report.contains("آنچه وارد حافظه یادگیری می‌شود"))
        assertTrue(ViralReportFormatter.quick(root).contains("هوک:"))
    }

    @Test fun walksNestedAndStringifiedLegacyReports() {
        val nested = JSONObject()
            .put("result", JSONObject().put("report", JSONObject().put(
                "analysis",
                "{\"hook\":\"قلاب داخل JSON متنی\",\"scenario\":{\"summary\":\"داستان داخل wrapper\"},\"cta\":\"ارسال برای دوست\"}"
            )))
        val report = ViralReportFormatter.full(nested)
        assertTrue(report.contains("قلاب داخل JSON متنی"))
        assertTrue(report.contains("داستان داخل wrapper"))
        assertTrue(report.contains("ارسال برای دوست"))
    }

    @Test fun neverHidesUnknownButUsefulSavedEvidence() {
        val root = JSONObject()
            .put("custom_semantic_block", JSONObject().put("what_model_saw", "یک خودرو وارد قاب شد"))
            .put("job_id", "technical")
        val report = ViralReportFormatter.full(root)
        assertTrue(report.contains("جزئیات تحلیل ذخیره‌شده"))
        assertTrue(report.contains("یک خودرو وارد قاب شد"))
        assertFalse(report.contains("technical"))
    }
}
