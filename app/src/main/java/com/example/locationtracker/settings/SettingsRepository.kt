package com.example.locationtracker.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    val trackingIntervalMs: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[TRACKING_INTERVAL_MS] ?: DEFAULT_INTERVAL_MS
    }

    val darkMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[DARK_MODE] ?: false
    }

    suspend fun setTrackingInterval(ms: Long) {
        context.dataStore.edit { it[TRACKING_INTERVAL_MS] = ms }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[DARK_MODE] = enabled }
    }

    companion object {
        const val DEFAULT_INTERVAL_MS = 10_000L
        private val TRACKING_INTERVAL_MS = longPreferencesKey("tracking_interval_ms")
        private val DARK_MODE = booleanPreferencesKey("dark_mode")
    }
}
