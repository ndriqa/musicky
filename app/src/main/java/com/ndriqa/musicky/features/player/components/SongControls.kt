package com.ndriqa.musicky.features.player.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.ShuffleOn
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ndriqa.musicky.core.data.PlayingState
import com.ndriqa.musicky.core.util.extensions.toFormattedTime
import com.ndriqa.musicky.core.util.helpers.MockHelper
import com.ndriqa.musicky.ui.theme.MusickyTheme
import com.ndriqa.musicky.ui.theme.PaddingCompact
import com.ndriqa.musicky.ui.theme.PaddingHalf
import com.ndriqa.musicky.ui.theme.SpaceMonoFontFamily

@Composable
internal fun ColumnScope.SongControls(
    playState: PlayingState,
    onPlayPauseClicked: () -> Unit,
    onNextClicked: () -> Unit,
    onPrevClicked: () -> Unit,
    onSeek: (Float) -> Unit,
    onShuffleClicked: () -> Unit,
    onRepeatClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(PaddingCompact),
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
            val shuffleIcon =
                if (playState.isShuffleEnabled) Icons.Rounded.ShuffleOn
                else Icons.Rounded.Shuffle

            val playPauseIcon =
                if (playState.isPlaying) Icons.Rounded.Pause
                else Icons.Rounded.PlayArrow

            ControlButton(
                onClick = onShuffleClicked,
                icon = shuffleIcon
            )
            ControlButton(
                onClick = onPrevClicked,
                icon = Icons.Rounded.SkipPrevious
            )
            ControlButton(
                onClick = onPlayPauseClicked,
                icon = playPauseIcon
            )
            ControlButton(
                onClick = onNextClicked,
                icon = Icons.Rounded.SkipNext
            )
            ControlButton(
                onClick = onRepeatClicked,
                icon = playState.repeatMode.icon
            )
        }
    }
}

@Composable
private fun RowScope.ControlButton(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.weight(1f),
        contentPadding = PaddingValues()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null
        )
    }
}

@Preview(showBackground = true, heightDp = 150)
@Composable
private fun SongControlPreview() {
    MusickyTheme {
        Column {
            SongControls(
                playState = MockHelper.getMockPlayingState(),
                onPlayPauseClicked = {  },
                onNextClicked = {  },
                onPrevClicked = {  },
                onSeek = {  },
                onShuffleClicked = {  },
                onRepeatClicked = {  }
            )
        }
    }
}