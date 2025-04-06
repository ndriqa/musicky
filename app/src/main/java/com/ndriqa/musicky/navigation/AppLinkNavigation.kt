package com.ndriqa.musicky.navigation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.ndriqa.musicky.R

fun ndriqaOtherApps(context: Context) {
    val developerId = context.getString(R.string.ndriqa_developer_id)
    val playStoreUri = "https://play.google.com/store/apps/dev?id=$developerId"
    Intent(Intent.ACTION_VIEW, playStoreUri.toUri())
        .apply { `package` = "com.android.vending" }
        .also {
            try {
                context.startActivity(it)
            } catch (e: ActivityNotFoundException) {
                context.startActivity(Intent(Intent.ACTION_VIEW, playStoreUri.toUri()))
            }
        }
}

fun ndriqaDonate(context: Context) {
    val donateUrl = "https://ko-fi.com/ndriqa"
    context.openExternalUrl(donateUrl)
}

fun Context.openExternalUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}