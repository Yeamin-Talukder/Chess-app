package com.example.chess.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.example.chess.game.board.BoardState
import com.example.chess.game.board.GameStatus
import com.example.chess.game.board.PieceColor
import com.example.chess.game.board.PieceType
import com.example.chess.game.board.Square
import com.example.chess.ui.components.LocalBoardSettings
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val DarkAppBg = Color(0xFF161512)
private val TopBarIconBtnBg = Color(0xFF262421)

private data class Particle(
    val x: Float,
    val y: Float,
    val color: Color,
    val size: Float,
    val speedX: Float,
    val speedY: Float,
    val rotation: Float,
    val rotationSpeed: Float
)

@Composable
private fun WinConfettiOverlay() {
    val confettiColors = listOf(
        Color(0xFFFFD700), Color(0xFFFF6B6B), Color(0xFF4ECDC4),
        Color(0xFF45B7D1), Color(0xFFA8E6CF), Color(0xFFFF8B94),
        Color(0xFFFFAA5E), Color(0xFFB4A7FF)
    )

    val particles = remember {
        List(80) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * -0.3f,
                color = confettiColors.random(),
                size = Random.nextFloat() * 14f + 6f,
                speedX = (Random.nextFloat() - 0.5f) * 0.003f,
                speedY = Random.nextFloat() * 0.004f + 0.002f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 8f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_progress"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            val px = (p.x + p.speedX * progress * 3000) % 1f
            val py = (p.y + p.speedY * progress * 3000) % 1.2f
            val angle = Math.toRadians((p.rotation + p.rotationSpeed * progress * 3000).toDouble())

            val cx = px * size.width
            val cy = py * size.height

            if (py < 1.05f) {
                val halfW = p.size / 2
                val halfH = p.size / 3

                val cosA = cos(angle).toFloat()
                val sinA = sin(angle).toFloat()

                val corners = listOf(
                    Offset(-halfW * cosA - (-halfH) * sinA, -halfW * sinA + (-halfH) * cosA),
                    Offset(halfW * cosA - (-halfH) * sinA, halfW * sinA + (-halfH) * cosA),
                    Offset(halfW * cosA - halfH * sinA, halfW * sinA + halfH * cosA),
                    Offset(-halfW * cosA - halfH * sinA, -halfW * sinA + halfH * cosA)
                ).map { Offset(cx + it.x, cy + it.y) }

                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(corners[0].x, corners[0].y)
                    corners.drop(1).forEach { lineTo(it.x, it.y) }
                    close()
                }
                drawPath(path, p.color)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreenLayout(
    title: String,
    opponentName: String,
    localPlayerName: String,
    isConnectionLost: Boolean,
    boardState: BoardState,
    localColor: PieceColor,
    promotionRequest: Pair<Square, Square>?,
    hintMove: String? = null,
    onSquareClick: (Square) -> Unit,
    onPromotionSelected: (PieceType) -> Unit,
    onPromotionCanceled: () -> Unit,
    onResign: () -> Unit,
    onDisconnect: () -> Unit,
    onNavigateBack: () -> Unit,
    moveHistory: List<String> = emptyList(),
    whiteCaptured: List<Pair<PieceType, Int>> = emptyList(),
    blackCaptured: List<Pair<PieceType, Int>> = emptyList(),
    showResignButton: Boolean = true,
    
    // Review Mode Parameters
    isReviewMode: Boolean = false,
    reviewBoardState: BoardState? = null,
    reviewEvalScore: Float = 0f,
    reviewEvalDepth: Int = 0,
    onStartReview: () -> Unit = {},
    onExitReview: () -> Unit = {},
    onReviewFirst: () -> Unit = {},
    onReviewPrevious: () -> Unit = {},
    onReviewNext: () -> Unit = {},
    onReviewLast: () -> Unit = {},
    isPlaying: Boolean = false,
    onPlayPause: (() -> Unit)? = null,
    onDeepAnalysis: (() -> Unit)? = null,
    onHintClick: (() -> Unit)? = null
) {
    // Determine which state to display
    val activeState = if (isReviewMode && reviewBoardState != null) reviewBoardState else boardState
    var showResignConfirm by remember { mutableStateOf(false) }
    var showMovesDialog by remember { mutableStateOf(false) }
    var showBackDialog by remember { mutableStateOf(false) }
    var showWinOverlay by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val isGameOver = boardState.gameStatus != GameStatus.ONGOING

    LaunchedEffect(boardState.gameStatus) {
        if (boardState.gameStatus == GameStatus.CHECKMATE ||
            boardState.gameStatus == GameStatus.RESIGNED ||
            boardState.gameStatus == GameStatus.TIME_OUT
        ) {
            showWinOverlay = true
        }
    }

    BackHandler(enabled = !isConnectionLost) {
        if (isGameOver) {
            onDisconnect()
            onNavigateBack()
        } else {
            showBackDialog = true
        }
    }

    val opponentColor = localColor.opposite()
    val opponentTime = formatTime(
        if (opponentColor == PieceColor.WHITE) activeState.whiteTimeMillis else activeState.blackTimeMillis
    )
    val localTime = formatTime(
        if (localColor == PieceColor.WHITE) activeState.whiteTimeMillis else activeState.blackTimeMillis
    )

    val localCaptured = if (localColor == PieceColor.WHITE) whiteCaptured else blackCaptured
    val opponentCaptured = if (opponentColor == PieceColor.WHITE) whiteCaptured else blackCaptured

    fun calcMaterial(captured: List<Pair<PieceType, Int>>): Int {
        return captured.sumOf { 
            when (it.first) {
                PieceType.PAWN -> 1
                PieceType.KNIGHT, PieceType.BISHOP -> 3
                PieceType.ROOK -> 5
                PieceType.QUEEN -> 9
                else -> 0
            } * it.second 
        }
    }
    
    val localMaterial = calcMaterial(localCaptured)
    val opponentMaterial = calcMaterial(opponentCaptured)
    val localMaterialDiff = (localMaterial - opponentMaterial).coerceAtLeast(0)
    val opponentMaterialDiff = (opponentMaterial - localMaterial).coerceAtLeast(0)

    val winnerName = when (boardState.gameStatus) {
        GameStatus.CHECKMATE -> if (boardState.turn == PieceColor.WHITE) opponentName else localPlayerName
        GameStatus.RESIGNED -> if (boardState.turn == PieceColor.WHITE) opponentName else localPlayerName
        GameStatus.TIME_OUT -> if (boardState.turn == PieceColor.WHITE) opponentName else localPlayerName
        else -> null
    }

    val boardSettings = LocalBoardSettings.current
    val isFlipped = if (boardSettings.boardRotation) {
        activeState.turn == PieceColor.BLACK
    } else {
        localColor == PieceColor.BLACK
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(TopBarIconBtnBg)
                            .clickable {
                                if (isReviewMode) {
                                    onExitReview()
                                } else if (isGameOver) {
                                    onDisconnect()
                                    onNavigateBack()
                                } else {
                                    showBackDialog = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                actions = {
                    if (isReviewMode && reviewEvalDepth > 0) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF262421),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF81B64C).copy(alpha = 0.6f)),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (reviewEvalDepth >= 15) Color(0xFF81B64C) else Color(0xFFE8A838))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$reviewEvalDepth depth",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        if (onDeepAnalysis != null) {
                            IconButton(onClick = onDeepAnalysis) {
                                Icon(Icons.Filled.Analytics, contentDescription = "Deep Analysis", tint = Color(0xFF81B64C))
                            }
                        }
                    } else if (!isGameOver && onHintClick != null) {
                        IconButton(onClick = onHintClick) {
                            Icon(Icons.Filled.Lightbulb, contentDescription = "Hint", tint = Color(0xFFF3C242))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkAppBg)
            )
        },
        containerColor = DarkAppBg
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (isConnectionLost) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Connection Lost",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PrimaryButton(text = "Exit", onClick = { onNavigateBack() })
                }
                return@Scaffold
            }

            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight().padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isReviewMode) {
                                EvaluationBarUI(
                                    evaluation = reviewEvalScore,
                                    isFlipped = isFlipped,
                                    modifier = Modifier.fillMaxHeight(0.8f).padding(end = 8.dp)
                                )
                            }
                            ChessBoardUI(
                                boardState = activeState,
                                hintMove = hintMove,
                                onSquareClick = onSquareClick,
                                isFlipped = isFlipped,
                                hideVerdict = isReviewMode
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(
                        modifier = Modifier.weight(0.7f).fillMaxHeight().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PlayerInfoBar(
                            playerName = opponentName,
                            timeLeft = opponentTime,
                            isActiveTurn = activeState.turn != localColor,
                            avatarText = "P2",
                            materialDiff = opponentMaterialDiff,
                            capturedPieces = opponentCaptured
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        PlayerInfoBar(
                            playerName = localPlayerName,
                            timeLeft = localTime,
                            isActiveTurn = activeState.turn == localColor,
                            avatarText = "P1",
                            materialDiff = localMaterialDiff,
                            capturedPieces = localCaptured
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp)) // Push chessboard top upper instead of weight(1f)

                    // 1. Move History Horizontal Row (placed above top player bar)
                    Box(modifier = Modifier.height(44.dp).fillMaxWidth().padding(horizontal = 12.dp)) {
                        HorizontalMoveHistoryRow(moves = moveHistory)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 2. Top Player Bar (Player 2 / Opponent)
                    PlayerInfoBar(
                        playerName = opponentName,
                        timeLeft = opponentTime,
                        isActiveTurn = activeState.turn != localColor,
                        avatarText = "P2",
                        materialDiff = opponentMaterialDiff,
                        capturedPieces = opponentCaptured,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. Chessboard UI with optional Evaluation Bar
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isReviewMode) {
                            EvaluationBarUI(
                                evaluation = reviewEvalScore,
                                isFlipped = isFlipped,
                                modifier = Modifier
                                    .weight(0.06f)
                                    .aspectRatio(0.1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .weight(0.94f)
                                    .aspectRatio(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                ChessBoardUI(
                                    boardState = activeState,
                                    hintMove = hintMove,
                                    onSquareClick = onSquareClick,
                                    isFlipped = isFlipped,
                                    hideVerdict = isReviewMode
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                ChessBoardUI(
                                    boardState = activeState,
                                    hintMove = hintMove,
                                    onSquareClick = onSquareClick,
                                    isFlipped = isFlipped,
                                    hideVerdict = isReviewMode
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4. Bottom Player Bar (Player 1 / You)
                    PlayerInfoBar(
                        playerName = localPlayerName,
                        timeLeft = localTime,
                        isActiveTurn = activeState.turn == localColor,
                        avatarText = "P1",
                        materialDiff = localMaterialDiff,
                        capturedPieces = localCaptured,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    
                    Spacer(modifier = Modifier.weight(1f)) // Push everything up to center board
                    
                    if (isReviewMode) {
                        GameReviewControls(
                            onFirstMove = onReviewFirst,
                            onPreviousMove = onReviewPrevious,
                            onNextMove = onReviewNext,
                            onLastMove = onReviewLast,
                            canGoBack = activeState.moveHistory.isNotEmpty(), // Simplified
                            canGoForward = true, // Simplified
                            isPlaying = isPlaying,
                            onPlayPause = onPlayPause,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // ── Promotion Dialog ─────────────────────────────────────────────
            promotionRequest?.let { _ ->
                PromotionDialog(
                    color = localColor,
                    onPieceSelected = onPromotionSelected,
                    onDismiss = onPromotionCanceled
                )
            }

            // ── Game Over Overlay ─────────────────────────────────────────────
            if (isGameOver && !isReviewMode) {
                val message = when (boardState.gameStatus) {
                    GameStatus.CHECKMATE -> "Checkmate!"
                    GameStatus.STALEMATE -> "Draw by Stalemate"
                    GameStatus.RESIGNED -> "Resigned"
                    GameStatus.TIME_OUT -> "Time Out!"
                    GameStatus.DRAW_FIFTY_MOVE -> "Draw — 50-Move Rule"
                    GameStatus.DRAW_REPETITION -> "Draw — Repetition"
                    GameStatus.DRAW_INSUFFICIENT_MATERIAL -> "Draw — Insufficient Material"
                    GameStatus.DRAW_AGREED -> "Draw by Agreement"
                    else -> "Game Over"
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = true,
                    enter = androidx.compose.animation.fadeIn(animationSpec = tween(700)) +
                            androidx.compose.animation.scaleIn(initialScale = 0.8f, animationSpec = tween(700, delayMillis = 100)),
                    exit = androidx.compose.animation.fadeOut(),
                    modifier = Modifier.fillMaxSize().zIndex(100f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.85f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {},
                        contentAlignment = Alignment.Center
                    ) {
                        if (showWinOverlay) {
                            WinConfettiOverlay()
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (winnerName != null) {
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF81B64C)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "👑", fontSize = 56.sp)
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "$winnerName Wins!",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(text = "🤝", fontSize = 56.sp)
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "It's a Draw!",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = message,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(36.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Button(
                                    onClick = {
                                        showWinOverlay = false
                                        onStartReview()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262421))
                                ) {
                                    Text("Review Game", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        showWinOverlay = false
                                        onDisconnect()
                                        onNavigateBack()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81B64C))
                                ) {
                                    Text("Exit Game", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // ── Back / Resign Confirm Dialog ─────────────────────────────────
            if (showBackDialog) {
                AlertDialog(
                    onDismissRequest = { showBackDialog = false },
                    title = { Text("Leave Game?") },
                    text = { Text("Do you want to resign and exit, or continue playing?") },
                    confirmButton = {
                        TextButton(onClick = {
                            showBackDialog = false
                            onResign()
                            onDisconnect()
                            onNavigateBack()
                        }) { Text("Resign & Exit", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showBackDialog = false }) {
                            Text("Resume Game")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun HorizontalMoveHistoryRow(
    moves: List<String>,
    modifier: Modifier = Modifier
) {
    if (moves.isEmpty()) return

    val listState = rememberLazyListState()

    LaunchedEffect(moves.size) {
        if (moves.isNotEmpty()) {
            listState.animateScrollToItem(moves.size / 2) // Roughly scroll to last pair
        }
    }

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        state = listState,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val pairedMoves = moves.chunked(2)
        itemsIndexed(pairedMoves) { idx, pair ->
            val moveNum = idx + 1
            val whiteMove = pair.getOrNull(0)
            val blackMove = pair.getOrNull(1)
            val isLastPair = (idx == pairedMoves.lastIndex)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$moveNum.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.padding(end = 4.dp)
                )

                if (whiteMove != null) {
                    val isWhiteActive = isLastPair && blackMove == null
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isWhiteActive) Color(0xFF383531) else Color.Transparent)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = whiteMove,
                            fontSize = 15.sp,
                            fontWeight = if (isWhiteActive) FontWeight.Bold else FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                if (blackMove != null) {
                    val isBlackActive = isLastPair
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isBlackActive) Color(0xFF383531) else Color.Transparent)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = blackMove,
                            fontSize = 15.sp,
                            fontWeight = if (isBlackActive) FontWeight.Bold else FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.width(16.dp)) // Extra padding at end
        }
    }
}

fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
