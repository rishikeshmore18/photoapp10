package com.example.photoapp10.core.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Global DataStore extension - ensures single instance across entire app
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

object UserPrefsKeys {
    val REMEMBER_DEVICE = booleanPreferencesKey("remember_device")
    val RESTORE_GATE_SHOWN = booleanPreferencesKey("restore_gate_shown")
}

// Make UserPrefs a singleton object to prevent multiple DataStore instances
object UserPrefs {
    
    fun rememberDeviceFlow(context: Context): Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[UserPrefsKeys.REMEMBER_DEVICE] ?: false }

    suspend fun setRememberDevice(context: Context, value: Boolean) {
        context.dataStore.edit { it[UserPrefsKeys.REMEMBER_DEVICE] = value }
    }

    fun restoreGateShownFlow(context: Context): Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[UserPrefsKeys.RESTORE_GATE_SHOWN] ?: false }

    suspend fun setRestoreGateShown(context: Context, value: Boolean) {
        context.dataStore.edit { it[UserPrefsKeys.RESTORE_GATE_SHOWN] = value }
    }
}


