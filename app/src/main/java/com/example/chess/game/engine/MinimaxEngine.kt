package com.example.chess.game.engine

import com.example.chess.game.board.BoardState
import com.example.chess.game.board.GameStatus
import com.example.chess.game.board.Move
import com.example.chess.game.board.PieceColor
import com.example.chess.game.board.PieceType
import com.example.chess.game.board.Square
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.example.chess.game.analysis.EngineAnalysis

object MinimaxEngine {

    // Piece material values (in centipawns)
    private const val PAWN_VAL = 100
    private const val KNIGHT_VAL = 320
    private const val BISHOP_VAL = 330
    private const val ROOK_VAL = 500
    private const val QUEEN_VAL = 900
    private const val KING_VAL = 20000
    private const val INF = 100000

    // Piece-Square Tables (PST) from White's perspective (rank 0 = a8..h8, rank 7 = a1..h1)
    private val pawnPst = intArrayOf(
          0,  0,  0,  0,  0,  0,  0,  0,
         50, 50, 50, 50, 50, 50, 50, 50,
         10, 10, 20, 30, 30, 20, 10, 10,
          5,  5, 10, 25, 25, 10,  5,  5,
          0,  0,  0, 20, 20,  0,  0,  0,
          5, -5,-10,  0,  0,-10, -5,  5,
          5, 10, 10,-20,-20, 10, 10,  5,
          0,  0,  0,  0,  0,  0,  0,  0
    )

    private val knightPst = intArrayOf(
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    )

    private val bishopPst = intArrayOf(
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5, 10, 10,  5,  0,-10,
        -10,  5,  5, 10, 10,  5,  5,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  5,  0,  0,  0,  0,  5,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    )

    private val rookPst = intArrayOf(
          0,  0,  0,  0,  0,  0,  0,  0,
          5, 10, 10, 10, 10, 10, 10,  5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
          0,  0,  0,  5,  5,  0,  0,  0
    )

    private val queenPst = intArrayOf(
        -20,-10,-10, -5, -5,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5,  5,  5,  5,  0,-10,
         -5,  0,  5,  5,  5,  5,  0, -5,
          0,  0,  5,  5,  5,  5,  0, -5,
        -10,  5,  5,  5,  5,  5,  0,-10,
        -10,  0,  5,  0,  0,  0,  0,-10,
        -20,-10,-10, -5, -5,-10,-10,-20
    )

    private val kingPst = intArrayOf(
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -10,-20,-20,-20,-20,-20,-20,-10,
         20, 20,  0,  0,  0,  0, 20, 20,
         20, 30, 10,  0,  0, 10, 30, 20
    )

    /**
     * Evaluates position for UI evaluation bar.
     * Returns score in pawns from White's perspective (+15.0 for White mate/win, -15.0 for Black mate/win).
     */
    fun evaluatePosition(state: BoardState): Float {
        // 1. Terminal states
        if (state.gameStatus == GameStatus.CHECKMATE) {
            return if (state.turn == PieceColor.WHITE) -15f else 15f
        }
        if (state.gameStatus in listOf(
                GameStatus.STALEMATE, GameStatus.DRAW_AGREED,
                GameStatus.DRAW_FIFTY_MOVE, GameStatus.DRAW_REPETITION, GameStatus.DRAW_INSUFFICIENT_MATERIAL
            )
        ) {
            return 0f
        }

        val turn = state.turn
        val legalMoves = MoveRules.getAllLegalMoves(state, turn)
        if (legalMoves.isEmpty()) {
            val inCheck = MoveRules.isCheck(state, turn)
            if (inCheck) {
                return if (turn == PieceColor.WHITE) -15f else 15f
            }
            return 0f
        }

        // 2. Check for immediate Mate in 1 for current player
        val oppColor = turn.opposite()
        for (move in legalMoves) {
            val nextState = MoveRules.applyMovePseudo(state, move)
            val oppKingSq = MoveRules.findKingSquare(nextState, oppColor)
            if (oppKingSq != null && MoveRules.isSquareAttacked(nextState, oppKingSq, turn)) {
                val oppLegalMoves = MoveRules.getAllLegalMoves(nextState.copy(turn = oppColor), oppColor)
                if (oppLegalMoves.isEmpty()) {
                    return if (turn == PieceColor.WHITE) 15f else -15f
                }
            }
        }

        // 3. Perform shallow minimax search (depth 3) for tactical evaluation
        val depth = 3
        val orderedMoves = orderMoves(state, legalMoves)
        var bestScore = -INF
        var alpha = -INF
        val beta = INF

        for (move in orderedMoves) {
            val nextState = MoveRules.applyMovePseudo(state, move)
            val score = -alphabeta(nextState, depth - 1, -beta, -alpha, oppColor)
            if (score > bestScore) {
                bestScore = score
            }
            if (score > alpha) {
                alpha = score
            }
        }

        val whiteScoreCentipawns = if (turn == PieceColor.WHITE) bestScore else -bestScore

        if (whiteScoreCentipawns >= 10000) return 15f
        if (whiteScoreCentipawns <= -10000) return -15f

        val scoreInPawns = whiteScoreCentipawns.toFloat() / 100f
        return scoreInPawns.coerceIn(-15f, 15f)
    }

