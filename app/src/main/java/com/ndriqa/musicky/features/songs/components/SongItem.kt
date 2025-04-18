package com.ndriqa.musicky.features.songs.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.ndriqa.musicky.R
import com.ndriqa.musicky.core.data.Song
import com.ndriqa.musicky.core.util.extensions.toFormattedTime
import com.ndriqa.musicky.features.player.components.MusicNoteIcon
import com.ndriqa.musicky.ui.theme.MusicIconArtworkSizeCompact
import com.ndriqa.musicky.ui.theme.PaddingCompact
import com.ndriqa.musicky.ui.theme.PaddingHalf
import com.ndriqa.musicky.ui.theme.PaddingNano

@Composable
internal fun SongItem(
    song: Song,
    isPlaying: Boolean,
    onSongTap: (Song) -> Unit,
    onAddNextInQueue: (Song) -> Unit,
    onDeleteSong: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val iconTint =
        if (isPlaying) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.onPrimaryContainer

    val iconBackground =
        if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.primary

    val songBackground =
        if (isPlaying) MaterialTheme.colorScheme.primary
        else Color.Transparent

    val iconShape = RoundedCornerShape(PaddingCompact)
    val fallbackIcon = rememberVectorPainter(Icons.Rounded.MusicNote)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSongTap(song) }
            .background(color = songBackground)
            .padding(horizontal = PaddingCompact, vertical = PaddingHalf),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var dropDownExpanded by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .size(MusicIconArtworkSizeCompact)
                .clip(shape = iconShape)
                .background(color = iconBackground, shape = iconShape),
            contentAlignment = Alignment.Center
        ) {
            val songArtworkUri = song.artworkUri

            if (songArtworkUri != null) {
                val imageRequest = ImageRequest.Builder(context)
                    .data(song.artworkUri)
                    .crossfade(true)
                    .diskCachePolicy(CachePolicy.ENABLED) // enable disk caching
                    .memoryCachePolicy(CachePolicy.ENABLED) // enable memory caching
                    .build()

                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    fallback = fallbackIcon
                )
            } else {
                MusicNoteIcon(
                    modifier = Modifier.size(20.dp),
                    tint = iconTint
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1F)
                .padding(start = PaddingCompact),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(space = PaddingNano, alignment = Alignment.CenterVertically)
        ) {
            SongTitle(text = song.title,)
            SongDescription(artist = song.artist, timer = song.duration.toFormattedTime())
        }

        Box(
            modifier = Modifier
                .size(MusicIconArtworkSizeCompact)
                .clip(shape = iconShape)
                .clickable(onClick = { dropDownExpanded = true })
        ) {

            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.align(Alignment.Center)
            )

            DropdownMenu(
                expanded = dropDownExpanded,
                onDismissRequest = { dropDownExpanded = false },
                offset = DpOffset(0.dp, PaddingCompact),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.add_next_in_queue)) },
                    onClick = {
                        dropDownExpanded = false
                        onAddNextInQueue(song)
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete_song)) },
                    onClick = {
                        dropDownExpanded = false
                        onDeleteSong(song)
                    }
                )
            }
        }
    }
}