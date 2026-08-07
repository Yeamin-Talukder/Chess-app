package com.example.chess.ui.screens.puzzle

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.database.ImportState
import com.example.chess.database.PuzzleEntity
import com.example.chess.database.PuzzleWithProgress
import com.example.chess.game.board.BoardState
import com.example.chess.game.board.PieceColor
import com.example.chess.game.board.PieceType
import com.example.chess.game.board.Square
import com.example.chess.game.engine.ChessEngine
import com.example.chess.repository.PuzzleRepository
import com.example.chess.repository.PuzzleStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Sample Puzzles ──────────────────────────────────────────────────────────────
// Bundled so the app works offline without a CSV import.
private val SAMPLE_PUZZLES = listOf(
    PuzzleEntity("sp001", "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4", "f3g5 d8f6 g5f7", 1200, 80, 90, 1000, "fork", "", ""),
    PuzzleEntity("sp002", "r2qkb1r/ppp2ppp/2n1pn2/3p4/3P4/2NBPN2/PPP2PPP/R1BQK2R w KQkq - 0 7", "c3d5 e6d5 f3e5", 1300, 90, 80, 900, "fork", "", ""),
    PuzzleEntity("sp003", "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPPBPPP/RNBQK2R b KQkq - 3 3", "c6d4 f3d4 e5d4", 1100, 70, 95, 1200, "capture", "", ""),
    PuzzleEntity("sp004", "r1bq1rk1/pppp1ppp/2n2n2/2b1p3/2B1P3/2NP1N2/PPP2PPP/R1BQ1RK1 b - - 0 7", "f6e4 c3e4 c5f2", 1400, 100, 85, 800, "pin", "", ""),
    PuzzleEntity("sp005", "rnbqk2r/pppp1ppp/5n2/2b1p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4", "d1e2 c5f2 e1f2", 1500, 110, 70, 700, "sacrifice", "", ""),
    PuzzleEntity("sp006", "r4rk1/pp3ppp/2p5/4n3/4P3/2N5/PPP2PPP/R4RK1 w - - 0 15", "c3e2 e5f3 e2f3", 1250, 85, 75, 850, "tactic", "", ""),
    PuzzleEntity("sp007", "5rk1/pp3ppp/2p5/8/4n3/4B3/PPP2PPP/R5K1 w - - 0 18", "e3b6 e4f2 g1f2", 1350, 95, 65, 600, "skewer", "", ""),
    PuzzleEntity("sp008", "r1b1k2r/pppp1ppp/2n2n2/4p3/4P3/2N2N2/PPPP1PPP/R1B1KB1R w KQkq - 4 5", "f3e5 c6e5 c3d5", 1200, 80, 88, 950, "fork", "", ""),
    PuzzleEntity("sp009", "rnb2rk1/ppp2ppp/3p4/3qp3/3PP3/2N2N2/PPP2PPP/R1BQK2R b KQ - 0 8", "d5d4 c3d5 d4c4", 1600, 120, 60, 500, "advanced", "", ""),
    PuzzleEntity("sp010", "r3kb1r/ppp2ppp/2n1q3/3pp3/4P3/2NP1N2/PPP2PPP/R1BQK2R w KQkq - 0 9", "f3e5 c6e5 d3d4", 1450, 105, 72, 650, "pin tactic", "", "")
)

// ── Puzzle Screen Mode ──────────────────────────────────────────────────────────

enum class PuzzleScreenMode {
    DASHBOARD,
    PLAYING,
    IMPORT
}

enum class PuzzleStatus {
    LOADING,
    THINKING,        // Player's turn to move
    OPPONENT_MOVING,  // Auto-playing opponent's reply
    CORRECT_MOVE,    // Flash green
    WRONG_MOVE,      // Flash red, allow retry
    SOLVED,
    FAILED
}

// ── UI State ────────────────────────────────────────────────────────────────────

data class PuzzleUiState(
    val screenMode: PuzzleScreenMode = PuzzleScreenMode.DASHBOARD,
    val puzzleStatus: PuzzleStatus = PuzzleStatus.LOADING,

    // Puzzle data
    val currentPuzzle: PuzzleEntity? = null,
    val boardState: BoardState = BoardState.initial(),
    val playerColor: PieceColor = PieceColor.WHITE,
    val solutionMoves: List<String> = emptyList(),
    val currentMoveIndex: Int = 0,

    // Legacy compat
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSolved: Boolean = false,
    val isFailed: Boolean = false,

    // Hints
    val hintLevel: Int = 0, // 0: none, 1: piece, 2: destination, 3: full move

    // Wrong move feedback
    val wrongMoveSquare: Square? = null,

    // Filters
    val ratingRange: ClosedFloatingPointRange<Float> = 800f..3000f,
    val selectedTheme: String? = null,
    val availableThemes: List<String> = emptyList(),

    // Stats
    val stats: PuzzleStats = PuzzleStats(),
    val recentlyPlayed: List<PuzzleWithProgress> = emptyList(),
    val puzzleCount: Int = 0,

    // Import
    val importState: ImportState = ImportState(),

    // Streak & rating (for animated display)
    val currentStreak: Int = 0,
    val userRating: Int = 1500,
    val ratingChange: Int = 0 // +/- after last solve
)

