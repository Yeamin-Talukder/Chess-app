package com.example.chess.ui.screens.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.chess.ui.screens.profile.ProfileViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToOfflineGame:     () -> Unit,
    onNavigateToWifiLobby:       () -> Unit,
    onNavigateToProfile:         () -> Unit,
    onNavigateToHistory:         () -> Unit,
    onNavigateToSettings:        () -> Unit,
    onNavigateToAbout:           () -> Unit,
    onNavigateToPuzzle:          () -> Unit,
    onNavigateToBot:             () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profile     by viewModel.profile.collectAsState(initial = null)
    val username    = profile?.username?.ifBlank { "Guest Player" } ?: "Guest Player"
    val gamesPlayed = profile?.gamesPlayed ?: 0
    val wins        = profile?.wins ?: 0
    val winRate     = if (gamesPlayed > 0) "%.0f%%".format(wins.toFloat() / gamesPlayed * 100) else "—"

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            // ── Hero Banner ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
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
                // Ambient Unicode Chess Animation & Perspective Grid
                com.example.chess.ui.components.ChessHeroAnimation(
                    accentColor = Color(0xFF4CAF50),
                    darkBgColor = Color(0xFF0B1910)
                )

                // Foreground content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateToAbout) {
                            Icon(Icons.Default.Info, contentDescription = "About",
                                tint = Color.White.copy(alpha = 0.85f))
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings",
                                tint = Color.White.copy(alpha = 0.85f))
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(top = 36.dp),
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

            Spacer(Modifier.height(16.dp))

            // ── Glassmorphic Profile Card ──────────────────────────────────────
            AnimatableCard(delay = 80) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.12f),
                                    Color(0xFF2E7D32).copy(alpha = 0.18f)
                                )
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.40f),
                                    Color.White.copy(alpha = 0.10f)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clickable { onNavigateToProfile() }
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFF4CAF50), Color(0xFF1B5E20)))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text      = profile?.avatar ?: username.firstOrNull()?.uppercaseChar()?.toString() ?: "G",
                                fontSize  = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color     = Color.White
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text      = username,
                                style     = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color     = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text  = "$gamesPlayed games · $wins wins · $winRate win rate",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.Person, contentDescription = "Profile",
                            tint     = Color(0xFF4CAF50),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── PLAY section ───────────────────────────────────────────────────
            AnimatableCard(delay = 140) {
                SectionLabel("PLAY", modifier = Modifier.padding(horizontal = 20.dp))
            }
            Spacer(Modifier.height(12.dp))

            // Offline Match – hero highlight card (Swapped position with Play vs Bot)
            AnimatableCard(delay = 190) {
                HeroModeCard(
                    title    = "Offline Match",
                    subtitle = "Pass & Play on One Device",
                    icon     = Icons.Default.PlayArrow,
                    gradient = listOf(Color(0xFF1B5E20), Color(0xFF2E7D32), Color(0xFF4CAF50)),
                    onClick  = onNavigateToOfflineGame,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(120.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Play vs Bot + Puzzles dual row (Swapped position with Offline Match)
            AnimatableCard(delay = 240) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GradientSquareCard(
                        title    = "Play vs Bot",
                        icon     = Icons.Rounded.SmartToy,
                        gradient = listOf(Color(0xFF7B1FA2), Color(0xFFC2185B)),
                        onClick  = onNavigateToBot,
                        modifier = Modifier.weight(1f).height(120.dp)
                    )
                    GradientSquareCard(
                        title    = "Puzzles",
                        icon     = Icons.Default.EmojiEvents,
                        gradient = listOf(Color(0xFFE65100), Color(0xFFFFB300)),
                        onClick  = onNavigateToPuzzle,
                        modifier = Modifier.weight(1f).height(120.dp)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── MULTIPLAYER section ────────────────────────────────────────────
            AnimatableCard(delay = 300) {
                SectionLabel("MULTIPLAYER", modifier = Modifier.padding(horizontal = 20.dp))
            }
            Spacer(Modifier.height(12.dp))

            AnimatableCard(delay = 350) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconSurfaceCard(
                        title   = "Wi-Fi Local Network",
                        icon    = Icons.Default.Wifi,
                        onClick = onNavigateToWifiLobby,
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── EXPLORE section ────────────────────────────────────────────────
            AnimatableCard(delay = 400) {
                SectionLabel("EXPLORE", modifier = Modifier.padding(horizontal = 20.dp))
            }
            Spacer(Modifier.height(12.dp))

            AnimatableCard(delay = 450) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExploreCard(
                        title    = "History",
                        subtitle = "Past games",
                        icon     = Icons.Default.DateRange,
                        onClick  = onNavigateToHistory,
                        modifier = Modifier.weight(1f)
                    )
                    ExploreCard(
                        title    = "Stats",
                        subtitle = "Your profile",
                        icon     = Icons.Default.Star,
                        onClick  = onNavigateToProfile,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(48.dp))
        }

    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Section label
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text          = text,
        style         = MaterialTheme.typography.labelMedium,
        fontWeight    = FontWeight.ExtraBold,
        letterSpacing = 2.5.sp,
        color         = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier      = modifier
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Hero gradient card (full-width tall)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HeroModeCard(
    title:    String,
    subtitle: String,
    icon:     ImageVector,
    gradient: List<Color>,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(14.dp, RoundedCornerShape(28.dp), spotColor = gradient.first().copy(alpha = 0.45f))
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(gradient))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick    = onClick
            )
    ) {
        // decorative orb
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 24.dp, y = 24.dp)
                .size(110.dp)
                .background(Color.White.copy(alpha = 0.12f), CircleShape)
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.width(18.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold, color = Color.White)
                Spacer(Modifier.height(3.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.80f))
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.PlayArrow, null,
                tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.size(28.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Square gradient card (half-width)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GradientSquareCard(
    title:    String,
    icon:     ImageVector,
    gradient: List<Color>,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(10.dp, RoundedCornerShape(22.dp), spotColor = gradient.first().copy(alpha = 0.35f))
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(gradient))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick    = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 12.dp, y = 12.dp)
                .size(64.dp)
                .background(Color.White.copy(alpha = 0.12f), CircleShape)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Icon(icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(30.dp))
            Text(title, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Surface icon card (multiplayer row)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun IconSurfaceCard(
    title:   String,
    icon:    ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick    = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(Color(0xFF4CAF50).copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title,
                    tint = Color(0xFF2E7D32), modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Explore card (half-width surface)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ExploreCard(
    title:    String,
    subtitle: String,
    icon:     ImageVector,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(108.dp)
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick    = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = title,
                tint = Color(0xFF4CAF50), modifier = Modifier.size(26.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Stagger fade-slide wrapper
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AnimatableCard(delay: Int, content: @Composable () -> Unit) {
    val alpha   = remember { Animatable(0f) }
    val offsetY = remember { Animatable(32f) }
    LaunchedEffect(Unit) {
        delay(delay.toLong())
        launch { alpha.animateTo(1f, tween(600)) }
        launch { offsetY.animateTo(0f, tween(600, easing = EaseOutCubic)) }
    }
    Box(modifier = Modifier.alpha(alpha.value).offset(y = offsetY.value.dp)) { content() }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Hero pawn – pulsing scale
// ─────────────────────────────────────────────────────────────────────────────
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

// ─────────────────────────────────────────────────────────────────────────────
//  Animated floating pieces behind hero blur
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AnimatedHeroBackground() {
    data class Cfg(
        val piece: String,
        val sx: Float, val sy: Float,
        val ex: Float, val ey: Float,
        val dur: Int, val alpha: Float, val sizeSp: Float
    )
    val cfgs = listOf(
        Cfg("♔", .04f, .10f, .16f, .82f, 7000, .22f, 34f),
        Cfg("♕", .82f, .04f, .70f, .88f, 8500, .16f, 26f),
        Cfg("♖", .28f, .00f, .38f, .95f, 6000, .18f, 28f),
        Cfg("♗", .62f, .12f, .50f, .86f, 9000, .14f, 22f),
        Cfg("♘", .10f, .60f, .22f, .04f, 7500, .16f, 30f),
        Cfg("♙", .74f, .55f, .84f, .06f, 6500, .18f, 24f),
        Cfg("♟", .44f, .84f, .54f, .02f, 5500, .12f, 20f),
        Cfg("♞", .90f, .38f, .78f, .80f, 10000, .15f, 28f),
    )
    Box(modifier = Modifier.fillMaxSize()) {
        cfgs.forEach { c ->
            HeroFloatingPiece(c.piece, c.sx, c.sy, c.ex, c.ey, c.dur, c.alpha, c.sizeSp.sp)
        }
    }
}

@Composable
private fun HeroFloatingPiece(
    piece: String,
    sx: Float, sy: Float,
    ex: Float, ey: Float,
    durMs: Int,
    alpha: Float,
    fontSize: androidx.compose.ui.unit.TextUnit
) {
    val inf = rememberInfiniteTransition(label = "fp_$piece$durMs")
    val px by inf.animateFloat(
        sx, ex,
        infiniteRepeatable(tween(durMs, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "fpx_$piece$durMs"
    )
    val py by inf.animateFloat(
        sy, ey,
        infiniteRepeatable(tween(durMs, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "fpy_$piece$durMs"
    )
    Text(
        text     = piece,
        fontSize = fontSize,
        color    = Color.White.copy(alpha = alpha),
        modifier = Modifier
            .fillMaxSize()
            .offset(x = (px * 360).dp, y = (py * 240).dp)
    )
}
