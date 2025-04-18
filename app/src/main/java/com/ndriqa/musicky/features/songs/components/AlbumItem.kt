package com.ndriqa.musicky.features.songs.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.ndriqa.musicky.R
import com.ndriqa.musicky.core.data.Album
import com.ndriqa.musicky.ui.theme.PaddingCompact
import com.ndriqa.musicky.ui.theme.PaddingHalf
import com.ndriqa.musicky.ui.theme.PaddingMini
import com.ndriqa.musicky.ui.theme.PaddingNano
import com.ndriqa.musicky.ui.theme.QuicksandFontFamily

@androidx.compose.runtime.Composable
internal fun AlbumItem(
    album: Album,
    isPlaying: Boolean,
    onAlbumPlay: (Album) -> Unit,
    onAlbumToQueue: (Album) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val contentBackground =
        if (isPlaying) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.onPrimaryContainer
    val contentTint =
        if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.primary

    var isFlipped by rememberSaveable { mutableStateOf(false) }
    val rotationY by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        label = "flipAnimation"
    )
    val cameraDistance = 12f * LocalDensity.current.density

    val albumShape = RoundedCornerShape(PaddingCompact)
    val fallbackIcon = rememberVectorPainter(Icons.Rounded.MusicNote)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(albumShape)
            .clickable(onClick = { isFlipped = !isFlipped })
            .graphicsLayer {
                this.rotationY = rotationY
                this.cameraDistance = cameraDistance
            }
    ) {
        val imageRequest = ImageRequest.Builder(context)
            .data(album.artworkUri)
            .crossfade(true)
            .diskCachePolicy(CachePolicy.ENABLED) // enable disk caching
            .memoryCachePolicy(CachePolicy.ENABLED) // enable memory caching
            .build()

        val textStyle = TextStyle(
            fontFamily = QuicksandFontFamily,
            shadow = Shadow(
                color = contentBackground,
                offset = Offset(2f, 2f),
                blurRadius = 4f
            )
        )

        if (rotationY < 90) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    fallback = fallbackIcon
                )

                Column(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(contentBackground, Color.Transparent),
                                startY = Float.POSITIVE_INFINITY,
                                endY = 0f,
                            )
                        )
                        .padding(PaddingCompact),
                    verticalArrangement = Arrangement.spacedBy(PaddingMini, alignment = Alignment.Bottom),
                ) {
                    Text(
                        text = album.name,
                        fontWeight = FontWeight.Bold,
                        color = contentTint,
                        style = textStyle
                    )

                    Text(
                        text = pluralStringResource(R.plurals.num_songs, album.size, album.size),
                        color = contentTint,
                        style = textStyle
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        width = 1.dp,
                        shape = albumShape
                    )
                    .graphicsLayer { this.rotationY = 180f },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val nestedScrollConnection = remember {
                    object : NestedScrollConnection {
                        override suspend fun onPreFling(available: Velocity): Velocity {
                            return available
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .nestedScroll(nestedScrollConnection),
                    contentPadding = PaddingValues(PaddingHalf),
                    verticalArrangement = Arrangement.spacedBy(PaddingMini)
                ) {
                    itemsIndexed(
                        items = album.songs,
                        key = { _, song -> song.id }
                    ) { index, song ->
                        val backgroundColor = MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.7f - (index % 2) * 0.4f
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = backgroundColor,
                                    shape = RoundedCornerShape(PaddingMini)
                                )
                                .padding(horizontal = PaddingMini, vertical = PaddingNano),
                            verticalArrangement = Arrangement.spacedBy(PaddingNano),
                        ) {
                            Text(
                                text = song.title,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = song.artist,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 10.sp
                            )
                        }
                    }
                }

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                )
                Button(onClick = { onAlbumPlay(album) }) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
//                    Text(stringResource(R.string.play_album))
                }
            }
        }
    }
}