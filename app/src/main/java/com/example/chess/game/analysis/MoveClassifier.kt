package com.example.chess.game.analysis

import kotlin.math.abs

object MoveClassifier {

    /**
     * Classifies a player's move based on the difference (cp loss) between the engine's 
     * evaluation of the board before the move and after the move.
     * 
     * @param engineEvalBefore Move eval in pawns (e.g. +1.5)
     * @param engineEvalAfter Move eval in pawns (e.g. +0.8)
     * @param isWhiteTurn True if the player who moved was White
     * @return MoveClassification
     */
    fun classifyMove(engineEvalBefore: Float, engineEvalAfter: Float, isWhiteTurn: Boolean): MoveClassification {
        // Convert to centipawns
        val beforeCp = (engineEvalBefore * 100).toInt()
        val afterCp = (engineEvalAfter * 100).toInt()
        
        // Calculate cp loss from the player's perspective
        // If White plays, eval dropping from 150 to 50 means 100 cp loss.
        // If Black plays, eval rising from -150 to -50 means 100 cp loss.
        val cpLoss = if (isWhiteTurn) {
            beforeCp - afterCp
        } else {
            afterCp - beforeCp
        }
        
        val absLoss = cpLoss.coerceAtLeast(0).toFloat()
        
        val category = when {
            absLoss <= 20 -> MoveCategory.BEST
            absLoss <= 50 -> MoveCategory.EXCELLENT
            absLoss <= 100 -> MoveCategory.GOOD
            absLoss <= 200 -> MoveCategory.INACCURACY
            absLoss <= 400 -> MoveCategory.MISTAKE
            else -> MoveCategory.BLUNDER
        }
        
        return MoveClassification(category, absLoss / 100f)
    }

    /**
     * Classifies considering forced mates.
     */
    fun classifyMoveWithMate(
        beforeMate: Int?,
        afterMate: Int?,
        beforeEval: Float,
        afterEval: Float,
        isWhiteTurn: Boolean
    ): MoveClassification {
        if (beforeMate != null && afterMate != null) {
            // Both are mates
            val playerBeforeMate = if (isWhiteTurn) beforeMate else -beforeMate
            val playerAfterMate = if (isWhiteTurn) afterMate else -afterMate
            
            if (playerBeforeMate > 0) { // We had a forced mate
                if (playerAfterMate > 0 && playerAfterMate <= playerBeforeMate) {
                    return MoveClassification(MoveCategory.BEST, 0f)
                } else if (playerAfterMate > 0) {
                    return MoveClassification(MoveCategory.EXCELLENT, 0f)
                } else {
                    return MoveClassification(MoveCategory.BLUNDER, 9.9f) // Missed mate
                }
            } else { // We were getting mated
                if (playerAfterMate < 0) {
                    return MoveClassification(MoveCategory.FORCED, 0f) // Still getting mated
                } else {
                    return MoveClassification(MoveCategory.BEST, 0f) // Escaped mate (shouldn't happen against perfect play)
                }
            }
        } else if (beforeMate != null) {
            // Had a mate, but lost it
            val playerBeforeMate = if (isWhiteTurn) beforeMate else -beforeMate
            if (playerBeforeMate > 0) {
                return MoveClassification(MoveCategory.BLUNDER, 9.9f) // Missed mate
            } else {
                return MoveClassification(MoveCategory.BEST, 0f) // Escaped mate!
            }
        } else if (afterMate != null) {
            // Found a mate, or blundered into one
            val playerAfterMate = if (isWhiteTurn) afterMate else -afterMate
            if (playerAfterMate > 0) {
                return MoveClassification(MoveCategory.BEST, 0f) // Found mate
            } else {
                return MoveClassification(MoveCategory.BLUNDER, 9.9f) // Blundered into mate
            }
        }
        
        // No mates involved
        return classifyMove(beforeEval, afterEval, isWhiteTurn)
    }
}
