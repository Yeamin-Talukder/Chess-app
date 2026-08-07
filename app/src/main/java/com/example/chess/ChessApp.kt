package com.example.chess

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ChessApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialization code if needed
    }
}
