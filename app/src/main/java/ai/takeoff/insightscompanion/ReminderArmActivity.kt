package ai.takeoff.insightscompanion

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class ReminderArmActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val account = intent.getStringExtra(EXTRA_ACCOUNT).orEmpty()
        val url = intent.getStringExtra(EXTRA_URL).orEmpty().trim()
        val selected = ManagedAccountStore(this).select(account)
        if (selected != null && url.isNotBlank()) {
            startActivity(Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, url)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            })
        }
        finish()
    }
    companion object { const val EXTRA_ACCOUNT = "reminder_account"; const val EXTRA_URL = "reminder_url" }
}
