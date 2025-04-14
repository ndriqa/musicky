package com.ndriqa.musicky.core.util.notifications

import android.app.NotificationManager

sealed class NotificationChannelInfo(
    val id: String,
    val name: String,
    val description: String,
    val importance: Int
) {
    object General : NotificationChannelInfo(
        id = "general_channel",
        name = "General Notifications",
        description = "General notifications for this app",
        importance = NotificationManager.IMPORTANCE_HIGH
    )

    object Playing : NotificationChannelInfo(
        id = "playing_channel",
        name = "Playing Notifications",
        description = "Main notification that shows up when the music is playing",
        importance = NotificationManager.IMPORTANCE_HIGH
    )
}
