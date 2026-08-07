package com.example.chess.game.engine

import com.example.chess.game.board.BoardState
import com.example.chess.game.board.ChessPiece
import com.example.chess.game.board.GameStatus
import com.example.chess.game.board.Move
import com.example.chess.game.board.PieceColor
import com.example.chess.game.board.PieceType
import com.example.chess.game.board.Square
import com.example.chess.utils.HapticManager
import com.example.chess.utils.SoundManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlin.math.abs

class ChessEngine(
    private val hapticManager: HapticManager? = null,
    private val soundManager: SoundManager? = null
) {
    private val _boardState = MutableStateFlow(BoardState.initial())
    val boardState: StateFlow<BoardState> = _boardState.asStateFlow()

    private val _promotionRequest = MutableStateFlow<Pair<Square, Square>?>(null)
    val promotionRequest: StateFlow<Pair<Square, Square>?> = _promotionRequest.asStateFlow()
    
    // --- Review Mode ---
    private val _reviewIndex = MutableStateFlow<Int?>(null)
    val reviewIndex: StateFlow<Int?> = _reviewIndex.asStateFlow()

    private val _reviewBoardState = MutableStateFlow<BoardState?>(null)
    val reviewBoardState: StateFlow<BoardState?> = _reviewBoardState.asStateFlow()
    // -------------------

    val stateHistory = mutableListOf<BoardState>()
    private var selectedSquare: Square? = null

    /** True once the first move of the game has been played — timer doesn't run before that. */
    private var timerStarted = false
    /** Increment in ms added to the player who just moved after each move. */
    private var incrementMillis: Long = 0L

    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null

    init {
        startTimer()
    }
    
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = engineScope.launch {
            while (isActive) {
                delay(100) // Tick every 100ms
                // Only tick after the first move has been played
                if (!timerStarted) continue
                val currentState = _boardState.value
                if (currentState.gameStatus == GameStatus.ONGOING) {
                    if (currentState.turn == PieceColor.WHITE) {
                        val newTime = (currentState.whiteTimeMillis - 100).coerceAtLeast(0)
                        _boardState.value = currentState.copy(whiteTimeMillis = newTime)
                        if (newTime == 0L) {
                            _boardState.value = _boardState.value.copy(gameStatus = GameStatus.TIME_OUT)
                        }
                    } else {
                        val newTime = (currentState.blackTimeMillis - 100).coerceAtLeast(0)
                        _boardState.value = currentState.copy(blackTimeMillis = newTime)
                        if (newTime == 0L) {
                            _boardState.value = _boardState.value.copy(gameStatus = GameStatus.TIME_OUT)
                        }
                    }
                }
            }
        }
    }

    fun selectSquare(square: Square) {
        val currentState = _boardState.value
        if (currentState.gameStatus != GameStatus.ONGOING) return

        val piece = currentState.pieces[square]

        if (selectedSquare == null) {
            // Selecting a piece to move
            if (piece != null && piece.color == currentState.turn) {
                selectedSquare = square
                val legalMoves = MoveRules.getAllLegalMoves(currentState, currentState.turn)
                    .filter { it.from == square }
                
                _boardState.value = currentState.copy(legalMovesForSelected = legalMoves)
            }
        } else {
            // Attempting to move
            val movesToSquare = currentState.legalMovesForSelected.filter { it.to == square }
            
            if (movesToSquare.isNotEmpty()) {
                val requiresPromotion = movesToSquare.first().promotion != null
                if (requiresPromotion) {
                    _promotionRequest.value = Pair(selectedSquare!!, square)
                    hapticManager?.playPromotion()
                    soundManager?.playPromotion()
                } else {
                    makeMove(movesToSquare.first())
                }
                selectedSquare = null
            } else if (piece != null && piece.color == currentState.turn) {
                // Switching selection to another piece
                selectedSquare = square
                val legalMoves = MoveRules.getAllLegalMoves(currentState, currentState.turn)
                    .filter { it.from == square }
                _boardState.value = currentState.copy(legalMovesForSelected = legalMoves)
            } else {
                // Deselect / Illegal Move
                if (piece != null) {
                    hapticManager?.playIllegalMove()
                }
                selectedSquare = null
                _boardState.value = currentState.copy(legalMovesForSelected = emptyList())
            }
        }
    }

    fun selectSquareAndReturnMove(square: Square): Move? {
        val lastMoveSize = stateHistory.size
        selectSquare(square)
        if (stateHistory.size > lastMoveSize) {
            return _boardState.value.lastMove
        }
        return null
    }

    fun applyPromotionAndReturnMove(promotionType: PieceType): Move? {
        val request = _promotionRequest.value ?: return null
        val currentState = _boardState.value
        val move = currentState.legalMovesForSelected.find { 
            it.from == request.first && it.to == request.second && it.promotion == promotionType 
        }
        
        _promotionRequest.value = null
        if (move != null) {
            makeMove(move)
            return move
        }
        return null
    }

    fun applyPromotion(promotionType: PieceType) {
        val request = _promotionRequest.value ?: return
        val currentState = _boardState.value
        val move = currentState.legalMovesForSelected.find { 
            it.from == request.first && it.to == request.second && it.promotion == promotionType 
        }
        
        _promotionRequest.value = null
        if (move != null) {
            makeMove(move)
        }
    }

    fun cancelPromotion() {
        _promotionRequest.value = null
    }

    private fun makeMove(move: Move) {
        val currentState = _boardState.value
        if (currentState.gameStatus != GameStatus.ONGOING) return

        // Start the timer on the very first move
        timerStarted = true

        // Save state for Undo & Review
        stateHistory.add(currentState)

        val newPieces = currentState.pieces.toMutableMap()
        val newCaptured = currentState.capturedPieces.toMutableList()
        val movingPiece = newPieces.remove(move.from) ?: return

        // 1. Handle normal capture
        val targetPiece = newPieces.remove(move.to)
        if (targetPiece != null) {
            newCaptured.add(targetPiece)
        }

        // 2. Handle En Passant Capture
        if (move.isEnPassant) {
            val epRank = if (movingPiece.color == PieceColor.WHITE) move.to.rank + 1 else move.to.rank - 1
            val epPawn = newPieces.remove(Square(move.to.file, epRank))
            if (epPawn != null) newCaptured.add(epPawn)
        }

        // 2. Handle Castling
        if (move.isKingsideCastle) {
            val rank = move.to.rank
            val rook = newPieces.remove(Square(7, rank))
            if (rook != null) newPieces[Square(5, rank)] = rook
        } else if (move.isQueensideCastle) {
            val rank = move.to.rank
            val rook = newPieces.remove(Square(0, rank))
            if (rook != null) newPieces[Square(3, rank)] = rook
        }

        // 3. Move the piece (handle promotion)
        val finalPiece = if (move.promotion != null) {
            movingPiece.copy(type = move.promotion)
        } else {
            movingPiece
        }
        newPieces[move.to] = finalPiece

        // 4. Update Castling Rights
        var wkc = currentState.whiteKingsideCastle
        var wqc = currentState.whiteQueensideCastle
        var bkc = currentState.blackKingsideCastle
        var bqc = currentState.blackQueensideCastle

        if (movingPiece.type == PieceType.KING) {
            if (movingPiece.color == PieceColor.WHITE) {
                wkc = false; wqc = false
            } else {
                bkc = false; bqc = false
            }
        } else if (movingPiece.type == PieceType.ROOK) {
            if (move.from == Square(7, 7)) wkc = false
            if (move.from == Square(0, 7)) wqc = false
            if (move.from == Square(7, 0)) bkc = false
            if (move.from == Square(0, 0)) bqc = false
        }
        // If a rook is captured on its starting square
        if (move.to == Square(7, 7)) wkc = false
        if (move.to == Square(0, 7)) wqc = false
        if (move.to == Square(7, 0)) bkc = false
        if (move.to == Square(0, 0)) bqc = false

        // 5. Update En Passant Target
        // White moves toward rank 0 (direction = -1), Black toward rank 7 (direction = +1).
        // The EP target is the square the pawn passed through (one step behind the landing square).
        val newEnPassantTarget = if (movingPiece.type == PieceType.PAWN && abs(move.from.rank - move.to.rank) == 2) {
            val direction = if (movingPiece.color == PieceColor.WHITE) -1 else 1
            Square(move.from.file, move.from.rank + direction)
        } else {
            null
        }

        // 6. Half-move clock & Full-move number & Move History
        val isCaptureOrPawnPush = move.isCapture || movingPiece.type == PieceType.PAWN
        val newHalfMove = if (isCaptureOrPawnPush) 0 else currentState.halfMoveClock + 1
        val newFullMove = if (currentState.turn == PieceColor.BLACK) currentState.fullMoveNumber + 1 else currentState.fullMoveNumber
        val newMoveHistory = currentState.moveHistory + move

        val nextTurn = currentState.turn.opposite()

        // Temporarily create the next state to evaluate Check and Game Over
        var nextState = currentState.copy(
            pieces = newPieces,
            turn = nextTurn,
            lastMove = move,
            legalMovesForSelected = emptyList(),
            whiteKingsideCastle = wkc,
            whiteQueensideCastle = wqc,
            blackKingsideCastle = bkc,
            blackQueensideCastle = bqc,
            enPassantTarget = newEnPassantTarget,
            halfMoveClock = newHalfMove,
            fullMoveNumber = newFullMove,
            moveHistory = newMoveHistory,
            capturedPieces = newCaptured
        )

        // 7. Check for Check, Checkmate, Stalemate
        val isInCheck = MoveRules.isCheck(nextState, nextTurn)
        val inCheckSquare = if (isInCheck) {
            nextState.pieces.entries.find { it.value.color == nextTurn && it.value.type == PieceType.KING }?.key
        } else null
        
        nextState = nextState.copy(inCheckSquare = inCheckSquare)

        val legalMovesForNextPlayer = MoveRules.getAllLegalMoves(nextState, nextTurn)
        
        var newStatus = GameStatus.ONGOING
        
        if (legalMovesForNextPlayer.isEmpty()) {
            newStatus = if (isInCheck) GameStatus.CHECKMATE else GameStatus.STALEMATE
        } else if (newHalfMove >= 100) { // 50 full moves = 100 half moves
            newStatus = GameStatus.DRAW_FIFTY_MOVE
        } else if (MoveRules.hasInsufficientMaterial(nextState)) {
            newStatus = GameStatus.DRAW_INSUFFICIENT_MATERIAL
        }

        // 8. Update Repetition History
        val positionHash = generatePositionHash(nextState)
        val newHistory = currentState.positionHashes.toMutableMap()
        newHistory[positionHash] = (newHistory[positionHash] ?: 0) + 1
        
        if (newHistory[positionHash]!! >= 3) {
            newStatus = GameStatus.DRAW_REPETITION
        }

        nextState = nextState.copy(
            gameStatus = newStatus,
            positionHashes = if (isCaptureOrPawnPush) emptyMap() else newHistory // Reset history on capture/pawn push
        )
        
        // Trigger Haptics & Sounds
        when (newStatus) {
            GameStatus.CHECKMATE -> {
                hapticManager?.playCheckmate()
                soundManager?.playCheckmate()
            }
            GameStatus.DRAW_FIFTY_MOVE, GameStatus.DRAW_REPETITION, GameStatus.DRAW_INSUFFICIENT_MATERIAL, GameStatus.STALEMATE -> {
                hapticManager?.playDraw()
                soundManager?.playDraw()
            }
            GameStatus.ONGOING -> {
                if (isInCheck) {
                    hapticManager?.playCheck()
                    soundManager?.playCheck()
                }
                else if (move.isCapture) {
                    hapticManager?.playCapture()
                    soundManager?.playCapture()
                }
                else if (move.isKingsideCastle || move.isQueensideCastle) {
                    hapticManager?.playMove()
                    soundManager?.playCastle()
                }
                else {
                    hapticManager?.playMove()
                    soundManager?.playMove()
                }
            }
            else -> {}
        }

        // Apply increment to the player who just moved
        if (incrementMillis > 0) {
            nextState = if (currentState.turn == PieceColor.WHITE) {
                nextState.copy(whiteTimeMillis = nextState.whiteTimeMillis + incrementMillis)
            } else {
                nextState.copy(blackTimeMillis = nextState.blackTimeMillis + incrementMillis)
            }
        }

        _boardState.value = nextState
    }

    fun applyUciMove(uci: String) {
        if (uci.length < 4) return
        val fromNotation = uci.substring(0, 2)
        val toNotation = uci.substring(2, 4)
        
        val fromSquare = Square.fromNotation(fromNotation) ?: return
        val toSquare = Square.fromNotation(toNotation) ?: return

        val currentState = _boardState.value
        val allLegalMoves = MoveRules.getAllLegalMoves(currentState, currentState.turn)
        val matchingMoves = allLegalMoves.filter { it.from == fromSquare && it.to == toSquare }

        if (matchingMoves.isNotEmpty()) {
            val targetMove = if (uci.length > 4) {
                val promType = when (uci[4]) {
                    'q' -> PieceType.QUEEN
                    'r' -> PieceType.ROOK
                    'b' -> PieceType.BISHOP
                    'n' -> PieceType.KNIGHT
                    else -> PieceType.QUEEN
                }
                matchingMoves.firstOrNull { it.promotion == promType } ?: matchingMoves.first()
            } else {
                matchingMoves.first()
            }

            selectedSquare = null
            _promotionRequest.value = null
            makeMove(targetMove)
        }
    }

    fun undo() {
        if (stateHistory.isNotEmpty()) {
            _boardState.value = stateHistory.removeLast()
            selectedSquare = null
            _promotionRequest.value = null
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun resign(color: PieceColor) {
        if (_boardState.value.gameStatus == GameStatus.ONGOING) {
            _boardState.value = _boardState.value.copy(gameStatus = GameStatus.RESIGNED)
        }
    }

    fun offerDraw() {
        if (_boardState.value.gameStatus == GameStatus.ONGOING) {
            _boardState.value = _boardState.value.copy(gameStatus = GameStatus.DRAW_AGREED)
        }
    }

    // ── Review Mode Functions ───────────────────────────────────────────

    fun startReview() {
        // Start at the final state
        _reviewIndex.value = stateHistory.size
        _reviewBoardState.value = _boardState.value
    }

    fun exitReview() {
        _reviewIndex.value = null
        _reviewBoardState.value = null
    }

    fun reviewPrevious() {
        val currentIndex = _reviewIndex.value ?: return
        if (currentIndex > 0) {
            val newIndex = currentIndex - 1
            _reviewIndex.value = newIndex
            _reviewBoardState.value = stateHistory.getOrNull(newIndex) ?: _boardState.value
        }
    }

    fun reviewNext() {
        val currentIndex = _reviewIndex.value ?: return
        if (currentIndex < stateHistory.size) {
            val newIndex = currentIndex + 1
            _reviewIndex.value = newIndex
            _reviewBoardState.value = if (newIndex == stateHistory.size) _boardState.value else (stateHistory.getOrNull(newIndex) ?: _boardState.value)
        }
    }

    fun reviewFirst() {
        _reviewIndex.value = 0
        _reviewBoardState.value = stateHistory.firstOrNull() ?: _boardState.value
    }

    fun reviewLast() {
        _reviewIndex.value = stateHistory.size
        _reviewBoardState.value = _boardState.value
    }

    fun restart() {
        timerStarted = false
        incrementMillis = 0L
        _boardState.value = BoardState.initial()
        stateHistory.clear()
        selectedSquare = null
        _promotionRequest.value = null
        startTimer()
    }

    fun loadFen(fen: String) {
        timerStarted = false
        incrementMillis = 0L
        _boardState.value = BoardState.fromFen(fen)
        stateHistory.clear()
        selectedSquare = null
        _promotionRequest.value = null
        startTimer()
    }

    /**
     * Set custom time control for both sides. Used when starting a new game with a specific
     * time limit.
     */
    fun setTimeControl(whiteMs: Long, blackMs: Long, incrementMs: Long = 0L) {
        incrementMillis = incrementMs
        timerStarted = false
        _boardState.value = _boardState.value.copy(
            whiteTimeMillis = whiteMs,
            blackTimeMillis = blackMs
        )
    }

    private fun generatePositionHash(state: BoardState): Int {
        var hash = 17
        state.pieces.entries.sortedBy { it.key.index }.forEach { (square, piece) ->
            hash = hash * 31 + square.index
            hash = hash * 31 + piece.type.ordinal
            hash = hash * 31 + piece.color.ordinal
        }
        hash = hash * 31 + state.turn.ordinal
        hash = hash * 31 + (if (state.whiteKingsideCastle) 1 else 0)
        hash = hash * 31 + (if (state.whiteQueensideCastle) 1 else 0)
        hash = hash * 31 + (if (state.blackKingsideCastle) 1 else 0)
        hash = hash * 31 + (if (state.blackQueensideCastle) 1 else 0)
        hash = hash * 31 + (state.enPassantTarget?.index ?: -1)
        return hash
    }
}
