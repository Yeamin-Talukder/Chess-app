package com.example.chess.database

data class PuzzleWithProgress(
    val puzzleId: String,
    val fen: String,
    val moves: String,
    val rating: Int,
    val ratingDeviation: Int,
    val popularity: Int,
    val nbPlays: Int,
    val themes: String,
    val gameUrl: String,
    val openingTags: String?,
    val isSolved: Boolean?,
    val isFailed: Boolean?,
    val isFavorite: Boolean?,
    val timeTakenMs: Long?,
    val bestTimeMs: Long?,
    val attempts: Int?,
    val hintsUsed: Int?,
    val puzzleRating: Int?,
    val playedAt: Long?
)