@HiltViewModel
class PuzzleViewModel @Inject constructor(
    private val puzzleRepository: PuzzleRepository
) : ViewModel() {

    val engine = ChessEngine()

    private val _uiState = MutableStateFlow(PuzzleUiState())
    val uiState: StateFlow<PuzzleUiState> = _uiState.asStateFlow()

    private var puzzleStartTime = 0L
    private var hintsUsedThisPuzzle = 0

    val allPuzzles = puzzleRepository.getAllPuzzlesWithProgress()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Collect board state from engine
        viewModelScope.launch {
            engine.boardState.collect { state ->
                _uiState.update { it.copy(boardState = state) }
            }
        }

        // Seed sample puzzles
        viewModelScope.launch {
            puzzleRepository.seedSamplePuzzlesIfEmpty(SAMPLE_PUZZLES)
        }

        // Collect import state
        viewModelScope.launch {
            puzzleRepository.importState.collect { importState ->
                _uiState.update { it.copy(importState = importState) }
            }
        }

        // Collect puzzle count reactively, filtering by selected theme and rating range
        viewModelScope.launch {
            combine(
                _uiState.map { Pair(it.selectedTheme, it.ratingRange) }.distinctUntilChanged(),
                puzzleRepository.getPuzzleCountFlow()
            ) { filter, _ ->
                val theme = filter.first
                val range = filter.second
                val minRating = range.start.toInt()
                val maxRating = range.endInclusive.toInt()
                if (theme != null) {
                    puzzleRepository.getPuzzleCountByThemeAndRating(theme, minRating, maxRating)
                } else {
                    puzzleRepository.getPuzzleCountByRating(minRating, maxRating)
                }
            }.collect { count ->
                _uiState.update { it.copy(puzzleCount = count) }
            }
        }

        // Load initial dashboard data
        refreshDashboard()
    }

    // ── Dashboard ───────────────────────────────────────────────────────────────

    fun refreshDashboard() {
        viewModelScope.launch {
            val stats = puzzleRepository.getPuzzleStats()
            val themes = puzzleRepository.getDistinctThemes()
            val recent = puzzleRepository.getRecentlyPlayed(5)
            val count = puzzleRepository.getPuzzleCount()
            val rating = puzzleRepository.userRating.first()
            val streak = puzzleRepository.currentStreak.first()

            _uiState.update {
                it.copy(
                    stats = stats,
                    availableThemes = themes,
                    recentlyPlayed = recent,
                    puzzleCount = count,
                    userRating = rating,
                    currentStreak = streak
                )
            }
        }
    }

    // ── Rating Filter ───────────────────────────────────────────────────────────

    fun setRatingRange(range: ClosedFloatingPointRange<Float>) {
        _uiState.update { it.copy(ratingRange = range) }
    }

    fun setThemeFilter(theme: String?) {
        _uiState.update { it.copy(selectedTheme = theme) }
    }

    // ── Puzzle Loading ──────────────────────────────────────────────────────────

    fun loadNextPuzzle() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update {
                it.copy(
                    screenMode = PuzzleScreenMode.PLAYING,
                    puzzleStatus = PuzzleStatus.LOADING,
                    isLoading = true,
                    error = null,
                    isSolved = false,
                    isFailed = false,
                    hintLevel = 0,
                    wrongMoveSquare = null,
                    ratingChange = 0
                )
            }

            hintsUsedThisPuzzle = 0
            val minRating = state.ratingRange.start.toInt()
            val maxRating = state.ratingRange.endInclusive.toInt()
            val theme = state.selectedTheme

            val puzzle = puzzleRepository.getRandomPuzzle(minRating, maxRating, theme)

            if (puzzle != null) {
                setupPuzzle(puzzle)
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "No puzzles found. Try adjusting the rating range or import more puzzles.",
                        puzzleStatus = PuzzleStatus.LOADING
                    )
                }
            }
        }
    }

    fun loadPuzzle(puzzleId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    screenMode = PuzzleScreenMode.PLAYING,
                    puzzleStatus = PuzzleStatus.LOADING,
                    isLoading = true,
                    error = null,
                    isSolved = false,
                    isFailed = false,
                    hintLevel = 0,
                    wrongMoveSquare = null,
                    ratingChange = 0
                )
            }
            hintsUsedThisPuzzle = 0
            val puzzle = puzzleRepository.getPuzzleById(puzzleId)

            if (puzzle != null) {
                setupPuzzle(puzzle)
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Puzzle not found",
                        puzzleStatus = PuzzleStatus.LOADING
                    )
                }
            }
        }
    }

    /**
     * Set up the board for a puzzle:
     * 1. Load FEN
     * 2. Determine player color (opposite of side to move in FEN)
     * 3. Play the opponent's blunder (first move in solution)
     */
    private suspend fun setupPuzzle(puzzle: PuzzleEntity) {
        engine.loadFen(puzzle.fen)
        val moves = puzzle.moves.split(" ")
        val playerColor = engine.boardState.value.turn.opposite()

        _uiState.update {
            it.copy(
                currentPuzzle = puzzle,
                isLoading = false,
                solutionMoves = moves,
                currentMoveIndex = 0,
                playerColor = playerColor,
                puzzleStatus = PuzzleStatus.OPPONENT_MOVING
            )
        }

        puzzleStartTime = System.currentTimeMillis()

        // Play the opponent's blunder move (always first in Lichess puzzles)
        delay(500)
        engine.applyUciMove(moves[0])
        _uiState.update {
            it.copy(
                currentMoveIndex = 1,
                puzzleStatus = PuzzleStatus.THINKING
            )
        }
    }

    // ── Navigation ──────────────────────────────────────────────────────────────

    fun clearPuzzle() {
        _uiState.update {
            it.copy(
                screenMode = PuzzleScreenMode.DASHBOARD,
                currentPuzzle = null,
                error = null
            )
        }
        refreshDashboard()
    }

    fun showImportScreen() {
        puzzleRepository.resetImportState()
        _uiState.update { it.copy(screenMode = PuzzleScreenMode.IMPORT) }
    }

    fun hideImportScreen() {
        _uiState.update { it.copy(screenMode = PuzzleScreenMode.DASHBOARD) }
        refreshDashboard()
    }

    // ── Move Handling ───────────────────────────────────────────────────────────

    fun onSquareClick(square: Square) {
        val state = _uiState.value
        if (state.puzzleStatus != PuzzleStatus.THINKING) return

        val lastMove = engine.selectSquareAndReturnMove(square)
        if (lastMove != null) {
            validateMove(lastMove.notation)
        }
    }

    fun onPromotionSelected(type: PieceType) {
        val lastMove = engine.applyPromotionAndReturnMove(type)
        if (lastMove != null) {
            validateMove(lastMove.notation)
        }
    }

    fun onPromotionCanceled() {
        engine.cancelPromotion()
    }

    /**
     * Validate the player's move against the solution sequence.
     *
     * Flow:
     * - Correct: Green flash → auto-play opponent reply → check if solved
     * - Wrong: Red flash → undo → allow retry
     */
    private fun validateMove(uci: String) {
        val state = _uiState.value
        if (state.currentMoveIndex >= state.solutionMoves.size) return

        val expectedMove = state.solutionMoves[state.currentMoveIndex]

        if (uci == expectedMove) {
            handleCorrectMove(state)
        } else {
            handleWrongMove(uci, state)
        }
    }

    private fun handleCorrectMove(state: PuzzleUiState) {
        val newIndex = state.currentMoveIndex + 1

        if (newIndex >= state.solutionMoves.size) {
            // Puzzle solved!
            _uiState.update {
                it.copy(
                    isSolved = true,
                    currentMoveIndex = newIndex,
                    puzzleStatus = PuzzleStatus.SOLVED,
                    hintLevel = 0
                )
            }
            completePuzzle(solved = true)
        } else {
            // Correct! Now play opponent's reply
            _uiState.update {
                it.copy(
                    currentMoveIndex = newIndex,
                    puzzleStatus = PuzzleStatus.CORRECT_MOVE,
                    hintLevel = 0,
                    wrongMoveSquare = null
                )
            }

            viewModelScope.launch {
                delay(400) // Brief green flash

                _uiState.update { it.copy(puzzleStatus = PuzzleStatus.OPPONENT_MOVING) }

                delay(300) // Opponent "thinks"
                engine.applyUciMove(state.solutionMoves[newIndex])
                val nextIndex = newIndex + 1

                _uiState.update { it.copy(currentMoveIndex = nextIndex) }

                if (nextIndex >= state.solutionMoves.size) {
                    // Puzzle solved after opponent's reply
                    _uiState.update {
                        it.copy(
                            isSolved = true,
                            puzzleStatus = PuzzleStatus.SOLVED
                        )
                    }
                    completePuzzle(solved = true)
                } else {
                    _uiState.update { it.copy(puzzleStatus = PuzzleStatus.THINKING) }
                }
            }
        }
    }

    private fun handleWrongMove(uci: String, state: PuzzleUiState) {
        // Parse the wrong move to get the destination square for red highlight
        val wrongSquare = if (uci.length >= 4) {
            Square.fromNotation(uci.substring(2, 4))
        } else null

        _uiState.update {
            it.copy(
                puzzleStatus = PuzzleStatus.WRONG_MOVE,
                wrongMoveSquare = wrongSquare
            )
        }

        engine.undo() // Undo the wrong move

        // Mark failed (but allow retry)
        viewModelScope.launch {
            val timeTaken = System.currentTimeMillis() - puzzleStartTime
            puzzleRepository.markPuzzleCompleted(
                state.currentPuzzle!!.puzzleId,
                false,
                timeTaken,
                hintsUsedThisPuzzle
            )

            // Calculate rating change for display
            val newRating = puzzleRepository.userRating.first()
            _uiState.update {
                it.copy(
                    ratingChange = newRating - state.userRating,
                    userRating = newRating,
                    isFailed = true
                )
            }
        }
    }

    private fun completePuzzle(solved: Boolean) {
        val state = _uiState.value
        viewModelScope.launch {
            val timeTaken = System.currentTimeMillis() - puzzleStartTime
            puzzleRepository.markPuzzleCompleted(
                state.currentPuzzle!!.puzzleId,
                solved,
                timeTaken,
                hintsUsedThisPuzzle
            )

            val newRating = puzzleRepository.userRating.first()
            val newStreak = puzzleRepository.currentStreak.first()
            _uiState.update {
                it.copy(
                    ratingChange = newRating - state.userRating,
                    userRating = newRating,
                    currentStreak = newStreak
                )
            }
        }
    }

    // ── Retry ───────────────────────────────────────────────────────────────────

    fun retry() {
        val state = _uiState.value
        if (state.currentPuzzle != null && (state.puzzleStatus == PuzzleStatus.WRONG_MOVE || state.isFailed)) {
            engine.loadFen(state.currentPuzzle.fen)
            _uiState.update {
                it.copy(
                    puzzleStatus = PuzzleStatus.OPPONENT_MOVING,
                    isFailed = false,
                    hintLevel = 0,
                    currentMoveIndex = 0,
                    wrongMoveSquare = null
                )
            }

            viewModelScope.launch {
                delay(300)
                engine.applyUciMove(state.solutionMoves[0])
                _uiState.update {
                    it.copy(
                        currentMoveIndex = 1,
                        puzzleStatus = PuzzleStatus.THINKING
                    )
                }
            }
        }
    }

    // ── Hints ───────────────────────────────────────────────────────────────────

    fun requestHint() {
        _uiState.update { state ->
            if (state.hintLevel < 3 && state.puzzleStatus == PuzzleStatus.THINKING) {
                hintsUsedThisPuzzle++
                state.copy(hintLevel = state.hintLevel + 1)
            } else state
        }
    }

    // ── Favorites ───────────────────────────────────────────────────────────────

    fun toggleFavorite() {
        val puzzleId = _uiState.value.currentPuzzle?.puzzleId ?: return
        viewModelScope.launch {
            puzzleRepository.toggleFavorite(puzzleId)
        }
    }

    // ── CSV Import ──────────────────────────────────────────────────────────────

    fun importCsv(uri: Uri, resolver: ContentResolver, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val count = puzzleRepository.importPuzzlesFromUri(uri, resolver)
            onResult(count)
            refreshDashboard()
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    /** The number of moves the player needs to make (every other move after the blunder). */
    fun playerMovesTotal(): Int {
        val total = _uiState.value.solutionMoves.size
        return (total - 1 + 1) / 2 // ceiling division of (total-1)/2
    }

    /** How many player moves have been completed. */
    fun playerMovesCompleted(): Int {
        val idx = _uiState.value.currentMoveIndex
        return (idx - 1 + 1) / 2 // moves 1, 3, 5... are player moves → map to 0, 1, 2...
    }
}
