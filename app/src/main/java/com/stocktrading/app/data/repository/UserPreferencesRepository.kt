package com.stocktrading.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    
    companion object {
        private val THEME_KEY = stringPreferencesKey("theme_mode")
        private val FIRST_LAUNCH_KEY = booleanPreferencesKey("is_first_launch")
        private val AUTO_REFRESH_KEY = booleanPreferencesKey("auto_refresh_enabled")
        private val REFRESH_INTERVAL_KEY = stringPreferencesKey("refresh_interval")
        private val LAST_SELECTED_WATCHLIST_KEY = stringPreferencesKey("last_selected_watchlist")
    }
    
    /**
     * Theme preferences
     */
    val themeMode: Flow<String> = dataStore.data.map { preferences ->
        preferences[THEME_KEY] ?: "system" // system, light, dark
    }
    
    suspend fun setThemeMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = mode
        }
    }
    
    /**
     * First launch detection
     */
    val isFirstLaunch: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[FIRST_LAUNCH_KEY] ?: true
    }
    
    suspend fun setFirstLaunchCompleted() {
        dataStore.edit { preferences ->
            preferences[FIRST_LAUNCH_KEY] = false
        }
    }
    
    /**
     * Auto refresh preferences
     */
    val isAutoRefreshEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[AUTO_REFRESH_KEY] ?: true
    }
    
    suspend fun setAutoRefreshEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_REFRESH_KEY] = enabled
        }
    }
    
    /**
     * Refresh interval preferences
     */
    val refreshInterval: Flow<String> = dataStore.data.map { preferences ->
        preferences[REFRESH_INTERVAL_KEY] ?: "5" // minutes
    }
    
    suspend fun setRefreshInterval(interval: String) {
        dataStore.edit { preferences ->
            preferences[REFRESH_INTERVAL_KEY] = interval
        }
    }
    
    /**
     * Last selected watchlist
     */
    val lastSelectedWatchlist: Flow<String?> = dataStore.data.map { preferences ->
        preferences[LAST_SELECTED_WATCHLIST_KEY]
    }
    
    suspend fun setLastSelectedWatchlist(watchlistId: String) {
        dataStore.edit { preferences ->
            preferences[LAST_SELECTED_WATCHLIST_KEY] = watchlistId
        }
    }
}