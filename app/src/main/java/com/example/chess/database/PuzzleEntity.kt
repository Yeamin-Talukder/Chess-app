package com.example.chess.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "puzzles",
    indices = [
        Index(value = ["rating"]),
        Index(value = ["themes"]),
        Index(value = ["rating", "themes"])
    ]
)
data class PuzzleEntity(
    @PrimaryKey val puzzleId: String,
    val fen: String,
    val moves: String,          // UCI moves space-separated, e.g. "e2e4 e7e5 g1f3"
    val rating: Int,
    val ratingDeviation: Int,
    val popularity: Int,
    val nbPlays: Int,
    val themes: String,         // Space-separated themes, e.g. "fork pin middlegame"
    val gameUrl: String,
    val openingTags: String = "" // Lichess opening tags, e.g. "Italian_Game Italian_Game_Two_Knights_Defense"
)
