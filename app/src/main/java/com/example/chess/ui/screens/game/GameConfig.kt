package com.example.chess.ui.screens.game

import com.example.chess.game.board.PieceColor
import com.example.chess.ui.screens.game.GameMode
import com.example.chess.ui.screens.game.BoardOrientation

/**
 * Configuration options for a new chess game.
 *
 * @property mode The selected game mode (offline, bluetooth, wifi, wifi direct).
 * @property timeMillis Initial time per side in milliseconds.
 * @property incrementMillis Increment per move in milliseconds.
 * @property orientation Board orientation – which side is at the bottom.
 * @property firstMove Which color moves first.
 */

data class GameConfig(
    val mode: GameMode = GameMode.LOCAL_OFFLINE,
    val timeHours: Int = 0,
    val timeMinutes: Int = 10,
    val timeSeconds: Int = 0,
    val incrementSeconds: Int = 0,
    val playerColorChoice: PlayerColorChoice = PlayerColorChoice.WHITE,
    val timeMillis: Long = 10L * 60 * 1000, // default 10 minutes
    val incrementMillis: Long = 0L,
    val orientation: BoardOrientation = BoardOrientation.WHITE_BOTTOM,
    val firstMove: PieceColor = PieceColor.WHITE,
    val player1Name: String = "Player 1",
    val player2Name: String = "Player 2"
)

enum class GameMode {
    LOCAL_OFFLINE,
    BLUETOOTH,
    WIFI,
    WIFI_DIRECT
}

enum class BoardOrientation {
    WHITE_BOTTOM,
    BLACK_BOTTOM,
    AUTO_ROTATE
}

enum class PlayerColorChoice {
    WHITE,
    BLACK,
    RANDOM
}
