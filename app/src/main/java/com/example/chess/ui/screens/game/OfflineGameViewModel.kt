package com.example.chess.ui.screens.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.database.GameHistoryEntity
import com.example.chess.game.board.GameStatus
import com.example.chess.game.board.BoardState
import com.example.chess.game.board.Move
import com.example.chess.game.board.PieceColor
import com.example.chess.game.board.PieceType
import com.example.chess.game.board.Square
import com.example.chess.game.board.ChessPiece
import com.example.chess.game.engine.ChessEngine
import com.example.chess.history.GameHistoryRepository
import com.example.chess.profile.UserProfileRepository
import com.example.chess.utils.HapticManager
import com.example.chess.utils.SoundManager
import com.example.chess.utils.PgnGenerator
import com.example.chess.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.chess.game.engine.EngineRepository
import com.example.chess.utils.FenGenerator
import com.example.chess.game.analysis.HintManager
import com.example.chess.ui.screens.game.GameConfig
import kotlinx.coroutines.flow.collectLatest

/** Piece point values for capture scoring. */
fun piecePoints(type: PieceType): Int = when (type) {
    PieceType.PAWN   -> 1
    PieceType.KNIGHT -> 3
    PieceType.BISHOP -> 3
    PieceType.ROOK   -> 5
    PieceType.QUEEN  -> 9
    PieceType.KING   -> 0
}

