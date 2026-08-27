package ai.takeoff.insightscompanion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL

class PayloadClientTest {
    @Test fun acceptsHttpsEndpoints() {
        PayloadClient.validateEndpoint("https://takeoff.example.com")
        PayloadClient.validateEndpoint("https://takeoff.example.com/")
    }

    @Test fun acceptsLiteralPrivateLanHttpEndpoints() {
        PayloadClient.validateEndpoint("http://10.0.0.5:8000")
        PayloadClient.validateEndpoint("http://192.168.1.20")
        PayloadClient.validateEndpoint("http://172.16.0.2")
        PayloadClient.validateEndpoint("http://172.31.255.254")
        PayloadClient.validateEndpoint("http://127.0.0.1:8000")
        PayloadClient.validateEndpoint("http://localhost:8000")
    }

    @Test fun rejectsLookalikeLanHostnamesAndPublicHttp() {
        assertThrows(IllegalArgumentException::class.java) { PayloadClient.validateEndpoint("http://10.evil.example") }
        assertThrows(IllegalArgumentException::class.java) { PayloadClient.validateEndpoint("http://192.168.evil.example") }
        assertThrows(IllegalArgumentException::class.java) { PayloadClient.validateEndpoint("http://8.8.8.8") }
        assertThrows(IllegalArgumentException::class.java) { PayloadClient.validateEndpoint("http://example.com") }
    }

    @Test fun rejectsEmbeddedEndpointCredentials() {
        assertThrows(IllegalArgumentException::class.java) {
            PayloadClient.validateEndpoint("https://user:password@takeoff.example.com")
        }
    }

    @Test fun viralPairingMigratesStalePublicEndpointsToProduction() {
        assertEquals(PayloadClient.PRODUCTION_ENDPOINT, PayloadClient.viralEndpoint(""))
        assertEquals(PayloadClient.PRODUCTION_ENDPOINT, PayloadClient.viralEndpoint("https://old-takeoff-service.onrender.com"))
        assertEquals(PayloadClient.PRODUCTION_ENDPOINT, PayloadClient.viralEndpoint("https://example.com/custom"))
        assertEquals(PayloadClient.PRODUCTION_ENDPOINT, PayloadClient.viralEndpoint("not a url"))
    }

    @Test fun viralPairingKeepsExplicitLocalDevelopmentEndpoint() {
        assertEquals("http://192.168.1.20:8000", PayloadClient.viralEndpoint("http://192.168.1.20:8000/"))
        assertEquals("http://localhost:8000", PayloadClient.viralEndpoint("http://localhost:8000"))
    }

    @Test fun companionRequestsNeverFollowRedirectsImplicitly() {
        val conn = URL("https://takeoff.example.com/v2/owner-outcomes/device").openConnection() as HttpURLConnection
        assertTrue(conn.instanceFollowRedirects)
        PayloadClient.disableRedirects(conn)
        assertFalse(conn.instanceFollowRedirects)
        conn.disconnect()
    }

    @Test fun onlyRecordedOwnerOutcomeIsDurableAcknowledgement() {
        val recorded = "{\"status\":\"recorded\",\"acknowledged\":true,\"durability\":\"persistent\",\"receipt_id\":\"cap-1\"}"
        val quarantined = "{\"status\":\"quarantined\",\"acknowledged\":true,\"durability\":\"persistent\",\"receipt_id\":\"cap-1\"}"
        assertTrue(PayloadClient.isDurablyAcknowledgedResponse(recorded, "cap-1"))
        assertFalse(PayloadClient.isDurablyAcknowledgedResponse(quarantined, "cap-1"))
        assertFalse(PayloadClient.isDurablyAcknowledgedResponse(recorded, "different"))
        assertFalse(PayloadClient.isDurablyAcknowledgedResponse("{\"status\":\"recorded\",\"acknowledged\":true,\"durability\":\"local_ephemeral\",\"receipt_id\":\"cap-1\"}", "cap-1"))
        assertFalse(PayloadClient.isDurablyAcknowledgedResponse("{\"status\":\"recorded\"}", "cap-1"))
        assertFalse(PayloadClient.isDurablyAcknowledgedResponse("not-json", "cap-1"))
        assertFalse(PayloadClient.isDurablyAcknowledgedResponse("", "cap-1"))
    }
}
