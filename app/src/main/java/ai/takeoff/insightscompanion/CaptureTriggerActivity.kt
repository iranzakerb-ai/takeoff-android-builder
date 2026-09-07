package ai.takeoff.insightscompanion

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper

/**
 * Notification trampoline used only to collapse the notification shade.
 *
 * MIUI can keep the notification/trampoline surface visible for a short period after an action is
 * tapped. Starting the capture countdown before this activity is removed races that transition and
 * can make MediaProjection OCR the shade/TakeOff instead of Instagram Reel Insights.
 *
 * First hide and remove this throw-away task, then dispatch ACTION_CAPTURE after a small UI-settle
 * delay. CaptureService has its own additional settle delay, so the actual frame is taken only after
 * Instagram has been restored to the foreground.
 */
class CaptureTriggerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        moveTaskToBack(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) finishAndRemoveTask() else finish()
        overridePendingTransition(0, 0)

        // Do not start CaptureService while this trampoline is still the foreground surface.
        // The application-process handler survives this no-history Activity finishing.
        Handler(Looper.getMainLooper()).postDelayed({
            applicationContext.startService(
                Intent(applicationContext, CaptureService::class.java)
                    .setAction(CaptureService.ACTION_CAPTURE)
            )
        }, 900L)
    }
}
