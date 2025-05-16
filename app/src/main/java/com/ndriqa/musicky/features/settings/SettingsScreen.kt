package com.ndriqa.musicky.features.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ndriqa.musicky.R
import com.ndriqa.musicky.core.preferences.DataStoreManager
import com.ndriqa.musicky.core.preferences.DataStoreManager.Companion.MAX_AUDIO_LENGTH
import com.ndriqa.musicky.core.preferences.DataStoreManager.Companion.MIN_AUDIO_LENGTH
import com.ndriqa.musicky.features.player.PlayerViewModel
import com.ndriqa.musicky.features.settings.SettingsKeys.KEY_MIN_AUDIO
import com.ndriqa.musicky.features.settings.SettingsKeys.KEY_NDRIQA
import com.ndriqa.musicky.features.settings.SettingsKeys.KEY_SORTING_MODE
import com.ndriqa.musicky.features.settings.SettingsKeys.KEY_VISUALIZER_RATE
import com.ndriqa.musicky.features.settings.SettingsKeys.KEY_VISUALIZER_TYPE
import com.ndriqa.musicky.features.settings.components.SettingAppVersion
import com.ndriqa.musicky.features.settings.components.SettingHighCaptureRate
import com.ndriqa.musicky.features.settings.components.SettingMinAudioLength
import com.ndriqa.musicky.features.settings.components.SettingNdriqa
import com.ndriqa.musicky.features.settings.components.SettingPreferredSortingMode
import com.ndriqa.musicky.features.settings.components.SettingPreferredVisualizer
import com.ndriqa.musicky.navigation.musickyPlayStore
import com.ndriqa.musicky.navigation.ndriqaDonate
import com.ndriqa.musicky.navigation.ndriqaOtherApps
import com.ndriqa.musicky.ui.components.TopBarUi
import com.ndriqa.musicky.ui.theme.MusickyTheme
import com.ndriqa.musicky.ui.theme.PaddingCompact

@Composable
fun SettingsScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel,
    playerViewModel: PlayerViewModel,
    preSelectedSetting: String? = null
) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val minAudioLength by settingsViewModel.minAudioLength.collectAsState()
    val preferredVisualizer by settingsViewModel.preferredVisualizer.collectAsState()
    val preferredSortingMode by settingsViewModel.preferredSortingMode.collectAsState()
    val highCaptureRate by settingsViewModel.highCaptureRate.collectAsState()
    val listState = rememberLazyListState()

    val minAudioLengthRatio by remember { derivedStateOf {
        (minAudioLength - MIN_AUDIO_LENGTH).toFloat() / (MAX_AUDIO_LENGTH - MIN_AUDIO_LENGTH)
    } }

    fun onAudioRatioChange(ratio: Float) {
        (MIN_AUDIO_LENGTH + (MAX_AUDIO_LENGTH - MIN_AUDIO_LENGTH) * ratio)
            .toInt()
            .coerceIn(MIN_AUDIO_LENGTH, MAX_AUDIO_LENGTH)
            .also { settingsViewModel.updateMinAudioLength(it) }
    }

    fun onPlayStoreRating() {
        activity
            ?.let { settingsViewModel.launchInAppReview(it) }
            ?: musickyPlayStore(context)
    }

    fun onOtherApps() {
        ndriqaOtherApps(context)
    }

    fun onDonate() {
        ndriqaDonate(context)
    }

    LaunchedEffect(preSelectedSetting) {
        SettingsKeys.settingsIndices[preSelectedSetting]?.let { settingIndex ->
            listState.animateScrollToItem(settingIndex)
        }
    }

    LaunchedEffect(highCaptureRate) {
        settingsViewModel.updateServiceHighCaptureRate(context)
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            TopBarUi(
                title = stringResource(R.string.settings),
                onNavButtonPress = navController::navigateUp
            )
        },
        bottomBar = { SettingAppVersion(settingsViewModel.appVersion) },
    ) { paddingValues ->
        val layoutDir = LocalLayoutDirection.current

        val paddings = PaddingValues(
            start = paddingValues.calculateStartPadding(layoutDir) + PaddingCompact,
            end = paddingValues.calculateEndPadding(layoutDir) + PaddingCompact,
            top = paddingValues.calculateTopPadding() + PaddingCompact,
            bottom = paddingValues.calculateBottomPadding() + PaddingCompact,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PaddingCompact),
            contentPadding = paddings,
            state = listState
        ) {
            item(key = KEY_MIN_AUDIO) {
                SettingMinAudioLength(
                    minAudioLength = minAudioLength,
                    minAudioLengthRatio = minAudioLengthRatio,
                    onAudioRatioChange = ::onAudioRatioChange,
                )
            }

            item(key = KEY_SORTING_MODE) {
                SettingPreferredSortingMode(
                    sortingMode = preferredSortingMode,
                    onSortingModeChange = settingsViewModel::updateSortingMode,
                    isInitiallyExpanded = preSelectedSetting == KEY_SORTING_MODE
                )
            }

            item(key = KEY_VISUALIZER_TYPE) {
                SettingPreferredVisualizer(
                    preferredVisualizer = preferredVisualizer,
                    onVisualizerTypeUpdate = settingsViewModel::updateVisualizerType,
                    isInitiallyExpanded = preSelectedSetting == KEY_VISUALIZER_TYPE
                )
            }

            item(key = KEY_VISUALIZER_RATE) {
                SettingHighCaptureRate(
                    highCaptureRate = highCaptureRate,
                    onHighCaptureRateToggle = settingsViewModel::toggleHighCaptureRate
                )
            }

            item(key = KEY_NDRIQA) {
                SettingNdriqa(
                    onPlayStoreRate = ::onPlayStoreRating,
                    onDonate = ::onDonate,
                    onNdriqaApps = ::onOtherApps,
                )
            }
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFFE6F6F5
)
@Composable
private fun SettingsScreenPreview() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val dataStoreManager = DataStoreManager(context)
    val playerViewModel = PlayerViewModel(context, dataStoreManager)
    val settingsViewModel = SettingsViewModel(dataStoreManager)

    MusickyTheme {
        SettingsScreen(
            navController = navController,
            settingsViewModel = settingsViewModel,
            playerViewModel = playerViewModel
        )
    }
}