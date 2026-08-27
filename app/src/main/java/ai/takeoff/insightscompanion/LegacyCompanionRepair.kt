package ai.takeoff.insightscompanion

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import java.util.UUID

/**
 * Compatibility bridge for already-deployed legacy servers that still require a
 * Companion credential on the public Instagram Share route.
 *
 * The normal v0.13.5 flow remains keyless. This class is invoked only after the
 * keyless request receives HTTP 401. It first tries an existing encrypted device
 * token, clears it if rejected, then offers one-time operator pairing and retries
 * the exact Reel automatically. No reusable bootstrap/master secret is embedded.
 */
class LegacyCompanionRepair(private val activity: Activity) {
    companion object {
        internal fun shouldRepair(statusCode: Int): Boolean = statusCode == 401

        internal fun extractIssuedToken(body: String): String {
            val root = runCatching { JSONObject(body) }.getOrNull() ?: return ""
            return root.optString("device_token").trim()
                .ifBlank { root.optString("companion_key").trim() }
        }
    }

    private val store = SecretStore(activity)

    fun recover(
        endpoint: String,
        reelUrl: String,
        niche: String,
        onFinished: (Pair<Int, String>) -> Unit,
        onFailure: (String) -> Unit,
    ) {
        val existing = store.get("api_key").orEmpty().trim()
        if (existing.isNotBlank()) {
            Thread {
                val attempt = runCatching {
                    PayloadClient.postViralEvidence(endpoint, existing, reelUrl, niche)
                }.getOrNull()
                activity.runOnUiThread {
                    when {
                        attempt == null -> promptPair(endpoint, reelUrl, niche, onFinished, onFailure)
                        attempt.first == 401 -> {
                            store.remove("api_key")
                            promptPair(endpoint, reelUrl, niche, onFinished, onFailure)
                        }
                        else -> onFinished(attempt)
                    }
                }
            }.start()
            return
        }
        promptPair(endpoint, reelUrl, niche, onFinished, onFailure)
    }

    private fun promptPair(
        endpoint: String,
        reelUrl: String,
        niche: String,
        onFinished: (Pair<Int, String>) -> Unit,
        onFailure: (String) -> Unit,
    ) {
        if (activity.isFinishing) {
            onFailure("پنجره اتصال باز نشد؛ دوباره تلاش کن.")
            return
        }
        val box = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(18), dp(4), dp(18), 0)
        }
        val input = EditText(activity).apply {
            hint = "کد اتصال امن"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            isSingleLine = true
        }
        val message = TextView(activity).apply {
            text = "این مرحله فقط برای سرور قدیمی است و بعد از ثبت توکن امن تکرار نمی‌شود."
            textSize = 12.5f
            setPadding(0, dp(10), 0, 0)
        }
        box.addView(input)
        box.addView(message)

        val dialog = AlertDialog.Builder(activity)
            .setTitle("بازیابی اتصال تیک‌آف")
            .setMessage("سرور فعلی هنوز احراز هویت قدیمی می‌خواهد. کد اتصال را یک‌بار وارد کن؛ همان ریلز خودکار دوباره ارسال می‌شود.")
            .setView(box)
            .setCancelable(false)
            .setPositiveButton("اتصال و ادامه", null)
            .setNegativeButton("بستن") { _, _ -> onFailure("اتصال انجام نشد.") }
            .create()

        dialog.setOnShowListener {
            val connect = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            connect.setOnClickListener {
                val code = input.text?.toString().orEmpty().trim()
                if (code.isBlank()) {
                    input.error = "کد اتصال را وارد کن"
                    return@setOnClickListener
                }
                connect.isEnabled = false
                connect.text = "در حال اتصال…"
                message.text = "در حال ساخت توکن امن و تلاش مجدد…"
                Thread {
                    val paired = runCatching {
                        PayloadClient.pairCompanion(endpoint, code, stableDeviceId(), deviceName())
                    }.getOrNull()
                    activity.runOnUiThread {
                        if (paired == null) {
                            connect.isEnabled = true
                            connect.text = "اتصال و ادامه"
                            message.text = "ارتباط با سرور برقرار نشد. اینترنت/VPN را بررسی کن."
                            return@runOnUiThread
                        }
                        if (paired.first !in 200..299) {
                            connect.isEnabled = true
                            connect.text = "اتصال و ادامه"
                            message.text = if (paired.first == 401) "کد اتصال پذیرفته نشد." else "اتصال امن کامل نشد (HTTP ${paired.first})."
                            return@runOnUiThread
                        }
                        val token = extractIssuedToken(paired.second)
                        if (token.isBlank()) {
                            connect.isEnabled = true
                            connect.text = "اتصال و ادامه"
                            message.text = "پاسخ اتصال معتبر نبود."
                            return@runOnUiThread
                        }
                        store.put("api_key", token)
                        dialog.dismiss()
                        retryWithToken(endpoint, token, reelUrl, niche, onFinished, onFailure)
                    }
                }.start()
            }
        }
        dialog.show()
    }

    private fun retryWithToken(
        endpoint: String,
        token: String,
        reelUrl: String,
        niche: String,
        onFinished: (Pair<Int, String>) -> Unit,
        onFailure: (String) -> Unit,
    ) {
        Thread {
            val result = runCatching {
                PayloadClient.postViralEvidence(endpoint, token, reelUrl, niche)
            }.getOrNull()
            activity.runOnUiThread {
                if (result == null) {
                    onFailure("اتصال ساخته شد اما تلاش مجدد به سرور نرسید.")
                    return@runOnUiThread
                }
                if (result.first == 401) {
                    store.remove("api_key")
                    onFailure("سرور توکن تازه را نپذیرفت؛ کد اتصال را دوباره بررسی کن.")
                    return@runOnUiThread
                }
                onFinished(result)
            }
        }.start()
    }

    private fun stableDeviceId(): String {
        val prefs = activity.getSharedPreferences("takeoff_companion_plain", Context.MODE_PRIVATE)
        val current = prefs.getString("device_id", "").orEmpty().trim()
        if (current.isNotBlank()) return current
        val created = UUID.randomUUID().toString()
        prefs.edit().putString("device_id", created).commit()
        return created
    }

    private fun deviceName(): String =
        "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(100).ifBlank { "TakeOff Android" }

    private fun dp(value: Int): Int =
        (value * activity.resources.displayMetrics.density).toInt()
}
