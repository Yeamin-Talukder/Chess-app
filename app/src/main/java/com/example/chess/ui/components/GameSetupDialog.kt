@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
package com.example.chess.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.chess.game.board.PieceColor
import com.example.chess.ui.screens.game.BoardOrientation
import com.example.chess.ui.screens.game.GameConfig
import com.example.chess.ui.screens.game.GameMode
import com.example.chess.ui.screens.game.PlayerColorChoice
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

// Chess.com theme colors
private val ChessDarkBg = Color(0xFF1C1A17)
private val ChessCardBg = Color(0xFF262421)
private val ChessValueBoxBg = Color(0xFF32302C)
private val ChessGreenPrimary = Color(0xFF81B64C)
private val ChessTextGrey = Color(0xFF989795)

/**
 * Redesigned Chess.com style "New Game" Setup Dialog for Offline Match with drum roller and pop-up animation.
 */
@Composable
fun GameSetupDialog(
    showDialog: MutableState<Boolean>,
    onConfirm: (GameConfig) -> Unit,
    currentUserName: String = "Player 1",
    onDismiss: () -> Unit = { showDialog.value = false }
) {
    var mainMinutes by remember { mutableIntStateOf(11) }
    var incSeconds by remember { mutableIntStateOf(1) }
    var colorChoice by remember { mutableStateOf(PlayerColorChoice.RANDOM) }

    var player1Name by remember { mutableStateOf(currentUserName) }
    var player2Name by remember { mutableStateOf("Player 2") }

    // Animation state for smooth dialog entrance / exit
    var isVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(showDialog.value) {
        if (showDialog.value) {
            isVisible = true
        }
    }

    fun handleExit(onComplete: () -> Unit) {
        coroutineScope.launch {
            isVisible = false
            delay(220) // Smooth exit transition duration
            onComplete()
        }
    }

    if (!showDialog.value) return

    Dialog(
        onDismissRequest = {
            handleExit { onDismiss() }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = scaleIn(
                initialScale = 0.82f,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 400f)
            ) + fadeIn(tween(250)),
            exit = scaleOut(
                targetScale = 0.85f,
                animationSpec = tween(200)
            ) + fadeOut(tween(200))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = ChessDarkBg,
                tonalElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ── Dialog Title ──────────────────────────────────────
                    Text(
                        text = "New Game",
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── PLAYERS Section ──────────────────────────────────
                    SectionLabelHeader("PLAYERS")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PlayerInputField(
                            label = "You",
                            value = player1Name,
                            onValueChange = { player1Name = it },
                            modifier = Modifier.weight(1f)
                        )
                        PlayerInputField(
                            label = "Opponent",
                            value = player2Name,
                            onValueChange = { player2Name = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── TIME CONTROL DRUM ROLLER CARD ────────────────────
                    TimeDrumRollerCard(
                        mainMinutes = mainMinutes,
                        onMainMinutesChange = { mainMinutes = it },
                        incSeconds = incSeconds,
                        onIncSecondsChange = { incSeconds = it }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── CHOOSE YOUR COLOR Section ────────────────────────
                    SectionLabelHeader("CHOOSE YOUR COLOR")
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ChessColorChoiceCard(
                            title = "White",
                            iconType = ColorIconType.WHITE_KING,
                            isSelected = colorChoice == PlayerColorChoice.WHITE,
                            onClick = { colorChoice = PlayerColorChoice.WHITE },
                            modifier = Modifier.weight(1f)
                        )
                        ChessColorChoiceCard(
                            title = "Random",
                            iconType = ColorIconType.RANDOM_SPLIT,
                            isSelected = colorChoice == PlayerColorChoice.RANDOM,
                            onClick = { colorChoice = PlayerColorChoice.RANDOM },
                            modifier = Modifier.weight(1f)
                        )
                        ChessColorChoiceCard(
                            title = "Black",
                            iconType = ColorIconType.BLACK_KING,
                            isSelected = colorChoice == PlayerColorChoice.BLACK,
                            onClick = { colorChoice = PlayerColorChoice.BLACK },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // ── ACTION BUTTONS ────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { handleExit { onDismiss() } }
                        ) {
                            Text(
                                text = "Cancel",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }

                        Button(
                            onClick = {
                                handleExit {
                                    val totalTimeMs = (mainMinutes * 60L) * 1000L
                                    val safeTimeMs = if (totalTimeMs == 0L) 10 * 60 * 1000L else totalTimeMs
                                    val incrementMs = (incSeconds) * 1000L

                                    val firstMove = when (colorChoice) {
                                        PlayerColorChoice.WHITE -> PieceColor.WHITE
                                        PlayerColorChoice.BLACK -> PieceColor.BLACK
                                        PlayerColorChoice.RANDOM ->
                                            if ((0..1).random() == 0) PieceColor.WHITE else PieceColor.BLACK
                                    }

                                    val config = GameConfig(
                                        mode = GameMode.LOCAL_OFFLINE,
                                        timeHours = 0,
                                        timeMinutes = mainMinutes,
                                        timeSeconds = 0,
                                        incrementSeconds = incSeconds,
                                        playerColorChoice = colorChoice,
                                        timeMillis = safeTimeMs,
                                        incrementMillis = incrementMs,
                                        orientation = BoardOrientation.WHITE_BOTTOM,
                                        firstMove = firstMove,
                                        player1Name = player1Name.trim().ifEmpty { "Player 1" },
                                        player2Name = player2Name.trim().ifEmpty { "Player 2" }
                                    )
                                    onConfirm(config)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ChessGreenPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .padding(start = 16.dp)
                        ) {
                            Text(
                                text = "Start Game",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabelHeader(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = ChessTextGrey,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PlayerInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ChessCardBg)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                ),
                cursorBrush = SolidColor(ChessGreenPrimary),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TimeDrumRollerCard(
    mainMinutes: Int,
    onMainMinutesChange: (Int) -> Unit,
    incSeconds: Int,
    onIncSecondsChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = ChessCardBg
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Main minutes drum roller
                DrumRollNumberPicker(
                    value = mainMinutes,
                    onValueChange = onMainMinutesChange,
                    range = 0..60,
                    label = "MAIN (MIN)",
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "+",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 4.dp).padding(bottom = 20.dp)
                )

                // Increment seconds drum roller
                DrumRollNumberPicker(
                    value = incSeconds,
                    onValueChange = onIncSecondsChange,
                    range = 0..60,
                    label = "INCREMENT (SEC)",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrumRollNumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    label: String,
    modifier: Modifier = Modifier,
    visibleItems: Int = 3,
    itemHeight: Dp = 48.dp
) {
    val count = range.last - range.first + 1
    val initialIdx = (value - range.first).coerceIn(0, count - 1)

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIdx)
    val snapBehavior = rememberSnapFlingBehavior(listState)

    // Sync selected value with listState firstVisibleItemIndex when scroll stops
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { firstIdx ->
                val newIndex = firstIdx.coerceIn(0, count - 1)
                onValueChange(range.first + newIndex)
            }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .height(itemHeight * visibleItems)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Selected Value Center Highlight Box (#32302C like screenshot)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .height(itemHeight)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ChessValueBoxBg)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            )

            // Drum roller LazyColumn with snapping
            LazyColumn(
                state = listState,
                flingBehavior = snapBehavior,
                modifier = Modifier
                    .height(itemHeight * visibleItems)
                    .fillMaxWidth()
            ) {
                // Top padding item
                item { Spacer(modifier = Modifier.height(itemHeight)) }

                items(count) { idx ->
                    val itemValue = range.first + idx
                    val isSelected = (range.first + listState.firstVisibleItemIndex) == itemValue

                    val itemAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 1.0f else 0.35f,
                        animationSpec = tween(150),
                        label = "drumAlpha_$idx"
                    )

                    Box(
                        modifier = Modifier
                            .height(itemHeight)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = itemValue.toString(),
                            fontSize = if (isSelected) 30.sp else 18.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = Color.White.copy(alpha = itemAlpha),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Bottom padding item
                item { Spacer(modifier = Modifier.height(itemHeight)) }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Column Sublabel below
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = ChessTextGrey
        )
    }
}

private enum class ColorIconType {
    WHITE_KING, BLACK_KING, RANDOM_SPLIT
}

@Composable
private fun ChessColorChoiceCard(
    title: String,
    iconType: ColorIconType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) ChessGreenPrimary else Color.Transparent
    val textColor = if (isSelected) ChessGreenPrimary else Color.White.copy(alpha = 0.7f)
    val bgColor = if (isSelected) ChessGreenPrimary.copy(alpha = 0.12f) else ChessCardBg

    Box(
        modifier = modifier
            .height(95.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (iconType) {
                ColorIconType.WHITE_KING -> {
                    // Filled White King Logo
                    Text(
                        text = "♚",
                        fontSize = 32.sp,
                        color = Color.White
                    )
                }
                ColorIconType.BLACK_KING -> {
                    // Filled Black King Logo
                    Text(
                        text = "♚",
                        fontSize = 32.sp,
                        color = Color(0xFF101010)
                    )
                }
                ColorIconType.RANDOM_SPLIT -> {
                    // Split Half-White & Half-Black Vertical Circle Logo for Random
                    RandomSplitColorCircleIcon(
                        isSelected = isSelected,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
private fun RandomSplitColorCircleIcon(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val centerPt = Offset(size.width / 2f, size.height / 2f)

        // Left half: White
        drawArc(
            color = Color.White,
            startAngle = 90f,
            sweepAngle = 180f,
            useCenter = true,
            size = Size(radius * 2f, radius * 2f),
            topLeft = Offset(centerPt.x - radius, centerPt.y - radius)
        )

        // Right half: Dark/Black
        drawArc(
            color = Color(0xFF262421),
            startAngle = 270f,
            sweepAngle = 180f,
            useCenter = true,
            size = Size(radius * 2f, radius * 2f),
            topLeft = Offset(centerPt.x - radius, centerPt.y - radius)
        )

        // Outer border stroke
        drawCircle(
            color = if (isSelected) ChessGreenPrimary else Color.White.copy(alpha = 0.85f),
            radius = radius,
            center = centerPt,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
