package com.example.chess.ui.screens.bot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.game.board.BoardState
import com.example.chess.game.board.GameStatus
import com.example.chess.game.board.Move
import com.example.chess.game.board.PieceColor
import com.example.chess.game.board.PieceType
import com.example.chess.game.board.Square
import com.example.chess.game.board.ChessPiece
import com.example.chess.game.engine.BotLevel
import com.example.chess.game.engine.ChessEngine
import com.example.chess.game.engine.EngineRepository
import com.example.chess.game.analysis.HintManager
import com.example.chess.utils.FenGenerator
import com.example.chess.history.GameHistoryRepository
import com.example.chess.profile.UserProfileRepository
import com.example.chess.settings.SettingsRepository
import com.example.chess.utils.HapticManager
import com.example.chess.utils.PgnGenerator
import com.example.chess.utils.SoundManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

fun piecePoints(type: PieceType): Int = when (type) {
    PieceType.PAWN   -> 1
    PieceType.KNIGHT -> 3
    PieceType.BISHOP -> 3
    PieceType.ROOK   -> 5
    PieceType.QUEEN  -> 9
    PieceType.KING   -> 0
}

@HiltViewModel
class BotGameViewModel @Inject constructor(
    private val hapticManager: HapticManager,
    private val soundManager: SoundManager,
    private val profileRepository: UserProfileRepository,
    private val gameHistoryRepository: GameHistoryRepository,
    private val engineRepository: EngineRepository,
    private val hintManager: HintManager
) : ViewModel() {

    private val engine = ChessEngine(hapticManager, soundManager)

    private val _boardState = MutableStateFlow(BoardState.initial())
    val boardState: StateFlow<BoardState> = _boardState.asStateFlow()

    private val _isBotThinking = MutableStateFlow(false)
    val isBotThinking: StateFlow<Boolean> = _isBotThinking.asStateFlow()

    private val _hintMove = MutableStateFlow<com.example.chess.game.analysis.EngineAnalysis?>(null)
    val hintMove = _hintMove.asStateFlow()

    private val _playerColor = MutableStateFlow(PieceColor.WHITE)
    val playerColor: StateFlow<PieceColor> = _playerColor.asStateFlow()

    private val _botLevel = MutableStateFlow(BotLevel.CASUAL)
    val botLevel: StateFlow<BotLevel> = _botLevel.asStateFlow()

    val promotionRequest = engine.promotionRequest

    // Move history text
    private val _moveHistory = MutableStateFlow<List<String>>(emptyList())
    val moveHistory: StateFlow<List<String>> = _moveHistory.asStateFlow()

    private val _whiteCaptured = MutableStateFlow<List<Pair<PieceType, Int>>>(emptyList())
    val whiteCaptured: StateFlow<List<Pair<PieceType, Int>>> = _whiteCaptured.asStateFlow()

    private val _blackCaptured = MutableStateFlow<List<Pair<PieceType, Int>>>(emptyList())
    val blackCaptured: StateFlow<List<Pair<PieceType, Int>>> = _blackCaptured.asStateFlow()

    private var botMoveJob: Job? = null
    private var gameSaved = false
    private var gameStartTime = System.currentTimeMillis()

    init {
        viewModelScope.launch {
            engineRepository.initialize()

            engine.boardState.collect { state ->
                _boardState.value = state
                
                if (state.moveHistory.size != _moveHistory.value.size) {
                    recomputeHistory(state.moveHistory)
                    updateCaptures(state)
                    _hintMove.value = null
                }

                if (state.gameStatus == GameStatus.ONGOING && 
                    state.turn != _playerColor.value && 
                    !_isBotThinking.value) {
                    triggerBotMove(state)
                }

                if (state.gameStatus != GameStatus.ONGOING && !gameSaved && state.moveHistory.isNotEmpty()) {
                    saveGame(state)
                }
            }
        }
    }

    fun startGame(colorChoice: com.example.chess.ui.screens.game.PlayerColorChoice, level: BotLevel) {
        val chosenColor = when (colorChoice) {
            com.example.chess.ui.screens.game.PlayerColorChoice.WHITE  -> PieceColor.WHITE
            com.example.chess.ui.screens.game.PlayerColorChoice.BLACK  -> PieceColor.BLACK
            com.example.chess.ui.screens.game.PlayerColorChoice.RANDOM -> if ((0..1).random() == 0) PieceColor.WHITE else PieceColor.BLACK
        }
        _playerColor.value = chosenColor
        _botLevel.value = level
        gameSaved = false
        gameStartTime = System.currentTimeMillis()
        _moveHistory.value = emptyList()
        _whiteCaptured.value = emptyList()
        _blackCaptured.value = emptyList()
        engine.restart()

        val currentBoard = engine.boardState.value
        if (currentBoard.gameStatus == GameStatus.ONGOING && currentBoard.turn != chosenColor) {
            triggerBotMove(currentBoard)
        }
    }

    fun onSquareClick(square: Square) {
        if (_boardState.value.turn == _playerColor.value && !_isBotThinking.value) {
            engine.selectSquare(square)
        }
    }

    fun onPromotionSelected(pieceType: PieceType) {
        engine.applyPromotion(pieceType)
    }

    fun onPromotionCanceled() {
        engine.cancelPromotion()
    }

    fun undoMove() {
        if (_isBotThinking.value) return
        engine.undo() // Undo bot's move
        engine.undo() // Undo player's move
    }

    fun resign() {
        engine.resign(_playerColor.value)
    }

    fun requestHint() {
        if (boardState.value.gameStatus != GameStatus.ONGOING) return
        if (isBotThinking.value) return
        viewModelScope.launch {
            val fen = FenGenerator.generateFen(boardState.value)
            val hint = hintManager.getHint(fen, boardState.value)
            _hintMove.value = hint
        }
    }

    private fun triggerBotMove(boardState: BoardState) {
        botMoveJob?.cancel()
        botMoveJob = viewModelScope.launch {
            _isBotThinking.value = true
            try {
                engineRepository.initialize()
                
                // Artificial delay for realism
                delay((500..1500).random().toLong())

                val fen = FenGenerator.generateFen(boardState)
                val bestMoveUci = engineRepository.getBestMove(fen, _botLevel.value, boardState)
                
                if (bestMoveUci != null) {
                    engine.applyUciMove(bestMoveUci)
                }
            } catch (e: Exception) {
                android.util.Log.e("BotGameViewModel", "Error in bot move", e)
            } finally {
                _isBotThinking.value = false
            }
        }
    }

    private fun recomputeHistory(moves: List<Move>) {
        var simPieces = BoardState.initial().pieces.toMutableMap()
        val wCaptured = mutableListOf<Pair<PieceType, Int>>()
        val bCaptured = mutableListOf<Pair<PieceType, Int>>()
        val newMoveHistory = mutableListOf<String>()

        for (move in moves) {
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

    private fun saveGame(finalState: BoardState) {
        gameSaved = true
        viewModelScope.launch {
            val playerWon = (finalState.gameStatus == GameStatus.CHECKMATE && finalState.turn != _playerColor.value) ||
                            (finalState.gameStatus == GameStatus.RESIGNED && finalState.turn != _playerColor.value)
            
            val isDraw = finalState.gameStatus in listOf(
                GameStatus.STALEMATE, GameStatus.DRAW_AGREED,
                GameStatus.DRAW_FIFTY_MOVE, GameStatus.DRAW_REPETITION, GameStatus.DRAW_INSUFFICIENT_MATERIAL
            )
            
            val resultStatus = when {
                isDraw -> "1/2-1/2"
                playerWon -> if (_playerColor.value == PieceColor.WHITE) "1-0" else "0-1"
                else -> if (_playerColor.value == PieceColor.WHITE) "0-1" else "1-0"
            }

            val pgn = PgnGenerator.generatePgn(
                moves = finalState.moveHistory,
                result = resultStatus,
                date = gameStartTime
            )

            val duration = System.currentTimeMillis() - gameStartTime
            
            val winnerStr = when (resultStatus) {
                "1-0"     -> "White"
                "0-1"     -> "Black"
                "1/2-1/2" -> "Draw"
                else      -> "Unknown"
            }

            val entity = com.example.chess.database.GameHistoryEntity(
                date = gameStartTime,
                winner = winnerStr,
                duration = duration,
                moveCount = finalState.moveHistory.size,
                pgn = pgn,
                opening = "Custom",
                result = resultStatus,
                isMultiplayer = false,
                player1Name = if (_playerColor.value == PieceColor.WHITE) "Player" else "Bot (${_botLevel.value.title})",
                player2Name = if (_playerColor.value == PieceColor.BLACK) "Player" else "Bot (${_botLevel.value.title})"
            )

            gameHistoryRepository.insertGame(entity)
            
            profileRepository.incrementStats(
                isWin = playerWon,
                isLoss = (!playerWon && !isDraw),
                isDraw = isDraw,
                playTimeMs = duration
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        botMoveJob?.cancel()
        engineRepository.shutdown()
    }
}
