package com.ndriqa.musicky.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ndriqa.musicky.core.data.SortingMode
import com.ndriqa.musicky.core.data.VisualizerType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

@Singleton
class DataStoreManager @Inject constructor(private val context: Context) {

    private val KeyDefaultVisualizer = stringPreferencesKey(STRING_KEY_DEFAULT_VISUALIZER)
    private val KeyHighCaptureRate = booleanPreferencesKey(BOOLEAN_KEY_HIGH_CAPTURE_RATE)
    private val KeyMinAudioLength = intPreferencesKey(INT_KEY_MIN_AUDIO_LENGTH)
    private val KeySongsSortingMode = stringPreferencesKey(STRING_KEY_PREFERRED_SORT)

    val highCaptureRate: Flow<Boolean> = context.dataStore.data
        .map { it[KeyHighCaptureRate] ?: false }

    val minAudioLength: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[KeyMinAudioLength] ?: DEFAULT_MIN_AUDIO_LENGTH }

    val defaultVisualizer: Flow<VisualizerType> = context.dataStore.data
        .map { prefs ->
            val type = prefs[KeyDefaultVisualizer] ?: VisualizerType.Circular.title
            VisualizerType.entries
                .find { it.title == type }
                ?: VisualizerType.Circular
        }

    val preferredSortingMode: Flow<SortingMode> = context.dataStore.data
        .map { prefs ->
            val mode = prefs[KeySongsSortingMode] ?: SortingMode.Default
            SortingMode.entries
                .find { it.mode == mode }
                ?: SortingMode.Default
        }

    suspend fun setMinAudioLength(value: Int) {
        context.dataStore.edit { prefs ->
            prefs[KeyMinAudioLength] = value
        }
    }

    suspend fun toggleHighCaptureRate() {
        context.dataStore.edit { preferences ->
            preferences[KeyHighCaptureRate] = !(preferences[KeyHighCaptureRate] ?: false)
        }
    }

    suspend fun setDefaultVisualizer(visualizerType: VisualizerType) {
        context.dataStore.edit { preferences ->
            preferences[KeyDefaultVisualizer] = visualizerType.title
        }
    }

    suspend fun setPreferredSortingMode(sortingMode: SortingMode) {
        context.dataStore.edit { preferences ->
            preferences[KeySongsSortingMode] = sortingMode.mode
        }
    }

    companion object {
        private const val STRING_KEY_EXAMPLE_PREF = "STRING_KEY_EXAMPLE_PREF"
        private const val BOOLEAN_KEY_VIBRATION = "BOOLEAN_KEY_VIBRATION"
        private const val BOOLEAN_KEY_SOUND = "BOOLEAN_KEY_SOUND"
        private const val BOOLEAN_KEY_HIGH_CAPTURE_RATE = "BOOLEAN_KEY_HIGH_CAPTURE_RATE"
        private const val INT_KEY_MIN_AUDIO_LENGTH = "KEY_MIN_AUDIO_LENGTH"
        private const val STRING_KEY_DEFAULT_VISUALIZER = "STRING_KEY_DEFAULT_VISUALIZER"
        private const val STRING_KEY_PREFERRED_SORT = "STRING_KEY_PREFERRED_SORT"

        const val DEFAULT_MIN_AUDIO_LENGTH = 45
        const val MIN_AUDIO_LENGTH = 30
        const val MAX_AUDIO_LENGTH = 120
    }
}