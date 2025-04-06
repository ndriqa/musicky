package com.ndriqa.musicky.core.data

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String?,
    val duration: Long, // in ms
    val dateAdded: String,
    val dateModified: String,
    val data: String,   // file path or content uri
    val artworkUri: Uri? = null
)
