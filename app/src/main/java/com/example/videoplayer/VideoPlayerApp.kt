package com.example.videoplayer

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.videoplayer.data.repository.VideoRepository
import com.example.videoplayer.presentation.HomeScreen
import com.example.videoplayer.presentation.LocalMediaScreen
import com.example.videoplayer.presentation.PlayerScreen
import com.example.videoplayer.presentation.SplashScreen
import com.example.videoplayer.viewmodel.VideoPlayerViewModel

@Composable
fun VideoPlayerApp() {
    val context = LocalContext.current
    val viewModel: VideoPlayerViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return VideoPlayerViewModel(VideoRepository(context.applicationContext)) as T
            }
        }
    )
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(navController = navController)
        }
        composable("home") {
            HomeScreen(navController = navController)
        }
        composable("localMedia") {
            LocalMediaScreen(navController = navController, viewModel = viewModel)
        }
        composable(
            route = "player/{videoId}",
            arguments = listOf(navArgument("videoId") { type = NavType.LongType })
        ) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getLong("videoId") ?: 0L
            PlayerScreen(
                videoId = videoId,
                viewmodel = viewModel,
                navController = navController
            )
        }
    }
}
