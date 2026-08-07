package com.example.chess.profile

import com.example.chess.database.UserProfileDao
import com.example.chess.database.UserProfileEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepository @Inject constructor(
    private val userProfileDao: UserProfileDao
) {
    val profile: Flow<UserProfileEntity?> = userProfileDao.getProfile()

    suspend fun getProfileSync(): UserProfileEntity {
        return userProfileDao.getProfileSync() ?: UserProfileEntity()
    }

    suspend fun updateProfile(entity: UserProfileEntity) {
        userProfileDao.insertOrUpdateProfile(entity)
    }

    suspend fun incrementStats(
        isWin: Boolean,
        isLoss: Boolean,
        isDraw: Boolean,
        playTimeMs: Long
    ) {
        val current = getProfileSync()
        
        val newGamesPlayed = current.gamesPlayed + 1
        val newWins = current.wins + (if (isWin) 1 else 0)
        val newLosses = current.losses + (if (isLoss) 1 else 0)
        val newDraws = current.draws + (if (isDraw) 1 else 0)
        val newPlayTime = current.totalPlayTime + playTimeMs
        
        val newWinRate = if (newGamesPlayed > 0) {
            (newWins.toFloat() / newGamesPlayed.toFloat()) * 100f
        } else 0f

        val updated = current.copy(
            gamesPlayed = newGamesPlayed,
            wins = newWins,
            losses = newLosses,
            draws = newDraws,
            winRate = newWinRate,
            totalPlayTime = newPlayTime
        )
        
        updateProfile(updated)
    }
}
