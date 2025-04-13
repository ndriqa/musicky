package com.ndriqa.musicky.core.data

import android.net.Uri

data class Album(
    val name: String,
    val songs: List<Song>
) {
    val size: Int
        get() = songs.size

    val artworkUri: Uri?
        get() = songs.firstOrNull()?.artworkUri
}
