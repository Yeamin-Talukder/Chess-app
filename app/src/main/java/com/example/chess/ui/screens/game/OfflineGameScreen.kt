package com.example.chess.ui.screens.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.chess.game.board.PieceColor
import com.example.chess.ui.components.GameScreenLayout
import com.example.chess.ui.components.GameSetupDialog
import com.example.chess.ui.screens.settings.SettingsViewModel
import androidx.compose.runtime.CompositionLocalProvider
import com.example.chess.ui.components.LocalBoardSettings
import com.example.chess.ui.components.BoardSettings

@Composable
fun OfflineGameScreen(
    onNavigateBack: () -> Unit,
    viewModel: OfflineGameViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val boardState       by viewModel.boardState.collectAsState()
    val promotionRequest by viewModel.promotionRequest.collectAsState()
    val localColor       by viewModel.localColor.collectAsState()
    val moveHistory      by viewModel.moveHistory.collectAsState()
    val whiteCaptured    by viewModel.whiteCaptured.collectAsState()
    val blackCaptured    by viewModel.blackCaptured.collectAsState()
    val player1Name      by viewModel.player1Name.collectAsState()
    val player2Name      by viewModel.player2Name.collectAsState()

    // Read the logged-in user's name from profile (null until loaded)
    val profileEntity    by viewModel.profileUsername.collectAsState(initial = null)
    val currentUserName  = profileEntity?.username?.ifBlank { "Player 1" } ?: "Player 1"

    val reviewIndex      by viewModel.reviewIndex.collectAsState()
    val reviewBoardState by viewModel.reviewBoardState.collectAsState()
    val evaluationScore  by viewModel.evaluationScore.collectAsState()
    val evaluationDepth  by viewModel.evaluationDepth.collectAsState()
    val hintMove         by viewModel.hintMove.collectAsState()

    val boardColors by settingsViewModel.boardColors.collectAsState()
    val pieceStyle by settingsViewModel.pieceStyle.collectAsState()
    val animationSpeed by settingsViewModel.animationSpeed.collectAsState()
    val boardRotation by settingsViewModel.boardRotation.collectAsState()
    val coordinates by settingsViewModel.coordinates.collectAsState()
    val legalMoveHighlight by settingsViewModel.legalMoveHighlight.collectAsState()
    
    val boardSettings = BoardSettings(
        boardColors = boardColors,
        pieceStyle = pieceStyle,
        animationSpeed = animationSpeed,
        boardRotation = boardRotation,
        coordinates = coordinates,
        legalMoveHighlight = legalMoveHighlight
    )

    val showDialog = remember { mutableStateOf(true) }

    if (showDialog.value) {
        GameSetupDialog(
            showDialog      = showDialog,
            currentUserName = currentUserName,
            onConfirm = { config ->
                viewModel.startGame(config)
                showDialog.value = false
            },
            onDismiss = onNavigateBack
        )
    } else {
        val localName    = if (localColor == PieceColor.WHITE) player1Name else player2Name
        val opponentName = if (localColor == PieceColor.WHITE) player2Name else player1Name

        CompositionLocalProvider(LocalBoardSettings provides boardSettings) {
            GameScreenLayout(
                title               = "Local Play",
                opponentName        = opponentName,
                localPlayerName     = localName,
                isConnectionLost    = false,
                boardState          = boardState,
                localColor          = localColor,
                promotionRequest    = promotionRequest,
                onSquareClick       = { if (reviewIndex == null) viewModel.onSquareClick(it) },
                onPromotionSelected = { viewModel.onPromotionSelected(it) },
                onPromotionCanceled = { viewModel.onPromotionCanceled() },
                onResign            = { viewModel.resign() },
                onDisconnect        = { },
                onNavigateBack      = onNavigateBack,
                moveHistory         = moveHistory,
                whiteCaptured       = whiteCaptured,
                blackCaptured       = blackCaptured,
                showResignButton    = true,
                isReviewMode        = reviewIndex != null,
                reviewBoardState    = reviewBoardState,
                reviewEvalScore     = evaluationScore,
                reviewEvalDepth     = evaluationDepth,
                onStartReview       = { viewModel.startReview() },
                onExitReview        = { viewModel.exitReview() },
                onReviewFirst       = { viewModel.reviewFirst() },
                onReviewPrevious    = { viewModel.reviewPrevious() },
                onReviewNext        = { viewModel.reviewNext() },
                onReviewLast        = { viewModel.reviewLast() },
                onHintClick         = { viewModel.requestHint() }
            )
        }
    }
}
