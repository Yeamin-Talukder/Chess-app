package com.example.chess.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Settings : Screen("settings")
    object OfflineGame : Screen("offline_game")
    object WifiLobby : Screen("wifi_lobby")
    object WifiGame : Screen("wifi_game")
    object Profile : Screen("profile")
    object History : Screen("history")
    object About : Screen("about")
    object Puzzle : Screen("puzzle")
    object Replay : Screen("replay/{gameId}") {
        fun createRoute(gameId: Long) = "replay/$gameId"
    }
    object BotSelection : Screen("bot_selection")
    object BotGame : Screen("bot_game/{color}/{level}") {
        fun createRoute(color: String, level: String) = "bot_game/$color/$level"
    }
    object Analysis : Screen("analysis/{gameId}") {
        fun createRoute(gameId: Long) = "analysis/$gameId"
    }
}
