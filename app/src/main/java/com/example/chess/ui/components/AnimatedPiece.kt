package com.example.chess.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.game.board.ChessPiece
import com.example.chess.game.board.PieceType
import com.example.chess.game.board.Square
import kotlin.math.roundToInt
import com.example.chess.ui.components.LocalBoardSettings

@Composable
fun AnimatedPiece(
    piece: ChessPiece,
    square: Square,
    squareSizePx: Float,
    isFlipped: Boolean,
    isCaptured: Boolean,
    onClick: () -> Unit
) {
    // Calculate logical x,y based on flip state
    val fileIndex = if (isFlipped) 7 - square.file else square.file
    val rankIndex = if (isFlipped) 7 - square.rank else square.rank

    val boardSettings = LocalBoardSettings.current
    
    val targetOffset = Offset(
        x = fileIndex * squareSizePx,
        y = rankIndex * squareSizePx
    )

    // Animate the offset smoothly
    val animatedOffset by animateOffsetAsState(
        targetValue = targetOffset,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = (500f * boardSettings.animationSpeed).coerceAtLeast(100f)
        ),
        label = "piece_movement_${piece.id}"
    )

    // Handle disappearance when captured
    AnimatedVisibility(
        visible = !isCaptured,
        exit = fadeOut() + scaleOut(targetScale = 0.5f),
        modifier = Modifier.offset {
            IntOffset(animatedOffset.x.roundToInt(), animatedOffset.y.roundToInt())
        }
    ) {
        val squareSizeDp = (squareSizePx / androidx.compose.ui.platform.LocalDensity.current.density).dp
        
        Box(
            modifier = Modifier
                .size(squareSizeDp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            val solidUnicode = when (piece.type) {
                PieceType.KING -> "♚"
                PieceType.QUEEN -> "♛"
                PieceType.ROOK -> "♜"
                PieceType.BISHOP -> "♝"
                PieceType.KNIGHT -> "♞"
                PieceType.PAWN -> "♟"
            }
            
            val isWhite = piece.color == com.example.chess.game.board.PieceColor.WHITE
            
            // Adjust appearance based on piece style setting
            val outlineColor: Color
            val fillColor: Color
            val strokeWidth: Float
            
            when (boardSettings.pieceStyle) {
                "Modern" -> {
                    // Modern: transparent fill, strong colored outline
                    outlineColor = if (isWhite) Color(0xFFE0E0E0) else Color(0xFF212121)
                    fillColor = Color.Transparent
                    strokeWidth = 9f
                }
                "Minimalist" -> {
                    // Minimalist: pure fill, no outline
                    outlineColor = Color.Transparent
                    fillColor = if (isWhite) Color(0xFFFAFAFA) else Color(0xFF121212)
                    strokeWidth = 0f
                }
                else -> {
                    // Classic
                    outlineColor = if (isWhite) Color.Black else Color.White
                    fillColor = if (isWhite) Color.White else Color.Black
                    strokeWidth = 7f
                }
            }
            
            // Outline layer
            if (strokeWidth > 0f) {
                Text(
                    text = solidUnicode,
                    style = TextStyle(
                        fontSize = (squareSizeDp.value * 0.82f).sp,
                        color = outlineColor,
                        drawStyle = Stroke(
                            miter = 10f,
                            width = strokeWidth,
                            join = StrokeJoin.Round
                        )
                    )
                )
            }
            
            // Fill layer
            if (fillColor != Color.Transparent) {
                Text(
                    text = solidUnicode,
                    style = TextStyle(
                        fontSize = (squareSizeDp.value * 0.82f).sp,
                        color = fillColor
                    )
                )
            }
        }
    }
}
