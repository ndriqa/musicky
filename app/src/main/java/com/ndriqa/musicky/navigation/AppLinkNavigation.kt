package com.ndriqa.musicky.navigation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.ndriqa.musicky.R

fun musickyPlayStore(context: Context) {
    val appUri = "market://details?id=com.ndriqa.musicky".toUri()
    Intent(Intent.ACTION_VIEW, appUri)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .also { context.startActivity(it) }
}


fun ndriqaOtherApps(context: Context) {
    val developerId = context.getString(R.string.ndriqa_developer_id)
    val playStoreUrl = "https://play.google.com/store/apps/dev?id=$developerId"
    val playStoreUri = playStoreUrl.toUri()
    Intent(Intent.ACTION_VIEW, playStoreUri)
        .setPackage("com.android.vending")
        .also {
            try {
                context.startActivity(it)
            } catch (e: ActivityNotFoundException) {
                context.startActivity(Intent(Intent.ACTION_VIEW, playStoreUri))
            }
        }
}

fun ndriqaDonate(context: Context) {
    val donateUrl = "https://ko-fi.com/ndriqa"
    context.openExternalUrl(donateUrl)
}

fun Context.openExternalUrl(url: String) {
    Intent(Intent.ACTION_VIEW, url.toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .also { intent ->
            if (intent.resolveActivity(packageManager) != null) startActivity(intent)
        }
}