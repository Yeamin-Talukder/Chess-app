package com.example.chess.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.database.UserProfileEntity
import com.example.chess.profile.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.chess.repository.PuzzleRepository

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: UserProfileRepository,
    private val puzzleRepository: PuzzleRepository
) : ViewModel() {

    private val _profile = MutableStateFlow(UserProfileEntity())
    val profile: StateFlow<UserProfileEntity> = _profile.asStateFlow()

    val puzzleRating = puzzleRepository.userRating
    val puzzleBestStreak = puzzleRepository.bestStreak
    val puzzleCurrentStreak = puzzleRepository.currentStreak
    val puzzleSolvedCount = puzzleRepository.solvedCount
    val puzzleFailedCount = puzzleRepository.failedCount

    init {
        viewModelScope.launch {
            repository.profile.collectLatest { entity ->
                if (entity != null) {
                    _profile.value = entity
                } else {
                    // Initialize if empty
                    repository.updateProfile(UserProfileEntity())
                }
            }
        }
    }

    fun updateProfileInfo(username: String, country: String, avatar: String) {
        val current = _profile.value
        val updated = current.copy(
            username = username,
            country = country,
            avatar = avatar
        )
        viewModelScope.launch {
            repository.updateProfile(updated)
        }
    }
}
