package com.ndriqa.musicky.features.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults.elevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ndriqa.musicky.core.util.helpers.MockHelper
import com.ndriqa.musicky.features.player.components.MusicDisc
import com.ndriqa.musicky.features.player.components.SongArtworkImage
import com.ndriqa.musicky.features.player.components.SongControls
import com.ndriqa.musicky.features.player.components.SongHeaderInfo
import com.ndriqa.musicky.features.player.components.SongVisualizer
import com.ndriqa.musicky.ui.theme.MusickyTheme
import com.ndriqa.musicky.ui.theme.PaddingDefault
import timber.log.Timber
import kotlin.math.abs

internal const val MAX_BYTE_VAL = 256

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
                        MusicDisc(playState)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun HustlePlayerCompactPreview() {
    val context = LocalContext.current
    val playerViewModel = PlayerViewModel(context).apply {
        updatePlayingStateTesting(MockHelper.getMockPlayingState())
    }

    MusickyTheme {
        HustlePlayer(
            hasVisualizerRecordingPermission = true,
            onExpandedUpdate = {  },
            isVisible = true,
            isExpanded = false,
            playerViewModel = playerViewModel
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HustlePlayerExpandedPreview() {
    val context = LocalContext.current
    val playerViewModel = PlayerViewModel(context).apply {
        updatePlayingStateTesting(MockHelper.getMockPlayingState())
    }

    MusickyTheme {
        HustlePlayer(
            hasVisualizerRecordingPermission = true,
            onExpandedUpdate = {  },
            modifier = Modifier.padding(
                bottom = PaddingDefault * 3,
                end = PaddingDefault * 2
            ),
            isVisible = true,
            isExpanded = true,
            playerViewModel = playerViewModel
        )
    }
}