package com.example.chess.ui.screens.multiplayer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.chess.ui.components.GameScreenLayout
import com.example.chess.ui.components.GameSetupDialog
import com.example.chess.ui.screens.game.PlayerColorChoice
import com.example.chess.game.board.PieceColor
import com.example.chess.ui.screens.settings.SettingsViewModel
import androidx.compose.runtime.CompositionLocalProvider
import com.example.chess.ui.components.LocalBoardSettings
import com.example.chess.ui.components.BoardSettings

@Composable
fun WifiGameScreen(
    onNavigateBack: () -> Unit,
    viewModel: WifiGameViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val boardState by viewModel.boardState.collectAsState()
    val promotionRequest by viewModel.promotionRequest.collectAsState()
    val localColor by viewModel.localPlayerColor.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val isGameStarted by viewModel.isGameStarted.collectAsState()
    val moveHistory by viewModel.moveHistory.collectAsState()
    val whiteCaptured by viewModel.whiteCaptured.collectAsState()
    val blackCaptured by viewModel.blackCaptured.collectAsState()

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

    val isConnected = connectionState is com.example.chess.network.wifi.WifiState.Connected
    val opponentName = if (isConnected) (connectionState as com.example.chess.network.wifi.WifiState.Connected).deviceName else "Unknown"

    if (!isGameStarted) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        modifier = Modifier.size(42.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Preparing your board",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isConnected) "Connecting you with $opponentName" else "Waiting for a secure local connection",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(28.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Syncing match details", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    } else {
        CompositionLocalProvider(LocalBoardSettings provides boardSettings) {
            GameScreenLayout(
                title = if (isConnected) "VS $opponentName" else "Disconnected",
                opponentName = opponentName,
                localPlayerName = "You",
                isConnectionLost = !isConnected,
                boardState = boardState,
                localColor = localColor,
                promotionRequest = promotionRequest,
                onSquareClick = { viewModel.onSquareClick(it) },
                onPromotionSelected = { viewModel.onPromotionSelected(it) },
                onPromotionCanceled = { viewModel.onPromotionCanceled() },
                onResign = { viewModel.wifiController.sendEvent(com.example.chess.network.GameEvent.Resign) },
                onDisconnect = { viewModel.wifiController.disconnect() },
                onNavigateBack = onNavigateBack,
                moveHistory = moveHistory,
                whiteCaptured = whiteCaptured,
                blackCaptured = blackCaptured,
                showResignButton = true
            )
        }
    }
}
