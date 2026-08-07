package com.example.chess.game.analysis

data class EngineAnalysis(
    val evaluation: Float,
    val mate: Int?,
    val bestMove: String,
    val principalVariation: List<String>,
    val depth: Int,
    val nodes: Long,
    val nps: Long
)
