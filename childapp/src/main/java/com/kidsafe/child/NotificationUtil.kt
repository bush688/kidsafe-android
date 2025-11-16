package com.kidsafe.child

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationUtil {
    private const val CHANNEL_ID = "kidsafe_alerts"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "KidSafe Alerts", NotificationManager.IMPORTANCE_HIGH))
        }
    }

    fun notifyTimeout(context: Context, minutes: Int) {
        ensureChannel(context)
        val n = NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("使用超时").setContentText("已达到每日${minutes}分钟限制").setPriority(NotificationCompat.PRIORITY_HIGH).build()
        NotificationManagerCompat.from(context).notify(1001, n)
    }

    fun notifyWindowBlocked(context: Context) {
        ensureChannel(context)
        val n = NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle("当前时段不可用").setContentText("已超出允许使用时间段").setPriority(NotificationCompat.PRIORITY_HIGH).build()
        NotificationManagerCompat.from(context).notify(1002, n)
    }
}