package com.example.chess.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A slim horizontal strip that shows the last [maxVisible] half-moves.
 *
 * [moves] is the flat list of half-move labels (e.g. ["♙e4", "♟e5", "♘f3", …]).
 * [isTop] controls whether the strip belongs to the opponent (top) or local player (bottom).
 * Top strip shows BLACK's moves (odd indices: 1, 3, 5, …).
 * Bottom strip shows WHITE's moves (even indices: 0, 2, 4, …).
 */
@Composable
fun MoveHistoryBar(
    moves: List<String>,
    isTop: Boolean,
    modifier: Modifier = Modifier,
    maxVisible: Int = 8
) {
    // Filter to the correct player's moves
    val playerMoves = moves.filterIndexed { index, _ ->
        if (isTop) index % 2 == 1 else index % 2 == 0
    }.takeLast(maxVisible)

    val scrollState = rememberScrollState()
    // Auto-scroll to end when new moves arrive
    LaunchedEffect(playerMoves.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        playerMoves.forEachIndexed { idx, label ->
            val moveNumber = (if (isTop) {
                // Black move number = index * 2 + 2 (half-move pair)
                idx * 2 + 2
            } else {
                idx * 2 + 1
            })
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = if (idx == playerMoves.lastIndex) 0.25f else 0.10f),
                tonalElevation = 0.dp
            ) {
                Text(
                    text = "$moveNumber. $label",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (idx == playerMoves.lastIndex) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
