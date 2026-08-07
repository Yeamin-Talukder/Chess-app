package com.example.chess.history

import com.example.chess.database.GameHistoryDao
import com.example.chess.database.GameHistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameHistoryRepository @Inject constructor(
    private val gameHistoryDao: GameHistoryDao
) {
    fun getAllGames(): Flow<List<GameHistoryEntity>> {
        return gameHistoryDao.getAllGames()
    }

    suspend fun insertGame(game: GameHistoryEntity) {
        gameHistoryDao.insertGame(game)
    }

    suspend fun deleteGame(game: GameHistoryEntity) {
        gameHistoryDao.deleteGame(game)
    }

    fun searchGames(query: String): Flow<List<GameHistoryEntity>> {
        return gameHistoryDao.searchGames(query)
    }

    suspend fun getGameById(id: Long): GameHistoryEntity? {
        return gameHistoryDao.getGameById(id)
    }
}
