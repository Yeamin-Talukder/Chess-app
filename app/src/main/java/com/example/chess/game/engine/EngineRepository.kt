package com.example.chess.game.engine

import com.example.chess.game.analysis.EngineAnalysis
import com.example.chess.game.board.BoardState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EngineRepository @Inject constructor(
    private val engineManager: UciEngineManager
) {

    suspend fun getBestMove(fen: String, level: BotLevel, fallbackState: BoardState): String? {
        return engineManager.getBestMove(fen, level, fallbackState)
    }

    fun getAnalysisFlow(fen: String, fallbackState: BoardState, searchDepth: Int): Flow<EngineAnalysis> {
        return engineManager.getEvaluationFlow(fen, fallbackState, searchDepth)
    }

    suspend fun getEvaluation(fen: String, fallbackState: BoardState): Float {
        return engineManager.getEvaluation(fen, fallbackState)
    }

    suspend fun initialize() {
        engineManager.initialize()
    }

    fun shutdown() {
        engineManager.shutdown()
    }
}
