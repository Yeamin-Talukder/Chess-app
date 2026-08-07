package com.example.chess.game.engine

import android.content.Context
import android.util.Log
import com.example.chess.game.board.BoardState
import com.example.chess.game.board.Move
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

import com.example.chess.game.analysis.EngineAnalysis

@Singleton
class UciEngineManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var process: Process? = null
    private var reader: BufferedReader? = null
    private var writer: OutputStreamWriter? = null
    
    private var isEngineReady = false
    private var hasBinary = false
    private val mutex = Mutex()

    /**
     * Initializes the engine by copying the binary from assets if needed and starting the process.
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            val engineFile = File(context.filesDir, "stockfish")
            
            // Check if binary exists in assets and copy it
            if (!engineFile.exists()) {
                val assetManager = context.assets
                val files = assetManager.list("")
                if (files?.contains("stockfish") == true) {
                    context.assets.open("stockfish").use { input ->
                        engineFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    engineFile.setExecutable(true)
                }
            }

            if (engineFile.exists() && engineFile.canExecute()) {
                hasBinary = true
                startProcess(engineFile.absolutePath)
            } else {
                hasBinary = false
                Log.w("UciEngineManager", "Stockfish binary not found. Using fallback Kotlin bot.")
            }
        } catch (e: Exception) {
            Log.e("UciEngineManager", "Error initializing engine: ${e.message}")
            hasBinary = false
        }
    }

    private fun startProcess(path: String) {
        val pb = ProcessBuilder(path)
        process = pb.start()
        reader = BufferedReader(InputStreamReader(process!!.inputStream))
        writer = OutputStreamWriter(process!!.outputStream)
        
        sendCommand("uci")
        
        // Wait for uciok
        while (true) {
            val line = reader?.readLine() ?: break
            if (line == "uciok") {
                isEngineReady = true
                break
            }
        }
    }

    fun sendCommand(cmd: String) {
        if (!hasBinary) return
        try {
            writer?.write("$cmd\n")
            writer?.flush()
        } catch (e: Exception) {
            Log.e("UciEngineManager", "Error sending command: $cmd", e)
        }
    }

    /**
     * Asks the engine to find the best move for the given FEN and BotLevel.
     */
    suspend fun getBestMove(fen: String, level: BotLevel, currentState: BoardState): String? = withContext(Dispatchers.IO) {
        if (!hasBinary) {
            // Minimax fallback AI with Alpha-Beta Pruning & Piece-Square Evaluation
            val bestMove = MinimaxEngine.findBestMove(currentState, level)
            return@withContext bestMove?.notation
        }

        mutex.withLock {
            try {
                // Ensure engine is stopped and flush unread output
                sendCommand(UciCommandBuilder.stop())
                sendCommand(UciCommandBuilder.isReady())
                while (true) {
                    val line = reader?.readLine() ?: break
                    if (line == "readyok") break
                }

                // Configure skill level
                sendCommand(UciCommandBuilder.setOption("Skill Level", level.skillLevel.toString()))
                
                // Set position
                sendCommand(UciCommandBuilder.position(fen))
                
                // Search
                sendCommand(UciCommandBuilder.goTime(level.moveTimeMs.toLong()))
                
                // Read output until bestmove
                while (true) {
                    val line = reader?.readLine() ?: break
                    if (line.startsWith("bestmove")) {
                        val parts = line.split(" ")
                        if (parts.size >= 2) {
                            return@withLock parts[1] // e.g. "e2e4"
                        }
                    }
                }
                return@withLock null
            } catch (e: Exception) {
                Log.e("UciEngineManager", "Error getting best move", e)
                null
            }
        }
    }

    fun getEvaluationFlow(fen: String, fallbackState: BoardState, searchDepth: Int = 20): Flow<EngineAnalysis> = flow {
        if (!hasBinary) {
            emitAll(MinimaxEngine.evaluatePositionProgressive(fallbackState))
            return@flow
        }

        mutex.withLock {
            try {
                sendCommand(UciCommandBuilder.stop())
                sendCommand(UciCommandBuilder.isReady())
                while (true) {
                    val line = reader?.readLine() ?: break
                    if (line == "readyok") break
                }

                sendCommand(UciCommandBuilder.position(fen))
                sendCommand(UciCommandBuilder.go(searchDepth))

                var lastEmittedDepth = 0
                val isBlackTurn = fen.split(" ").getOrNull(1) == "b"
                var lastAnalysis: EngineAnalysis? = null

                while (true) {
                    val line = reader?.readLine() ?: break
                    
                    if (line.startsWith("info") && line.contains("depth")) {
                        val analysis = EngineParser.parseAnalysis(line, isBlackTurn = isBlackTurn)
                        if (analysis != null) {
                            // Retain best move from PV if available, and don't overwrite with nulls unless we have a better one
                            lastAnalysis = analysis.copy(
                                bestMove = analysis.bestMove.ifEmpty { lastAnalysis?.bestMove ?: "" }
                            )
                            if (analysis.depth > lastEmittedDepth) {
                                lastEmittedDepth = analysis.depth
                                emit(lastAnalysis)
                            }
                        }
                    }

                    if (line.startsWith("bestmove")) {
                        val bestMove = EngineParser.parseAnalysis(line, bestMoveLine = line, isBlackTurn = isBlackTurn)?.bestMove
                            ?: line.split(" ").getOrNull(1) ?: ""
                            
                        if (lastAnalysis != null) {
                            emit(lastAnalysis.copy(bestMove = bestMove))
                        } else {
                            // If we didn't get any info lines, just emit a basic one
                            emit(EngineAnalysis(0f, null, bestMove, emptyList(), searchDepth, 0L, 0L))
                        }
                        break
                    }
                    yield() // Check for cancellation
                }
            } catch (e: Exception) {
                Log.e("UciEngineManager", "Error getting evaluation flow", e)
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getEvaluation(fen: String, fallbackState: BoardState): Float = withContext(Dispatchers.IO) {
        if (!hasBinary) {
            return@withContext MinimaxEngine.evaluatePosition(fallbackState)
        }
        
        mutex.withLock {
            try {
                sendCommand(UciCommandBuilder.stop())
                sendCommand(UciCommandBuilder.isReady())
                while (true) {
                    val line = reader?.readLine() ?: break
                    if (line == "readyok") break
                }

                sendCommand(UciCommandBuilder.position(fen))
                sendCommand(UciCommandBuilder.go(20))
                var finalScore = 0f
                while (true) {
                    val line = reader?.readLine() ?: break
                    if (line.startsWith("info") && line.contains("score cp")) {
                        val match = Regex("score cp (-?\\d+)").find(line)
                        if (match != null) {
                            finalScore = match.groupValues[1].toFloat() / 100f
                        }
                    } else if (line.startsWith("info") && line.contains("score mate")) {
                        val match = Regex("score mate (-?\\d+)").find(line)
                        if (match != null) {
                            val mateIn = match.groupValues[1].toInt()
                            finalScore = if (mateIn > 0) 15f else -15f
                        }
                    }
                    if (line.startsWith("bestmove")) {
                        val isBlackTurn = fen.split(" ").getOrNull(1) == "b"
                        if (isBlackTurn) finalScore = -finalScore
                        return@withLock finalScore
                    }
                }
                val isBlackTurn = fen.split(" ").getOrNull(1) == "b"
                if (isBlackTurn) finalScore = -finalScore
                return@withLock finalScore
            } catch (e: Exception) {
                Log.e("UciEngineManager", "Error getting evaluation", e)
                0f
            }
        }
    }

    fun shutdown() {
        if (hasBinary) {
            sendCommand("quit")
            process?.destroy()
        }
        process = null
        reader = null
        writer = null
        isEngineReady = false
    }
}
