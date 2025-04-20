package com.ndriqa.musicky.features.settings

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ndriqa.musicky.R
import com.ndriqa.musicky.core.data.VisualizerType
import com.ndriqa.musicky.core.preferences.DataStoreManager.Companion.MAX_AUDIO_LENGTH
import com.ndriqa.musicky.core.preferences.DataStoreManager.Companion.MIN_AUDIO_LENGTH
import com.ndriqa.musicky.features.player.PlayerViewModel
import com.ndriqa.musicky.features.settings.components.SettingsItemSection
import com.ndriqa.musicky.features.settings.components.SettingMinAudioLength
import com.ndriqa.musicky.features.settings.components.SettingPreferredVisualizer
import com.ndriqa.musicky.features.settings.components.SettingsItemTitle
import com.ndriqa.musicky.ui.theme.PaddingCompact
import com.ndriqa.musicky.ui.theme.PaddingDefault

@Composable
fun SettingsScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel,
    playerViewModel: PlayerViewModel
) {
    val minAudioLength by settingsViewModel.minAudioLength.collectAsState()
    val preferredVisualizer by settingsViewModel.preferredVisualizer.collectAsState()

    val scrollState = rememberScrollState()
    val minAudioLengthRatio by remember { derivedStateOf {
        (minAudioLength - MIN_AUDIO_LENGTH).toFloat() / (MAX_AUDIO_LENGTH - MIN_AUDIO_LENGTH)
    } }

    fun onAudioRatioChange(ratio: Float) {
        (MIN_AUDIO_LENGTH + (MAX_AUDIO_LENGTH - MIN_AUDIO_LENGTH) * ratio)
            .toInt()
            .coerceIn(MIN_AUDIO_LENGTH, MAX_AUDIO_LENGTH)
            .also { settingsViewModel.updateMinAudioLength(it) }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaddingDefault)
            ) {
                Text(
                    text = stringResource(R.string.settings),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .scrollable(scrollState, Orientation.Vertical),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PaddingDefault)
        ) {
            SettingMinAudioLength(
                minAudioLength = minAudioLength,
                minAudioLengthRatio = minAudioLengthRatio,
                onAudioRatioChange = ::onAudioRatioChange,
            )

            SettingPreferredVisualizer(
                preferredVisualizer = preferredVisualizer,
                onVisualizerTypeUpdate = settingsViewModel::updateVisualizerType
            )
        }
    }
}