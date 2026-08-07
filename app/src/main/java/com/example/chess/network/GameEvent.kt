package com.example.chess.network

import kotlinx.serialization.Serializable

@Serializable
sealed class GameEvent {
    @Serializable
    data class Move(val uci: String) : GameEvent()
    
    @Serializable
    object UndoRequest : GameEvent()
    
    @Serializable
    data class UndoResponse(val accepted: Boolean) : GameEvent()
    
    @Serializable
    object DrawOffer : GameEvent()
    
    @Serializable
    data class DrawResponse(val accepted: Boolean) : GameEvent()
    
    @Serializable
    object Resign : GameEvent()
    
    @Serializable
    data class SyncState(val pgn: String) : GameEvent()

    @Serializable
    data class GameConfig(val hostColor: com.example.chess.game.board.PieceColor, val timeControl: Long, val increment: Long) : GameEvent()
    
    @Serializable
    object HeartbeatPing : GameEvent()
    
    @Serializable
    object HeartbeatPong : GameEvent()
    
    @Serializable
    data class MatchRequest(val challengerName: String, val timeControl: Long, val increment: Long, val hostColor: com.example.chess.game.board.PieceColor) : GameEvent()
    
    @Serializable
    data class MatchResponse(val accepted: Boolean) : GameEvent()
}
