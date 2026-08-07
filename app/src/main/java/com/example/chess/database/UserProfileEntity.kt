package com.example.chess.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val avatar: String = "G",
    val username: String = "Guest Player",
    val country: String = "Global",
    val gamesPlayed: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val winRate: Float = 0f,
    val favoriteBoard: String = "Default",
    val favoritePieces: String = "Standard",
    val totalPlayTime: Long = 0L // in milliseconds
)
