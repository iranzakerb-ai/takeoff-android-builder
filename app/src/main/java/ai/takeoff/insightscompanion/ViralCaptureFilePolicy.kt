package ai.takeoff.insightscompanion

import java.io.File

/**
 * Privacy policy for temporary Viral Autopsy captures.
 *
 * Device-captured MP4 bytes are transient transport material, not durable app state.
 * They must be removed after upload/error and also if the foreground service is torn
 * down unexpectedly. This helper is deliberately tiny and testable without Android.
 */
object ViralCaptureFilePolicy {
    fun deleteTemporary(file: File?): Boolean {
        if (file == null || !file.exists()) return true
        return runCatching { file.delete() }.getOrDefault(false)
    }
}
