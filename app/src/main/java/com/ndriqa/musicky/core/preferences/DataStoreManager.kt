package com.ndriqa.musicky.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

class DataStoreManager @Inject constructor(private val context: Context) {

    private val KeyExamplePref = stringPreferencesKey(STRING_KEY_EXAMPLE_PREF)
    private val KeyPreferredDifficulty = stringPreferencesKey(STRING_KEY_EXAMPLE_PREF)
    private val KeyEnableVibration = booleanPreferencesKey(BOOLEAN_KEY_VIBRATION)
    private val KeyEnableSound = booleanPreferencesKey(BOOLEAN_KEY_SOUND)

    val enableVibration: Flow<Boolean> = context.dataStore.data
        .map { it[KeyEnableVibration] ?: true }

    val enableSound: Flow<Boolean> = context.dataStore.data
        .map { it[KeyEnableSound] ?: true }

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

    companion object {
        private const val STRING_KEY_EXAMPLE_PREF = "STRING_KEY_EXAMPLE_PREF"
        private const val BOOLEAN_KEY_VIBRATION = "BOOLEAN_KEY_VIBRATION"
        private const val BOOLEAN_KEY_SOUND = "BOOLEAN_KEY_SOUND"
        private const val STRING_KEY_PREFERRED_DIFFICULTY = "STRING_KEY_PREFERRED_DIFFICULTY"
    }
}