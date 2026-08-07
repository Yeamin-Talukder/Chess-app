package com.example.chess.ui.screens.multiplayer

import com.example.chess.network.wifidirect.WifiDirectController
import com.example.chess.network.wifi.WifiState
import com.example.chess.network.GameEvent
import com.example.chess.game.board.Square
import com.example.chess.game.board.PieceColor
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import app.cash.turbine.test
import io.mockk.coVerify

@OptIn(ExperimentalCoroutinesApi::class)
class WifiDirectGameViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var controller: WifiDirectController
    private lateinit var viewModel: WifiDirectGameViewModel

    private lateinit var hapticManager: com.example.chess.utils.HapticManager
    private lateinit var soundManager: com.example.chess.utils.SoundManager

    private val connectionStateFlow = MutableStateFlow<WifiState>(WifiState.Hosting)
    private val eventFlow = MutableSharedFlow<GameEvent>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        hapticManager = mockk(relaxed = true)
        soundManager = mockk(relaxed = true)
        controller = mockk(relaxed = true)
        
        every { controller.connectionState } returns connectionStateFlow
        every { controller.incomingEvents } returns eventFlow

        viewModel = WifiDirectGameViewModel(hapticManager, soundManager, controller)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial states are correct`() = runTest {
        viewModel.connectionState.test {
            val item = awaitItem()
            assertThat(item).isEqualTo(WifiState.Hosting)
        }
        
        viewModel.localPlayerColor.test {
            val item = awaitItem()
            assertThat(item).isEqualTo(PieceColor.WHITE)
        }
    }

    @Test
    fun `connection state updates from controller`() = runTest {
        viewModel.connectionState.test {
            assertThat(awaitItem()).isEqualTo(WifiState.Hosting)
            
            connectionStateFlow.value = WifiState.Connected("Player 2")
            
            val connectedItem = awaitItem()
            assertThat(connectedItem).isInstanceOf(WifiState.Connected::class.java)
            assertThat((connectedItem as WifiState.Connected).deviceName).isEqualTo("Player 2")
            
            // If we host, we are White
            val color = viewModel.localPlayerColor.value
            assertThat(color).isEqualTo(PieceColor.WHITE)
        }
    }

    @Test
    fun `receiving network move updates board state`() = runTest {
        val moveEvent = GameEvent.Move("e2e4")
        
        viewModel.boardState.test {
            val initial = awaitItem()
            assertThat(initial.pieces[Square(4, 6)]).isNotNull() // White pawn on e2
            
            // Send event in background to prevent blocking Turbine
            this@runTest.launch { eventFlow.emit(moveEvent) }
            
            // Skip the intermediate selection state emission
            skipItems(1)
            
            val afterMove = awaitItem()
            assertThat(afterMove.pieces[Square(4, 6)]).isNull()
            assertThat(afterMove.pieces[Square(4, 4)]).isNotNull()
        }
    }

    @Test
    fun `local move sends event to controller`() = runTest {
        viewModel.onSquareClick(Square(4, 6)) // Select e2
        viewModel.onSquareClick(Square(4, 4)) // Move to e4
        
        coVerify(exactly = 1) { 
            controller.sendEvent(GameEvent.Move("e2e4"))
        }
    }
}
