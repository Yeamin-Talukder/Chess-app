package com.example.chess.ui.components

import androidx.compose.runtime.compositionLocalOf

data class BoardSettings(
    val boardColors: String = "Wood",
    val pieceStyle: String = "Classic",
    val animationSpeed: Float = 1.0f,
    val boardRotation: Boolean = false,
    val coordinates: Boolean = true,
    val legalMoveHighlight: Boolean = true
)

val LocalBoardSettings = compositionLocalOf { BoardSettings() }
