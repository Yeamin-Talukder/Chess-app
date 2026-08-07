package com.example.chess.ui.screens.analysis

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.database.GameHistoryEntity
import com.example.chess.game.analysis.EngineAnalysis
import com.example.chess.game.analysis.MoveClassification
import com.example.chess.game.analysis.MoveClassifier
import com.example.chess.game.board.BoardState
import com.example.chess.game.board.Move
import com.example.chess.game.board.PieceColor
import com.example.chess.game.board.Square
import com.example.chess.game.engine.ChessEngine
import com.example.chess.game.engine.EngineRepository
import com.example.chess.history.GameHistoryRepository
import com.example.chess.utils.FenGenerator
import com.example.chess.utils.HapticManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val historyRepository: GameHistoryRepository,
    private val hapticManager: HapticManager,
    private val engineRepository: EngineRepository
) : ViewModel() {

    private val engine = ChessEngine(hapticManager, null)
    val boardState = engine.boardState

    private val _gameEntity = MutableStateFlow<GameHistoryEntity?>(null)
    val gameEntity: StateFlow<GameHistoryEntity?> = _gameEntity.asStateFlow()

    private val _currentMoveIndex = MutableStateFlow(0)
    val currentMoveIndex: StateFlow<Int> = _currentMoveIndex.asStateFlow()

    private val _totalMoves = MutableStateFlow(0)
    val totalMoves: StateFlow<Int> = _totalMoves.asStateFlow()

    private val _currentAnalysis = MutableStateFlow<EngineAnalysis?>(null)
    val currentAnalysis: StateFlow<EngineAnalysis?> = _currentAnalysis.asStateFlow()

    private val _moveClassification = MutableStateFlow<MoveClassification?>(null)
    val moveClassification: StateFlow<MoveClassification?> = _moveClassification.asStateFlow()
    
    // We can store previous evaluations to correctly classify the move
    // evalCache[moveIndex] = engine eval BEFORE making moveIndex
    private val evalCache = mutableMapOf<Int, EngineAnalysis>()

    private var moves: List<String> = emptyList() // UCI moves

    init {
        val gameId = savedStateHandle.get<Long>("gameId")
        if (gameId != null) {
            viewModelScope.launch {
                val game = historyRepository.getGameById(gameId)
                if (game != null) {
                    _gameEntity.value = game
                    parsePgnToMoves(game.pgn)
                }
            }
        }
        
        viewModelScope.launch {
            boardState.collectLatest { state ->
                val fen = FenGenerator.generateFen(state)
                val idx = _currentMoveIndex.value
                
                // Reset classification while computing
                _moveClassification.value = null
                _currentAnalysis.value = null
                
                engineRepository.getAnalysisFlow(fen, state, 22).collect { analysis ->
                    _currentAnalysis.value = analysis
                    evalCache[idx] = analysis
                    
                    // Once we have at least depth 10, try to classify the *previous* move
                    if (analysis.depth >= 10 && idx > 0) {
                        val beforeEval = evalCache[idx - 1]
                        if (beforeEval != null) {
                            val isWhiteMove = idx % 2 != 0 // If index is 1, white made the 1st move (index 0)
                            _moveClassification.value = MoveClassifier.classifyMoveWithMate(
                                beforeMate = beforeEval.mate,
                                afterMate = analysis.mate,
                                beforeEval = beforeEval.evaluation,
                                afterEval = analysis.evaluation,
                                isWhiteTurn = isWhiteMove
                            )
                        }
                    }
                }
            }
        }
    }

    private fun parsePgnToMoves(pgn: String) {
        val lines = pgn.lines()
        val movesLine = lines.lastOrNull { it.isNotBlank() && !it.trimStart().startsWith("[") } ?: pgn
        
        val cleaned = movesLine.replace(Regex("\\d+\\.\\s*"), "")
            .replace("1-0", "")
            .replace("0-1", "")
            .replace("1/2-1/2", "")
            .replace("*", "")
            .trim()

        if (cleaned.isNotEmpty()) {
            moves = cleaned.split("\\s+".toRegex()).filter { it.length >= 4 }
            _totalMoves.value = moves.size
        }
    }

    fun nextMove() {
        if (_currentMoveIndex.value < _totalMoves.value) {
            val uci = moves[_currentMoveIndex.value]
            applyUciMove(uci)
            _currentMoveIndex.value++
        }
    }

    fun prevMove() {
        if (_currentMoveIndex.value > 0) {
            _currentMoveIndex.value--
            engine.undo()
        }
    }

    fun seekToStart() {
        seekTo(0)
    }

    fun seekToEnd() {
        seekTo(_totalMoves.value)
    }

    fun seekTo(index: Int) {
        if (index in 0.._totalMoves.value) {
            if (index == _currentMoveIndex.value) return
            engine.restart()
            for (i in 0 until index) {
                applyUciMove(moves[i])
            }
            _currentMoveIndex.value = index
        }
    }

    private fun applyUciMove(uci: String) {
        val fromSquare = Square.fromNotation(uci.substring(0, 2))
        val toSquare = Square.fromNotation(uci.substring(2, 4))

        if (fromSquare != null && toSquare != null) {
            engine.selectSquare(fromSquare)
            engine.selectSquare(toSquare)
            if (uci.length > 4) {
                // Promotion
                val prom = when (uci[4]) {
                    'q' -> com.example.chess.game.board.PieceType.QUEEN
                    'r' -> com.example.chess.game.board.PieceType.ROOK
                    'b' -> com.example.chess.game.board.PieceType.BISHOP
                    'n' -> com.example.chess.game.board.PieceType.KNIGHT
                    else -> com.example.chess.game.board.PieceType.QUEEN
                }
                engine.applyPromotion(prom)
            }
        }
    }
}
