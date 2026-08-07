package com.example.chess.game.engine

enum class BotLevel(
    val title: String,
    val elo: Int,
    val skillLevel: Int, // Stockfish skill level (0-20)
    val depth: Int,      // Max search depth
    val moveTimeMs: Int, // Max thinking time in ms
    val avatar: String,
    val description: String
) {
    BEGINNER("Beginner", 600, 0, 1, 100, "🐣", "Makes quick, simple moves. Perfect for learning."),
    CASUAL("Casual", 1000, 3, 2, 250, "🌱", "Fun & relaxed opponent with occasional mistakes."),
    INTERMEDIATE("Intermediate", 1500, 8, 5, 500, "⚔️", "Solid tactical play with good opening sense."),
    ADVANCED("Advanced", 2000, 14, 10, 1000, "🧠", "Strong engine evaluation and deep calculation."),
    MASTER("Master", 2500, 18, 15, 1500, "🔥", "Near grandmaster strength with sharp tactics."),
    GRANDMASTER("Grandmaster", 3000, 20, 24, 3000, "👑", "Stockfish peak performance. Maximum challenge.")
}
