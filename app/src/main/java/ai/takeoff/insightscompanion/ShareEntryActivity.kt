package ai.takeoff.insightscompanion

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle

/**
 * External Share trampoline for TakeOff V4 learning.
 *
 * Every Instagram Reel/post URL becomes an independent encrypted queue item.
 * Starting a new share never cancels or replaces an older analysis. The phone is
 * only a dispatcher/status console; public media retrieval and deep learning run
 * on the server.
 */
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
            for (i in 0 until clip.itemCount) {
                clip.getItemAt(i).text?.toString()?.let(texts::add)
            }
        }
        val urls = texts.flatMap(SharedMediaQueue::extractUrls).distinct()
        val account = runCatching { ManagedAccountStore(this).selected()?.normalizedHandle }.getOrNull()
        val prefs = getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE)
        val niche = account?.let { prefs.getString("niche_$it", null) }
            ?.takeIf { !it.isNullOrBlank() }
            ?: prefs.getString("viral_analysis_niche", "عمومی").orEmpty().ifBlank { "عمومی" }
        val queue = SharedMediaQueue(this)
        var added = 0
        urls.forEach { url ->
            runCatching { queue.enqueue(url, niche, account) }.getOrNull()?.let { item ->
                SharedMediaWork.enqueue(this, item)
                added++
            }
        }
        queue.trimCompleted()
        startActivity(
            Intent(this, ViralShareActivity::class.java)
                .putExtra(ViralShareActivity.EXTRA_ADDED_COUNT, added)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finishAndRemoveTask()
    }
}
