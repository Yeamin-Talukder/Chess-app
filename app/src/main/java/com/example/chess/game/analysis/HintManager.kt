package com.example.chess.game.analysis

import com.example.chess.game.board.BoardState
import com.example.chess.game.engine.EngineRepository
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.takeWhile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HintManager @Inject constructor(
    private val engineRepository: EngineRepository
) {
    suspend fun getHint(fen: String, fallbackState: BoardState): EngineAnalysis? {
        // Collect flow until depth 20 or it finishes
        val flow = engineRepository.getAnalysisFlow(fen, fallbackState, 20)
        return flow.takeWhile { it.depth <= 20 }.lastOrNull()
    }
}
