package ai.takeoff.insightscompanion

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/** Copies transient shared media URIs into app-private storage so WorkManager can
 * consume them after the originating share activity loses URI permission. */
object SharedMediaIntake {
    private const val MAX_BYTES = 220L * 1024L * 1024L

    data class Saved(val path: String, val mime: String?)

    fun mediaUris(intent: Intent): List<Uri> {
        val out = linkedSetOf<Uri>()
        @Suppress("DEPRECATION")
        intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let(out::add)
        @Suppress("DEPRECATION")
        intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.forEach(out::add)
        intent.clipData?.let { clip ->
            for (i in 0 until clip.itemCount) clip.getItemAt(i).uri?.let(out::add)
        }
        return out.toList()
    }

    fun persist(context: Context, uri: Uri): Saved? {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri)
        if (mime != null && !mime.startsWith("video/")) return null
        val dir = File(context.filesDir, "viral-input").apply { mkdirs() }
        val target = File(dir, "shared-${System.currentTimeMillis()}-${uri.toString().hashCode().toUInt()}.mp4")
        return try {
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(1024 * 1024)
                    var total = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n <= 0) break
                        total += n
                        if (total > MAX_BYTES) throw IllegalArgumentException("shared_media_too_large")
                        output.write(buffer, 0, n)
                    }
                    if (total <= 0L) throw IllegalArgumentException("shared_media_empty")
                }
            } ?: return null
            Saved(target.absolutePath, mime ?: "video/mp4")
        } catch (_: Exception) {
            target.delete()
            null
        }
    }
}
