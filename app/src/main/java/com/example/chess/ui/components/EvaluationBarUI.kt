package com.example.chess.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun EvaluationBarUI(
    evaluation: Float, // Score from White's perspective (+ is winning for White)
    mate: Int? = null, // Forced mate in X moves (+ is White mate, - is Black mate)
    isFlipped: Boolean, // True if playing as Black (White at top)
    modifier: Modifier = Modifier
) {
    // Determine how full the bar is with White color (0.0 to 1.0)
    // A score of +10 means 100% white. -10 means 100% black. 0 means 50%.
    val winThreshold = 10f
    val clampedEval = evaluation.coerceIn(-winThreshold, winThreshold)
    
    // Base percentage: 50% is equal. +10 goes to 100% (1.0), -10 goes to 0% (0.0).
    val whitePercentage = (clampedEval + winThreshold) / (winThreshold * 2)

    val animatedWhitePercentage by animateFloatAsState(
        targetValue = whitePercentage,
        animationSpec = tween(durationMillis = 600),
        label = "EvalBarAnim"
    )

    // Layout configuration depending on who is at the bottom
    val topColor = if (isFlipped) Color.White else Color(0xFF262421)
    val bottomColor = if (isFlipped) Color(0xFF262421) else Color.White

    val topPercentage = if (isFlipped) animatedWhitePercentage else 1f - animatedWhitePercentage
    val bottomPercentage = 1f - topPercentage

    val absEval = if (mate != null) {
        "M${abs(mate)}"
    } else {
        String.format("%.1f", abs(evaluation))
    }

    Box(
        modifier = modifier
            .width(20.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, Color(0xFF2C2A27), RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(topPercentage.coerceAtLeast(0.01f))
                    .background(topColor),
                contentAlignment = Alignment.TopCenter
            ) {
                // If the top color is winning, show the score at the top
                if (topPercentage > 0.55f) {
                    Text(
                        text = absEval,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (topColor == Color.White) Color.Black else Color.White,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Bottom Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(bottomPercentage.coerceAtLeast(0.01f))
                    .background(bottomColor),
                contentAlignment = Alignment.BottomCenter
            ) {
                // If the bottom color is winning, show the score at the bottom
                if (bottomPercentage >= 0.45f) {
                    Text(
                        text = absEval,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (bottomColor == Color.White) Color.Black else Color.White,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}
