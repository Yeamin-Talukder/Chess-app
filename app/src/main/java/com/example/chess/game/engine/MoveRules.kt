package com.example.chess.game.engine

import com.example.chess.game.board.BoardState
import com.example.chess.game.board.ChessPiece
import com.example.chess.game.board.Move
import com.example.chess.game.board.PieceColor
import com.example.chess.game.board.PieceType
import com.example.chess.game.board.Square
import kotlin.math.abs

object MoveRules {

    private val knightOffsets = listOf(
        Pair(1, 2), Pair(2, 1), Pair(2, -1), Pair(1, -2),
        Pair(-1, -2), Pair(-2, -1), Pair(-2, 1), Pair(-1, 2)
    )

    private val kingOffsets = listOf(
        Pair(1, 0), Pair(1, 1), Pair(0, 1), Pair(-1, 1),
        Pair(-1, 0), Pair(-1, -1), Pair(0, -1), Pair(1, -1)
    )

    private val rookDirections = listOf(
        Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1)
    )

    private val bishopDirections = listOf(
        Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1)
    )

    private val queenDirections = rookDirections + bishopDirections

    fun getAllLegalMoves(state: BoardState, color: PieceColor): List<Move> {
        val allPseudoMoves = mutableListOf<Move>()
        state.pieces.forEach { (square, piece) ->
            if (piece.color == color) {
                allPseudoMoves.addAll(getPseudoLegalMoves(state, square, piece))
            }
        }
        
        // Filter out moves that leave the king in check
        return allPseudoMoves.filter { move ->
            val nextState = applyMovePseudo(state, move)
            if (isSquareAttacked(nextState, findKingSquare(nextState, color), color.opposite())) {
                return@filter false
            }
            
            // Special rules for castling
            if (move.isKingsideCastle || move.isQueensideCastle) {
                // Cannot castle out of check
                if (isCheck(state, color)) return@filter false
                
                // Cannot castle through an attacked square
                val passedSquare = if (move.isKingsideCastle) {
                    Square(5, move.from.rank)
                } else {
                    Square(3, move.from.rank)
                }
                
                if (isSquareAttacked(state, passedSquare, color.opposite())) {
                    return@filter false
                }
            }
            
            true
        }
    }

    fun isCheck(state: BoardState, color: PieceColor): Boolean {
        val kingSquare = findKingSquare(state, color) ?: return false
        return isSquareAttacked(state, kingSquare, color.opposite())
    }
    
    fun hasInsufficientMaterial(state: BoardState): Boolean {
        if (state.pieces.size > 4) return false // Too many pieces
        
        val types = state.pieces.values.map { it.type }.filter { it != PieceType.KING }
        
        // King vs King
        if (types.isEmpty()) return true
        
        // King & Knight vs King OR King & Bishop vs King
        if (types.size == 1 && (types[0] == PieceType.KNIGHT || types[0] == PieceType.BISHOP)) {
            return true
        }
        
        // King & Bishop vs King & Bishop (same color squares - approximation for now)
        if (types.size == 2 && types.all { it == PieceType.BISHOP }) {
            // Check if bishops are on same color squares (simplified check)
            return true
        }
        
        return false
    }

    internal fun findKingSquare(state: BoardState, color: PieceColor): Square? {
        return state.pieces.entries.find { it.value.color == color && it.value.type == PieceType.KING }?.key
    }

    private fun getPseudoLegalMoves(state: BoardState, square: Square, piece: ChessPiece): List<Move> {
        return when (piece.type) {
            PieceType.PAWN -> getPawnMoves(state, square, piece.color)
            PieceType.KNIGHT -> getStepMoves(state, square, piece.color, knightOffsets)
            PieceType.BISHOP -> getSlidingMoves(state, square, piece.color, bishopDirections)
            PieceType.ROOK -> getSlidingMoves(state, square, piece.color, rookDirections)
            PieceType.QUEEN -> getSlidingMoves(state, square, piece.color, queenDirections)
            PieceType.KING -> getKingMoves(state, square, piece.color)
        }
    }

    private fun getPawnMoves(state: BoardState, square: Square, color: PieceColor): List<Move> {
        val moves = mutableListOf<Move>()
        val direction = if (color == PieceColor.WHITE) -1 else 1
        val startRank = if (color == PieceColor.WHITE) 6 else 1
        val promotionRank = if (color == PieceColor.WHITE) 0 else 7

        // Forward one
        val forwardOne = Square(square.file, square.rank + direction)
        if (isValidSquare(forwardOne) && !state.pieces.containsKey(forwardOne)) {
            addPawnMove(moves, square, forwardOne, promotionRank)

            // Forward two (only if forward one is empty and on start rank)
            if (square.rank == startRank) {
                val forwardTwo = Square(square.file, square.rank + direction * 2)
                if (!state.pieces.containsKey(forwardTwo)) {
                    moves.add(Move(square, forwardTwo))
                }
            }
        }

        // Captures
        val captureFiles = listOf(square.file - 1, square.file + 1)
        for (f in captureFiles) {
            val captureSquare = Square(f, square.rank + direction)
            if (isValidSquare(captureSquare)) {
                val targetPiece = state.pieces[captureSquare]
                if (targetPiece != null && targetPiece.color != color) {
                    addPawnMove(moves, square, captureSquare, promotionRank, isCapture = true)
                } else if (captureSquare == state.enPassantTarget) {
                    moves.add(Move(square, captureSquare, isCapture = true, isEnPassant = true))
                }
            }
        }

        return moves
    }

    private fun addPawnMove(
        moves: MutableList<Move>, 
        from: Square, 
        to: Square, 
        promotionRank: Int, 
        isCapture: Boolean = false
    ) {
        if (to.rank == promotionRank) {
            moves.add(Move(from, to, promotion = PieceType.QUEEN, isCapture = isCapture))
            moves.add(Move(from, to, promotion = PieceType.ROOK, isCapture = isCapture))
            moves.add(Move(from, to, promotion = PieceType.BISHOP, isCapture = isCapture))
            moves.add(Move(from, to, promotion = PieceType.KNIGHT, isCapture = isCapture))
        } else {
            moves.add(Move(from, to, isCapture = isCapture))
        }
    }

    private fun getStepMoves(state: BoardState, square: Square, color: PieceColor, offsets: List<Pair<Int, Int>>): List<Move> {
        val moves = mutableListOf<Move>()
        for ((df, dr) in offsets) {
            val target = Square(square.file + df, square.rank + dr)
            if (isValidSquare(target)) {
                val targetPiece = state.pieces[target]
                if (targetPiece == null) {
                    moves.add(Move(square, target))
                } else if (targetPiece.color != color) {
                    moves.add(Move(square, target, isCapture = true))
                }
            }
        }
        return moves
    }

    private fun getKingMoves(state: BoardState, square: Square, color: PieceColor): List<Move> {
        val moves = getStepMoves(state, square, color, kingOffsets).toMutableList()
        
        // Castling (pseudo-legal checks only, Engine filters if passing through check)
        val rank = if (color == PieceColor.WHITE) 7 else 0
        val kingside = if (color == PieceColor.WHITE) state.whiteKingsideCastle else state.blackKingsideCastle
        val queenside = if (color == PieceColor.WHITE) state.whiteQueensideCastle else state.blackQueensideCastle

        if (square.rank == rank && square.file == 4) {
            // Kingside
            if (kingside && !state.pieces.containsKey(Square(5, rank)) && !state.pieces.containsKey(Square(6, rank))) {
                moves.add(Move(square, Square(6, rank), isKingsideCastle = true))
            }
            // Queenside
            if (queenside && !state.pieces.containsKey(Square(3, rank)) && !state.pieces.containsKey(Square(2, rank)) && !state.pieces.containsKey(Square(1, rank))) {
                moves.add(Move(square, Square(2, rank), isQueensideCastle = true))
            }
        }
        
        return moves
    }

    private fun getSlidingMoves(state: BoardState, square: Square, color: PieceColor, directions: List<Pair<Int, Int>>): List<Move> {
        val moves = mutableListOf<Move>()
        for ((df, dr) in directions) {
            var current = Square(square.file + df, square.rank + dr)
            while (isValidSquare(current)) {
                val targetPiece = state.pieces[current]
                if (targetPiece == null) {
                    moves.add(Move(square, current))
                } else {
                    if (targetPiece.color != color) {
                        moves.add(Move(square, current, isCapture = true))
                    }
                    break // Stop sliding after hitting a piece
                }
                current = Square(current.file + df, current.rank + dr)
            }
        }
        return moves
    }

    fun isSquareAttacked(state: BoardState, square: Square?, byColor: PieceColor): Boolean {
        if (square == null) return false
        
        // Check Knight attacks
        for ((df, dr) in knightOffsets) {
            val target = Square(square.file + df, square.rank + dr)
            if (isValidSquare(target) && state.pieces[target]?.color == byColor && state.pieces[target]?.type == PieceType.KNIGHT) {
                return true
            }
        }
        
        // Check King attacks
        for ((df, dr) in kingOffsets) {
            val target = Square(square.file + df, square.rank + dr)
            if (isValidSquare(target) && state.pieces[target]?.color == byColor && state.pieces[target]?.type == PieceType.KING) {
                return true
            }
        }
        
        // Check Pawn attacks
        val pawnDir = if (byColor == PieceColor.WHITE) 1 else -1 // If white is attacking, pawns come from above (rank + 1)
        for (df in listOf(-1, 1)) {
            val target = Square(square.file + df, square.rank + pawnDir)
            if (isValidSquare(target) && state.pieces[target]?.color == byColor && state.pieces[target]?.type == PieceType.PAWN) {
                return true
            }
        }
        
        // Check sliding attacks
        if (isAttackedBySlider(state, square, byColor, rookDirections, listOf(PieceType.ROOK, PieceType.QUEEN))) return true
        if (isAttackedBySlider(state, square, byColor, bishopDirections, listOf(PieceType.BISHOP, PieceType.QUEEN))) return true
        
        return false
    }

    private fun isAttackedBySlider(
        state: BoardState, 
        square: Square, 
        byColor: PieceColor, 
        directions: List<Pair<Int, Int>>, 
        types: List<PieceType>
    ): Boolean {
        for ((df, dr) in directions) {
            var current = Square(square.file + df, square.rank + dr)
            while (isValidSquare(current)) {
                val piece = state.pieces[current]
                if (piece != null) {
                    if (piece.color == byColor && types.contains(piece.type)) {
                        return true
                    }
                    break // Blocked
                }
                current = Square(current.file + df, current.rank + dr)
            }
        }
        return false
    }

    private fun isValidSquare(square: Square): Boolean {
        return square.file in 0..7 && square.rank in 0..7
    }

    fun applyMovePseudo(state: BoardState, move: Move): BoardState {
        // Quickly generate next board state without full logic just for check detection
        val newPieces = state.pieces.toMutableMap()
        val movingPiece = newPieces.remove(move.from) ?: return state
        
        if (move.isEnPassant) {
            val epRank = if (movingPiece.color == PieceColor.WHITE) move.to.rank + 1 else move.to.rank - 1
            newPieces.remove(Square(move.to.file, epRank))
        }
        
        newPieces[move.to] = movingPiece
        
        return state.copy(pieces = newPieces)
    }
}
