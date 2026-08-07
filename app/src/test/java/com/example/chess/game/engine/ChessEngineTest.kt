package com.example.chess.game.engine

import com.example.chess.game.board.GameStatus
import com.example.chess.game.board.PieceColor
import com.example.chess.game.board.PieceType
import com.example.chess.game.board.Square
import com.example.chess.utils.HapticManager
import com.example.chess.utils.SoundManager
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.junit.Before
import org.junit.Test

class ChessEngineTest {

    private lateinit var hapticManager: HapticManager
    private lateinit var soundManager: SoundManager
    private lateinit var engine: ChessEngine

    @Before
    fun setup() {
        hapticManager = mockk(relaxed = true)
        soundManager = mockk(relaxed = true)
        engine = ChessEngine(hapticManager, soundManager)
    }

    @Test
    fun `initial board state is correctly setup`() {
        val state = engine.boardState.value
        assertThat(state.turn).isEqualTo(PieceColor.WHITE)
        assertThat(state.gameStatus).isEqualTo(GameStatus.ONGOING)
        assertThat(state.pieces.size).isEqualTo(32)
    }

    @Test
    fun `selecting an empty square does not change selection`() {
        engine.selectSquare(Square(4, 4)) // Empty square
        val state = engine.boardState.value
        assertThat(state.legalMovesForSelected).isEmpty()
    }

    @Test
    fun `selecting own piece highlights legal moves`() {
        // Select White pawn at e2 (file 4, rank 6)
        engine.selectSquare(Square(4, 6))
        val state = engine.boardState.value
        assertThat(state.legalMovesForSelected).isNotEmpty()
        // e2 pawn can move to e3 or e4
        assertThat(state.legalMovesForSelected.map { it.to }).containsExactly(
            Square(4, 5), Square(4, 4)
        )
    }

    @Test
    fun `moving piece changes turn and updates board`() {
        // Move White pawn e2 to e4
        engine.selectSquare(Square(4, 6))
        engine.selectSquare(Square(4, 4))
        
        var state = engine.boardState.value
        assertThat(state.turn).isEqualTo(PieceColor.BLACK)
        assertThat(state.pieces[Square(4, 4)]?.type).isEqualTo(PieceType.PAWN)
        assertThat(state.pieces[Square(4, 6)]).isNull()
        
        // Black's turn, select e7 pawn (file 4, rank 1)
        engine.selectSquare(Square(4, 1))
        engine.selectSquare(Square(4, 3)) // e5
        
        state = engine.boardState.value
        assertThat(state.turn).isEqualTo(PieceColor.WHITE)
    }
}
