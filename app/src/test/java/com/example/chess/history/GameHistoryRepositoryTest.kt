package com.example.chess.history

import com.example.chess.database.GameHistoryDao
import com.example.chess.database.GameHistoryEntity
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class GameHistoryRepositoryTest {

    private lateinit var gameHistoryDao: GameHistoryDao
    private lateinit var repository: GameHistoryRepository

    @Before
    fun setup() {
        gameHistoryDao = mockk()
        repository = GameHistoryRepository(gameHistoryDao)
    }

    @Test
    fun `insertGame calls dao insert`() = runTest {
        val game = GameHistoryEntity(
            id = 1,
            date = 1000L,
            winner = "White",
            duration = 5000L,
            moveCount = 10,
            pgn = "1. e4",
            opening = "Custom",
            result = "1-0",
            isMultiplayer = false
        )
        
        coEvery { gameHistoryDao.insertGame(game) } returns Unit

        repository.insertGame(game)

        coVerify(exactly = 1) { gameHistoryDao.insertGame(game) }
    }

    @Test
    fun `deleteGame calls dao delete`() = runTest {
        val game = GameHistoryEntity(id = 1, date = 0L, winner = "", duration = 0L, moveCount = 0, pgn = "", opening = "", result = "", isMultiplayer = false)
        
        coEvery { gameHistoryDao.deleteGame(game) } returns Unit

        repository.deleteGame(game)

        coVerify(exactly = 1) { gameHistoryDao.deleteGame(game) }
    }

    @Test
    fun `getAllGames returns flow from dao`() = runTest {
        val gameList = listOf(
            GameHistoryEntity(id = 1, date = 0L, winner = "", duration = 0L, moveCount = 0, pgn = "", opening = "", result = "", isMultiplayer = false)
        )
        
        every { gameHistoryDao.getAllGames() } returns flowOf(gameList)

        val result = repository.getAllGames().first()
        assertThat(result).isEqualTo(gameList)
    }
}
