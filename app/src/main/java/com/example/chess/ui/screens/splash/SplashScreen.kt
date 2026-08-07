package com.example.chess.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.ui.components.ChessHeroAnimation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onNavigateToHome: () -> Unit) {
    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800)
            )
        }
        delay(1500)
        onNavigateToHome()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1B5E20),
                            Color(0xFF2E7D32),
                            Color(0xFF388E3C),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            ChessHeroAnimation(
                accentColor = Color(0xFF4CAF50),
                darkBgColor = Color(0xFF0B1910)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .scale(scale.value)
                        .alpha(alpha.value),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedHeroPawn()
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text      = "CHESS",
                        style     = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 8.sp,
                        color     = Color.White
                    )
                    Text(
                        text  = "Play. Learn. Win.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedHeroPawn() {
    val inf = rememberInfiniteTransition(label = "pawn_pulse")
    val scale by inf.animateFloat(
        initialValue  = 0.92f,
        targetValue   = 1.06f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "pawn_scale"
    )
    Text(
        text     = "♟",
        fontSize = 58.sp,
        color    = Color.White,
        modifier = Modifier
            .scale(scale)
            .shadow(10.dp, CircleShape, spotColor = Color.White.copy(alpha = 0.4f))
            .background(Color.White.copy(alpha = 0.1f), CircleShape)
            .padding(14.dp)
    )
}
