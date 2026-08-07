package com.example.chess.database

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * Progress state emitted during CSV import.
 */
data class ImportState(
    val phase: String = "Idle",
    val importedCount: Int = 0,
    val errorCount: Int = 0,
    val isRunning: Boolean = false,
    val isComplete: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Robust CSV importer for the Lichess puzzle database.
 *
 * Expected CSV format (headerless or with header row):
 * PuzzleId,FEN,Moves,Rating,RatingDeviation,Popularity,NbPlays,Themes,GameUrl,OpeningTags
 *
 * Handles:
 * - CSVs with or without a header row
 * - Malformed rows (skipped and counted as errors)
 * - Batch inserts in chunks of [BATCH_SIZE] for memory efficiency
 * - Coroutine cancellation support
 * - Progress emission via [importState]
 */
@Singleton
class PuzzleCsvImporter @Inject constructor(
    private val puzzleDao: PuzzleDao
) {
    companion object {
        private const val BATCH_SIZE = 5000
    }

    private val _importState = MutableStateFlow(ImportState())
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    /**
     * Import puzzles from a user-selected file URI.
     * @return total number of puzzles successfully imported.
     */
    suspend fun importFromUri(uri: Uri, contentResolver: ContentResolver): Int {
        val inputStream = withContext(Dispatchers.IO) {
            contentResolver.openInputStream(uri)
        } ?: run {
            _importState.value = ImportState(
                phase = "Error",
                errorMessage = "Could not open file",
                isComplete = true
            )
            return 0
        }
        return importFromStream(inputStream)
    }

    /**
     * Import puzzles from an asset file.
     * @return total number of puzzles successfully imported.
     */
    suspend fun importFromAssets(context: Context, filename: String): Int {
        val inputStream = try {
            context.assets.open(filename)
        } catch (e: Exception) {
            return 0
        }
        return importFromStream(inputStream)
    }

    /**
     * Core import logic — streams CSV line by line to avoid OOM on large files.
     */
    private suspend fun importFromStream(inputStream: InputStream): Int = withContext(Dispatchers.IO) {
        if (_importState.value.isRunning) return@withContext 0

        _importState.value = ImportState(phase = "Reading…", isRunning = true)

        var imported = 0
        var errors = 0
        val batch = mutableListOf<PuzzleEntity>()

        try {
            val reader = BufferedReader(InputStreamReader(inputStream), 8192)

            reader.useLines { lines ->
                val iterator = lines.iterator()
                if (!iterator.hasNext()) return@useLines

                // Peek at first line — skip if it's a header
                val firstLine = iterator.next()
                if (!firstLine.startsWith("PuzzleId", ignoreCase = true)) {
                    // Not a header — parse it as data
                    parseLine(firstLine)?.let { batch.add(it) } ?: errors++
                }

                while (iterator.hasNext()) {
                    if (!coroutineContext.isActive) {
                        // Cancellation requested
                        if (batch.isNotEmpty()) {
                            puzzleDao.insertPuzzles(batch)
                            imported += batch.size
                            batch.clear()
                        }
                        _importState.value = ImportState(
                            phase = "Cancelled",
                            importedCount = imported,
                            errorCount = errors,
                            isComplete = true
                        )
                        return@withContext imported
                    }

                    val line = iterator.next()
                    val entity = parseLine(line)
                    if (entity != null) {
                        batch.add(entity)
                    } else {
                        errors++
                    }

                    if (batch.size >= BATCH_SIZE) {
                        puzzleDao.insertPuzzles(batch)
                        imported += batch.size
                        batch.clear()
                        _importState.value = ImportState(
                            phase = "Importing…",
                            importedCount = imported,
                            errorCount = errors,
                            isRunning = true
                        )
                    }
                }
            }

            // Flush remaining batch
            if (batch.isNotEmpty()) {
                puzzleDao.insertPuzzles(batch)
                imported += batch.size
            }

            _importState.value = ImportState(
                phase = "Complete",
                importedCount = imported,
                errorCount = errors,
                isRunning = false,
                isComplete = true
            )
        } catch (e: Exception) {
            // Flush whatever we have on error
            if (batch.isNotEmpty()) {
                try {
                    puzzleDao.insertPuzzles(batch)
                    imported += batch.size
                } catch (_: Exception) { /* ignore flush failure */ }
            }
            _importState.value = ImportState(
                phase = "Error",
                importedCount = imported,
                errorCount = errors,
                isRunning = false,
                isComplete = true,
                errorMessage = e.message ?: "Unknown import error"
            )
        }

        imported
    }

    /**
     * Parse a single CSV line into a [PuzzleEntity].
     * Returns null for malformed lines instead of crashing.
     *
     * Lichess CSV columns:
     * 0: PuzzleId, 1: FEN, 2: Moves, 3: Rating, 4: RatingDeviation,
     * 5: Popularity, 6: NbPlays, 7: Themes, 8: GameUrl, 9: OpeningTags
     */
    private fun parseLine(line: String): PuzzleEntity? {
        if (line.isBlank()) return null
        val parts = line.split(",")
        if (parts.size < 8) return null

        val puzzleId = parts[0].trim()
        val fen = parts[1].trim()
        val moves = parts[2].trim()
        val rating = parts[3].trim().toIntOrNull() ?: return null

        if (puzzleId.isEmpty() || fen.isEmpty() || moves.isEmpty()) return null

        return PuzzleEntity(
            puzzleId = puzzleId,
            fen = fen,
            moves = moves,
            rating = rating,
            ratingDeviation = parts[4].trim().toIntOrNull() ?: 75,
            popularity = parts[5].trim().toIntOrNull() ?: 0,
            nbPlays = parts[6].trim().toIntOrNull() ?: 0,
            themes = parts.getOrNull(7)?.trim() ?: "",
            gameUrl = parts.getOrNull(8)?.trim() ?: "",
            openingTags = parts.getOrNull(9)?.trim() ?: ""
        )
    }

    /**
     * Reset import state so the UI can show fresh progress on next import.
     */
    fun resetState() {
        _importState.value = ImportState()
    }
}
