package com.example.chess.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.game.board.BoardState
import com.example.chess.game.board.GameStatus
import com.example.chess.game.board.Square
import kotlin.math.roundToInt

// Base highlight colors
private val ChessLastMoveHighlight = Color(0xFFB5D66E)
private val ChessSelectionHighlight = Color(0xFFF5F682)
private val ChessCheckHighlight = Color(0xFFE65100)

private fun getBoardColors(style: String): Pair<Color, Color> {
    return when(style) {
        "Green" -> Pair(Color(0xFFE9EDCC), Color(0xFF779556))
        "Blue" -> Pair(Color(0xFFDEE3E6), Color(0xFF8CA2AD))
        "Glass" -> Pair(Color(0xFFE0E0E0), Color(0xFF9E9E9E))
        "Tournament" -> Pair(Color(0xFFF0D9B5), Color(0xFFB58863))
        else -> Pair(Color(0xFFEEEED2), Color(0xFF769656)) // Wood
    }
}

// ── Verdict Stamp Composable ────────────────────────────────────────────────
@Composable
private fun VerdictStamp(status: GameStatus, modifier: Modifier = Modifier) {
    if (status == GameStatus.ONGOING) return

    val text = when (status) {
        GameStatus.CHECKMATE -> "CHECKMATE"
        GameStatus.STALEMATE, GameStatus.DRAW_REPETITION, 
        GameStatus.DRAW_FIFTY_MOVE, GameStatus.DRAW_INSUFFICIENT_MATERIAL, 
        GameStatus.DRAW_AGREED -> "DRAW"
        GameStatus.RESIGNED -> "RESIGNED"
        GameStatus.TIME_OUT -> "TIME OUT"
        else -> "GAME OVER"
    }
    val color = if (text == "CHECKMATE" || text == "TIME OUT") Color(0xFFE84A4A) else Color(0xFF4A90E2)

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(status) {
        kotlinx.coroutines.delay(300)
        visible = true
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.scaleIn(initialScale = 2f, animationSpec = tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing)) +
                androidx.compose.animation.fadeIn(animationSpec = tween(300)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .border(6.dp, color, RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(
                text = text,
                color = color,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                style = TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.5f), Offset(4f, 4f), 8f)),
                modifier = Modifier.rotate(-15f)
            )
        }
    }
}

