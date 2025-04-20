package com.ndriqa.musicky.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ndriqa.musicky.BuildConfig
import com.ndriqa.musicky.core.data.VisualizerType
import com.ndriqa.musicky.core.preferences.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager
): ViewModel() {
    val appVersion = "v. ${BuildConfig.VERSION_NAME}"

    val minAudioLength = dataStoreManager.minAudioLength
        .stateIn(viewModelScope, SharingStarted.Eagerly, DataStoreManager.DEFAULT_MIN_AUDIO_LENGTH)

    val preferredVisualizer = dataStoreManager.defaultVisualizer
        .stateIn(viewModelScope, SharingStarted.Eagerly, VisualizerType.LineCenter)

    fun updateMinAudioLength(value: Int) {
        viewModelScope.launch {
            dataStoreManager.setMinAudioLength(value)
        }
    }

    fun updateVisualizerType(visualizerType: VisualizerType) {
        viewModelScope.launch {
            dataStoreManager.setDefaultVisualizer(visualizerType)
        }
    }
}