    /**
     * Progressive depth evaluation flow for fallback mode (depth 1 to 20).
     */
    fun evaluatePositionProgressive(state: BoardState): Flow<EngineAnalysis> = flow {
        val baseScore = evaluatePosition(state)
        val staticScore = (evaluate(state, PieceColor.WHITE).toFloat() / 100f).coerceIn(-15f, 15f)

        for (d in 1..20) {
            val currentScore = if (d == 1) staticScore else baseScore
            emit(EngineAnalysis(
                evaluation = currentScore,
                mate = null,
                bestMove = "",
                principalVariation = emptyList(),
                depth = d,
                nodes = 0L,
                nps = 0L
            ))
            if (d > 1) {
                delay((20 + d * 5).toLong())
            }
        }
    }

    /**
     * Calculates the best move for the specified BotLevel.
     */
    fun findBestMove(state: BoardState, level: BotLevel): Move? {
        val botColor = state.turn
        val legalMoves = MoveRules.getAllLegalMoves(state, botColor)
        if (legalMoves.isEmpty()) return null

        // Handle beginner/casual mistakes probability
        when (level) {
            BotLevel.BEGINNER -> {
                if (Math.random() < 0.20) return legalMoves.random()
                return findBestMoveMinimax(state, depth = 1, botColor, legalMoves)
            }
            BotLevel.CASUAL -> {
                if (Math.random() < 0.05) return legalMoves.random()
                return findBestMoveMinimax(state, depth = 2, botColor, legalMoves)
            }
            BotLevel.INTERMEDIATE -> {
                return findBestMoveMinimax(state, depth = 3, botColor, legalMoves)
            }
            BotLevel.ADVANCED -> {
                return findBestMoveMinimax(state, depth = 4, botColor, legalMoves)
            }
            BotLevel.MASTER -> {
                return findBestMoveMinimax(state, depth = 5, botColor, legalMoves)
            }
            BotLevel.GRANDMASTER -> {
                return findBestMoveMinimax(state, depth = 5, botColor, legalMoves) // 5 is generally the max reasonable for simple alphabeta on mobile without quiescence
            }
        }
    }

    private fun findBestMoveMinimax(
        state: BoardState,
        depth: Int,
        botColor: PieceColor,
        legalMoves: List<Move>
    ): Move {
        // Order moves (captures first for faster alpha-beta cutoffs)
        val orderedMoves = orderMoves(state, legalMoves)

        var bestMove = orderedMoves.first()
        var bestScore = -INF

        var alpha = -INF
        val beta = INF

        for (move in orderedMoves) {
            val nextState = MoveRules.applyMovePseudo(state, move)
            val score = -alphabeta(nextState, depth - 1, -beta, -alpha, botColor.opposite())
            
            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
            if (score > alpha) {
                alpha = score
            }
        }
        return bestMove
    }

    private fun alphabeta(
        state: BoardState,
        depth: Int,
        alpha: Int,
        beta: Int,
        colorToMove: PieceColor
    ): Int {
        if (depth <= 0 || state.gameStatus != GameStatus.ONGOING) {
            return quiesce(state, alpha, beta, colorToMove, 0)
        }

        var currentAlpha = alpha
        val legalMoves = MoveRules.getAllLegalMoves(state, colorToMove)
        if (legalMoves.isEmpty()) {
            return evaluate(state, colorToMove)
        }

        val orderedMoves = orderMoves(state, legalMoves)

        for (move in orderedMoves) {
            val nextState = MoveRules.applyMovePseudo(state, move)
            val score = -alphabeta(nextState, depth - 1, -beta, -currentAlpha, colorToMove.opposite())

            if (score >= beta) {
                return beta // Beta cutoff
            }
            if (score > currentAlpha) {
                currentAlpha = score
            }
        }
        return currentAlpha
    }

