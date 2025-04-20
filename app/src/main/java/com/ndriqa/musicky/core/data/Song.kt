package com.ndriqa.musicky.core.data

import android.net.Uri
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "songs")
@Parcelize
data class Song(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String?,
    val duration: Long, // in ms
    val dateAdded: String,
    val dateModified: String,
    val data: String,   // file path or content uri
    val artworkUri: Uri? = null
): Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Song) return false

        return id == other.id &&
                title == other.title &&
                artist == other.artist &&
                album == other.album &&
                duration == other.duration &&
                dateModified == other.dateModified &&
                data == other.data &&
                artworkUri == other.artworkUri
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + (album?.hashCode() ?: 0)
        result = 31 * result + duration.hashCode()
        result = 31 * result + dateModified.hashCode()
        result = 31 * result + data.hashCode()
        result = 31 * result + (artworkUri?.hashCode() ?: 0)
        return result
    }
}
