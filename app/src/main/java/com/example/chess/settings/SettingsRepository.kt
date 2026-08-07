package com.example.chess.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val SOUND_VOLUME = floatPreferencesKey("sound_volume")
        val THEME = stringPreferencesKey("theme")
        val BOARD_COLORS = stringPreferencesKey("board_colors")
        val PIECE_STYLE = stringPreferencesKey("piece_style")
        val ANIMATION_SPEED = floatPreferencesKey("animation_speed")
        val BOARD_ROTATION = booleanPreferencesKey("board_rotation")
        val COORDINATES = booleanPreferencesKey("coordinates")
        val LEGAL_MOVE_HIGHLIGHT = booleanPreferencesKey("legal_move_highlight")
        val LANGUAGE = stringPreferencesKey("language")
    }

    val hapticsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[HAPTICS_ENABLED] ?: true
    }

    val soundEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SOUND_ENABLED] ?: true
    }

    val soundVolume: Flow<Float> = dataStore.data.map { preferences ->
        preferences[SOUND_VOLUME] ?: 1.0f
    }

    val theme: Flow<String> = dataStore.data.map { preferences ->
        preferences[THEME] ?: "System"
    }

    val boardColors: Flow<String> = dataStore.data.map { preferences ->
        preferences[BOARD_COLORS] ?: "Wood"
    }

    val pieceStyle: Flow<String> = dataStore.data.map { preferences ->
        preferences[PIECE_STYLE] ?: "Classic"
    }

    val animationSpeed: Flow<Float> = dataStore.data.map { preferences ->
        preferences[ANIMATION_SPEED] ?: 1.0f
    }

    val boardRotation: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[BOARD_ROTATION] ?: false
    }

    val coordinates: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[COORDINATES] ?: true
    }

    val legalMoveHighlight: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[LEGAL_MOVE_HIGHLIGHT] ?: true
    }

    val language: Flow<String> = dataStore.data.map { preferences ->
        preferences[LANGUAGE] ?: "English"
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[HAPTICS_ENABLED] = enabled
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SOUND_ENABLED] = enabled
        }
    }

    suspend fun setSoundVolume(volume: Float) {
        dataStore.edit { preferences ->
            preferences[SOUND_VOLUME] = volume
        }
    }

    suspend fun setTheme(theme: String) {
        dataStore.edit { preferences -> preferences[THEME] = theme }
    }

    suspend fun setBoardColors(boardColors: String) {
        dataStore.edit { preferences -> preferences[BOARD_COLORS] = boardColors }
    }

    suspend fun setPieceStyle(pieceStyle: String) {
        dataStore.edit { preferences -> preferences[PIECE_STYLE] = pieceStyle }
    }

    suspend fun setAnimationSpeed(speed: Float) {
        dataStore.edit { preferences -> preferences[ANIMATION_SPEED] = speed }
    }

    suspend fun setBoardRotation(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[BOARD_ROTATION] = enabled }
    }

    suspend fun setCoordinates(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[COORDINATES] = enabled }
    }

    suspend fun setLegalMoveHighlight(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[LEGAL_MOVE_HIGHLIGHT] = enabled }
    }

    suspend fun setLanguage(language: String) {
        dataStore.edit { preferences -> preferences[LANGUAGE] = language }
    }
}