@Composable
fun ChessBoardUI(
    boardState: BoardState,
    isFlipped: Boolean = false,
    selectedSquare: Square? = null,
    hintMove: String? = null, // e.g., "e2e4"
    hideVerdict: Boolean = false,
    onSquareClick: (Square) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val boardSettings = LocalBoardSettings.current
    val (ChessLightSquare, ChessDarkSquare) = getBoardColors(boardSettings.boardColors)

    // 1. Check Pulse Animation
    val infiniteTransition = rememberInfiniteTransition(label = "check_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // 2. Checkmate Shake Animation
    var shakeTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(boardState.gameStatus) {
        if (boardState.gameStatus == GameStatus.CHECKMATE) {
            shakeTrigger = true
            kotlinx.coroutines.delay(500)
            shakeTrigger = false
        }
    }
    val shakeOffset by animateFloatAsState(
        targetValue = if (shakeTrigger) 1f else 0f,
        animationSpec = keyframes {
            durationMillis = 500
            0f at 0
            -20f at 50
            20f at 100
            -15f at 150
            15f at 200
            -10f at 250
            10f at 300
            -5f at 350
            5f at 400
            0f at 500
        },
        label = "checkmate_shake"
    )

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .offset { IntOffset(shakeOffset.roundToInt(), 0) }
            .shadow(16.dp, RoundedCornerShape(4.dp))
            .border(4.dp, Color(0xFF2C2A27), RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
            .background(ChessDarkSquare)
    ) {
        val boardSizePx = constraints.maxWidth.toFloat()
        val squareSizePx = boardSizePx / 8f

        // 1. Draw Board, Highlights, and Coordinates
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isFlipped) {
                    detectTapGestures { offset ->
                        var file = (offset.x / squareSizePx).toInt().coerceIn(0, 7)
                        var rank = (offset.y / squareSizePx).toInt().coerceIn(0, 7)

                        if (isFlipped) {
                            file = 7 - file
                            rank = 7 - rank
                        }
                        onSquareClick(Square(file, rank))
                    }
                }
        ) {
            for (rank in 0 until 8) {
                for (file in 0 until 8) {
                    val visualFile = if (isFlipped) 7 - file else file
                    val visualRank = if (isFlipped) 7 - rank else rank

                    val isLight = (file + rank) % 2 == 0
                    val baseColor = if (isLight) ChessLightSquare else ChessDarkSquare

                    val topLeft = Offset(visualFile * squareSizePx, visualRank * squareSizePx)
                    val size = Size(squareSizePx, squareSizePx)

                    // Draw base square
                    drawRect(color = baseColor, topLeft = topLeft, size = size)

                    // Add subtle premium gradient overlay for depth
                    val gradient = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.1f), Color.Black.copy(alpha = 0.05f)),
                        center = Offset(topLeft.x + squareSizePx / 2, topLeft.y + squareSizePx / 2),
                        radius = squareSizePx
                    )
                    drawRect(brush = gradient, topLeft = topLeft, size = size)

                    val currentSquare = Square(file, rank)

                    // Highlight last move
                    val isLastMove = boardState.lastMove?.from == currentSquare || boardState.lastMove?.to == currentSquare
                    if (isLastMove) {
                        drawRect(color = ChessLastMoveHighlight.copy(alpha = 0.8f), topLeft = topLeft, size = size)
                    }

                    // Highlight selected
                    if (selectedSquare == currentSquare) {
                        drawRect(color = ChessSelectionHighlight.copy(alpha = 0.85f), topLeft = topLeft, size = size)
                    }

                    // Highlight Check
                    if (boardState.inCheckSquare == currentSquare) {
                        drawRect(color = ChessCheckHighlight.copy(alpha = pulseAlpha), topLeft = topLeft, size = size)
                    }

                    // Draw legal moves indicators
                    if (boardSettings.legalMoveHighlight && boardState.legalMovesForSelected.any { it.to == currentSquare }) {
                        val center = Offset(topLeft.x + squareSizePx / 2, topLeft.y + squareSizePx / 2)
                        val isCapture = boardState.pieces.containsKey(currentSquare)
                        if (isCapture) {
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.25f),
                                radius = squareSizePx / 2.2f,
                                center = center,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f)
                            )
                        } else {
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.2f),
                                radius = squareSizePx / 6f,
                                center = center
                            )
                        }
                    }

                    // Board Coordinates
                    if (boardSettings.coordinates) {
                        val textColor = if (isLight) ChessDarkSquare else ChessLightSquare
                        val textStyle = TextStyle(
                            color = textColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Draw Ranks (1-8) on the left (file == 0)
                        if (visualFile == 0) {
                            val rankText = (8 - rank).toString()
                            val textLayoutResult = textMeasurer.measure(rankText, textStyle)
                            drawText(
                                textLayoutResult = textLayoutResult,
                                color = textColor,
                                topLeft = Offset(
                                    topLeft.x + 4.dp.toPx(),
                                    topLeft.y + 2.dp.toPx()
                                )
                            )
                        }

                        // Draw Files (a-h) on the bottom (rank == 7)
                        if (visualRank == 7) {
                            val fileText = ('a' + file).toString()
                            val textLayoutResult = textMeasurer.measure(fileText, textStyle)
                            drawText(
                                textLayoutResult = textLayoutResult,
                                color = textColor,
                                topLeft = Offset(
                                    topLeft.x + squareSizePx - textLayoutResult.size.width - 4.dp.toPx(),
                                    topLeft.y + squareSizePx - textLayoutResult.size.height - 2.dp.toPx()
                                )
                            )
                        }
                    }
                }
            }

            // Draw Hint Arrow
            if (hintMove != null && hintMove.length >= 4) {
                val fromFile = hintMove[0] - 'a'
                val fromRank = 7 - (hintMove[1] - '1')
                val toFile = hintMove[2] - 'a'
                val toRank = 7 - (hintMove[3] - '1')

                val vFromFile = if (isFlipped) 7 - fromFile else fromFile
                val vFromRank = if (isFlipped) 7 - fromRank else fromRank
                val vToFile = if (isFlipped) 7 - toFile else toFile
                val vToRank = if (isFlipped) 7 - toRank else toRank

                val start = Offset(vFromFile * squareSizePx + squareSizePx / 2, vFromRank * squareSizePx + squareSizePx / 2)
                val end = Offset(vToFile * squareSizePx + squareSizePx / 2, vToRank * squareSizePx + squareSizePx / 2)

                // Draw line
                val arrowColor = Color(0xFFFF9800).copy(alpha = 0.8f) // Orange highlight
                drawLine(
                    color = arrowColor,
                    start = start,
                    end = end,
                    strokeWidth = squareSizePx / 5f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )

                // Draw arrowhead
                val angle = kotlin.math.atan2(end.y - start.y, end.x - start.x)
                val arrowSize = squareSizePx / 3f
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(end.x, end.y)
                    lineTo(
                        end.x - arrowSize * kotlin.math.cos(angle - Math.PI / 6).toFloat(),
                        end.y - arrowSize * kotlin.math.sin(angle - Math.PI / 6).toFloat()
                    )
                    lineTo(
                        end.x - arrowSize * kotlin.math.cos(angle + Math.PI / 6).toFloat(),
                        end.y - arrowSize * kotlin.math.sin(angle + Math.PI / 6).toFloat()
                    )
                    close()
                }
                drawPath(path = path, color = arrowColor)
            }
        }

        // 2. Draw Pieces
        Box(modifier = Modifier.fillMaxSize()) {
            boardState.pieces.forEach { (square, piece) ->
                key(piece.id) {
                    AnimatedPiece(
                        piece = piece,
                        square = square,
                        squareSizePx = squareSizePx,
                        isFlipped = isFlipped,
                        isCaptured = false,
                        onClick = { onSquareClick(square) }
                    )
                }
            }
        }

        // 3. Verdict Stamp Overlay
        if (!hideVerdict) {
            VerdictStamp(
                status = boardState.gameStatus,
                modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
            )
        }
    }
}
