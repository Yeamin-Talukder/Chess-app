package com.example.chess.game.board
import androidx.compose.runtime.Immutable

data class Square(val file: Int, val rank: Int) {
    val index: Int get() = rank * 8 + file
    val notation: String get() = "${('a' + file)}${8 - rank}"

    companion object {
        fun fromIndex(index: Int) = Square(index % 8, index / 8)
        fun fromNotation(notation: String): Square? {
            if (notation.length != 2) return null
            val f = notation[0] - 'a'
            val r = 8 - (notation[1] - '0')
            if (f !in 0..7 || r !in 0..7) return null
            return Square(f, r)
        }
    }
}

enum class PieceColor {
    WHITE, BLACK;
    fun opposite() = if (this == WHITE) BLACK else WHITE
}

enum class PieceType {
    PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING
}

data class ChessPiece(
    val id: String,
    val type: PieceType,
    val color: PieceColor
) {
    val unicode: String
        get() = when (color) {
            PieceColor.WHITE -> when (type) {
                PieceType.KING -> "♔"
                PieceType.QUEEN -> "♕"
                PieceType.ROOK -> "♖"
                PieceType.BISHOP -> "♗"
                PieceType.KNIGHT -> "♘"
                PieceType.PAWN -> "♙"
            }
            PieceColor.BLACK -> when (type) {
                PieceType.KING -> "♚"
                PieceType.QUEEN -> "♛"
                PieceType.ROOK -> "♜"
                PieceType.BISHOP -> "♝"
                PieceType.KNIGHT -> "♞"
                PieceType.PAWN -> "♟"
            }
        }
}

data class Move(
    val from: Square,
    val to: Square,
    val promotion: PieceType? = null,
    val isCapture: Boolean = false,
    val isCheck: Boolean = false,
    val isKingsideCastle: Boolean = false,
    val isQueensideCastle: Boolean = false,
    val isEnPassant: Boolean = false
) {
    val notation: String get() = "${from.notation}${to.notation}${promotion?.name?.first()?.lowercase() ?: ""}"
}

enum class GameStatus {
    ONGOING,
    CHECKMATE,
    STALEMATE,
    DRAW_REPETITION,
    DRAW_FIFTY_MOVE,
    DRAW_INSUFFICIENT_MATERIAL,
    RESIGNED,
    DRAW_AGREED,
    TIME_OUT
}

