package com.example.chess.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PuzzleDao {

    // ── Insert ──────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPuzzles(puzzles: List<PuzzleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: PuzzleProgressEntity)

    // ── Counts ──────────────────────────────────────────────────────────────────

    @Query("SELECT COUNT(*) FROM puzzles")
    suspend fun getPuzzleCount(): Int

    @Query("SELECT COUNT(*) FROM puzzles")
    fun getPuzzleCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM puzzles WHERE rating BETWEEN :minRating AND :maxRating")
    suspend fun getPuzzleCountByRating(minRating: Int, maxRating: Int): Int

    @Query("""
        SELECT COUNT(*) FROM puzzles 
        WHERE themes LIKE '%' || :theme || '%' 
        AND rating BETWEEN :minRating AND :maxRating
    """)
    suspend fun getPuzzleCountByThemeAndRating(theme: String, minRating: Int, maxRating: Int): Int

    // ── Random Puzzle Selection ─────────────────────────────────────────────────

    @Query("""
        SELECT * FROM puzzles 
        WHERE rating BETWEEN :minRating AND :maxRating
        AND puzzleId NOT IN (SELECT puzzleId FROM puzzle_progress WHERE isSolved = 1)
        ORDER BY RANDOM() LIMIT 1
    """)
    suspend fun getRandomUnsolvedPuzzle(minRating: Int, maxRating: Int): PuzzleEntity?

    @Query("""
        SELECT * FROM puzzles 
        WHERE rating BETWEEN :minRating AND :maxRating
        ORDER BY RANDOM() LIMIT 1
    """)
    suspend fun getRandomPuzzle(minRating: Int, maxRating: Int): PuzzleEntity?

    @Query("""
        SELECT * FROM puzzles 
        WHERE themes LIKE '%' || :theme || '%' 
        AND rating BETWEEN :minRating AND :maxRating
        AND puzzleId NOT IN (SELECT puzzleId FROM puzzle_progress WHERE isSolved = 1)
        ORDER BY RANDOM() LIMIT 1
    """)
    suspend fun getRandomUnsolvedPuzzleByTheme(theme: String, minRating: Int, maxRating: Int): PuzzleEntity?

    @Query("""
        SELECT * FROM puzzles 
        WHERE themes LIKE '%' || :theme || '%' 
        AND rating BETWEEN :minRating AND :maxRating
        ORDER BY RANDOM() LIMIT 1
    """)
    suspend fun getRandomPuzzleByTheme(theme: String, minRating: Int, maxRating: Int): PuzzleEntity?

    // ── Single Puzzle / Progress ────────────────────────────────────────────────

    @Query("SELECT * FROM puzzles WHERE puzzleId = :puzzleId")
    suspend fun getPuzzleById(puzzleId: String): PuzzleEntity?

    @Query("SELECT * FROM puzzle_progress WHERE puzzleId = :puzzleId")
    suspend fun getPuzzleProgress(puzzleId: String): PuzzleProgressEntity?

    // ── Offset-based (legacy/fallback) ──────────────────────────────────────────

    @Query("SELECT * FROM puzzles WHERE rating BETWEEN :minRating AND :maxRating LIMIT 1 OFFSET :offset")
    suspend fun getPuzzleAtOffset(minRating: Int, maxRating: Int, offset: Int): PuzzleEntity?

    @Query("""
        SELECT * FROM puzzles 
        WHERE themes LIKE '%' || :theme || '%' 
        AND rating BETWEEN :minRating AND :maxRating 
        LIMIT 1 OFFSET :offset
    """)
    suspend fun getPuzzleAtOffsetWithTheme(theme: String, minRating: Int, maxRating: Int, offset: Int): PuzzleEntity?

    // ── Themes ──────────────────────────────────────────────────────────────────

    @Query("SELECT DISTINCT themes FROM puzzles LIMIT 500")
    suspend fun getRawThemeStrings(): List<String>

    // ── Statistics ───────────────────────────────────────────────────────────────

    @Query("SELECT COUNT(*) FROM puzzle_progress WHERE isSolved = 1")
    fun getSolvedPuzzleCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM puzzle_progress WHERE isFailed = 1")
    fun getFailedPuzzleCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM puzzle_progress WHERE isSolved = 1")
    suspend fun getSolvedCountSync(): Int

    @Query("SELECT COUNT(*) FROM puzzle_progress WHERE isFailed = 1")
    suspend fun getFailedCountSync(): Int

    @Query("SELECT AVG(timeTakenMs) FROM puzzle_progress WHERE isSolved = 1 AND timeTakenMs > 0")
    suspend fun getAverageSolveTime(): Long?

    @Query("SELECT MIN(bestTimeMs) FROM puzzle_progress WHERE isSolved = 1 AND bestTimeMs > 0")
    suspend fun getBestSolveTime(): Long?

    // ── Favorites ───────────────────────────────────────────────────────────────

    @Query("SELECT * FROM puzzle_progress WHERE isFavorite = 1")
    fun getFavoritePuzzles(): Flow<List<PuzzleProgressEntity>>

    // ── All Puzzles with Progress ───────────────────────────────────────────────

    @Query("""
        SELECT p.puzzleId, p.fen, p.moves, p.rating, p.ratingDeviation, p.popularity, p.nbPlays, 
               p.themes, p.gameUrl, p.openingTags,
               pp.isSolved, pp.isFailed, pp.isFavorite, pp.timeTakenMs, pp.bestTimeMs,
               pp.attempts, pp.hintsUsed, pp.puzzleRating, pp.playedAt 
        FROM puzzles p 
        LEFT JOIN puzzle_progress pp ON p.puzzleId = pp.puzzleId
        ORDER BY p.rating ASC
    """)
    fun getAllPuzzlesWithProgress(): Flow<List<PuzzleWithProgress>>

    @Query("""
        SELECT p.puzzleId, p.fen, p.moves, p.rating, p.ratingDeviation, p.popularity, p.nbPlays, 
               p.themes, p.gameUrl, p.openingTags,
               pp.isSolved, pp.isFailed, pp.isFavorite, pp.timeTakenMs, pp.bestTimeMs,
               pp.attempts, pp.hintsUsed, pp.puzzleRating, pp.playedAt 
        FROM puzzles p 
        LEFT JOIN puzzle_progress pp ON p.puzzleId = pp.puzzleId
        WHERE pp.isSolved = 1 OR pp.isFailed = 1
        ORDER BY pp.playedAt DESC
        LIMIT :limit
    """)
    suspend fun getRecentlyPlayed(limit: Int = 10): List<PuzzleWithProgress>

    // ── Cleanup ─────────────────────────────────────────────────────────────────

    @Query("DELETE FROM puzzles")
    suspend fun deleteAllPuzzles()

    @Query("DELETE FROM puzzle_progress")
    suspend fun deleteAllProgress()
}
