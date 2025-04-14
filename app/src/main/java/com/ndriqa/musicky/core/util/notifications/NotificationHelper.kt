package com.ndriqa.musicky.core.util.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import com.ndriqa.musicky.MainActivity
import com.ndriqa.musicky.R
import com.ndriqa.musicky.ui.theme.AppColor

object NotificationHelper {

    private val notificationChannels = listOf(
        NotificationChannelInfo.General,
        NotificationChannelInfo.Playing
    )

    fun createNotificationChannels(context: Context) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationChannels.forEach { channelInfo ->
            val channel = NotificationChannel(
                /* id = */ channelInfo.id,
                /* name = */channelInfo.name,
                /* importance = */channelInfo.importance
            ).apply {
                description =
                    channelInfo.description
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendNotification(
        context: Context,
        channelId: String,
        title: String,
        message: String,
        notificationId: Int = defaultNotificationId()
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(context, MainActivity::class.java)
            .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }

        val pendingIntent = PendingIntent.getActivity(
            /* context = */ context,
            /* requestCode = */ 0,
            /* intent = */ intent,
            /* flags = */ PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setColorized(true)
            .setColor(AppColor.toArgb())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun defaultNotificationId(): Int = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
}