@Immutable
data class BoardState(
    val pieces: Map<Square, ChessPiece> = emptyMap(),
    val turn: PieceColor = PieceColor.WHITE,
    val lastMove: Move? = null,
    val inCheckSquare: Square? = null,
    val legalMovesForSelected: List<Move> = emptyList(),
    val gameStatus: GameStatus = GameStatus.ONGOING,
    val whiteTimeMillis: Long = 10 * 60 * 1000L,
    val blackTimeMillis: Long = 10 * 60 * 1000L,
    
    // FEN / State tracking parts
    val whiteKingsideCastle: Boolean = true,
    val whiteQueensideCastle: Boolean = true,
    val blackKingsideCastle: Boolean = true,
    val blackQueensideCastle: Boolean = true,
    val enPassantTarget: Square? = null,
    val halfMoveClock: Int = 0,
    val fullMoveNumber: Int = 1,
    
    // Hash counts for 3-fold repetition
    val positionHashes: Map<Int, Int> = emptyMap(),
    
    // Move history
    val moveHistory: List<Move> = emptyList(),
    
    // Captured pieces
    val capturedPieces: List<ChessPiece> = emptyList()
) {
    companion object {
        fun initial(): BoardState {
            val initialPieces = mutableMapOf<Square, ChessPiece>()
            
            // Black pieces
            initialPieces[Square(0, 0)] = ChessPiece("rook_b_1", PieceType.ROOK, PieceColor.BLACK)
            initialPieces[Square(1, 0)] = ChessPiece("knight_b_1", PieceType.KNIGHT, PieceColor.BLACK)
            initialPieces[Square(2, 0)] = ChessPiece("bishop_b_1", PieceType.BISHOP, PieceColor.BLACK)
            initialPieces[Square(3, 0)] = ChessPiece("queen_b", PieceType.QUEEN, PieceColor.BLACK)
            initialPieces[Square(4, 0)] = ChessPiece("king_b", PieceType.KING, PieceColor.BLACK)
            initialPieces[Square(5, 0)] = ChessPiece("bishop_b_2", PieceType.BISHOP, PieceColor.BLACK)
            initialPieces[Square(6, 0)] = ChessPiece("knight_b_2", PieceType.KNIGHT, PieceColor.BLACK)
            initialPieces[Square(7, 0)] = ChessPiece("rook_b_2", PieceType.ROOK, PieceColor.BLACK)
            for (i in 0..7) {
                initialPieces[Square(i, 1)] = ChessPiece("pawn_b_$i", PieceType.PAWN, PieceColor.BLACK)
            }

            // White pieces
            for (i in 0..7) {
                initialPieces[Square(i, 6)] = ChessPiece("pawn_w_$i", PieceType.PAWN, PieceColor.WHITE)
            }
            initialPieces[Square(0, 7)] = ChessPiece("rook_w_1", PieceType.ROOK, PieceColor.WHITE)
            initialPieces[Square(1, 7)] = ChessPiece("knight_w_1", PieceType.KNIGHT, PieceColor.WHITE)
            initialPieces[Square(2, 7)] = ChessPiece("bishop_w_1", PieceType.BISHOP, PieceColor.WHITE)
            initialPieces[Square(3, 7)] = ChessPiece("queen_w", PieceType.QUEEN, PieceColor.WHITE)
            initialPieces[Square(4, 7)] = ChessPiece("king_w", PieceType.KING, PieceColor.WHITE)
            initialPieces[Square(5, 7)] = ChessPiece("bishop_w_2", PieceType.BISHOP, PieceColor.WHITE)
            initialPieces[Square(6, 7)] = ChessPiece("knight_w_2", PieceType.KNIGHT, PieceColor.WHITE)
            initialPieces[Square(7, 7)] = ChessPiece("rook_w_2", PieceType.ROOK, PieceColor.WHITE)

            return BoardState(pieces = initialPieces)
        }

        fun fromFen(fen: String): BoardState {
            val parts = fen.split(" ")
            if (parts.size < 4) return initial()
            
            val pieces = mutableMapOf<Square, ChessPiece>()
            val boardPart = parts[0]
            var rank = 0
            var file = 0
            var pieceId = 0
            
            for (char in boardPart) {
                if (char == '/') {
                    rank++
                    file = 0
                } else if (char.isDigit()) {
                    file += char.digitToInt()
                } else {
                    val color = if (char.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
                    val type = when(char.lowercaseChar()) {
                        'p' -> PieceType.PAWN
                        'n' -> PieceType.KNIGHT
                        'b' -> PieceType.BISHOP
                        'r' -> PieceType.ROOK
                        'q' -> PieceType.QUEEN
                        'k' -> PieceType.KING
                        else -> PieceType.PAWN
                    }
                    pieces[Square(file, rank)] = ChessPiece("p_${pieceId++}", type, color)
                    file++
                }
            }
            
            val turn = if (parts[1] == "w") PieceColor.WHITE else PieceColor.BLACK
            
            val castling = parts[2]
            val wkc = castling.contains('K')
            val wqc = castling.contains('Q')
            val bkc = castling.contains('k')
            val bqc = castling.contains('q')
            
            val ep = if (parts[3] != "-") Square.fromNotation(parts[3]) else null
            val halfMove = if (parts.size > 4) parts[4].toIntOrNull() ?: 0 else 0
            val fullMove = if (parts.size > 5) parts[5].toIntOrNull() ?: 1 else 1
            
            return BoardState(
                pieces = pieces,
                turn = turn,
                whiteKingsideCastle = wkc,
                whiteQueensideCastle = wqc,
                blackKingsideCastle = bkc,
                blackQueensideCastle = bqc,
                enPassantTarget = ep,
                halfMoveClock = halfMove,
                fullMoveNumber = fullMove
            )
        }
    }
}
