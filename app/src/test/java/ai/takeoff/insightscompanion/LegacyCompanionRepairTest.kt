package ai.takeoff.insightscompanion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyCompanionRepairTest {
    @Test
    fun only401TriggersLegacyRepair() {
        assertTrue(LegacyCompanionRepair.shouldRepair(401))
        assertFalse(LegacyCompanionRepair.shouldRepair(400))
        assertFalse(LegacyCompanionRepair.shouldRepair(403))
        assertFalse(LegacyCompanionRepair.shouldRepair(500))
        assertFalse(LegacyCompanionRepair.shouldRepair(200))
    }

    @Test
    fun issuedTokenExtractionSupportsLegacyAndCurrentShapes() {
        assertEquals("device-token", LegacyCompanionRepair.extractIssuedToken("""{"device_token":"device-token"}"""))
        assertEquals("legacy-token", LegacyCompanionRepair.extractIssuedToken("""{"companion_key":"legacy-token"}"""))
        assertEquals("device-token", LegacyCompanionRepair.extractIssuedToken("""{"device_token":"device-token","companion_key":"legacy-token"}"""))
        assertEquals("", LegacyCompanionRepair.extractIssuedToken("not-json"))
        assertEquals("", LegacyCompanionRepair.extractIssuedToken("{}"))
    }
}
