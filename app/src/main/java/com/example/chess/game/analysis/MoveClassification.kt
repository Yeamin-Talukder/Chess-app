package com.example.chess.game.analysis

import androidx.compose.ui.graphics.Color

enum class MoveCategory(val title: String, val color: Color) {
    BEST("Best", Color(0xFF81B64C)),
    EXCELLENT("Excellent", Color(0xFF7FA650)),
    GOOD("Good", Color(0xFF95BB4A)),
    INACCURACY("Inaccuracy", Color(0xFFF3C242)),
    MISTAKE("Mistake", Color(0xFFE88138)),
    BLUNDER("Blunder", Color(0xFFB33430)),
    BOOK("Book", Color(0xFFD5A47D)),
    FORCED("Forced", Color(0xFF7490A2)),
    UNKNOWN("Unknown", Color.Gray)
}

data class MoveClassification(
    val category: MoveCategory,
    val cpLoss: Float
)
