package ai.takeoff.insightscompanion

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle

/** External Share trampoline for TakeOff viral learning. */
class ShareEntryActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ingest(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        ingest(intent)
    }

    private fun ingest(source: Intent) {
        val texts = mutableListOf<String>()
        source.getStringExtra(Intent.EXTRA_TEXT)?.let(texts::add)
        source.getCharSequenceArrayListExtra(Intent.EXTRA_TEXT)?.forEach { texts += it.toString() }
        source.clipData?.let { clip ->
            for (i in 0 until clip.itemCount) clip.getItemAt(i).text?.toString()?.let(texts::add)
        }
        val urls = texts.flatMap(SharedMediaQueue::extractUrls).distinct()
        val savedMedia = SharedMediaIntake.mediaUris(source).mapNotNull { SharedMediaIntake.persist(this, it) }
        val account = runCatching { ManagedAccountStore(this).selected()?.normalizedHandle }.getOrNull()
        val prefs = getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE)
        val niche = account?.let { prefs.getString("niche_$it", null) }
            ?.takeIf { !it.isNullOrBlank() }
            ?: prefs.getString("viral_analysis_niche", "عمومی").orEmpty().ifBlank { "عمومی" }
        val queue = SharedMediaQueue(this)
        var added = 0

        if (urls.isNotEmpty()) {
            urls.forEachIndexed { index, url ->
                runCatching { queue.enqueue(url, niche, account) }.getOrNull()?.let { original ->
                    val media = savedMedia.getOrNull(index) ?: if (urls.size == 1) savedMedia.firstOrNull() else null
                    val item = if (media != null) queue.attachLocalMedia(original.localId, media.path, media.mime) ?: original else original
                    SharedMediaWork.enqueue(this, item)
                    added++
                }
            }
        } else if (savedMedia.isNotEmpty()) {
            // A downloaded/original Reel can be shared into TakeOff after a URL-only
            // analysis stopped at media_required. Attach it to the newest such job.
            val target = queue.newestAwaitingMedia()
            if (target != null) {
                val media = savedMedia.first()
                queue.attachLocalMedia(target.localId, media.path, media.mime)?.let {
                    SharedMediaWork.enqueue(this, it)
                    added++
                }
            }
        }

        queue.trimCompleted()
        startActivity(
            Intent(this, ViralShareActivity::class.java)
                .putExtra(ViralShareActivity.EXTRA_ADDED_COUNT, added)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finishAndRemoveTask()
    }
}
