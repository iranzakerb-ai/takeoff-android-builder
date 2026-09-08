package ai.takeoff.insightscompanion

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ViralJobClientContractTest {
    @Test fun productionViralEndpointIsVercel() {
        assertEquals("https://takeoff-seven-puce.vercel.app", PayloadClient.PRODUCTION_ENDPOINT)
        assertEquals(PayloadClient.PRODUCTION_ENDPOINT, PayloadClient.viralEndpoint("https://takeoff-virality-engine.onrender.com"))
        assertEquals(PayloadClient.PRODUCTION_ENDPOINT, ViralStreamClient.PRODUCTION_ENDPOINT)
    }

    @Test fun pollUsesPostPathAndJsonToken() {
        assertEquals("/v2/viral-jobs/job_ABC-123/poll", ViralJobClient.pollPath("job_ABC-123"))
        assertEquals("/v2/viral-jobs/job%2Fwith%2Fslash/poll", ViralJobClient.pollPath("job/with/slash"))
        assertEquals("secret-token", JSONObject(ViralJobClient.pollPayload("secret-token")).getString("token"))
    }

    @Test fun backendProgressIsNormalizedForCurrentUi() {
        val raw = JSONObject()
            .put("job_id", "job-1")
            .put("status", "processing")
            .put("stage", "learning_persist")
            .put("stage_progress_percent", 25)
            .put("overall_progress_percent", 92)
            .put("media_downloaded_bytes", 1234L)
            .put("media_total_bytes", 4321L)
        val out = ViralJobClient.normalizeForUi(raw)
        assertEquals("progress", out.getString("type"))
        assertEquals("ثبت در حافظه یادگیری", out.getString("stage_label"))
        assertEquals(1234L, out.getLong("downloaded_bytes"))
        assertEquals(4321L, out.getLong("total_bytes"))
    }

    @Test fun failedJobBecomesPersianUiErrorWithoutInventingMetrics() {
        val out = ViralJobClient.normalizeForUi(JSONObject()
            .put("job_id", "job-2")
            .put("status", "failed")
            .put("stage", "failed")
            .put("error_code", "learning_store_failed"))
        assertEquals("error", out.getString("type"))
        assertEquals("learning_store_failed", out.getString("technical_error_code"))
        assertEquals("ثبت نتیجه در حافظه یادگیری ناموفق بود.", out.getString("error_code"))
        assertTrue(!out.has("downloaded_bytes"))
    }

    @Test fun knownFailuresHaveSafePersianMessages() {
        assertEquals("فایل اصلی این ریلز در حال حاضر قابل دریافت نیست.", ViralJobClient.errorMessageFa("media_url_unavailable"))
        assertEquals("سرویس Gemini موقتاً شلوغ است؛ دوباره تلاش می‌شود.", ViralJobClient.errorMessageFa("gemini_http_429"))
        assertEquals("نشست تحلیل منقضی یا نامعتبر شده است.", ViralJobClient.errorMessageFa("invalid_or_expired_poll_token"))
        assertEquals("زمان تحلیل بیش از حد مجاز شد؛ دوباره تلاش کنید.", ViralJobClient.errorMessageFa("analysis_timeout"))
    }
}
