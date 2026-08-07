package com.example.chess.game.engine

object UciCommandBuilder {
    fun setOption(name: String, value: String): String = "setoption name $name value $value"
    
    fun position(fen: String): String = "position fen $fen"
    
    fun go(depth: Int): String = "go depth $depth"
    
    fun goTime(timeMs: Long): String = "go movetime $timeMs"
    
    fun stop(): String = "stop"
    
    fun isReady(): String = "isready"
    
    fun uci(): String = "uci"
    
    fun quit(): String = "quit"
}
