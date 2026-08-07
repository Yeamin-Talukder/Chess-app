package com.example.chess.game.engine

import com.example.chess.game.board.BoardState
import com.example.chess.game.board.PieceColor
import com.example.chess.game.board.PieceType

/**
 * A static evaluation engine for calculating board advantage without AI lookahead.
 * Evaluates based on:
 * 1. Material Count
 * 2. Positional Piece-Square Tables (PST)
 * 
 * Returns a score from White's perspective. Positive = White is winning, Negative = Black is winning.
 * 1.0 roughly equals 1 pawn advantage.
 */
object GameEvaluator {

    // Standard piece values
    private const val PAWN_VAL = 1.0f
    private const val KNIGHT_VAL = 3.0f
    private const val BISHOP_VAL = 3.0f
    private const val ROOK_VAL = 5.0f
    private const val QUEEN_VAL = 9.0f

    // Simplified Piece-Square Tables (mirrored for Black)
    // Positive values encourage pieces to move to those squares, negative discourages.

    private val PAWN_PST = arrayOf(
        0.0f,  0.0f,  0.0f,  0.0f,  0.0f,  0.0f,  0.0f,  0.0f,
        0.5f,  0.5f,  0.5f,  0.5f,  0.5f,  0.5f,  0.5f,  0.5f,
        0.1f,  0.1f,  0.2f,  0.3f,  0.3f,  0.2f,  0.1f,  0.1f,
        0.05f, 0.05f, 0.1f,  0.25f, 0.25f, 0.1f,  0.05f, 0.05f,
        0.0f,  0.0f,  0.0f,  0.2f,  0.2f,  0.0f,  0.0f,  0.0f,
        0.05f, -0.05f,-0.1f, 0.0f,  0.0f, -0.1f, -0.05f, 0.05f,
        0.05f, 0.1f,  0.1f,  -0.2f, -0.2f, 0.1f,  0.1f,  0.05f,
        0.0f,  0.0f,  0.0f,  0.0f,  0.0f,  0.0f,  0.0f,  0.0f
    )

    private val KNIGHT_PST = arrayOf(
        -0.5f, -0.4f, -0.3f, -0.3f, -0.3f, -0.3f, -0.4f, -0.5f,
        -0.4f, -0.2f,  0.0f,  0.0f,  0.0f,  0.0f, -0.2f, -0.4f,
        -0.3f,  0.0f,  0.1f,  0.15f, 0.15f, 0.1f,  0.0f, -0.3f,
        -0.3f,  0.05f, 0.15f, 0.2f,  0.2f,  0.15f, 0.05f, -0.3f,
        -0.3f,  0.0f,  0.15f, 0.2f,  0.2f,  0.15f, 0.0f, -0.3f,
        -0.3f,  0.05f, 0.1f,  0.15f, 0.15f, 0.1f,  0.05f, -0.3f,
        -0.4f, -0.2f,  0.0f,  0.05f, 0.05f, 0.0f, -0.2f, -0.4f,
        -0.5f, -0.4f, -0.3f, -0.3f, -0.3f, -0.3f, -0.4f, -0.5f
    )

    // Center control for Bishops
    private val BISHOP_PST = arrayOf(
        -0.2f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f, -0.2f,
        -0.1f,  0.0f,  0.0f,  0.0f,  0.0f,  0.0f,  0.0f, -0.1f,
        -0.1f,  0.0f,  0.05f, 0.1f,  0.1f,  0.05f, 0.0f, -0.1f,
        -0.1f,  0.05f, 0.05f, 0.1f,  0.1f,  0.05f, 0.05f, -0.1f,
        -0.1f,  0.0f,  0.1f,  0.1f,  0.1f,  0.1f,  0.0f, -0.1f,
        -0.1f,  0.1f,  0.1f,  0.1f,  0.1f,  0.1f,  0.1f, -0.1f,
        -0.1f,  0.05f, 0.0f,  0.0f,  0.0f,  0.0f,  0.05f, -0.1f,
        -0.2f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f, -0.2f
    )

    fun evaluate(boardState: BoardState): Float {
        var score = 0.0f

        for ((square, piece) in boardState.pieces) {
            val isWhite = piece.color == PieceColor.WHITE
            val multiplier = if (isWhite) 1.0f else -1.0f

            // Material Value
            val materialVal = when (piece.type) {
                PieceType.PAWN -> PAWN_VAL
                PieceType.KNIGHT -> KNIGHT_VAL
                PieceType.BISHOP -> BISHOP_VAL
                PieceType.ROOK -> ROOK_VAL
                PieceType.QUEEN -> QUEEN_VAL
                PieceType.KING -> 0f // King is infinite, handled by checkmate game status
            }

            // Positional Value
            val file = square.file
            val rank = square.rank
            val visualRank = if (isWhite) rank else 7 - rank // Mirror rank for Black
            val tableIndex = visualRank * 8 + file

            val positionalVal = when (piece.type) {
                PieceType.PAWN -> PAWN_PST[tableIndex]
                PieceType.KNIGHT -> KNIGHT_PST[tableIndex]
                PieceType.BISHOP -> BISHOP_PST[tableIndex]
                else -> 0f
            }

            score += (materialVal + positionalVal) * multiplier
        }

        // Clamp the evaluation score to realistic visual bounds (-15 to +15)
        return score.coerceIn(-15f, 15f)
    }
}
