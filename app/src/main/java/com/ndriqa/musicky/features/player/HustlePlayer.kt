package com.ndriqa.musicky.features.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.border
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
import androidx.compose.material3.Scaffold
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
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ndriqa.musicky.core.preferences.DataStoreManager
import com.ndriqa.musicky.core.util.helpers.MockHelper
import com.ndriqa.musicky.features.player.components.MusicDisc
import com.ndriqa.musicky.features.player.components.SongArtworkImage
import com.ndriqa.musicky.features.player.components.SongControls
import com.ndriqa.musicky.features.player.components.SongHeaderInfo
import com.ndriqa.musicky.features.player.components.SongTopBar
import com.ndriqa.musicky.features.player.components.SongVisualizer
import com.ndriqa.musicky.features.settings.SettingsViewModel
import com.ndriqa.musicky.navigation.Screens
import com.ndriqa.musicky.ui.theme.DiscSize
import com.ndriqa.musicky.ui.theme.DiscSizeOffsetMax
import com.ndriqa.musicky.ui.theme.MusickyTheme
import com.ndriqa.musicky.ui.theme.PaddingDefault
import kotlin.math.abs

@Composable
fun HustlePlayer(
    hasVisualizerRecordingPermission: Boolean,
    onExpandedUpdate: (Boolean) -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier,
    isVisible: Boolean = false,
    isExpanded: Boolean = false,
    playerViewModel: PlayerViewModel,
    settingsViewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val playerShape = if (isExpanded) RoundedCornerShape(PaddingDefault) else CircleShape
    val playState by playerViewModel.playingState.collectAsState()
    val waveform by playerViewModel.waveform.collectAsState()
    val audioFeatures by playerViewModel.audioFeatures.collectAsState()
    val fftFeatures by playerViewModel.fftFeatures.collectAsState()
    val preferredVisualizer by settingsViewModel.preferredVisualizer.collectAsState()

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

    val discSizeOffset = DiscSizeOffsetMax * (audioFeatures.normalizedDisturbance - 0.5).toFloat()
    val discSize = DiscSize + discSizeOffset
    val animatedDiscSize by animateDpAsState(targetValue = discSize)

    fun onSettingsClick() {
        onExpandedUpdate(false)
        navController.navigate(Screens.Settings) { launchSingleTop = true }
    }

    fun toggleTimer(millis: Long?) {
        playerViewModel.toggleTimer(context, millis)
    }

    AnimatedVisibility(isVisible) {
        AnimatedContent(targetState = isExpanded) { expanded ->
            val sizeModifier =
                if (!expanded) Modifier
                    .size(animatedDiscSize)
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
                    .then(gestureModifier)
                    .border(
                        width = 1.dp,
                        shape = playerShape,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                shape = playerShape,
                containerColor = MaterialTheme.colorScheme.primary,
                elevation = elevation(
                    defaultElevation = fabElevation,
                    pressedElevation = fabElevation,
                    focusedElevation = fabElevation,
                    hoveredElevation = fabElevation
                )
            ) {
                Scaffold(
                    topBar = {
                        if (isExpanded) {
                            SongTopBar(
                                playingState = playState,
                                selectedVisualizerType = preferredVisualizer,
                                onSettingsClick = ::onSettingsClick,
                                onTimeSelected = ::toggleTimer,
                                onVisualizerChange = settingsViewModel::updateVisualizerType,
                            )
                        }
                    }
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (isExpanded) Modifier.padding(paddingValues) else Modifier)
                            .padding(if (isExpanded) PaddingDefault else 0.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        val song = playState.currentSong
                        if (expanded && song != null) {
                            SongArtworkImage(song.artworkUri)
                            SongHeaderInfo(song)
                            if (hasVisualizerRecordingPermission) {
                                SongVisualizer(
                                    waveform = waveform,
                                    audioFeatures = audioFeatures,
                                    fftFeatures = fftFeatures,
                                    type = preferredVisualizer
                                )
                            }
                            SongControls(
                                playState = playState,
                                onPlayPauseClicked = { playerViewModel.playPause(context) },
                                onNextClicked = { playerViewModel.next(context) },
                                onPrevClicked = { playerViewModel.previous(context) },
                                onSeek = { playerViewModel.seekToProgress(context, it) },
                                onShuffleClicked = { playerViewModel.toggleShuffle(context) },
                                onRepeatClicked = { playerViewModel.toggleRepeat(context) }
                            )
//                            NextSongUi(playState)
                        } else {
                            MusicDisc(playState)
                        }
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
    val dataStoreManager = DataStoreManager(context)
    val playerViewModel = PlayerViewModel(context, dataStoreManager).apply {
        updatePlayingStateTesting(MockHelper.getMockPlayingState())
    }
    val settingsViewModel = SettingsViewModel(dataStoreManager)
    val navController = rememberNavController()

    MusickyTheme {
        HustlePlayer(
            hasVisualizerRecordingPermission = true,
            onExpandedUpdate = {  },
            navController = navController,
            isVisible = true,
            isExpanded = false,
            playerViewModel = playerViewModel,
            settingsViewModel = settingsViewModel
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HustlePlayerExpandedPreview() {
    val context = LocalContext.current
    val dataStoreManager = DataStoreManager(context)
    val playerViewModel = PlayerViewModel(context, dataStoreManager).apply {
        updatePlayingStateTesting(MockHelper.getMockPlayingState())
    }
    val settingsViewModel = SettingsViewModel(dataStoreManager)
    val navController = rememberNavController()

    MusickyTheme {
        HustlePlayer(
            hasVisualizerRecordingPermission = true,
            onExpandedUpdate = {  },
            navController = navController,
            modifier = Modifier.padding(
                bottom = PaddingDefault * 3,
                end = PaddingDefault * 2
            ),
            isVisible = true,
            isExpanded = true,
            playerViewModel = playerViewModel,
            settingsViewModel = settingsViewModel
        )
    }
}