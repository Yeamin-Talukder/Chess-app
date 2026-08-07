package com.example.chess.ui.screens.replay

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.database.GameHistoryEntity
import com.example.chess.game.board.PieceType
import com.example.chess.game.board.PieceColor
import com.example.chess.game.board.Square
import com.example.chess.game.board.Move
import com.example.chess.game.board.ChessPiece
import com.example.chess.game.board.BoardState
import com.example.chess.game.engine.ChessEngine
import com.example.chess.utils.FenGenerator
import com.example.chess.game.engine.EngineRepository
import com.example.chess.history.GameHistoryRepository
import com.example.chess.utils.HapticManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReplayViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val historyRepository: GameHistoryRepository,
    private val hapticManager: HapticManager,
    private val engineRepository: EngineRepository
) : ViewModel() {

    private val engine = ChessEngine(hapticManager, null) // No sound during fast replay to avoid overlap
    val boardState = engine.boardState

    private val _gameEntity = MutableStateFlow<GameHistoryEntity?>(null)
    val gameEntity: StateFlow<GameHistoryEntity?> = _gameEntity.asStateFlow()

    private val _currentMoveIndex = MutableStateFlow(0)
    val currentMoveIndex: StateFlow<Int> = _currentMoveIndex.asStateFlow()

    private val _totalMoves = MutableStateFlow(0)
    val totalMoves: StateFlow<Int> = _totalMoves.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _whiteCaptured = MutableStateFlow<List<Pair<PieceType, Int>>>(emptyList())
    val whiteCaptured: StateFlow<List<Pair<PieceType, Int>>> = _whiteCaptured.asStateFlow()

    private val _blackCaptured = MutableStateFlow<List<Pair<PieceType, Int>>>(emptyList())
    val blackCaptured: StateFlow<List<Pair<PieceType, Int>>> = _blackCaptured.asStateFlow()

    private val _moveHistory = MutableStateFlow<List<String>>(emptyList())
    val moveHistory: StateFlow<List<String>> = _moveHistory.asStateFlow()

    private val _evaluationScore = MutableStateFlow(0f)
    val evaluationScore: StateFlow<Float> = _evaluationScore.asStateFlow()

    private val _evaluationDepth = MutableStateFlow(0)
    val evaluationDepth: StateFlow<Int> = _evaluationDepth.asStateFlow()

    private var moves: List<String> = emptyList() // UCI notation moves, e.g. "e2e4"
    private var playbackJob: Job? = null

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
            boardState.collect { state ->
                if (state.moveHistory.size != _moveHistory.value.size) {
                    recomputeHistory(state.moveHistory)
                    updateCaptures(state)
                }
            }
        }

        viewModelScope.launch {
            engineRepository.initialize()
            // Live eval depth is 15 for replay
            boardState.collectLatest { state ->
                val fen = FenGenerator.generateFen(state)
                engineRepository.getAnalysisFlow(fen, state, 15).collect { analysis ->
                    _evaluationScore.value = analysis.evaluation
                    _evaluationDepth.value = analysis.depth
                }
            }
        }
    }
    
    private fun piecePoints(type: PieceType): Int = when (type) {
        PieceType.PAWN   -> 1
        PieceType.KNIGHT -> 3
        PieceType.BISHOP -> 3
        PieceType.ROOK   -> 5
        PieceType.QUEEN  -> 9
        PieceType.KING   -> 0
    }
    
    private fun recomputeHistory(engineMoves: List<Move>) {
        var simPieces = BoardState.initial().pieces.toMutableMap()
        val newMoveHistory = mutableListOf<String>()

        for (move in engineMoves) {
            val movingPiece = simPieces[move.from] ?: continue
            
            val label = if (move.isKingsideCastle) "O-O"
            else if (move.isQueensideCastle) "O-O-O"
            else "${movingPiece.unicode}${move.to.notation}"
            
            newMoveHistory.add(label)

            simPieces[move.to] = if (move.promotion != null) movingPiece.copy(type = move.promotion) else movingPiece
        }

        _moveHistory.value = newMoveHistory
    }

    private fun updateCaptures(state: BoardState) {
        val wCaptured = state.capturedPieces.filter { it.color == PieceColor.BLACK }
            .groupBy { it.type }
            .map { it.key to it.value.size }

        val bCaptured = state.capturedPieces.filter { it.color == PieceColor.WHITE }
            .groupBy { it.type }
            .map { it.key to it.value.size }

        _whiteCaptured.value = wCaptured
        _blackCaptured.value = bCaptured
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

    fun playPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    private fun play() {
        if (_currentMoveIndex.value >= _totalMoves.value) return
        _isPlaying.value = true
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            while (_currentMoveIndex.value < _totalMoves.value && _isPlaying.value) {
                nextMove()
                delay(800)
            }
            _isPlaying.value = false
        }
    }

    private fun pause() {
        _isPlaying.value = false
        playbackJob?.cancel()
    }

    fun nextMove() {
        if (_currentMoveIndex.value < _totalMoves.value) {
            val uci = moves[_currentMoveIndex.value]
            applyUciMove(uci)
            _currentMoveIndex.value++
        } else {
            pause()
        }
    }

    fun prevMove() {
        if (_currentMoveIndex.value > 0) {
            pause() // Stop playback if manually stepping back
            _currentMoveIndex.value--
            engine.undo()
        }
    }

    fun seekTo(index: Int) {
        pause()
        if (index in 0.._totalMoves.value) {
            if (index == _currentMoveIndex.value) return
            
            // Rebuild from scratch (more robust than undoing many times)
            engine.restart()
            for (i in 0 until index) {
                applyUciMove(moves[i])
            }
            _currentMoveIndex.value = index
        }
    }

    fun seekToStart() {
        seekTo(0)
    }

    fun seekToEnd() {
        seekTo(_totalMoves.value)
    }

    private fun applyUciMove(uci: String) {
        // e.g. e2e4 or e7e8q
        val fromNotation = uci.substring(0, 2)
        val toNotation = uci.substring(2, 4)
        
        val fromSquare = Square.fromNotation(fromNotation)
        val toSquare = Square.fromNotation(toNotation)

        if (fromSquare != null && toSquare != null) {
            engine.selectSquare(fromSquare)
            engine.selectSquare(toSquare)
            
            if (uci.length > 4) {
                // Promotion!
                val prom = uci[4]
                val type = when (prom) {
                    'q' -> PieceType.QUEEN
                    'r' -> PieceType.ROOK
                    'b' -> PieceType.BISHOP
                    'n' -> PieceType.KNIGHT
                    else -> PieceType.QUEEN
                }
                engine.applyPromotion(type)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
        engineRepository.shutdown()
    }
}
