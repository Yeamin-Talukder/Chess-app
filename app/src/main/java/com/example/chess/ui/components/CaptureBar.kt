package com.example.chess.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.game.board.PieceType

/** Maps a piece type to its unicode character (for display in the capture bar). */
private fun pieceUnicode(type: PieceType): String = when (type) {
    PieceType.PAWN   -> "♟"
    PieceType.KNIGHT -> "♞"
    PieceType.BISHOP -> "♝"
    PieceType.ROOK   -> "♜"
    PieceType.QUEEN  -> "♛"
    PieceType.KING   -> "♚"
}

/**
 * A horizontal row showing pieces captured by one player together with a net
 * material score prefix (e.g. "+3").
 *
 * [captured] = list of (PieceType, pointValue) captured by this player.
 * [opponentCaptured] = list of (PieceType, pointValue) captured by the opponent.
 *   Used to compute the net material advantage.
 */
@Composable
fun CaptureBar(
    captured: List<Pair<PieceType, Int>>,
    opponentCaptured: List<Pair<PieceType, Int>>,
    modifier: Modifier = Modifier
) {
    val myPoints  = captured.sumOf { it.second }
    val oppPoints = opponentCaptured.sumOf { it.second }
    val netScore  = myPoints - oppPoints

    // Group by piece type and sort by value descending
    val grouped = captured
        .groupBy { it.first }
        .entries
        .sortedByDescending { it.value.first().second }

    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Piece icons (grouped)
        grouped.forEach { (type, list) ->
            repeat(list.size) {
                Text(
                    text = pieceUnicode(type),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            }
        }

        // Net score badge
        if (netScore > 0) {
            Text(
                text = "+$netScore",
                modifier = Modifier.padding(start = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp
            )
        }
    }
}
