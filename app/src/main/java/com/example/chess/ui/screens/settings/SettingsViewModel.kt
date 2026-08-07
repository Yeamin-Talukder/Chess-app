package com.example.chess.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val hapticsEnabled: StateFlow<Boolean> = settingsRepository.hapticsEnabled
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    val soundEnabled: StateFlow<Boolean> = settingsRepository.soundEnabled
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    val soundVolume: StateFlow<Float> = settingsRepository.soundVolume
        .stateIn(viewModelScope, SharingStarted.Lazily, 1.0f)

    val theme: StateFlow<String> = settingsRepository.theme
        .stateIn(viewModelScope, SharingStarted.Lazily, "System")

    val boardColors: StateFlow<String> = settingsRepository.boardColors
        .stateIn(viewModelScope, SharingStarted.Lazily, "Wood")

    val pieceStyle: StateFlow<String> = settingsRepository.pieceStyle
        .stateIn(viewModelScope, SharingStarted.Lazily, "Classic")

    val animationSpeed: StateFlow<Float> = settingsRepository.animationSpeed
        .stateIn(viewModelScope, SharingStarted.Lazily, 1.0f)

    val boardRotation: StateFlow<Boolean> = settingsRepository.boardRotation
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val coordinates: StateFlow<Boolean> = settingsRepository.coordinates
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    val legalMoveHighlight: StateFlow<Boolean> = settingsRepository.legalMoveHighlight
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    val language: StateFlow<String> = settingsRepository.language
        .stateIn(viewModelScope, SharingStarted.Lazily, "English")

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHapticsEnabled(enabled)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSoundEnabled(enabled)
        }
    }

    fun setSoundVolume(volume: Float) {
        viewModelScope.launch {
            settingsRepository.setSoundVolume(volume)
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { settingsRepository.setTheme(theme) }
    }

    fun setBoardColors(colors: String) {
        viewModelScope.launch { settingsRepository.setBoardColors(colors) }
    }

    fun setPieceStyle(style: String) {
        viewModelScope.launch { settingsRepository.setPieceStyle(style) }
    }

    fun setAnimationSpeed(speed: Float) {
        viewModelScope.launch { settingsRepository.setAnimationSpeed(speed) }
    }

    fun setBoardRotation(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setBoardRotation(enabled) }
    }

    fun setCoordinates(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setCoordinates(enabled) }
    }

    fun setLegalMoveHighlight(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setLegalMoveHighlight(enabled) }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch { settingsRepository.setLanguage(language) }
    }
}
