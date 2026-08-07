package com.example.chess.game.rules

import com.example.chess.game.board.BoardState
import com.example.chess.game.board.ChessPiece
import com.example.chess.game.board.PieceColor
import com.example.chess.game.board.PieceType
import com.example.chess.game.board.Square
import com.example.chess.game.engine.MoveRules
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MoveValidationTest {

    @Test
    fun `pawn can capture diagonally`() {
        val pieces = mutableMapOf<Square, ChessPiece>()
        pieces[Square(4, 4)] = ChessPiece("p1", PieceType.PAWN, PieceColor.WHITE) // e4
        pieces[Square(3, 3)] = ChessPiece("p2", PieceType.PAWN, PieceColor.BLACK) // d5
        pieces[Square(5, 3)] = ChessPiece("p3", PieceType.PAWN, PieceColor.BLACK) // f5
        
        val state = BoardState(pieces = pieces, turn = PieceColor.WHITE)
        val moves = MoveRules.getAllLegalMoves(state, PieceColor.WHITE)
        
        val e4Moves = moves.filter { it.from == Square(4, 4) }
        assertThat(e4Moves.map { it.to }).containsExactly(
            Square(4, 3), // e5 forward
            Square(3, 3), // d5 capture
            Square(5, 3)  // f5 capture
        )
    }

    @Test
    fun `knight can jump over pieces`() {
        val state = BoardState.initial()
        val moves = MoveRules.getAllLegalMoves(state, PieceColor.WHITE)
        val knightMoves = moves.filter { it.from == Square(1, 7) } // b1 knight
        
        assertThat(knightMoves.map { it.to }).containsExactly(
            Square(0, 5), // a3
            Square(2, 5)  // c3
        )
    }

    @Test
    fun `castling is valid when path is clear`() {
        val pieces = mutableMapOf<Square, ChessPiece>()
        pieces[Square(4, 7)] = ChessPiece("k", PieceType.KING, PieceColor.WHITE) // e1
        pieces[Square(7, 7)] = ChessPiece("r", PieceType.ROOK, PieceColor.WHITE) // h1
        
        val state = BoardState(pieces = pieces, turn = PieceColor.WHITE)
        val moves = MoveRules.getAllLegalMoves(state, PieceColor.WHITE)
        
        val kingMoves = moves.filter { it.from == Square(4, 7) }
        val kingsideCastle = kingMoves.find { it.to == Square(6, 7) }
        assertThat(kingsideCastle).isNotNull()
        assertThat(kingsideCastle!!.isKingsideCastle).isTrue()
    }
}
