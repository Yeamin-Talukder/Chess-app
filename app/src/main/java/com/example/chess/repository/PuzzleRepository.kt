package com.example.chess.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.chess.database.PuzzleCsvImporter
import com.example.chess.database.PuzzleDao
import com.example.chess.database.PuzzleEntity
import com.example.chess.database.PuzzleProgressEntity
import com.example.chess.database.PuzzleWithProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Puzzle statistics snapshot used by the UI.
 */
data class PuzzleStats(
    val totalPuzzles: Int = 0,
    val solved: Int = 0,
    val failed: Int = 0,
    val winRate: Float = 0f,
    val averageTimeMs: Long = 0,
    val bestTimeMs: Long = 0,
    val userRating: Int = 1500,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0
)

@Singleton
class PuzzleRepository @Inject constructor(
    private val puzzleDao: PuzzleDao,
    private val puzzleCsvImporter: PuzzleCsvImporter,
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context
) {
    companion object {
        val USER_PUZZLE_RATING = intPreferencesKey("user_puzzle_rating")
        val CURRENT_STREAK = intPreferencesKey("current_streak")
        val BEST_STREAK = intPreferencesKey("best_streak")
    }

    // ── Observable Flows ────────────────────────────────────────────────────────

    val userRating: Flow<Int> = dataStore.data.map { it[USER_PUZZLE_RATING] ?: 1500 }
    val currentStreak: Flow<Int> = dataStore.data.map { it[CURRENT_STREAK] ?: 0 }
    val bestStreak: Flow<Int> = dataStore.data.map { it[BEST_STREAK] ?: 0 }
    val solvedCount: Flow<Int> = puzzleDao.getSolvedPuzzleCount()
    val failedCount: Flow<Int> = puzzleDao.getFailedPuzzleCount()

    val importState = puzzleCsvImporter.importState

    fun getAllPuzzlesWithProgress(): Flow<List<PuzzleWithProgress>> {
        return puzzleDao.getAllPuzzlesWithProgress()
    }

    // ── Puzzle Retrieval ────────────────────────────────────────────────────────

    suspend fun getPuzzleById(puzzleId: String): PuzzleEntity? {
        return puzzleDao.getPuzzleById(puzzleId)
    }

    /**
     * Get a random puzzle matching the given rating range and optional theme.
     * Prefers unsolved puzzles but falls back to any puzzle in range.
     */
    suspend fun getRandomPuzzle(
        minRating: Int = 800,
        maxRating: Int = 3000,
        theme: String? = null
    ): PuzzleEntity? {
        // Try unsolved first
        val unsolved = if (theme != null) {
            puzzleDao.getRandomUnsolvedPuzzleByTheme(theme, minRating, maxRating)
        } else {
            puzzleDao.getRandomUnsolvedPuzzle(minRating, maxRating)
        }
        if (unsolved != null) return unsolved

        // Fall back to any puzzle in range (may be already solved)
        val any = if (theme != null) {
            puzzleDao.getRandomPuzzleByTheme(theme, minRating, maxRating)
        } else {
            puzzleDao.getRandomPuzzle(minRating, maxRating)
        }
        if (any != null) return any

        // Extreme fallback: pick ANY puzzle
        return puzzleDao.getRandomPuzzle(0, 5000)
    }

    /**
     * @deprecated Use [getRandomPuzzle] with explicit rating range.
     * Kept for backward compatibility with existing ViewModel.
     */
    suspend fun getPuzzle(rating: Int, theme: String? = null): PuzzleEntity? {
        return getRandomPuzzle(rating - 200, rating + 200, theme)
    }

    // ── Themes ──────────────────────────────────────────────────────────────────

    /**
     * Extract all distinct themes from the puzzle database.
     * Lichess stores themes as space-separated strings per puzzle,
     * so we need to split and de-duplicate.
     */
    suspend fun getDistinctThemes(): List<String> = withContext(Dispatchers.IO) {
        val rawStrings = puzzleDao.getRawThemeStrings()
        rawStrings.flatMap { it.split(" ") }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }

    // ── Recently Played ─────────────────────────────────────────────────────────

    suspend fun getRecentlyPlayed(limit: Int = 5): List<PuzzleWithProgress> {
        return puzzleDao.getRecentlyPlayed(limit)
    }

    // ── Puzzle Completion ───────────────────────────────────────────────────────

    /**
     * Mark a puzzle as completed with improved Elo-like rating adjustment.
     *
     * The adjustment is based on the difference between the user's rating and the
     * puzzle's rating — harder puzzles give more points, easier ones give fewer.
     */
    suspend fun markPuzzleCompleted(
        puzzleId: String,
        solved: Boolean,
        timeTakenMs: Long,
        hintsUsed: Int = 0
    ) {
        val puzzle = puzzleDao.getPuzzleById(puzzleId)
        val existing = puzzleDao.getPuzzleProgress(puzzleId)

        val progress = if (existing != null) {
            existing.copy(
                isSolved = existing.isSolved || solved,
                isFailed = existing.isFailed || !solved,
                timeTakenMs = timeTakenMs,
                bestTimeMs = if (solved && timeTakenMs > 0) {
                    if (existing.bestTimeMs > 0) min(existing.bestTimeMs, timeTakenMs)
                    else timeTakenMs
                } else existing.bestTimeMs,
                attempts = existing.attempts + 1,
                hintsUsed = existing.hintsUsed + hintsUsed,
                puzzleRating = puzzle?.rating ?: existing.puzzleRating,
                playedAt = System.currentTimeMillis()
            )
        } else {
            PuzzleProgressEntity(
                puzzleId = puzzleId,
                isSolved = solved,
                isFailed = !solved,
                timeTakenMs = timeTakenMs,
                bestTimeMs = if (solved) timeTakenMs else 0,
                attempts = 1,
                hintsUsed = hintsUsed,
                puzzleRating = puzzle?.rating ?: 0,
                playedAt = System.currentTimeMillis()
            )
        }
        puzzleDao.insertProgress(progress)

        // Update user rating & streaks
        dataStore.edit { preferences ->
            val currentRating = preferences[USER_PUZZLE_RATING] ?: 1500
            val streak = preferences[CURRENT_STREAK] ?: 0
            val best = preferences[BEST_STREAK] ?: 0

            val puzzleRating = puzzle?.rating ?: currentRating
            val ratingDiff = puzzleRating - currentRating

            if (solved) {
                // Harder puzzle = more reward (8 to 15 points)
                val gain = when {
                    ratingDiff > 200  -> 15
                    ratingDiff > 100  -> 13
                    ratingDiff > 0    -> 11
                    ratingDiff > -100 -> 9
                    else              -> 8
                }
                // Reduce gain if hints were used
                val adjustedGain = max(4, gain - (hintsUsed * 2))
                preferences[USER_PUZZLE_RATING] = currentRating + adjustedGain

                val newStreak = streak + 1
                preferences[CURRENT_STREAK] = newStreak
                if (newStreak > best) {
                    preferences[BEST_STREAK] = newStreak
                }
            } else {
                // Easier puzzle = more penalty
                val loss = when {
                    ratingDiff < -200 -> 12
                    ratingDiff < -100 -> 10
                    ratingDiff < 0    -> 8
                    ratingDiff < 100  -> 6
                    else              -> 5
                }
                preferences[USER_PUZZLE_RATING] = max(400, currentRating - loss)
                preferences[CURRENT_STREAK] = 0
            }
        }
    }

    // ── Favorites ───────────────────────────────────────────────────────────────

    suspend fun toggleFavorite(puzzleId: String) {
        val existing = puzzleDao.getPuzzleProgress(puzzleId)
        if (existing != null) {
            puzzleDao.insertProgress(existing.copy(isFavorite = !existing.isFavorite))
        } else {
            puzzleDao.insertProgress(PuzzleProgressEntity(puzzleId = puzzleId, isFavorite = true))
        }
    }

    fun getFavoritePuzzles(): Flow<List<PuzzleProgressEntity>> {
        return puzzleDao.getFavoritePuzzles()
    }

    // ── Stats ───────────────────────────────────────────────────────────────────

    suspend fun getPuzzleStats(): PuzzleStats {
        val totalPuzzles = puzzleDao.getPuzzleCount()
        val solved = puzzleDao.getSolvedCountSync()
        val failed = puzzleDao.getFailedCountSync()
        val avgTime = puzzleDao.getAverageSolveTime() ?: 0
        val bestTime = puzzleDao.getBestSolveTime() ?: 0

        val prefs = dataStore.data.map { preferences ->
            Triple(
                preferences[USER_PUZZLE_RATING] ?: 1500,
                preferences[CURRENT_STREAK] ?: 0,
                preferences[BEST_STREAK] ?: 0
            )
        }.first()
        val (rating, streak, best) = prefs

        val total = solved + failed
        val winRate = if (total > 0) solved.toFloat() / total else 0f

        return PuzzleStats(
            totalPuzzles = totalPuzzles,
            solved = solved,
            failed = failed,
            winRate = winRate,
            averageTimeMs = avgTime,
            bestTimeMs = bestTime,
            userRating = rating,
            currentStreak = streak,
            bestStreak = best
        )
    }

    suspend fun getPuzzleCount(): Int = puzzleDao.getPuzzleCount()

    fun getPuzzleCountFlow(): Flow<Int> = puzzleDao.getPuzzleCountFlow()

    suspend fun getPuzzleCountByRating(minRating: Int, maxRating: Int): Int =
        puzzleDao.getPuzzleCountByRating(minRating, maxRating)

    suspend fun getPuzzleCountByThemeAndRating(theme: String, minRating: Int, maxRating: Int): Int =
        puzzleDao.getPuzzleCountByThemeAndRating(theme, minRating, maxRating)

    // ── CSV Import ──────────────────────────────────────────────────────────────

    suspend fun importPuzzlesFromUri(uri: Uri, contentResolver: ContentResolver): Int {
        return puzzleCsvImporter.importFromUri(uri, contentResolver)
    }

    fun resetImportState() {
        puzzleCsvImporter.resetState()
    }

    // ── Seeding ─────────────────────────────────────────────────────────────────

    suspend fun seedSamplePuzzlesIfEmpty(samples: List<PuzzleEntity>) {
        val count = puzzleDao.getPuzzleCount()
        if (count < 10000) {
            // First try loading from bundled assets CSV
            try {
                val importCount = puzzleCsvImporter.importFromAssets(context, "puzzles.csv")
                if (importCount > 0) return
            } catch (_: Exception) { /* ignore */ }

            // Fall back to hardcoded samples if database is fully empty
            if (count == 0) {
                puzzleDao.insertPuzzles(samples)
            }
        }
    }

    // ── Reset ───────────────────────────────────────────────────────────────────

    suspend fun resetAllProgress() {
        puzzleDao.deleteAllProgress()
        dataStore.edit { preferences ->
            preferences[USER_PUZZLE_RATING] = 1500
            preferences[CURRENT_STREAK] = 0
            preferences[BEST_STREAK] = 0
        }
    }
}
