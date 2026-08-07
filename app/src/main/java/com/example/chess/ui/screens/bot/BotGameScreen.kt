package com.example.chess.ui.screens.bot

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.chess.game.board.PieceColor
import com.example.chess.game.engine.BotLevel
import com.example.chess.ui.components.GameScreenLayout

import com.example.chess.ui.screens.game.PlayerColorChoice

@Composable
fun BotGameScreen(
    playerColorChoice: PlayerColorChoice,
    botLevel: BotLevel,
    onNavigateBack: () -> Unit,
    viewModel: BotGameViewModel = hiltViewModel()
) {
    val boardState by viewModel.boardState.collectAsState()
    val playerColor by viewModel.playerColor.collectAsState()
    val isBotThinking by viewModel.isBotThinking.collectAsState()
    val promotionRequest by viewModel.promotionRequest.collectAsState(initial = null)
    val hintMove by viewModel.hintMove.collectAsState()
    
    val moveHistory by viewModel.moveHistory.collectAsState()
    val whiteCaptured by viewModel.whiteCaptured.collectAsState()
    val blackCaptured by viewModel.blackCaptured.collectAsState()

    var gameStarted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!gameStarted) {
            viewModel.startGame(playerColorChoice, botLevel)
            gameStarted = true
        }
    }

    GameScreenLayout(
        title = "Play vs ${botLevel.title}",
        opponentName = if (isBotThinking) "Bot is thinking..." else "Bot (${botLevel.elo})",
        localPlayerName = "You",
        isConnectionLost = false,
        boardState = boardState,
        localColor = playerColor,
        promotionRequest = promotionRequest,
        onSquareClick = { viewModel.onSquareClick(it) },
        onPromotionSelected = { viewModel.onPromotionSelected(it) },
        onPromotionCanceled = { viewModel.onPromotionCanceled() },
        onResign = { viewModel.resign() },
        onDisconnect = {},
        onNavigateBack = onNavigateBack,
        moveHistory = moveHistory,
        whiteCaptured = whiteCaptured,
        blackCaptured = blackCaptured,
        showResignButton = true,
        hintMove = hintMove?.bestMove,
        onHintClick = { viewModel.requestHint() }
    )
}
