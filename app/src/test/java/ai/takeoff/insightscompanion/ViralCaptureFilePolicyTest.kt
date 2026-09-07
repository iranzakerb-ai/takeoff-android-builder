package ai.takeoff.insightscompanion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class ViralCaptureFilePolicyTest {
    @Test
    fun deletesTemporaryCaptureBytes() {
        val dir = Files.createTempDirectory("takeoff-viral-capture-test").toFile()
        val file = java.io.File(dir, "capture.mp4")
        file.writeBytes(ByteArray(1024) { 7 })
        assertTrue(file.exists())

        assertTrue(ViralCaptureFilePolicy.deleteTemporary(file))
        assertFalse(file.exists())
        assertTrue(ViralCaptureFilePolicy.deleteTemporary(file))
        assertTrue(ViralCaptureFilePolicy.deleteTemporary(null))

        dir.delete()
    }
}