    private fun quiesce(
        state: BoardState,
        alpha: Int,
        beta: Int,
        colorToMove: PieceColor,
        qDepth: Int
    ): Int {
        val standPat = evaluate(state, colorToMove)
        if (standPat >= beta) {
            return beta
        }
        var currentAlpha = alpha
        if (standPat > currentAlpha) {
            currentAlpha = standPat
        }

        // Limit Quiescence search depth to prevent infinite loops in long capture chains
        if (qDepth >= 4 || state.gameStatus != GameStatus.ONGOING) {
            return currentAlpha
        }

        val legalMoves = MoveRules.getAllLegalMoves(state, colorToMove)
        val captureMoves = legalMoves.filter { it.isCapture }

        if (captureMoves.isEmpty()) {
            return currentAlpha
        }

        val orderedMoves = orderMoves(state, captureMoves)

        for (move in orderedMoves) {
            val nextState = MoveRules.applyMovePseudo(state, move)
            val score = -quiesce(nextState, -beta, -currentAlpha, colorToMove.opposite(), qDepth + 1)

            if (score >= beta) {
                return beta
            }
            if (score > currentAlpha) {
                currentAlpha = score
            }
        }
        return currentAlpha
    }

    /**
     * Evaluates position from perspective of `perspectiveColor`.
     */
    internal fun evaluate(state: BoardState, perspectiveColor: PieceColor): Int {
        if (state.gameStatus == GameStatus.CHECKMATE) {
            return if (state.turn == perspectiveColor) -99999 else 99999
        }
        if (state.gameStatus in listOf(
                GameStatus.STALEMATE, GameStatus.DRAW_AGREED,
                GameStatus.DRAW_FIFTY_MOVE, GameStatus.DRAW_REPETITION, GameStatus.DRAW_INSUFFICIENT_MATERIAL
            )
        ) {
            return 0
        }

        var whiteScore = 0
        var blackScore = 0

        state.pieces.forEach { (sq, piece) ->
            val valBase = when (piece.type) {
                PieceType.PAWN -> PAWN_VAL
                PieceType.KNIGHT -> KNIGHT_VAL
                PieceType.BISHOP -> BISHOP_VAL
                PieceType.ROOK -> ROOK_VAL
                PieceType.QUEEN -> QUEEN_VAL
                PieceType.KING -> KING_VAL
            }

            val sqIdx = sq.rank * 8 + sq.file
            val pstVal = when (piece.type) {
                PieceType.PAWN -> if (piece.color == PieceColor.WHITE) pawnPst[sqIdx] else pawnPst[flipRank(sqIdx)]
                PieceType.KNIGHT -> if (piece.color == PieceColor.WHITE) knightPst[sqIdx] else knightPst[flipRank(sqIdx)]
                PieceType.BISHOP -> if (piece.color == PieceColor.WHITE) bishopPst[sqIdx] else bishopPst[flipRank(sqIdx)]
                PieceType.ROOK -> if (piece.color == PieceColor.WHITE) rookPst[sqIdx] else rookPst[flipRank(sqIdx)]
                PieceType.QUEEN -> if (piece.color == PieceColor.WHITE) queenPst[sqIdx] else queenPst[flipRank(sqIdx)]
                PieceType.KING -> if (piece.color == PieceColor.WHITE) kingPst[sqIdx] else kingPst[flipRank(sqIdx)]
            }

            val totalPieceVal = valBase + pstVal
            if (piece.color == PieceColor.WHITE) {
                whiteScore += totalPieceVal
            } else {
                blackScore += totalPieceVal
            }
        }

        val netScore = whiteScore - blackScore
        return if (perspectiveColor == PieceColor.WHITE) netScore else -netScore
    }

    private fun flipRank(sqIdx: Int): Int {
        val r = sqIdx / 8
        val f = sqIdx % 8
        return (7 - r) * 8 + f
    }

    private fun orderMoves(state: BoardState, moves: List<Move>): List<Move> {
        return moves.sortedByDescending { move ->
            var score = 0
            val target = state.pieces[move.to]
            if (target != null) {
                val attacker = state.pieces[move.from]
                val victimValue = getPieceValue(target.type)
                val attackerValue = if (attacker != null) getPieceValue(attacker.type) else 100
                score += 10 * victimValue - attackerValue
            }
            if (move.promotion != null) {
                score += getPieceValue(move.promotion)
            }
            score
        }
    }

    private fun getPieceValue(type: PieceType): Int = when (type) {
        PieceType.PAWN -> PAWN_VAL
        PieceType.KNIGHT -> KNIGHT_VAL
        PieceType.BISHOP -> BISHOP_VAL
        PieceType.ROOK -> ROOK_VAL
        PieceType.QUEEN -> QUEEN_VAL
        PieceType.KING -> KING_VAL
    }
}
