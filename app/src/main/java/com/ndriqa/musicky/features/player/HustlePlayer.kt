package com.ndriqa.musicky.features.player

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.ndriqa.musicky.core.data.PlayingState
import com.ndriqa.musicky.core.data.Song
import com.ndriqa.musicky.core.util.extensions.toFormattedTime
import com.ndriqa.musicky.core.util.extensions.waveformToPath
import com.ndriqa.musicky.ui.theme.MusicIconArtworkSizeBig
import com.ndriqa.musicky.ui.theme.PaddingDefault
import com.ndriqa.musicky.ui.theme.PaddingHalf
import com.ndriqa.musicky.ui.theme.PaddingMini
import com.ndriqa.musicky.ui.theme.SpaceMonoFontFamily
import timber.log.Timber
import kotlin.math.abs

private const val MAX_BYTE_VAL = 256

@Composable
fun HustlePlayer(
    hasVisualizerRecordingPermission: Boolean,
    onExpandedUpdate: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isVisible: Boolean = false,
    isExpanded: Boolean = false,
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val playerShape = if (isExpanded) RoundedCornerShape(PaddingDefault) else CircleShape
    val playState by playerViewModel.playingState.collectAsState()
    val waveform by playerViewModel.waveform.collectAsState()
    val currentSong by remember { derivedStateOf { playState.currentSong } }

    val averageSongEnergy by playerViewModel.averageSongEnergy.collectAsState()
    val currentEnergy = waveform.map { abs(it.toInt()) }.average().toInt().toByte()
    val pulse by playerViewModel.pulse.collectAsState(false)

    val gestureModifier = Modifier.pointerInput(Unit) {
        detectVerticalDragGestures { change, dragAmount ->
            onExpandedUpdate(when {
                dragAmount > 20f -> false
                dragAmount < -20f -> true
                else -> isExpanded
            })
        }
    }

    val startOffsetPadding = PaddingDefault * 2
    val topOffsetPadding = PaddingDefault * 3
    val fabElevation = 0.dp

    LaunchedEffect(currentSong) {
        playerViewModel.resetSongAverageEnergy()
    }

    LaunchedEffect(pulse) {
        if (pulse) {
            Timber.tag("pulse").d("average: $averageSongEnergy, current: $currentEnergy")
        }
    }

    AnimatedVisibility(isVisible) {
        AnimatedContent(targetState = isExpanded) { expanded ->
            val sizeModifier =
                if (!expanded) Modifier
                    .size(60.dp)
                else Modifier
                    .fillMaxSize()
                    .padding(
                        start = startOffsetPadding,
                        top = topOffsetPadding
                    )

            FloatingActionButton(
                onClick = { onExpandedUpdate(!isExpanded) },
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
                        if (hasVisualizerRecordingPermission) {
                            SongVisualizer(waveform, pulse)
                        }
                        SongControls(
                            playState = playState,
                            onPlayPauseClicked = { playerViewModel.playPause(context) },
                            onNextClicked = { playerViewModel.next(context) },
                            onPrevClicked = { playerViewModel.previous(context) },
                            onSeek = { playerViewModel.seekToProgress(context, it) }
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
                    targetValue = if (maxPosition != 0L) currentPosition.toFloat() / maxPosition else 0f,
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
                            val percent = if (width != 0f) change.position.x / width else 0f
                            onSeek(percent.coerceIn(0f, 1f))
                        }
                        detectTapGestures { offset ->
                            val width = size.width.toFloat()
                            val percent = if (width != 0f) offset.x / width else 0f
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
private fun ColumnScope.SongVisualizer(
    waveform: ByteArray,
    pulse: Boolean = false,
) {
    val lineColor = MaterialTheme.colorScheme.onPrimaryContainer

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(MAX_BYTE_VAL.dp)
            .padding(horizontal = PaddingDefault),
    ) {
        drawPath(
            path = waveform.waveformToPath(size.width, size.height),
            brush = SolidColor(lineColor),
            style = Stroke(
                width = 1.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
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
private fun ColumnScope.SongArtworkImage(artworkImageUri: Uri?) {
    val artworkShape = RoundedCornerShape(PaddingDefault)
    val fallbackIcon = rememberVectorPainter(Icons.Rounded.MusicNote)

    Box(
        modifier = Modifier
            .weight(1f)
//            .scale(animatedScale)
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