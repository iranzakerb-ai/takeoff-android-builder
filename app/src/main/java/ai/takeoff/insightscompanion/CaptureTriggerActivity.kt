package ai.takeoff.insightscompanion

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper

class CaptureTriggerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        moveTaskToBack(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) finishAndRemoveTask() else finish()
        overridePendingTransition(0, 0)
        Handler(Looper.getMainLooper()).postDelayed({
            applicationContext.startService(Intent(applicationContext, CaptureService::class.java).setAction(CaptureService.ACTION_CAPTURE))
        }, 900L)
    }
}
