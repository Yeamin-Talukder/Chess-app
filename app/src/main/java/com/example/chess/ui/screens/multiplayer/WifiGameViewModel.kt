package com.example.chess.ui.screens.multiplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.game.board.GameStatus
import com.example.chess.game.board.Move
import com.example.chess.game.board.PieceColor
import com.example.chess.game.board.PieceType
import com.example.chess.game.board.Square
import com.example.chess.game.engine.ChessEngine
import com.example.chess.network.GameEvent
import com.example.chess.database.GameHistoryEntity
import com.example.chess.history.GameHistoryRepository
import com.example.chess.network.wifi.WifiController
import com.example.chess.network.wifi.WifiState
import com.example.chess.profile.UserProfileRepository
import com.example.chess.utils.HapticManager
import com.example.chess.utils.PgnGenerator
import com.example.chess.utils.SoundManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WifiGameViewModel @Inject constructor(
    private val hapticManager: HapticManager,
    private val soundManager: SoundManager,
    private val profileRepository: UserProfileRepository,
    private val historyRepository: GameHistoryRepository,
    val wifiController: WifiController
) : ViewModel() {

    private val engine = ChessEngine(hapticManager, soundManager)
    val boardState = engine.boardState
    val promotionRequest = engine.promotionRequest
    
    private val _moveHistory = MutableStateFlow<List<String>>(emptyList())
    val moveHistory: StateFlow<List<String>> = _moveHistory.asStateFlow()

    private val _whiteCaptured = MutableStateFlow<List<Pair<PieceType, Int>>>(emptyList())
    val whiteCaptured: StateFlow<List<Pair<PieceType, Int>>> = _whiteCaptured.asStateFlow()

    private val _blackCaptured = MutableStateFlow<List<Pair<PieceType, Int>>>(emptyList())
    val blackCaptured: StateFlow<List<Pair<PieceType, Int>>> = _blackCaptured.asStateFlow()
    
    private var gameStartTime = 0L
    
    private val _localPlayerColor = MutableStateFlow(PieceColor.WHITE)
    val localPlayerColor: StateFlow<PieceColor> = _localPlayerColor.asStateFlow()
    
    val connectionState = wifiController.connectionState
    val discoveredServices = wifiController.discoveredServices
    val isHost = wifiController.isHost
    
    private val _isGameStarted = MutableStateFlow(false)
    val isGameStarted: StateFlow<Boolean> = _isGameStarted.asStateFlow()

    init {
        viewModelScope.launch {
            wifiController.incomingEvents.collectLatest { event ->
                handleNetworkEvent(event)
            }
        }
        viewModelScope.launch {
            wifiController.connectionState.collectLatest { state ->
                if (state is WifiState.Connected && state.request != null) {
                    val req = state.request
                    // If I am the host (challenged player), my color is the opposite of the challenger's requested color.
                    // If I am the client (challenger), my color is the requested color.
                    val myColor = if (isHost) {
                        if (req.hostColor == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
                    } else {
                        req.hostColor
                    }
                    _localPlayerColor.value = myColor
                    engine.restart()
                    engine.setTimeControl(req.timeControl, req.timeControl, req.increment)
                    gameStartTime = System.currentTimeMillis()
                    _moveHistory.value = emptyList()
                    _whiteCaptured.value = emptyList()
                    _blackCaptured.value = emptyList()
                    _isGameStarted.value = true
                }
            }
        }
        viewModelScope.launch {
            boardState.collectLatest { state ->
                if (state.moveHistory.size != _moveHistory.value.size) {
                    recomputeHistory(state.moveHistory)
                    updateCaptures(state)
                }

                if (state.gameStatus != GameStatus.ONGOING && _isGameStarted.value) {
                    saveGame(state)
                    _isGameStarted.value = false
                }
            }
        }
    }

    private fun recomputeHistory(moves: List<Move>) {
        var simPieces = com.example.chess.game.board.BoardState.initial().pieces.toMutableMap()
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

    private fun updateCaptures(state: com.example.chess.game.board.BoardState) {
        val wCaptured = state.capturedPieces.filter { it.color == PieceColor.BLACK }
            .groupBy { it.type }
            .map { it.key to it.value.size }
        val bCaptured = state.capturedPieces.filter { it.color == PieceColor.WHITE }
            .groupBy { it.type }
            .map { it.key to it.value.size }
        _whiteCaptured.value = wCaptured
        _blackCaptured.value = bCaptured
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
            val opponentName = if (connectionState.value is WifiState.Connected) (connectionState.value as WifiState.Connected).deviceName else "Unknown"
            val localName = profileRepository.getProfileSync().username.ifBlank { "You" }
            
            val (player1, player2) = if (localPlayerColor.value == PieceColor.WHITE) {
                Pair(localName, opponentName)
            } else {
                Pair(opponentName, localName)
            }

            val entity = GameHistoryEntity(
                date = gameStartTime,
                winner = winnerStr,
                duration = duration,
                moveCount = state.moveHistory.size,
                pgn = pgn,
                opening = "Custom",
                result = resultStr,
                isMultiplayer = true,
                player1Name = player1,
                player2Name = player2
            )

            historyRepository.insertGame(entity)

            val isWin  = resultStr == "1-0" && localPlayerColor.value == PieceColor.WHITE || resultStr == "0-1" && localPlayerColor.value == PieceColor.BLACK
            val isLoss = resultStr == "0-1" && localPlayerColor.value == PieceColor.WHITE || resultStr == "1-0" && localPlayerColor.value == PieceColor.BLACK
            val isDraw = resultStr == "1/2-1/2"
            profileRepository.incrementStats(isWin, isLoss, isDraw, duration)
        }
    }

    fun startLobby() {
        viewModelScope.launch {
            val profile = profileRepository.getProfileSync()
            val username = profile.username.ifBlank { "Guest" }
            wifiController.startLobby(
                username, 
                profile.wins, 
                profile.losses, 
                profile.avatar
            )
        }
    }

    fun sendChallenge(serviceInfo: android.net.nsd.NsdServiceInfo, timeControl: Long, increment: Long, myColor: PieceColor) {
        viewModelScope.launch {
            val profile = profileRepository.getProfileSync()
            val username = profile.username.ifBlank { "Guest" }
            val req = GameEvent.MatchRequest(username, timeControl, increment, myColor)
            wifiController.sendChallenge(serviceInfo, req)
        }
    }

    fun acceptChallenge(opponentName: String) {
        wifiController.acceptChallenge(opponentName)
    }

    fun rejectChallenge() {
        wifiController.rejectChallenge()
    }

    private fun handleNetworkEvent(event: GameEvent) {
        when (event) {
            is GameEvent.GameConfig -> {
                // Deprecated in new auto-discovery flow. Configuration is done via MatchRequest.
            }
            is GameEvent.Move -> {
                engine.applyUciMove(event.uci)
            }
            is GameEvent.Resign -> {
                val opponentColor = if (localPlayerColor.value == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
                engine.resign(opponentColor)
            }
            is GameEvent.DrawOffer -> {}
            is GameEvent.DrawResponse -> {
                if (event.accepted) engine.offerDraw()
            }
            is GameEvent.UndoRequest -> { }
            is GameEvent.UndoResponse -> { }
            is GameEvent.SyncState -> { }
            is GameEvent.HeartbeatPing -> {} // Handled internally by controller
            is GameEvent.MatchRequest -> { }
            is GameEvent.MatchResponse -> { }
            else -> { }
        }
    }

    fun onSquareClick(square: Square) {
        if (!isGameStarted.value) return
        if (boardState.value.turn != localPlayerColor.value) return
        
        val moveGenerated = engine.selectSquareAndReturnMove(square)
        if (moveGenerated != null) {
            val uci = moveGenerated.notation
            wifiController.sendEvent(GameEvent.Move(uci))
        }
    }

    fun onPromotionSelected(type: PieceType) {
        val moveGenerated = engine.applyPromotionAndReturnMove(type)
        if (moveGenerated != null) {
            val uci = moveGenerated.notation
            wifiController.sendEvent(GameEvent.Move(uci))
        }
    }

    fun onPromotionCanceled() {
        engine.cancelPromotion()
    }

    fun resign() {
        engine.resign(localPlayerColor.value)
        wifiController.sendEvent(GameEvent.Resign)
    }

    fun disconnect() {
        wifiController.disconnect()
    }
}
