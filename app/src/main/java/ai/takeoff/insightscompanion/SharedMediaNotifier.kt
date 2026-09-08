package ai.takeoff.insightscompanion

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object SharedMediaNotifier {
    private const val CHANNEL_ID = "takeoff_reels_learning_channel"
    private const val CHANNEL_NAME = "یادگیری و مخزن ریلز تیک‌آف"
    private const val CHANNEL_DESC = "اعلان دریافت ریلز جدید از دایرکت و پیشرفت پردازش در مخزن سوپابیس"

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 80, 100, 180)
            }
            nm.createNotificationChannel(channel)
        }
    }

    fun notifyReelReceived(context: Context, shortcode: String, url: String) {
        val appContext = context.applicationContext
        ensureChannel(appContext)
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val intent = Intent(appContext, ViralShareActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("opened_from_notification", true)
            putExtra("target_shortcode", shortcode)
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            shortcode.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val cleanCode = shortcode.ifBlank { "Instagram" }
        val bigText = "ریلز جدید ($cleanCode) با موفقیت دریافت شد.\nموتور هوش مصنوعی چندوجهی در حال استخراج هوک، صحنه‌ها، گفتار و ثبت الگوها در مخزن سوپابیس است. برای مشاهده روند زنده کلیک کنید."

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("ریلز جدید دریافت شد! 🚀")
            .setContentText("در حال آنالیز چندوجهی، یادگیری عمیق و ثبت الگوها در مخزن سوپابیس...")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = ("recv_" + shortcode).hashCode()
        nm.notify(notificationId, notification)
    }

    fun notifyReelCompleted(context: Context, shortcode: String, promotionStatus: String) {
        val appContext = context.applicationContext
        ensureChannel(appContext)
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val intent = Intent(appContext, ViralShareActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("opened_from_notification", true)
            putExtra("target_shortcode", shortcode)
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            shortcode.hashCode() xor 0x1234,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val cleanCode = shortcode.ifBlank { "Instagram" }
        val promoFa = if (promotionStatus.equals("PROMOTE", true)) "تایید الگوهای ۹۹٪ ویروسی" else "ثبت در آرشیو یادگیری"

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("تحلیل و یادگیری کامل شد! ✅")
            .setContentText("الگوهای ریلز $cleanCode در مخزن سوپابیس ثبت گردید ($promoFa).")
            .setStyle(NotificationCompat.BigTextStyle().bigText("تحلیل کامل ریلز $cleanCode پایان یافت.\nهوک‌ها، دیالوگ‌ها، دستور زبان بصری و فرضیات ماندگاری مخاطب با موفقیت در پایگاه داده ذخیره شدند."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = ("comp_" + shortcode).hashCode()
        nm.notify(notificationId, notification)
    }
}
