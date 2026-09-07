package ai.takeoff.insightscompanion

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalInsightStoreMetricPolicyTest {
    @Test
    fun finiteMetricsRejectsNaNInfinityAndNonNumericValues() {
        val input = JSONObject()
            .put("views", 1234)
            .put("share_rate", "0.125")
            .put("nan", "NaN")
            .put("positive_inf", "Infinity")
            .put("negative_inf", "-Infinity")
            .put("garbage", "abc")
            .put("nested", JSONObject().put("x", 1))
            .put("missing", JSONObject.NULL)

        val out = LocalInsightStore.finiteMetrics(input)

        assertEquals(1234.0, out["views"]!!, 0.0)
        assertEquals(0.125, out["share_rate"]!!, 0.0)
        assertEquals(setOf("views", "share_rate"), out.keys)
        assertTrue(out.values.all { it.isFinite() })
        assertFalse(out.containsKey("nan"))
        assertFalse(out.containsKey("positive_inf"))
        assertFalse(out.containsKey("negative_inf"))
    }
}
