package com.example.chess.utils

import com.example.chess.game.board.BoardState
import com.example.chess.game.board.PieceColor
import com.example.chess.game.board.PieceType
import com.example.chess.game.board.Square

object FenGenerator {
    fun generateFen(state: BoardState): String {
        val fenBuilder = StringBuilder()

        // 1. Piece placement
        for (rank in 0..7) {
            var emptyCount = 0
            for (file in 0..7) {
                val piece = state.pieces[Square(file, rank)]
                if (piece == null) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        fenBuilder.append(emptyCount)
                        emptyCount = 0
                    }
                    val char = when (piece.type) {
                        PieceType.PAWN -> 'p'
                        PieceType.KNIGHT -> 'n'
                        PieceType.BISHOP -> 'b'
                        PieceType.ROOK -> 'r'
                        PieceType.QUEEN -> 'q'
                        PieceType.KING -> 'k'
                    }
                    fenBuilder.append(if (piece.color == PieceColor.WHITE) char.uppercaseChar() else char)
                }
            }
            if (emptyCount > 0) {
                fenBuilder.append(emptyCount)
            }
            if (rank < 7) {
                fenBuilder.append('/')
            }
        }

        // 2. Active color
        fenBuilder.append(if (state.turn == PieceColor.WHITE) " w " else " b ")

        // 3. Castling availability
        var castling = ""
        if (state.whiteKingsideCastle) castling += "K"
        if (state.whiteQueensideCastle) castling += "Q"
        if (state.blackKingsideCastle) castling += "k"
        if (state.blackQueensideCastle) castling += "q"
        if (castling.isEmpty()) castling = "-"
        fenBuilder.append(castling)
        fenBuilder.append(" ")

        // 4. En passant target square
        val ep = state.enPassantTarget?.notation ?: "-"
        fenBuilder.append(ep)
        fenBuilder.append(" ")

        // 5. Halfmove clock
        fenBuilder.append(state.halfMoveClock)
        fenBuilder.append(" ")

        // 6. Fullmove number
        fenBuilder.append(state.fullMoveNumber)

        return fenBuilder.toString()
    }
}
