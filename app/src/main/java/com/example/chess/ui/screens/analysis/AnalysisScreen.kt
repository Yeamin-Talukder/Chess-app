package com.example.chess.ui.screens.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.chess.game.board.PieceColor
import com.example.chess.ui.components.ChessBoardUI
import com.example.chess.ui.components.EvaluationBarUI
import com.example.chess.ui.components.GameReviewControls
import com.example.chess.ui.components.MoveClassificationBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnalysisViewModel = hiltViewModel()
) {
    val boardState by viewModel.boardState.collectAsState()
    val currentAnalysis by viewModel.currentAnalysis.collectAsState()
    val classification by viewModel.moveClassification.collectAsState()
    
    val p1Color = PieceColor.WHITE
    val isFlipped = false

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Game Analysis", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF161512))
            )
        },
        containerColor = Color(0xFF161512)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Engine Status Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MoveClassificationBadge(classification = classification)
                
                if (currentAnalysis != null) {
                    val a = currentAnalysis!!
                    Text(
                        text = "Depth ${a.depth} • ${a.nodes / 1000}k nodes",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                } else {
                    Text(
                        text = "Analyzing...",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Board + Eval Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EvaluationBarUI(
                    evaluation = currentAnalysis?.evaluation ?: 0f,
                    mate = currentAnalysis?.mate,
                    isFlipped = isFlipped,
                    modifier = Modifier
                        .weight(0.08f)
                        .aspectRatio(0.1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .weight(0.92f)
                        .aspectRatio(1f)
                ) {
                    ChessBoardUI(
                        boardState = boardState,
                        onSquareClick = { },
                        isFlipped = isFlipped,
                        hideVerdict = true
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Best Move & PV
            if (currentAnalysis != null && currentAnalysis!!.bestMove.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF262421)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Best Move: ", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(currentAnalysis!!.bestMove, color = Color(0xFF81B64C), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        if (currentAnalysis!!.principalVariation.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Line:", color = Color.Gray, fontSize = 12.sp)
                            Text(
                                text = currentAnalysis!!.principalVariation.take(6).joinToString(" "),
                                color = Color.LightGray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Controls
            GameReviewControls(
                onFirstMove = { viewModel.seekToStart() },
                onPreviousMove = { viewModel.prevMove() },
                onNextMove = { viewModel.nextMove() },
                onLastMove = { viewModel.seekToEnd() },
                canGoBack = true,
                canGoForward = true,
                isPlaying = false,
                onPlayPause = null
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
