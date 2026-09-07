package ai.takeoff.insightscompanion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingReminderPolicyTest {
    @Test
    fun candidateKeepsTheOwningAccountAndLatestDueWindow() {
        val item = PendingReminderPolicy.candidate(
            account = "@Client.Page",
            url = "https://www.instagram.com/reel/ABC123/",
            shortcode = "ABC123",
            dueTargetsHours = listOf(1.0, 3.0),
        )!!
        assertEquals("client.page", item.account)
        assertEquals("ABC123", item.shortcode)
        assertEquals("https://www.instagram.com/reel/ABC123/", item.url)
        assertEquals(3.0, item.targetHours, 0.0)
    }

    @Test
    fun invalidOrEmptyDueItemsAreIgnored() {
        assertNull(PendingReminderPolicy.candidate("client", "https://instagram.com/reel/x/", "x", emptyList()))
        assertNull(PendingReminderPolicy.candidate("", "https://instagram.com/reel/x/", "x", listOf(1.0)))
        assertNull(PendingReminderPolicy.candidate("client", "", "x", listOf(1.0)))
    }

    @Test
    fun reminderRejectsNonInstagramOrMismatchedUrls() {
        assertNull(PendingReminderPolicy.candidate("client", "https://example.com/reel/x/", "x", listOf(1.0)))
        assertNull(PendingReminderPolicy.candidate("client", "http://instagram.com/reel/x/", "x", listOf(1.0)))
        assertNull(PendingReminderPolicy.candidate("client", "https://evil.instagram.com/reel/x/", "x", listOf(1.0)))
        assertNull(PendingReminderPolicy.candidate("client", "https://instagram.com/stories/x/", "x", listOf(1.0)))
        assertNull(PendingReminderPolicy.candidate("client", "https://instagram.com/reel/y/", "x", listOf(1.0)))
    }

    @Test
    fun reminderCanonicalizesSupportedInstagramUrls() {
        val item = PendingReminderPolicy.candidate(
            "client",
            "https://instagram.com/p/ABC_123/?utm_source=share",
            "ABC_123",
            listOf(24.0),
        )!!
        assertEquals("https://www.instagram.com/p/ABC_123/", item.url)
    }

    @Test
    fun transientHttpFailuresAreRetriedButConfigurationErrorsAreNot() {
        assertTrue(PendingReminderPolicy.isRetryableHttpStatus(408))
        assertTrue(PendingReminderPolicy.isRetryableHttpStatus(429))
        assertTrue(PendingReminderPolicy.isRetryableHttpStatus(500))
        assertTrue(PendingReminderPolicy.isRetryableHttpStatus(503))
        assertFalse(PendingReminderPolicy.isRetryableHttpStatus(400))
        assertFalse(PendingReminderPolicy.isRetryableHttpStatus(401))
        assertFalse(PendingReminderPolicy.isRetryableHttpStatus(403))
        assertFalse(PendingReminderPolicy.isRetryableHttpStatus(404))
    }

    @Test
    fun cappedRunRetriesOnlyWhenWorkRemains() {
        assertTrue(PendingReminderPolicy.shouldRetryForBacklog(notified = 5, limit = 5, hasUnprocessed = true))
        assertFalse(PendingReminderPolicy.shouldRetryForBacklog(notified = 5, limit = 5, hasUnprocessed = false))
        assertFalse(PendingReminderPolicy.shouldRetryForBacklog(notified = 4, limit = 5, hasUnprocessed = true))
        assertFalse(PendingReminderPolicy.shouldRetryForBacklog(notified = 5, limit = 0, hasUnprocessed = true))
    }

    @Test
    fun notificationIdentitySeparatesDifferentClientAccounts() {
        val first = PendingReminderCandidate("client.one", "https://instagram.com/reel/x/", "x", 24.0)
        val second = PendingReminderCandidate("client.two", "https://instagram.com/reel/x/", "x", 24.0)
        assertNotEquals(PendingReminderPolicy.notificationId(first), PendingReminderPolicy.notificationId(second))
        assertNotEquals(PendingReminderPolicy.requestCode(first), PendingReminderPolicy.requestCode(second))
        assertNotEquals(PendingReminderPolicy.armIntentIdentityUri(first), PendingReminderPolicy.armIntentIdentityUri(second))
        assertTrue(PendingReminderPolicy.notificationId(first) >= 2402)
    }

    @Test
    fun notificationIdentityDoesNotCollapseOldHundredThousandBucketCollision() {
        val first = PendingReminderCandidate("client", "https://instagram.com/reel/r20/", "r20", 24.0)
        val second = PendingReminderCandidate("client", "https://instagram.com/reel/r1296/", "r1296", 24.0)
        assertEquals(37_665, (PendingReminderPolicy.dedupeKey(first).hashCode() and 0x7fffffff) % 100_000)
        assertEquals(37_665, (PendingReminderPolicy.dedupeKey(second).hashCode() and 0x7fffffff) % 100_000)
        assertNotEquals(PendingReminderPolicy.notificationId(first), PendingReminderPolicy.notificationId(second))
    }

    @Test
    fun armIntentIdentitySurvivesRequestCodeHashCollision() {
        val first = PendingReminderCandidate("client", "https://instagram.com/reel/an/", "an", 24.0)
        val second = PendingReminderCandidate("client", "https://instagram.com/reel/c0/", "c0", 24.0)
        assertEquals(PendingReminderPolicy.requestCode(first), PendingReminderPolicy.requestCode(second))
        assertNotEquals(PendingReminderPolicy.armIntentIdentityUri(first), PendingReminderPolicy.armIntentIdentityUri(second))
    }

    @Test
    fun dedupeIdentityIsStableForSameCaptureWindow() {
        val first = PendingReminderCandidate("client.one", "https://instagram.com/reel/x/", "x", 24.0)
        val sameWindowDifferentUrlFormatting = PendingReminderCandidate("client.one", "https://www.instagram.com/reel/x/", "x", 24.0)
        assertEquals(PendingReminderPolicy.dedupeKey(first), PendingReminderPolicy.dedupeKey(sameWindowDifferentUrlFormatting))
    }

    @Test
    fun dedupeIdentityAllowsLaterWindowsToNotify() {
        val dayOne = PendingReminderCandidate("client.one", "https://instagram.com/reel/x/", "x", 24.0)
        val dayTwo = PendingReminderCandidate("client.one", "https://instagram.com/reel/x/", "x", 48.0)
        assertNotEquals(PendingReminderPolicy.dedupeKey(dayOne), PendingReminderPolicy.dedupeKey(dayTwo))
        assertNotEquals(PendingReminderPolicy.notificationId(dayOne), PendingReminderPolicy.notificationId(dayTwo))
    }
}
