package com.ndriqa.musicky.features.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.testing.FakeReviewManager
import com.ndriqa.musicky.BuildConfig
import com.ndriqa.musicky.core.data.SortingMode
import com.ndriqa.musicky.core.data.VisualizerType
import com.ndriqa.musicky.core.preferences.DataStoreManager
import com.ndriqa.musicky.core.services.PlayerService
import com.ndriqa.musicky.navigation.musickyPlayStore
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
        .stateIn(viewModelScope, SharingStarted.Eagerly, VisualizerType.Circular)

    val preferredSortingMode = dataStoreManager.preferredSortingMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, SortingMode.Default)

    val highCaptureRate = dataStoreManager.highCaptureRate
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun launchInAppReview(activity: Activity) {
        val reviewManager =
            if (BuildConfig.DEBUG) FakeReviewManager(activity)
            else ReviewManagerFactory.create(activity)
        val request = reviewManager.requestReviewFlow()

        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = reviewManager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener {
                    // review flow finished, no way to know if user left a review
                }
            } else musickyPlayStore(activity)
        }
    }

    fun updateServiceHighCaptureRate(context: Context) {
        Intent(context, PlayerService::class.java)
            .setAction(PlayerService.ACTION_HIGH_RATE_UPDATE)
            .putExtra(PlayerService.EXTRA_HIGH_CAPTURE_RATE, highCaptureRate.value)
            .also { context.startService(it) }
    }

    fun toggleHighCaptureRate() {
        viewModelScope.launch {
            dataStoreManager.toggleHighCaptureRate()
        }
    }

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

    fun updateSortingMode(sortingMode: SortingMode) {
        viewModelScope.launch {
            dataStoreManager.setPreferredSortingMode(sortingMode)
        }
    }
}