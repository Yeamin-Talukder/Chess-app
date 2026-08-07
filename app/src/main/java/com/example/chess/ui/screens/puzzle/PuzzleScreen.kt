package com.example.chess.ui.screens.puzzle

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.chess.game.board.PieceColor
import com.example.chess.theme.*
import com.example.chess.ui.components.ChessBoardUI
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ═════════════════════════════════════════════════════════════════════════════════
// Root Screen — routes between Dashboard, Playing, Import
// ═════════════════════════════════════════════════════════════════════════════════

@Composable
fun PuzzleScreen(
    onNavigateBack: () -> Unit,
    viewModel: PuzzleViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    AnimatedContent(
        targetState = state.screenMode,
        transitionSpec = {
            when {
                targetState == PuzzleScreenMode.PLAYING ->
                    slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                targetState == PuzzleScreenMode.DASHBOARD ->
                    slideInHorizontally { -it } + fadeIn() togetherWith
                            slideOutHorizontally { it } + fadeOut()
                else -> fadeIn() togetherWith fadeOut()
            }
        },
        label = "PuzzleScreenTransition"
    ) { mode ->
        when (mode) {
            PuzzleScreenMode.DASHBOARD -> PuzzleDashboardScreen(
                onNavigateBack = onNavigateBack,
                viewModel = viewModel,
                state = state
            )
            PuzzleScreenMode.PLAYING -> PuzzleBoardScreen(
                onNavigateBack = { viewModel.clearPuzzle() },
                viewModel = viewModel,
                state = state
            )
            PuzzleScreenMode.IMPORT -> ImportScreen(
                onBack = { viewModel.hideImportScreen() },
                viewModel = viewModel,
                state = state
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════════
// Dashboard Screen
// ═════════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PuzzleDashboardScreen(
    onNavigateBack: () -> Unit,
    viewModel: PuzzleViewModel,
    state: PuzzleUiState
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.importCsv(uri, context.contentResolver) { count ->
                Toast.makeText(context, "Imported $count puzzles", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Puzzles",
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showImportScreen() }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Import Puzzles")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Hero Rating Card ────────────────────────────────────────────
            item {
                HeroRatingCard(
                    rating = state.userRating,
                    streak = state.currentStreak,
                    stats = state.stats
                )
            }

            // ── Start Training Button ───────────────────────────────────────
            item {
                StartTrainingButton(
                    onClick = { viewModel.loadNextPuzzle() },
                    puzzleCount = state.puzzleCount
                )
            }

            // ── Rating Filter ───────────────────────────────────────────────
            item {
                RatingFilterSection(
                    ratingRange = state.ratingRange,
                    onRangeChange = { viewModel.setRatingRange(it) }
                )
            }

            // ── Theme Filter ────────────────────────────────────────────────
            if (state.availableThemes.isNotEmpty()) {
                item {
                    ThemeFilterSection(
                        themes = state.availableThemes,
                        selectedTheme = state.selectedTheme,
                        onThemeSelected = { viewModel.setThemeFilter(it) }
                    )
                }
            }

            // ── Quick Stats Row ─────────────────────────────────────────────
            item {
                QuickStatsRow(stats = state.stats)
            }

            // ── Recently Played ─────────────────────────────────────────────
            if (state.recentlyPlayed.isNotEmpty()) {
                item {
                    Text(
                        "Recent Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                }
                itemsIndexed(state.recentlyPlayed) { index, puzzle ->
                    RecentPuzzleCard(
                        puzzle = puzzle,
                        index = index,
                        onClick = { viewModel.loadPuzzle(puzzle.puzzleId) }
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════════
// Hero Rating Card
// ═════════════════════════════════════════════════════════════════════════════════

@Composable
private fun HeroRatingCard(
    rating: Int,
    streak: Int,
    stats: com.example.chess.repository.PuzzleStats
) {
    val animatedRating by animateIntAsState(
        targetValue = rating,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "rating"
    )

    val ratingColor = getRatingColor(rating)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                RoundedCornerShape(24.dp)
            )
            .padding(24.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "PUZZLE RATING",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 2.sp,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "$animatedRating",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        color = ratingColor,
                        lineHeight = 48.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        getRatingTierLabel(rating),
                        style = MaterialTheme.typography.bodySmall,
                        color = ratingColor.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Streak
                if (streak > 0) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val infiniteTransition = rememberInfiniteTransition(label = "fire")
                        val fireScale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.15f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "fire_scale"
                        )
                        Text(
                            "🔥",
                            fontSize = 36.sp,
                            modifier = Modifier.scale(fireScale)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "$streak streak",
                            style = MaterialTheme.typography.labelMedium,
                            color = puzzle_streakFire,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            Spacer(Modifier.height(12.dp))

            // Mini stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MiniStat(label = "Solved", value = "${stats.solved}", icon = Icons.Default.CheckCircle, color = puzzle_correct)
                MiniStat(label = "Failed", value = "${stats.failed}", icon = Icons.Default.Cancel, color = puzzle_wrong)
                MiniStat(
                    label = "Win Rate",
                    value = "${(stats.winRate * 100).toInt()}%",
                    icon = Icons.Default.TrendingUp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, icon: ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ═════════════════════════════════════════════════════════════════════════════════
// Start Training Button
// ═════════════════════════════════════════════════════════════════════════════════

@Composable
private fun StartTrainingButton(onClick: () -> Unit, puzzleCount: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "btn_pulse"
    )

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(60.dp)
            .scale(pulseScale),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp
        )
    ) {
        Icon(
            Icons.Default.PlayArrow,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                "Start Training",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            if (puzzleCount > 0) {
                Text(
                    "$puzzleCount puzzles available",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════════
// Rating Filter
// ═════════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RatingFilterSection(
    ratingRange: ClosedFloatingPointRange<Float>,
    onRangeChange: (ClosedFloatingPointRange<Float>) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Rating Range",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${ratingRange.start.toInt()} – ${ratingRange.endInclusive.toInt()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(8.dp))
        RangeSlider(
            value = ratingRange,
            onValueChange = onRangeChange,
            valueRange = 400f..3200f,
            steps = 27, // Every 100 points
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════════
// Theme Filter
// ═════════════════════════════════════════════════════════════════════════════════

@Composable
private fun ThemeFilterSection(
    themes: List<String>,
    selectedTheme: String?,
    onThemeSelected: (String?) -> Unit
) {
    Column(modifier = Modifier.padding(start = 20.dp)) {
        Text(
            "Theme",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 20.dp)
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "All" chip
            FilterChip(
                selected = selectedTheme == null,
                onClick = { onThemeSelected(null) },
                label = { Text("All") },
                leadingIcon = if (selectedTheme == null) {
                    { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(16.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            themes.take(20).forEach { theme ->
                FilterChip(
                    selected = selectedTheme == theme,
                    onClick = { onThemeSelected(if (selectedTheme == theme) null else theme) },
                    label = { Text(formatThemeName(theme), maxLines = 1) },
                    leadingIcon = if (selectedTheme == theme) {
                        { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            Spacer(Modifier.width(20.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════════
// Quick Stats
// ═════════════════════════════════════════════════════════════════════════════════

@Composable
private fun QuickStatsRow(stats: com.example.chess.repository.PuzzleStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            label = "Best Streak",
            value = "${stats.bestStreak}",
            icon = "🔥"
        )
        StatCard(
            modifier = Modifier.weight(1f),
            label = "Avg Time",
            value = formatTime(stats.averageTimeMs),
            icon = "⏱"
        )
        StatCard(
            modifier = Modifier.weight(1f),
            label = "Best Time",
            value = formatTime(stats.bestTimeMs),
            icon = "⚡"
        )
    }
}

@Composable
private fun StatCard(modifier: Modifier, label: String, value: String, icon: String) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Column {
            Text(icon, fontSize = 20.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════════
// Recent Puzzle Card
// ═════════════════════════════════════════════════════════════════════════════════

@Composable
private fun RecentPuzzleCard(
    puzzle: com.example.chess.database.PuzzleWithProgress,
    index: Int,
    onClick: () -> Unit
) {
    val solved = puzzle.isSolved == true
    val statusColor = if (solved) puzzle_correct else puzzle_wrong
    val statusIcon = if (solved) Icons.Default.CheckCircle else Icons.Default.Cancel

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        puzzle.puzzleId,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RatingBadge(rating = puzzle.rating, small = true)
                        if (puzzle.themes.isNotEmpty()) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                formatThemeName(puzzle.themes.split(" ").firstOrNull() ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (puzzle.timeTakenMs != null && puzzle.timeTakenMs > 0) {
                    Text(
                        formatTime(puzzle.timeTakenMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    }

// ═════════════════════════════════════════════════════════════════════════════════
// Puzzle Board Screen (Solving)
// ═════════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PuzzleBoardScreen(
    onNavigateBack: () -> Unit,
    viewModel: PuzzleViewModel,
    state: PuzzleUiState
) {
    var shakeOffset by remember { mutableStateOf(0f) }

    // Shake on wrong move
    LaunchedEffect(state.puzzleStatus) {
        if (state.puzzleStatus == PuzzleStatus.WRONG_MOVE) {
            repeat(4) {
                animate(0f, 15f, animationSpec = tween(50)) { value, _ -> shakeOffset = value }
                animate(15f, -15f, animationSpec = tween(50)) { value, _ -> shakeOffset = value }
            }
            animate(-15f, 0f, animationSpec = tween(50)) { value, _ -> shakeOffset = value }
        }
    }

    // Status bar flash colors
    val boardOverlayColor by animateColorAsState(
        targetValue = when (state.puzzleStatus) {
            PuzzleStatus.CORRECT_MOVE -> puzzle_correct.copy(alpha = 0.15f)
            PuzzleStatus.WRONG_MOVE -> puzzle_wrong.copy(alpha = 0.2f)
            else -> Color.Transparent
        },
        animationSpec = tween(300),
        label = "boardOverlay"
    )

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top Bar ─────────────────────────────────────────────────────
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Extension,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            state.currentPuzzle?.puzzleId ?: "Puzzle",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    state.currentPuzzle?.let { puzzle ->
                        RatingBadge(rating = puzzle.rating, small = false)
                        Spacer(Modifier.width(12.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            // ── Turn Indicator + Status ─────────────────────────────────────
            val statusText = when (state.puzzleStatus) {
                PuzzleStatus.LOADING -> "Loading puzzle…"
                PuzzleStatus.THINKING -> if (state.playerColor == PieceColor.WHITE) "White to move" else "Black to move"
                PuzzleStatus.OPPONENT_MOVING -> "Opponent is playing…"
                PuzzleStatus.CORRECT_MOVE -> "Correct! ✓"
                PuzzleStatus.WRONG_MOVE -> "That's not right"
                PuzzleStatus.SOLVED -> "Puzzle Solved!"
                PuzzleStatus.FAILED -> "Try again"
            }
            val statusColor = when (state.puzzleStatus) {
                PuzzleStatus.CORRECT_MOVE, PuzzleStatus.SOLVED -> puzzle_correct
                PuzzleStatus.WRONG_MOVE, PuzzleStatus.FAILED -> puzzle_wrong
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            val statusBg = when (state.puzzleStatus) {
                PuzzleStatus.CORRECT_MOVE, PuzzleStatus.SOLVED -> puzzle_correct.copy(alpha = 0.15f)
                PuzzleStatus.WRONG_MOVE, PuzzleStatus.FAILED -> puzzle_wrong.copy(alpha = 0.15f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(20.dp))
                    .background(statusBg)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    statusText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = statusColor
                )
            }

            Spacer(Modifier.height(4.dp))

            // ── Move Progress Dots ──────────────────────────────────────────
            val totalPlayerMoves = viewModel.playerMovesTotal()
            val completedMoves = viewModel.playerMovesCompleted()
            if (totalPlayerMoves > 0) {
                MoveProgressDots(
                    total = totalPlayerMoves,
                    completed = completedMoves,
                    isSolved = state.isSolved,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── The Board ───────────────────────────────────────────────────
            val boardSize = LocalConfiguration.current.screenWidthDp.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(boardSize)
                    .graphicsLayer { translationX = shakeOffset }
                    .background(boardOverlayColor)
            ) {
                val hintMoveStr = if (
                    state.hintLevel >= 2 &&
                    state.currentMoveIndex < state.solutionMoves.size &&
                    state.puzzleStatus == PuzzleStatus.THINKING
                ) {
                    state.solutionMoves[state.currentMoveIndex]
                } else null

                ChessBoardUI(
                    boardState = state.boardState,
                    isFlipped = state.playerColor == PieceColor.BLACK,
                    hintMove = hintMoveStr,
                    onSquareClick = { viewModel.onSquareClick(it) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Themes Tags ─────────────────────────────────────────────────
            state.currentPuzzle?.let { puzzle ->
                if (puzzle.themes.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        puzzle.themes.split(" ").filter { it.isNotBlank() }.forEach { theme ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    formatThemeName(theme),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Hint Text ───────────────────────────────────────────────────
            AnimatedVisibility(
                visible = state.hintLevel > 0 && state.puzzleStatus == PuzzleStatus.THINKING
            ) {
                val hint = buildHintText(state)
                Text(
                    text = hint,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    textAlign = TextAlign.Center,
                    color = puzzle_hint,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.weight(1f))
        }

        // ── Bottom Action Bar ───────────────────────────────────────────────
        AnimatedVisibility(
            visible = state.puzzleStatus != PuzzleStatus.SOLVED,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(28.dp))
                    .shadow(8.dp, RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.puzzleStatus == PuzzleStatus.WRONG_MOVE || state.isFailed) {
                    // Retry Button
                    Button(
                        onClick = { viewModel.retry() },
                        colors = ButtonDefaults.buttonColors(containerColor = puzzle_wrong),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Retry", fontWeight = FontWeight.Bold)
                    }
                } else if (state.puzzleStatus == PuzzleStatus.THINKING) {
                    // Hint Button
                    TextButton(
                        onClick = { viewModel.requestHint() },
                        enabled = state.hintLevel < 3
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = if (state.hintLevel < 3) puzzle_hint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Hint (${state.hintLevel}/3)",
                            color = if (state.hintLevel < 3) puzzle_hint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                }

                // Skip Button
                TextButton(
                    onClick = { viewModel.loadNextPuzzle() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Skip")
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }

        // ── Solved Overlay ──────────────────────────────────────────────────
        AnimatedVisibility(
            visible = state.puzzleStatus == PuzzleStatus.SOLVED,
            enter = fadeIn(tween(400)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.fillMaxSize()
        ) {
            SolvedOverlay(
                state = state,
                onNextPuzzle = { viewModel.loadNextPuzzle() },
                onBackToDashboard = { viewModel.clearPuzzle() }
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════════
// Move Progress Dots
// ═════════════════════════════════════════════════════════════════════════════════

@Composable
private fun MoveProgressDots(
    total: Int,
    completed: Int,
    isSolved: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until total.coerceAtMost(10)) {
            val dotColor = when {
                isSolved -> puzzle_correct
                i < completed -> puzzle_correct
                i == completed -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            }
            val dotSize = if (i == completed && !isSolved) 10.dp else 8.dp
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
        if (total > 10) {
            Text(
                "+${total - 10}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════════
// Solved Overlay
// ═════════════════════════════════════════════════════════════════════════════════

@Composable
private fun SolvedOverlay(
    state: PuzzleUiState,
    onNextPuzzle: () -> Unit,
    onBackToDashboard: () -> Unit
) {
    val checkScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 200f),
        label = "checkScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Success icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .scale(checkScale)
                    .clip(CircleShape)
                    .background(puzzle_correct.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = puzzle_correct,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "Puzzle Solved!",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(8.dp))

            // Rating change
            val ratingChange = state.ratingChange
            val ratingText = if (ratingChange >= 0) "+$ratingChange" else "$ratingChange"
            val ratingColor = if (ratingChange >= 0) puzzle_correct else puzzle_wrong
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Rating: ${state.userRating}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    ratingText,
                    fontWeight = FontWeight.Bold,
                    color = ratingColor,
                    fontSize = 18.sp
                )
            }

            if (state.currentStreak > 1) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "🔥 ${state.currentStreak} puzzle streak!",
                    color = puzzle_streakFire,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onNextPuzzle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = puzzle_correct)
            ) {
                Text("Next Puzzle", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(10.dp))

            TextButton(onClick = onBackToDashboard) {
                Text(
                    "Back to Dashboard",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════════
// Import Screen
// ═════════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportScreen(
    onBack: () -> Unit,
    viewModel: PuzzleViewModel,
    state: PuzzleUiState
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.importCsv(uri, context.contentResolver) { count ->
                Toast.makeText(context, "Imported $count puzzles!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Import Puzzles", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Info Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Lichess Puzzle Database",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Download the Lichess puzzle CSV from database.lichess.org and import it here. " +
                                "The file contains millions of puzzles with ratings, themes, and opening tags.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Expected format: PuzzleId, FEN, Moves, Rating, RatingDeviation, Popularity, NbPlays, Themes, GameUrl, OpeningTags",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Import Button
            Button(
                onClick = { launcher.launch(arrayOf("text/comma-separated-values", "text/csv", "*/*")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = !state.importState.isRunning
            ) {
                if (state.importState.isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Importing…")
                } else {
                    Icon(Icons.Default.FileUpload, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("Select CSV File", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            // Import Progress
            val importState = state.importState
            if (importState.isRunning || importState.isComplete) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(importState.phase, fontWeight = FontWeight.Bold)
                            if (importState.isRunning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else if (importState.isComplete && importState.errorMessage == null) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = puzzle_correct,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        if (importState.isRunning) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        Text(
                            "${importState.importedCount} puzzles imported",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (importState.errorCount > 0) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${importState.errorCount} rows skipped",
                                style = MaterialTheme.typography.bodySmall,
                                color = puzzle_wrong
                            )
                        }

                        if (importState.errorMessage != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                importState.errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = puzzle_wrong
                            )
                        }
                    }
                }
            }

            // Current puzzle count
            Spacer(Modifier.height(8.dp))
            Text(
                "${state.puzzleCount} puzzles in database",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════════
// Rating Badge
// ═════════════════════════════════════════════════════════════════════════════════

@Composable
fun RatingBadge(rating: Int, small: Boolean = false) {
    val color = getRatingColor(rating)
    val fontSize = if (small) 11.sp else 13.sp
    val hPad = if (small) 8.dp else 10.dp
    val vPad = if (small) 2.dp else 4.dp

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = hPad, vertical = vPad)
    ) {
        Text(
            "$rating",
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════════
// Helpers
// ═════════════════════════════════════════════════════════════════════════════════

private fun getRatingColor(rating: Int): Color {
    return when {
        rating < 1200 -> puzzle_ratingBeginner
        rating < 1600 -> puzzle_ratingInter
        rating < 2000 -> puzzle_ratingAdvanced
        rating < 2500 -> puzzle_ratingExpert
        else -> puzzle_ratingMaster
    }
}

private fun getRatingTierLabel(rating: Int): String {
    return when {
        rating < 1200 -> "Beginner"
        rating < 1600 -> "Intermediate"
        rating < 2000 -> "Advanced"
        rating < 2500 -> "Expert"
        else -> "Master"
    }
}

private fun formatThemeName(theme: String): String {
    return theme.replace("_", " ")
        .replaceFirstChar { it.uppercase() }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "—"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
}

private fun buildHintText(state: PuzzleUiState): String {
    val moves = state.solutionMoves
    val idx = state.currentMoveIndex
    if (idx >= moves.size) return ""
    val move = moves[idx]
    if (move.length < 4) return ""

    val fromFile = 'a' + (move[0] - 'a')
    val fromRank = move[1]
    val toFile = 'a' + (move[2] - 'a')
    val toRank = move[3]

    return when (state.hintLevel) {
        1 -> "💡 Focus on the piece at $fromFile$fromRank"
        2 -> "💡 Try moving $fromFile$fromRank to $toFile$toRank"
        3 -> "💡 The best move is $fromFile$fromRank → $toFile$toRank"
        else -> ""
    }
}
