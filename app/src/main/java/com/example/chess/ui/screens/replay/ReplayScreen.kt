package com.example.chess.ui.screens.replay


import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.chess.ui.components.GameScreenLayout
import com.example.chess.game.board.PieceColor
@Composable
fun ReplayScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAnalysis: (Long) -> Unit,
    viewModel: ReplayViewModel = hiltViewModel()
) {
    val boardState by viewModel.boardState.collectAsState()
    val gameEntity by viewModel.gameEntity.collectAsState()
    val currentMoveIndex by viewModel.currentMoveIndex.collectAsState()
    val totalMoves by viewModel.totalMoves.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val whiteCaptured by viewModel.whiteCaptured.collectAsState()
    val blackCaptured by viewModel.blackCaptured.collectAsState()
    val moveHistory by viewModel.moveHistory.collectAsState()
    val evalScore by viewModel.evaluationScore.collectAsState()
    val evalDepth by viewModel.evaluationDepth.collectAsState()

    val p1Name = gameEntity?.player1Name ?: "Player 1"
    val p2Name = gameEntity?.player2Name ?: "Player 2"
    val result = gameEntity?.result ?: ""

    // In replay mode, the "live" state IS the active state as it steps forward
    GameScreenLayout(
        title = "Game History • $result",
        opponentName = p2Name,
        localPlayerName = p1Name,
        isConnectionLost = false,
        boardState = boardState,
        localColor = PieceColor.WHITE,
        promotionRequest = null,
        onSquareClick = { },
        onPromotionSelected = { },
        onPromotionCanceled = { },
        onResign = { },
        onDisconnect = { },
        onNavigateBack = onNavigateBack,
        moveHistory = moveHistory,
        whiteCaptured = whiteCaptured,
        blackCaptured = blackCaptured,
        showResignButton = false,
        isReviewMode = true,
        reviewBoardState = boardState,
        reviewEvalScore = evalScore,
        reviewEvalDepth = evalDepth,
        onStartReview = { },
        onExitReview = { onNavigateBack() },
        onReviewFirst = { viewModel.seekToStart() },
        onReviewPrevious = { viewModel.prevMove() },
        onReviewNext = { viewModel.nextMove() },
        onReviewLast = { viewModel.seekToEnd() },
        isPlaying = isPlaying,
        onPlayPause = { viewModel.playPause() },
        onDeepAnalysis = {
            gameEntity?.id?.let { onNavigateToAnalysis(it) }
        }
    )
}

