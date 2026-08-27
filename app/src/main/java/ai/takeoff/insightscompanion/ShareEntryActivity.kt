package ai.takeoff.insightscompanion

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * External ACTION_SEND trampoline.
 *
 * Public Instagram Reel analysis is deliberately isolated from private Owner
 * Insights authentication. A share creates exactly one visible TakeOff analyzer
 * and never mutates the private companion credential store.
 */
class ShareEntryActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        forwardIntoAnalyzer(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        forwardIntoAnalyzer(intent)
    }

    private fun forwardIntoAnalyzer(source: Intent) {
        val sharedText = source.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        val analyzer = Intent(this, ViralShareActivity::class.java)
            .putExtra(Intent.EXTRA_TEXT, sharedText)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        startActivity(analyzer)
        finishAndRemoveTask()
    }
}
