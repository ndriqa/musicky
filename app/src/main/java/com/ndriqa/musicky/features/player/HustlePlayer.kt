package com.ndriqa.musicky.features.player

import android.net.Uri
import android.widget.ProgressBar
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults.elevation
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.ndriqa.musicky.core.data.PlayingState
import com.ndriqa.musicky.core.data.Song
import com.ndriqa.musicky.core.util.extensions.debugBorder
import com.ndriqa.musicky.core.util.extensions.toFormattedTime
import com.ndriqa.musicky.ui.theme.DefaultPlayerElevation
import com.ndriqa.musicky.ui.theme.MusicIconArtworkSizeBig
import com.ndriqa.musicky.ui.theme.MusicIconArtworkSizeCompact
import com.ndriqa.musicky.ui.theme.PaddingDefault
import com.ndriqa.musicky.ui.theme.PaddingHalf
import com.ndriqa.musicky.ui.theme.PaddingMini
import com.ndriqa.musicky.ui.theme.PaddingNano
import com.ndriqa.musicky.ui.theme.SpaceMonoFontFamily
import kotlin.random.Random

@Composable
fun HustlePlayer(
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    var isExpanded by remember { mutableStateOf(false) }
    val playerShape by remember { derivedStateOf {
        if (isExpanded) RoundedCornerShape(PaddingDefault)
        else CircleShape
    } }

    val playState by playerViewModel.playingState.collectAsState()
    val isVisible by remember { derivedStateOf { playState.currentSong != null } }

    val gestureModifier = Modifier.pointerInput(Unit) {
        detectVerticalDragGestures { change, dragAmount ->
            isExpanded = when {
                dragAmount > 20f -> false
                dragAmount < -20f -> true
                else -> isExpanded
            }
        }
    }

    val offsetPadding = PaddingDefault * 2
    val fabElevation = DefaultPlayerElevation

    AnimatedVisibility(isVisible) {
        AnimatedContent(
            targetState = isExpanded,
        ) { expanded ->
            val sizeModifier =
                if (!expanded) Modifier
                    .size(60.dp)
                else Modifier
                    .fillMaxSize()
                    .padding(start = offsetPadding, top = offsetPadding)

            FloatingActionButton(
                onClick = { isExpanded = !isExpanded },
                modifier = modifier
                    .then(sizeModifier)
                    .then(gestureModifier),
                shape = playerShape,
                containerColor = MaterialTheme.colorScheme.primary,
                elevation = elevation(
                    defaultElevation = fabElevation,
                    pressedElevation = fabElevation,
                    focusedElevation = fabElevation,
                    hoveredElevation = fabElevation
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isExpanded) PaddingDefault else 0.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val song = playState.currentSong
                    if (expanded && song != null) {
                        SongArtworkImage(song.artworkUri)
                        SongHeaderInfo(song)
                        SongEqualizer(playState)
                        SongControls(
                            playState = playState,
                            onPlayPauseClicked = playerViewModel::togglePlayPause,
                            onNextClicked = playerViewModel::next,
                            onPrevClicked = playerViewModel::previous,
                            onSeek = playerViewModel::seekToProgress
                        )
                    } else {
                        val artworkShape = RoundedCornerShape(PaddingDefault)
                        val fallbackIcon = rememberVectorPainter(Icons.Rounded.MusicNote)

                        Box(
                            modifier = Modifier
                                .size(MusicIconArtworkSizeBig)
                                .clip(shape = artworkShape)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = artworkShape
                                )
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "rotation")
                            val rotation by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 5000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "infinite rotation"
                            )

                            AsyncImage(
                                model = song?.artworkUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        if (playState.isPlaying) {
                                            rotationZ = rotation
                                        }
                                    }
                                ,
                                fallback = fallbackIcon
                            )

                            Spacer(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                            alpha = .5F
                                        )
                                    )
                            )

                            Icon(
                                imageVector = Icons.Rounded.RadioButtonChecked,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .5f),
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.SongControls(
    playState: PlayingState,
    onPlayPauseClicked: () -> Unit,
    onNextClicked: () -> Unit,
    onPrevClicked: () -> Unit,
    onSeek: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1F),
        verticalArrangement = Arrangement.spacedBy(PaddingHalf, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PaddingHalf)
        ) {
            val currentPosition = playState.currentPosition
            val maxPosition = playState.currentSong?.duration ?: currentPosition
            val progress = remember { Animatable(0f) }

            LaunchedEffect(currentPosition) {
                progress.animateTo(
                    targetValue = currentPosition.toFloat() / maxPosition,
                    animationSpec = tween(durationMillis = 300)
                )
            }

            Text(
                text = currentPosition.toFormattedTime(),
                fontFamily = SpaceMonoFontFamily
            )

            Box(
                modifier = Modifier
                    .weight(1F)
                    .height(24.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, _ ->
                            val width = size.width.toFloat()
                            val percent = change.position.x / width
                            onSeek(percent.coerceIn(0f, 1f))
                        }
                        detectTapGestures { offset ->
                            val width = size.width.toFloat()
                            val percent = offset.x / width
                            onSeek(percent)
                        }
                    }
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .align(Alignment.Center),
                    progress = { progress.value },
                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    strokeCap = StrokeCap.Butt,
                    gapSize = 3.dp,
                    drawStopIndicator = { } // disabling the stop dot thingy
                )
            }

            Text(
                text = maxPosition.toFormattedTime(),
                fontFamily = SpaceMonoFontFamily
            )
        }

        Row {
            Button(onClick = onPrevClicked) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = null
                )
            }
            Button(onClick = onPlayPauseClicked) {
                Icon(
                    imageVector =
                        if (playState.isPlaying) Icons.Rounded.Pause
                        else Icons.Rounded.PlayArrow,
                    contentDescription = null
                )
            }
            Button(onClick = onNextClicked) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.SongEqualizer(playState: PlayingState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1F)
            .heightIn(max = 200.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val barShape = CircleShape
        repeat(times = 25) {
            Spacer(
                modifier = Modifier
                    .fillMaxHeight(Random.nextFloat())
                    .weight(1F)
                    .clip(barShape)
                    .background(
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = barShape
                    )
            )
        }
    }
}

@Composable
private fun ColumnScope.SongHeaderInfo(song: Song) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1F)
            .heightIn(max = 200.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PaddingMini, Alignment.CenterVertically)
    ) {
        Text(
            text = song.title,
            fontSize = 25.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Text(
            text = song.artist,
            fontSize = 19.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .5f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ColumnScope.SongArtworkImage(
    artworkImageUri: Uri?
) {
    val artworkShape = RoundedCornerShape(PaddingDefault)
    val fallbackIcon = rememberVectorPainter(Icons.Rounded.MusicNote)

    Box(
        modifier = Modifier.weight(1f)
    ) {
        Box(
            modifier = Modifier
                .size(MusicIconArtworkSizeBig)
                .clip(shape = artworkShape)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = artworkShape
                )
                .align(Alignment.Center)
        ) {
            AsyncImage(
                model = artworkImageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                fallback = fallbackIcon
            )
        }
    }
}