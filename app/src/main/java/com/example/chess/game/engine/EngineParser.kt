package com.example.chess.game.engine

import com.example.chess.game.analysis.EngineAnalysis
import android.util.Log

object EngineParser {
    
    fun parseAnalysis(line: String, bestMoveLine: String? = null, isBlackTurn: Boolean = false): EngineAnalysis? {
        if (!line.startsWith("info") || !line.contains("depth")) return null
        
        try {
            val depth = Regex("depth (\\d+)").find(line)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val nodes = Regex("nodes (\\d+)").find(line)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            val nps = Regex("nps (\\d+)").find(line)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            
            var score: Float = 0f
            var mate: Int? = null
            
            if (line.contains("score cp")) {
                val cpMatch = Regex("score cp (-?\\d+)").find(line)
                if (cpMatch != null) {
                    score = cpMatch.groupValues[1].toFloat() / 100f
                    if (isBlackTurn) score = -score
                }
            } else if (line.contains("score mate")) {
                val mateMatch = Regex("score mate (-?\\d+)").find(line)
                if (mateMatch != null) {
                    mate = mateMatch.groupValues[1].toInt()
                    score = if (mate > 0) 15f else -15f
                    if (isBlackTurn) {
                        mate = -mate
                        score = -score
                    }
                }
            }
            
            val pvString = line.substringAfter(" pv ", "").trim()
            val pvList = if (pvString.isNotEmpty()) pvString.split(" ") else emptyList()
            
            // Best move might come in this line's PV, or we wait for bestmove line
            var bestMove = pvList.firstOrNull() ?: ""
            
            if (bestMoveLine != null && bestMoveLine.startsWith("bestmove")) {
                val parts = bestMoveLine.split(" ")
                if (parts.size >= 2) {
                    bestMove = parts[1]
                }
            }
            
            return EngineAnalysis(
                evaluation = score,
                mate = mate,
                bestMove = bestMove,
                principalVariation = pvList,
                depth = depth,
                nodes = nodes,
                nps = nps
            )
        } catch (e: Exception) {
            Log.e("EngineParser", "Error parsing info line: $line", e)
            return null
        }
    }
}
