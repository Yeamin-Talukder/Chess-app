package com.example.chess.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "puzzle_progress")
data class PuzzleProgressEntity(
    @PrimaryKey val puzzleId: String,
    val isSolved: Boolean = false,
    val isFailed: Boolean = false,
    val isFavorite: Boolean = false,
    val timeTakenMs: Long = 0,
    val bestTimeMs: Long = 0,
    val attempts: Int = 1,
    val hintsUsed: Int = 0,
    val puzzleRating: Int = 0,    // Snapshot of puzzle rating at time of play
    val playedAt: Long = System.currentTimeMillis()
)
