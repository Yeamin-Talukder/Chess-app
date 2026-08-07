package com.example.chess.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.example.chess.network.wifi.WifiController

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplicationContext(
        @ApplicationContext context: Context
    ): Context = context

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> {
        return androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(
            produceFile = { java.io.File(context.filesDir, "datastore/settings.preferences_pb") }
        )
    }

    @Provides
    @Singleton
    fun provideChessDatabase(@ApplicationContext context: Context): com.example.chess.database.ChessDatabase {
        return androidx.room.Room.databaseBuilder(
            context,
            com.example.chess.database.ChessDatabase::class.java,
            "chess_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideGameHistoryDao(database: com.example.chess.database.ChessDatabase): com.example.chess.database.GameHistoryDao {
        return database.gameHistoryDao()
    }

    @Provides
    @Singleton
    fun provideUserProfileDao(database: com.example.chess.database.ChessDatabase): com.example.chess.database.UserProfileDao {
        return database.userProfileDao()
    }

    @Provides
    @Singleton
    fun providePuzzleDao(database: com.example.chess.database.ChessDatabase): com.example.chess.database.PuzzleDao {
        return database.puzzleDao()
    }

    @Provides
    @Singleton
    fun provideWifiController(@ApplicationContext context: Context): WifiController {
        return WifiController(context)
    }
}
