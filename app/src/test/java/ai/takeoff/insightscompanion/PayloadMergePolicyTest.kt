package ai.takeoff.insightscompanion

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PayloadMergePolicyTest {
    @Test
    fun latestReviewedNoScenarioClearsStaleScenarioAttribution() {
        val target = JSONObject()
            .put("observed_at", 100.0)
            .put("scenario_id", "s1")
            .put("operator_reviewed", true)
            .put("execution_fidelity", "complete")
            .put("metrics", JSONObject().put("views", 100))

        val incoming = JSONObject()
            .put("observed_at", 120.0)
            .put("operator_reviewed", true)
            .put("execution_fidelity", "unknown")
            .put("metrics", JSONObject().put("shares", 7))

        PayloadMergePolicy.merge(target, incoming)

        assertFalse(target.has("scenario_id"))
        assertTrue(target.getBoolean("operator_reviewed"))
        assertEquals("unknown", target.getString("execution_fidelity"))
        assertEquals(100, target.getJSONObject("metrics").getInt("views"))
        assertEquals(7, target.getJSONObject("metrics").getInt("shares"))
        assertEquals(120.0, target.getDouble("observed_at"), 0.0)
    }

    @Test
    fun latestReviewedScenarioAndFidelityReplaceOlderChoice() {
        val target = JSONObject()
            .put("observed_at", 100.0)
            .put("operator_reviewed", true)
            .put("execution_fidelity", "unknown")
            .put("metrics", JSONObject().put("views", 100))

        val incoming = JSONObject()
            .put("observed_at", 130.0)
            .put("scenario_id", "s1")
            .put("operator_reviewed", true)
            .put("execution_fidelity", "minor_changes")
            .put("metrics", JSONObject().put("saves", 5))

        PayloadMergePolicy.merge(target, incoming)

        assertEquals("s1", target.getString("scenario_id"))
        assertTrue(target.getBoolean("operator_reviewed"))
        assertEquals("minor_changes", target.getString("execution_fidelity"))
        assertEquals(5, target.getJSONObject("metrics").getInt("saves"))
    }

    @Test
    fun delayedOlderCaptureCannotRollBackNewerReviewOrMetric() {
        val target = JSONObject()
            .put("observed_at", 200.0)
            .put("scenario_id", "new-scenario")
            .put("operator_reviewed", true)
            .put("execution_fidelity", "minor_changes")
            .put("metrics", JSONObject().put("views", 250).put("shares", 12))
            .put("ocr", JSONObject().put("raw_text_sha256", "new-hash").put("page_hint", "overview"))

        val delayedOlder = JSONObject()
            .put("observed_at", 190.0)
            .put("scenario_id", "old-scenario")
            .put("operator_reviewed", true)
            .put("execution_fidelity", "complete")
            .put("metrics", JSONObject().put("views", 100).put("saves", 4))
            .put("ocr", JSONObject().put("raw_text_sha256", "old-hash").put("page_hint", "interactions"))

        PayloadMergePolicy.merge(target, delayedOlder)

        assertEquals(200.0, target.getDouble("observed_at"), 0.0)
        assertEquals("new-scenario", target.getString("scenario_id"))
        assertEquals("minor_changes", target.getString("execution_fidelity"))
        assertEquals(250, target.getJSONObject("metrics").getInt("views"))
        assertEquals(12, target.getJSONObject("metrics").getInt("shares"))
        assertEquals(4, target.getJSONObject("metrics").getInt("saves"))
        assertEquals("new-hash", target.getJSONObject("ocr").getString("raw_text_sha256"))
        assertTrue(target.getJSONObject("ocr").getString("page_hint").contains("overview"))
        assertTrue(target.getJSONObject("ocr").getString("page_hint").contains("interactions"))
    }
}
