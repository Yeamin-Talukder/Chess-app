package com.example.chess.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameHistoryDao {
    @Query("SELECT * FROM game_history ORDER BY date DESC")
    fun getAllGames(): Flow<List<GameHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameHistoryEntity)

    @Delete
    suspend fun deleteGame(game: GameHistoryEntity)

    @Query("SELECT * FROM game_history WHERE pgn LIKE '%' || :query || '%' OR opening LIKE '%' || :query || '%' OR player1Name LIKE '%' || :query || '%' OR player2Name LIKE '%' || :query || '%' OR winner LIKE '%' || :query || '%' OR result LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchGames(query: String): Flow<List<GameHistoryEntity>>

    @Query("SELECT * FROM game_history WHERE id = :id LIMIT 1")
    suspend fun getGameById(id: Long): GameHistoryEntity?
}
