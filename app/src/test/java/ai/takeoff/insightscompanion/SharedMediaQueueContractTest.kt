package ai.takeoff.insightscompanion

import org.json.JSONArray
import org.json.JSONObject
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

    @Test fun trimmingCompletedQueueKeepsNewestCompletedItemsAndAllActiveItems() {
        val source = JSONArray()
            .put(JSONObject().put("local_id", "old-1").put("status", "completed"))
            .put(JSONObject().put("local_id", "active").put("status", "processing"))
            .put(JSONObject().put("local_id", "old-2").put("status", "completed"))
            .put(JSONObject().put("local_id", "new-1").put("status", "completed"))
            .put(JSONObject().put("local_id", "new-2").put("status", "completed"))

        val trimmed = SharedMediaQueue.trimCompletedItems(source, 2)
        val ids = (0 until trimmed.length()).map { trimmed.getJSONObject(it).getString("local_id") }

        assertEquals(listOf("active", "new-1", "new-2"), ids)
    }
}
