package ai.takeoff.insightscompanion

import org.junit.Assert.assertEquals
import org.junit.Test

class ManagedAccountStoreTest {
    @Test fun normalizesInstagramHandlesForStableAttribution() {
        assertEquals("takeoff.content", ManagedAccount.normalizeHandle(" @TakeOff.Content "))
        assertEquals("client_page.2", ManagedAccount.normalizeHandle("client_page.2"))
    }
}
