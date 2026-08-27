package ai.takeoff.insightscompanion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ViralShareErrorPolicyTest {
    @Test
    fun arbitraryRemoteDetailIsNeverSurfaced() {
        val bodies = listOf(
            """{"detail":"Bearer SECRET https://example.com/?token=abc cookie=session123","companion_key":"TOPSECRET"}""",
            """{"detail":"SECRET abc123 session token privatevalue"}""",
            """{"detail":"database password hunter2"}""",
        )
        for (body in bodies) {
            val shown = ViralShareActivity.safeServerErrorBody(body)
            assertEquals("جزئیات امنی برای نمایش نیست.", shown)
            assertFalse(shown.contains("SECRET"))
            assertFalse(shown.contains("token"))
            assertFalse(shown.contains("password"))
            assertFalse(shown.contains("privatevalue"))
        }
    }

    @Test
    fun onlyBoundedMachineErrorCodeMayBeShown() {
        assertEquals("کد خطا: apify_timeout", ViralShareActivity.safeServerErrorBody("""{"error_code":"apify_timeout","detail":"secret"}"""))
        assertEquals("جزئیات امنی برای نمایش نیست.", ViralShareActivity.safeServerErrorBody("""{"error_code":"bad code: token=secret"}"""))
        assertEquals("جزئیات امنی برای نمایش نیست.", ViralShareActivity.safeServerErrorBody("not-json SECRET"))
    }
}
