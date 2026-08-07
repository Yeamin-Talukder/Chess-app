package com.example.chess.utils

import com.example.chess.game.board.Move
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PgnGenerator {

    fun generatePgn(
        moves: List<Move>,
        whitePlayer: String = "White",
        blackPlayer: String = "Black",
        result: String = "*",
        date: Long = System.currentTimeMillis()
    ): String {
        val df = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        val dateString = df.format(Date(date))

        val header = """
            [Event "Local Game"]
            [Site "Chess App"]
            [Date "$dateString"]
            [Round "1"]
            [White "$whitePlayer"]
            [Black "$blackPlayer"]
            [Result "$result"]
        """.trimIndent()

        val movesBuilder = StringBuilder()
        var fullMove = 1

        for (i in moves.indices) {
            if (i % 2 == 0) {
                movesBuilder.append("$fullMove. ")
                fullMove++
            }
            movesBuilder.append("${moves[i].notation} ")
        }

        movesBuilder.append(result)

        return "$header\n\n${movesBuilder.toString().trim()}"
    }
}