@HiltViewModel
class OfflineGameViewModel @Inject constructor(
    private val hapticManager: HapticManager,
    private val soundManager: SoundManager,
    private val historyRepository: GameHistoryRepository,
    private val userProfileRepository: UserProfileRepository,
    private val settingsRepository: SettingsRepository,
    private val engineRepository: EngineRepository,
    private val hintManager: HintManager
) : ViewModel() {

    private val engine = ChessEngine(hapticManager, soundManager)

    // Holds the selected game configuration before the game starts
    private var currentConfig: GameConfig? = null

    val boardState = engine.boardState
    val promotionRequest = engine.promotionRequest
    val boardRotation = settingsRepository.boardRotation

    // --- Review Mode ---
    val reviewIndex = engine.reviewIndex
    val reviewBoardState = engine.reviewBoardState
    // -------------------

    // --- Move history (SAN-style labels e.g. "e4", "e5", …) ---
    private val _moveHistory = MutableStateFlow<List<String>>(emptyList())
    val moveHistory: StateFlow<List<String>> = _moveHistory.asStateFlow()

    // --- Captured pieces per side:
    //     key = color of the player who MADE the capture (i.e., who owns the captured pieces)
    //     value = list of (capturedPieceType, pointValue) ---
    private val _whiteCaptured = MutableStateFlow<List<Pair<PieceType, Int>>>(emptyList())
    val whiteCaptured: StateFlow<List<Pair<PieceType, Int>>> = _whiteCaptured.asStateFlow()

    private val _blackCaptured = MutableStateFlow<List<Pair<PieceType, Int>>>(emptyList())
    val blackCaptured: StateFlow<List<Pair<PieceType, Int>>> = _blackCaptured.asStateFlow()

    // --- The local player's color (determined by game config) ---
    private val _localColor = MutableStateFlow(PieceColor.WHITE)
    val localColor: StateFlow<PieceColor> = _localColor.asStateFlow()

    // --- Player display names ---
    private val _player1Name = MutableStateFlow("Player 1")
    val player1Name: StateFlow<String> = _player1Name.asStateFlow()

    private val _player2Name = MutableStateFlow("Player 2")
    val player2Name: StateFlow<String> = _player2Name.asStateFlow()

    // --- Expose profile username so Setup Dialog can pre-fill ---
    val profileUsername = userProfileRepository.profile

    private var gameStartTime = System.currentTimeMillis()
    private var gameSaved = false

    private val _evaluationScore = MutableStateFlow(0f)
    val evaluationScore: StateFlow<Float> = _evaluationScore.asStateFlow()

    private val _evaluationDepth = MutableStateFlow(0)
    val evaluationDepth = _evaluationDepth.asStateFlow()

    private val _hintMove = MutableStateFlow<com.example.chess.game.analysis.EngineAnalysis?>(null)
    val hintMove = _hintMove.asStateFlow()

    // Keep a snapshot of pieces from the previous state to detect captures
    private var previousPieces = boardState.value.pieces.toMap()

    init {
        viewModelScope.launch {
            boardState.collect { state ->
                // Only recompute if the number of moves has changed
                if (state.moveHistory.size != _moveHistory.value.size) {
                    recomputeHistory(state.moveHistory)
                    updateCaptures(state)
                    _hintMove.value = null
                }

                // Save game on terminal state
                if (state.gameStatus != GameStatus.ONGOING && !gameSaved) {
                    saveGame(state)
                    gameSaved = true
                } else if (state.gameStatus == GameStatus.ONGOING) {
                    gameSaved = false
                }
            }
        }

        viewModelScope.launch {
            engineRepository.initialize()
            // Live eval depth is 15
            reviewBoardState.collectLatest { state ->
                if (state != null) {
                    val fen = FenGenerator.generateFen(state)
                    engineRepository.getAnalysisFlow(fen, state, 15).collect { analysis ->
                        _evaluationScore.value = analysis.evaluation
                        _evaluationDepth.value = analysis.depth
                    }
                } else {
                    _evaluationScore.value = 0f
                    _evaluationDepth.value = 0
                }
            }
        }
    }

    fun requestHint() {
        if (boardState.value.gameStatus != GameStatus.ONGOING) return
        viewModelScope.launch {
            val fen = FenGenerator.generateFen(boardState.value)
            val hint = hintManager.getHint(fen, boardState.value)
            _hintMove.value = hint
        }
    }

    private fun saveGame(state: com.example.chess.game.board.BoardState) {
        viewModelScope.launch {
            val resultStr = when (state.gameStatus) {
                GameStatus.CHECKMATE -> if (state.turn == PieceColor.WHITE) "0-1" else "1-0"
                GameStatus.RESIGNED -> if (state.turn == PieceColor.WHITE) "0-1" else "1-0"
                GameStatus.DRAW_AGREED, GameStatus.DRAW_FIFTY_MOVE,
                GameStatus.DRAW_REPETITION, GameStatus.DRAW_INSUFFICIENT_MATERIAL,
                GameStatus.STALEMATE -> "1/2-1/2"
                else -> "*"
            }

            val winnerStr = when (resultStr) {
                "1-0"     -> "White"
                "0-1"     -> "Black"
                "1/2-1/2" -> "Draw"
                else      -> "Unknown"
            }

            val pgn = PgnGenerator.generatePgn(
                moves = state.moveHistory,
                result = resultStr,
                date = gameStartTime
            )

            val duration = System.currentTimeMillis() - gameStartTime

            val entity = GameHistoryEntity(
                date = gameStartTime,
                winner = winnerStr,
                duration = duration,
                moveCount = state.moveHistory.size,
                pgn = pgn,
                opening = "Custom",
                result = resultStr,
                isMultiplayer = false,
                player1Name = _player1Name.value,
                player2Name = _player2Name.value
            )

            historyRepository.insertGame(entity)

            val isWin  = resultStr == "1-0"
            val isLoss = resultStr == "0-1"
            val isDraw = resultStr == "1/2-1/2"
            userProfileRepository.incrementStats(isWin, isLoss, isDraw, duration)
        }
    }

    fun onSquareClick(square: Square) {
        engine.selectSquare(square)
    }

    fun onPromotionSelected(type: PieceType) {
        engine.applyPromotion(type)
    }

    fun onPromotionCanceled() {
        engine.cancelPromotion()
    }

    fun undo() {
        engine.undo()
    }

    fun resign() {
        val color = boardState.value.turn
        engine.resign(color)
    }

    fun offerDraw() {
        engine.offerDraw()
    }

    // ── Review Mode Functions ───────────────────────────────────────────
    fun startReview() = engine.startReview()
    fun exitReview() = engine.exitReview()
    fun reviewPrevious() = engine.reviewPrevious()
    fun reviewNext() = engine.reviewNext()
    fun reviewFirst() = engine.reviewFirst()
    fun reviewLast() = engine.reviewLast()
    // ────────────────────────────────────────────────────────────────────

    fun restart() {
        gameStartTime = System.currentTimeMillis()
        gameSaved = false
        currentConfig = null
        _moveHistory.value = emptyList()
        _whiteCaptured.value = emptyList()
        _blackCaptured.value = emptyList()
        previousPieces = engine.boardState.value.pieces.toMap()
        engine.restart()
    }

    /**
     * Initialise the game with the provided configuration.
     */
    fun startGame(config: GameConfig) {
        currentConfig = config

        // Determine local color
        val chosenColor = when (config.playerColorChoice) {
            PlayerColorChoice.WHITE  -> PieceColor.WHITE
            PlayerColorChoice.BLACK  -> PieceColor.BLACK
            PlayerColorChoice.RANDOM -> if ((0..1).random() == 0) PieceColor.WHITE else PieceColor.BLACK
        }
        _localColor.value = chosenColor

        // Store names
        _player1Name.value = config.player1Name
        _player2Name.value = config.player2Name

        // Apply time control with increment
        engine.setTimeControl(config.timeMillis, config.timeMillis, config.incrementMillis)

        // Reset state
        _moveHistory.value = emptyList()
        _whiteCaptured.value = emptyList()
        _blackCaptured.value = emptyList()
        previousPieces = engine.boardState.value.pieces.toMap()
    }

    /** Recompute move history labels and captured pieces from full move history. */
    private fun recomputeHistory(moves: List<Move>) {
        var simPieces = com.example.chess.game.board.BoardState.initial().pieces.toMutableMap()
        val wCaptured = mutableListOf<Pair<PieceType, Int>>()
        val bCaptured = mutableListOf<Pair<PieceType, Int>>()
        val newMoveHistory = mutableListOf<String>()

        for (move in moves) {
            val movingPiece = simPieces[move.from] ?: continue
            
            val label = if (move.isKingsideCastle) "O-O"
            else if (move.isQueensideCastle) "O-O-O"
            else "${movingPiece.unicode}${move.to.notation}"
            
            newMoveHistory.add(label)

            if (move.isEnPassant) {
                val epRank = if (movingPiece.color == PieceColor.WHITE) move.to.rank + 1 else move.to.rank - 1
                simPieces.remove(Square(move.to.file, epRank))
            } else {
                simPieces.remove(move.to)
            }
            simPieces.remove(move.from)
            
            if (move.isKingsideCastle) {
                val rFrom = Square(7, move.from.rank)
                val rTo = Square(5, move.from.rank)
                simPieces[rTo] = simPieces.remove(rFrom)!!
            } else if (move.isQueensideCastle) {
                val rFrom = Square(0, move.from.rank)
                val rTo = Square(3, move.from.rank)
                simPieces[rTo] = simPieces.remove(rFrom)!!
            }
            
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

    override fun onCleared() {
        super.onCleared()
        engineRepository.shutdown()
    }
}
