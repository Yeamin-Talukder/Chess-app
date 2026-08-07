package com.example.chess.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.chess.game.board.BoardState
import com.example.chess.game.board.PieceColor
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameScreenLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun layoutRendersCorrectly() {
        composeTestRule.setContent {
            GameScreenLayout(
                title = "Local Play",
                opponentName = "Black Player",
                localPlayerName = "White Player",
                isConnectionLost = false,
                boardState = BoardState.initial(),
                localColor = PieceColor.WHITE,
                promotionRequest = null,
                onSquareClick = {},
                onPromotionSelected = {},
                onPromotionCanceled = {},
                onResign = {},
                onDisconnect = {},
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithText("Local Play").assertExists()
        composeTestRule.onNodeWithText("Black Player").assertExists()
        composeTestRule.onNodeWithText("White Player").assertExists()
    }

    @Test
    fun clickResignShowsDialog() {
        composeTestRule.setContent {
            GameScreenLayout(
                title = "Local Play",
                opponentName = "Black Player",
                localPlayerName = "White Player",
                isConnectionLost = false,
                boardState = BoardState.initial(),
                localColor = PieceColor.WHITE,
                promotionRequest = null,
                onSquareClick = {},
                onPromotionSelected = {},
                onPromotionCanceled = {},
                onResign = {},
                onDisconnect = {},
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithText("Resign").performClick()
        composeTestRule.onNodeWithText("Are you sure you want to resign this match?").assertExists()
        composeTestRule.onNodeWithText("Yes").assertExists()
    }
}
