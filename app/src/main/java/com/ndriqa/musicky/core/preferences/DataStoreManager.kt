package com.ndriqa.musicky.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ndriqa.musicky.core.data.VisualizerType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

@Singleton
class DataStoreManager @Inject constructor(private val context: Context) {

    private val KeyExamplePref = stringPreferencesKey(STRING_KEY_EXAMPLE_PREF)
    private val KeyPreferredDifficulty = stringPreferencesKey(STRING_KEY_EXAMPLE_PREF)
    private val KeyDefaultVisualizer = stringPreferencesKey(STRING_KEY_DEFAULT_VISUALIZER)
    private val KeyEnableVibration = booleanPreferencesKey(BOOLEAN_KEY_VIBRATION)
    private val KeyEnableSound = booleanPreferencesKey(BOOLEAN_KEY_SOUND)
    private val KeyMinAudioLength = intPreferencesKey(INT_KEY_MIN_AUDIO_LENGTH)

    val enableVibration: Flow<Boolean> = context.dataStore.data
        .map { it[KeyEnableVibration] ?: true }

    val enableSound: Flow<Boolean> = context.dataStore.data
        .map { it[KeyEnableSound] ?: true }

    val minAudioLength: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[KeyMinAudioLength] ?: DEFAULT_MIN_AUDIO_LENGTH }

    val defaultVisualizer: Flow<VisualizerType> = context.dataStore.data
        .map { prefs ->
            val type = prefs[KeyDefaultVisualizer] ?: VisualizerType.LineCenter.title
            VisualizerType.entries
                .find { it.title == type }
                ?: VisualizerType.LineCenter
        }

    suspend fun setMinAudioLength(value: Int) {
        context.dataStore.edit { prefs ->
            prefs[KeyMinAudioLength] = value
        }
    }

    suspend fun toggleVibration() {
        context.dataStore.edit { preferences ->
            preferences[KeyEnableVibration] = !(preferences[KeyEnableVibration] ?: true)
        }
    }

    suspend fun toggleSound() {
        context.dataStore.edit { preferences ->
            preferences[KeyEnableSound] = !(preferences[KeyEnableSound] ?: true)
        }
    }

    suspend fun changePrefSuspend(newData: String) {
        context.dataStore.edit { preferences ->
            preferences[KeyExamplePref] = newData
        }
    }

    suspend fun setDefaultVisualizer(visualizerType: VisualizerType) {
        context.dataStore.edit { preferences ->
            preferences[KeyDefaultVisualizer] = visualizerType.title
        }
    }

    companion object {
        private const val STRING_KEY_EXAMPLE_PREF = "STRING_KEY_EXAMPLE_PREF"
        private const val BOOLEAN_KEY_VIBRATION = "BOOLEAN_KEY_VIBRATION"
        private const val BOOLEAN_KEY_SOUND = "BOOLEAN_KEY_SOUND"
        private const val INT_KEY_MIN_AUDIO_LENGTH = "KEY_MIN_AUDIO_LENGTH"
        private const val STRING_KEY_DEFAULT_VISUALIZER = "STRING_KEY_DEFAULT_VISUALIZER"

        const val DEFAULT_MIN_AUDIO_LENGTH = 45
        const val MIN_AUDIO_LENGTH = 30
        const val MAX_AUDIO_LENGTH = 120
    }
}