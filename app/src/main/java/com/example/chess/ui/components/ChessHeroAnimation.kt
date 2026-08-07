package com.example.chess.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class FloatingPieceData(
    val char: String,
    val xRatio: Float,
    val sizeSp: Float,
    val durationSec: Float,
    val delaySec: Float,
    val maxAlpha: Float,
    val rotationSpeed: Float
)

@Composable
fun ChessHeroAnimation(
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF4CAF50),
    darkBgColor: Color = Color(0xFF0B1910)
) {
    // Infinite continuous time ticker in seconds for 100% glitch-free continuous movement
    val infiniteTransition = rememberInfiniteTransition(label = "heroContinuousTime")
    val continuousTimeSec by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3600f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "continuousTimeSec"
    )

    val pieces = remember {
        listOf(
            FloatingPieceData("♞", 0.08f, 110f, 22f, 0f, 0.07f, 25f),
            FloatingPieceData("♛", 0.22f, 42f, 12f, 2f, 0.22f, 45f),
            FloatingPieceData("♟", 0.38f, 26f, 15f, 5f, 0.28f, -30f),
            FloatingPieceData("♜", 0.70f, 140f, 26f, 3f, 0.06f, 15f),
            FloatingPieceData("♝", 0.55f, 48f, 16f, 1f, 0.20f, 35f),
            FloatingPieceData("♟", 0.85f, 24f, 11f, 7f, 0.32f, -50f),
            FloatingPieceData("♚", 0.48f, 58f, 19f, 9f, 0.18f, 20f)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(darkBgColor)
    ) {
        // 1. Perspective 3D Checkered Board with Infinite Scroll
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val tileSize = 70f
            val scrollProgress = (continuousTimeSec % 5f) / 5f
            val offsetY = scrollProgress * tileSize

            val cols = (width / tileSize).toInt() + 4
            val rows = (height / tileSize).toInt() + 4

            for (r in -2..rows) {
                for (c in -2..cols) {
                    if ((r + c) % 2 == 0) {
                        val x = c * tileSize
                        val y = r * tileSize + offsetY
                        val fadeRatio = (1f - (y / height)).coerceIn(0f, 1f)
                        val tileColor = accentColor.copy(alpha = 0.14f * fadeRatio)

                        drawRect(
                            color = tileColor,
                            topLeft = Offset(x, y),
                            size = Size(tileSize, tileSize)
                        )
                    }
                }
            }
        }

        // 2. Continuous Ambient Floating Unicode Pieces (Bottom to Top)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            pieces.forEach { p ->
                val timeWithDelay = continuousTimeSec + p.delaySec
                val progress = (timeWithDelay % p.durationSec) / p.durationSec

                // Bottom to top rise: starts below canvas (height + 80), ends above canvas (-100)
                val startY = height + 80f
                val endY = -100f
                val yPos = startY + progress * (endY - startY)
                val xPos = width * p.xRatio
                val rotation = progress * 360f * (if (p.rotationSpeed > 0) 1f else -1f)

                // Smooth fade-in at bottom (0..0.15) and fade-out at top (0.8..1.0)
                val alphaMultiplier = when {
                    progress < 0.15f -> progress / 0.15f
                    progress > 0.80f -> (1.0f - progress) / 0.20f
                    else -> 1.0f
                }.coerceIn(0f, 1f)

                val finalAlpha = p.maxAlpha * alphaMultiplier
                if (finalAlpha > 0.005f) {
                    rotate(degrees = rotation, pivot = Offset(xPos, yPos)) {
                        drawContext.canvas.nativeCanvas.drawText(
                            p.char,
                            xPos,
                            yPos,
                            android.graphics.Paint().apply {
                                textSize = p.sizeSp * density
                                color = android.graphics.Color.argb(
                                    (finalAlpha * 255).toInt(),
                                    255, 255, 255
                                )
                                isAntiAlias = true
                                setShadowLayer(
                                    20f, 0f, 0f,
                                    android.graphics.Color.argb(
                                        (finalAlpha * 180).toInt(),
                                        76, 175, 80
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }

        // 3. Fading from the Bottom Effect Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent, // Visible at top
                            darkBgColor.copy(alpha = 0.40f),
                            darkBgColor // Smooth fade to background at bottom
                        )
                    )
                )
        )
    }
}
