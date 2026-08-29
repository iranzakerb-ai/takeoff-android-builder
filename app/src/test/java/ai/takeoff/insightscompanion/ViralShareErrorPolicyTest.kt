package ai.takeoff.insightscompanion

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ViralShareErrorPolicyTest {
    @Test
    fun unknownTechnicalErrorNeverEchoesRemoteSecretText() {
        val shown = ViralJobClient.errorMessageFa("Bearer SECRET token=abc password=hunter2")
        assertEquals("خطای فنی در پردازش رخ داد.", shown)
        assertFalse(shown.contains("SECRET"))
        assertFalse(shown.contains("token"))
        assertFalse(shown.contains("password"))
        assertFalse(shown.contains("hunter2"))
    }

    @Test
    fun failedServerCodeIsMappedToPersianAndTechnicalCodeIsPreservedSeparately() {
        val out = ViralJobClient.normalizeForUi(
            JSONObject()
                .put("status", "failed")
                .put("stage", "failed")
                .put("error_code", "media_download_failed")
        )
        assertEquals("error", out.getString("type"))
        assertEquals("media_download_failed", out.getString("technical_error_code"))
        assertEquals("دریافت ویدیو از منبع ناموفق بود.", out.getString("error_code"))
    }
}
