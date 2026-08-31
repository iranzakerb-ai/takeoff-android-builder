package ai.takeoff.insightscompanion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedMediaQueueContractTest {
    @Test fun extractsMultipleInstagramMediaUrlsFromOneShareText() {
        val urls = SharedMediaQueue.extractUrls(
            "one https://www.instagram.com/reel/ABC_123/?utm_source=x two https://instagram.com/p/XYZ-9/"
        )
        assertEquals(2, urls.size)
        assertEquals("https://www.instagram.com/reel/ABC_123/", urls[0])
        assertEquals("https://www.instagram.com/p/XYZ-9/", urls[1])
    }

    @Test fun rejectsNonInstagramUrls() {
        assertTrue(SharedMediaQueue.extractUrls("https://example.com/p/ABC/").isEmpty())
    }
}
