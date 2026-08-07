package com.example.chess.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GameReviewControls(
    onFirstMove: () -> Unit,
    onPreviousMove: () -> Unit,
    onNextMove: () -> Unit,
    onLastMove: () -> Unit,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isPlaying: Boolean = false,
    onPlayPause: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF262421))
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val disabledColor = Color.White.copy(alpha = 0.2f)
        val enabledColor = Color.White

        IconButton(onClick = onFirstMove, enabled = canGoBack) {
            Icon(
                Icons.Default.SkipPrevious, 
                contentDescription = "First Move",
                tint = if (canGoBack) enabledColor else disabledColor,
                modifier = Modifier.size(32.dp)
            )
        }

        IconButton(onClick = onPreviousMove, enabled = canGoBack) {
            Icon(
                Icons.Default.FastRewind, 
                contentDescription = "Previous Move",
                tint = if (canGoBack) enabledColor else disabledColor,
                modifier = Modifier.size(36.dp)
            )
        }

        if (onPlayPause != null) {
            IconButton(onClick = onPlayPause) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        IconButton(onClick = onNextMove, enabled = canGoForward) {
            Icon(
                Icons.Default.FastForward, 
                contentDescription = "Next Move",
                tint = if (canGoForward) enabledColor else disabledColor,
                modifier = Modifier.size(36.dp)
            )
        }

        IconButton(onClick = onLastMove, enabled = canGoForward) {
            Icon(
                Icons.Default.SkipNext, 
                contentDescription = "Last Move",
                tint = if (canGoForward) enabledColor else disabledColor,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
