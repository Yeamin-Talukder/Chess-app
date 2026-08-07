package com.example.chess.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_history")
data class GameHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long,
    val winner: String,
    val duration: Long,
    val moveCount: Int,
    val pgn: String,
    val opening: String,
    val result: String,
    val isMultiplayer: Boolean,
    val player1Name: String = "Player 1",
    val player2Name: String = "Player 2"
)
