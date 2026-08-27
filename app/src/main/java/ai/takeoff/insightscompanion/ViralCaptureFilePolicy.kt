package ai.takeoff.insightscompanion

import java.io.File

object ViralCaptureFilePolicy {
    fun deleteTemporary(file: File?): Boolean {
        if (file == null || !file.exists()) return true
        return runCatching { file.delete() }.getOrDefault(false)
    }
}
