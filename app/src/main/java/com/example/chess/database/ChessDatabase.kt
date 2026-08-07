package com.example.chess.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        GameHistoryEntity::class, 
        UserProfileEntity::class,
        PuzzleEntity::class,
        PuzzleProgressEntity::class
    ], 
    version = 6, 
    exportSchema = false
)
abstract class ChessDatabase : RoomDatabase() {
    abstract fun gameHistoryDao(): GameHistoryDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun puzzleDao(): PuzzleDao
}
