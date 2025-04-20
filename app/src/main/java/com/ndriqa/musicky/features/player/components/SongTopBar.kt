package com.ndriqa.musicky.features.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TimerOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ndriqa.musicky.R
import com.ndriqa.musicky.core.data.PlayingState
import com.ndriqa.musicky.core.data.VisualizerType
import com.ndriqa.musicky.core.util.extensions.ifNull
import com.ndriqa.musicky.features.player.PlayerViewModel.Companion.TIMER_MAXIMUM_MIN
import com.ndriqa.musicky.features.player.PlayerViewModel.Companion.TIMER_MINIMUM_MIN
import com.ndriqa.musicky.features.player.PlayerViewModel.Companion.TIMER_STEPS
import com.ndriqa.musicky.ui.theme.PaddingDefault
import com.ndriqa.musicky.ui.theme.PaddingMini
import com.ndriqa.musicky.ui.theme.SpaceMonoFontFamily

@Composable
fun SongTopBar(
    playingState: PlayingState,
    selectedVisualizerType: VisualizerType,
    onSettingsClick: () -> Unit,
    onVisualizerChange: (VisualizerType) -> Unit,
    onTimeSelected: (Long?) -> Unit, // millis
    modifier: Modifier = Modifier
) {
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    val containerColor = MaterialTheme.colorScheme.primaryContainer
    val initialTimerValue = TIMER_MINIMUM_MIN + (TIMER_MAXIMUM_MIN - TIMER_MINIMUM_MIN) / 2

    var showTimerDialog by remember { mutableStateOf(false) }
    var timerSliderValue by remember { mutableFloatStateOf(initialTimerValue) }

    fun updateTimerSliderValue(newValue: Float) {
        timerSliderValue = newValue
    }

    fun confirmTimerDialogAction() {
        showTimerDialog = false

        onTimeSelected(
            if (playingState.autoStopTimeLeft == null) {
                timerSliderValue.toLong().times(60_000) // millis
            } else null
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VisualizerSelector(
            selectedVisualizerType = selectedVisualizerType,
            onVisualizerChange = onVisualizerChange
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { showTimerDialog = true },
            contentPadding = PaddingValues(PaddingMini)
        ) {
            val isTimerOn = playingState.autoStopTimeLeft != null
            val timerIcon =
                if (isTimerOn) Icons.Rounded.Timer
                else Icons.Rounded.TimerOff

            AnimatedVisibility(isTimerOn) {
                Text(
                    text = playingState.timeLeft.ifNull { "" },
                    fontFamily = SpaceMonoFontFamily,
                    modifier = Modifier.padding(horizontal = PaddingMini)
                )
            }

            Icon(
                imageVector = timerIcon,
                contentDescription = null
            )
        }

        Button(
            onClick = onSettingsClick,
            contentPadding = PaddingValues(PaddingMini)
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null,
            )
        }

        if (showTimerDialog) {
            AlertDialog(
                onDismissRequest = { showTimerDialog = false },
                title = { Text(stringResource(R.string.auto_stop_music)) },
                text = {
                    Column {

                        if (playingState.autoStopTimeLeft == null) {
                            Text(text = stringResource(
                                R.string.stop_playback_after_minutes,
                                timerSliderValue.toInt()
                            ))
                            Spacer(Modifier.height(PaddingDefault))
                            Slider(
                                value = timerSliderValue,
                                onValueChange = ::updateTimerSliderValue,
                                valueRange = TIMER_MINIMUM_MIN..TIMER_MAXIMUM_MIN,
                                steps = TIMER_STEPS,
                                colors = SliderDefaults.colors(
                                    activeTrackColor = contentColor,
                                    thumbColor = contentColor,
                                    inactiveTickColor = contentColor,
                                    activeTickColor = containerColor
                                )
                            )
                        } else {
                            Text(stringResource(R.string.are_you_sure_you_want_to_disable_timer))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = ::confirmTimerDialogAction) {
                        val buttonText = stringResource(
                            if (playingState.autoStopTimeLeft == null) R.string.set
                            else R.string.disable_timer
                        )

                        Text(
                            text = buttonText,
                            color = contentColor
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimerDialog = false }) {
                        Text(
                            text = stringResource(R.string.cancel),
                            color = contentColor
                        )
                    }
                }
            )
        }
    }
}