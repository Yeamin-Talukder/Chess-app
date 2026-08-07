package com.example.chess.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameHistoryDaoTest {

    private lateinit var database: ChessDatabase
    private lateinit var dao: GameHistoryDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ChessDatabase::class.java
        ).allowMainThreadQueries().build()
        
        dao = database.gameHistoryDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveGame() = runTest {
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

        dao.insertGame(game)

        val games = dao.getAllGames().first()
        assertThat(games).contains(game)
    }

    @Test
    fun deleteGame() = runTest {
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

        dao.insertGame(game)
        dao.deleteGame(game)

        val games = dao.getAllGames().first()
        assertThat(games).isEmpty()
    }
}
