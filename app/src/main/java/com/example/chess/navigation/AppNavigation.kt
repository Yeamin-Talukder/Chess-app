package com.example.chess.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.platform.LocalContext
import com.example.chess.ui.screens.about.AboutScreen
import com.example.chess.ui.screens.game.OfflineGameScreen
import com.example.chess.ui.screens.history.HistoryScreen
import com.example.chess.ui.screens.home.HomeScreen
import com.example.chess.ui.screens.profile.ProfileScreen
import com.example.chess.ui.screens.settings.SettingsScreen
import com.example.chess.ui.screens.splash.SplashScreen
import com.example.chess.ui.screens.multiplayer.WifiLobbyScreen
import com.example.chess.ui.screens.multiplayer.WifiGameScreen
import com.example.chess.ui.screens.bot.BotSelectionScreen
import com.example.chess.ui.screens.bot.BotGameScreen
import com.example.chess.game.engine.BotLevel
import com.example.chess.game.board.PieceColor

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = { 
            slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) + fadeIn(tween(400)) 
        },
        exitTransition = { 
            slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(400)) + fadeOut(tween(400)) 
        },
        popEnterTransition = { 
            androidx.compose.animation.scaleIn(initialScale = 0.95f, animationSpec = tween(400)) + fadeIn(tween(400)) 
        },
        popExitTransition = { 
            slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) + fadeOut(tween(400)) 
        }
    ) {
        composable(
            route = Screen.Splash.route,
            exitTransition = { fadeOut(animationSpec = tween(600)) }
        ) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(
            route = Screen.Home.route,
            enterTransition = {
                if (initialState.destination.route == Screen.Splash.route) {
                    fadeIn(animationSpec = tween(600))
                } else {
                    slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) + fadeIn(tween(400))
                }
            }
        ) {
            HomeScreen(
                onNavigateToOfflineGame = { navController.navigate(Screen.OfflineGame.route) },
                onNavigateToWifiLobby = { navController.navigate(Screen.WifiLobby.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) },
                onNavigateToPuzzle = { navController.navigate(Screen.Puzzle.route) },
                onNavigateToBot = { navController.navigate(Screen.BotSelection.route) }
            )
        }
        
        composable(route = Screen.OfflineGame.route) {
            OfflineGameScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(route = Screen.WifiLobby.route) {
            WifiLobbyScreen(
                viewModel = androidx.hilt.navigation.compose.hiltViewModel<com.example.chess.ui.screens.multiplayer.WifiGameViewModel>(),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGame = { 
                    navController.navigate(Screen.WifiGame.route) {
                        popUpTo(Screen.WifiLobby.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(route = Screen.WifiGame.route) {
            WifiGameScreen(
                onNavigateBack = { 
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0)
                    }
                }
            )
        }
        
        // Removed WifiDirect and Bluetooth Composables
        
        composable(route = Screen.Profile.route) {
            ProfileScreen(onNavigateBack = { navController.popBackStack() })
        }
        
        composable(route = Screen.History.route) {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToReplay = { gameId ->
                    navController.navigate(Screen.Replay.createRoute(gameId))
                }
            )
        }

        composable(route = Screen.Settings.route) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
        
        composable(route = Screen.About.route) {
            AboutScreen(onNavigateBack = { navController.popBackStack() })
        }
        
        composable(route = Screen.Puzzle.route) {
            com.example.chess.ui.screens.puzzle.PuzzleScreen(onNavigateBack = { navController.popBackStack() })
        }
        
        composable(
            route = Screen.Replay.route,
            arguments = listOf(androidx.navigation.navArgument("gameId") { type = androidx.navigation.NavType.LongType })
        ) {
            com.example.chess.ui.screens.replay.ReplayScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAnalysis = { gameId -> navController.navigate(Screen.Analysis.createRoute(gameId)) }
            )
        }
        
        composable(route = Screen.BotSelection.route) {
            BotSelectionScreen(
                onNavigateBack = { navController.popBackStack() },
                onStartGame = { color, level ->
                    navController.navigate(Screen.BotGame.createRoute(color.name, level.name)) {
                        popUpTo(Screen.BotSelection.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(
            route = Screen.BotGame.route,
            arguments = listOf(
                androidx.navigation.navArgument("color") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("level") { type = androidx.navigation.NavType.StringType }
            )
        ) { backStackEntry ->
            val colorStr = backStackEntry.arguments?.getString("color") ?: com.example.chess.ui.screens.game.PlayerColorChoice.WHITE.name
            val levelStr = backStackEntry.arguments?.getString("level") ?: BotLevel.CASUAL.name
            
            BotGameScreen(
                playerColorChoice = com.example.chess.ui.screens.game.PlayerColorChoice.valueOf(colorStr),
                botLevel = BotLevel.valueOf(levelStr),
                onNavigateBack = { navController.navigate(Screen.Home.route) { popUpTo(0) } }
            )
        }
        
        composable(
            route = Screen.Analysis.route,
            arguments = listOf(androidx.navigation.navArgument("gameId") { type = androidx.navigation.NavType.LongType })
        ) {
            com.example.chess.ui.screens.analysis.AnalysisScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
