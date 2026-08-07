package com.example.chess.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.game.board.PieceType

private val PlayerCardBg = Color(0xFF21201C)
private val InactiveTimerBg = Color(0xFF2B2925)
private val ActiveTimerGreen = Color(0xFF81B64C)
private val BlueAvatarBg = Color(0xFF2563EB)

@Composable
fun PlayerInfoBar(
    playerName: String,
    timeLeft: String,
    isActiveTurn: Boolean = false,
    avatarText: String = if (playerName.contains("2")) "P2" else "P1",
    materialDiff: Int = 0,
    capturedPieces: List<Pair<PieceType, Int>> = emptyList(),
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = PlayerCardBg,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar Badge (P1 / P2 on blue background)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BlueAvatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = avatarText,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Name & Material Advantage
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = playerName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    if (capturedPieces.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            capturedPieces.forEach { (type, count) ->
                                val pieceIcon = when (type) {
                                    PieceType.PAWN -> "♙"
                                    PieceType.KNIGHT -> "♘"
                                    PieceType.BISHOP -> "♗"
                                    PieceType.ROOK -> "♖"
                                    PieceType.QUEEN -> "♕"
                                    PieceType.KING -> "♔"
                                }
                                Text(
                                    text = if (count > 1) "$pieceIcon x$count " else "$pieceIcon ",
                                    fontSize = 12.sp,
                                    color = Color.LightGray
                                )
                            }
                            if (materialDiff > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF2E7D32).copy(alpha = 0.4f))
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "+$materialDiff",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF81C784)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Digital Clock Box (glowing green when active, dark when inactive)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isActiveTurn) ActiveTimerGreen else InactiveTimerBg)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = timeLeft,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            }
        }
    }
}
