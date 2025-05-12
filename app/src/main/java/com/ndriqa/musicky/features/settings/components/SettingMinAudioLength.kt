package com.ndriqa.musicky.features.settings.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ndriqa.musicky.R

@Composable
internal fun SettingMinAudioLength(
    minAudioLength: Int,
    minAudioLengthRatio: Float,
    onAudioRatioChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsItemSection(
        modifier = modifier,
        content = {
            SettingsItemTitle(title = stringResource(R.string.minimum_song_length))
            SettingsItemText(stringResource(R.string.num_seconds, minAudioLength))
            Slider(
                value = minAudioLengthRatio,
                onValueChange = onAudioRatioChange,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    thumbColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    )
}