
package com.example.videoplayer

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import com.example.videoplayer.presentation.GalleryScreen
import com.example.videoplayer.presentation.HomeScreen
import com.example.videoplayer.presentation.LocalMediaScreen
import com.example.videoplayer.presentation.PlayerScreen
import com.example.videoplayer.presentation.SplashScreen
import com.example.videoplayer.viewmodel.VideoPlayerViewModel

@Composable
fun VideoPlayerApp() {

    val context = LocalContext.current

    val videoPlayerViewModel: VideoPlayerViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return VideoPlayerViewModel(
                    VideoRepository(context.applicationContext)
                ) as T
            }
        }
    )

    val navController = rememberNavController()

    NavHost(
        navController    = navController,
        startDestination = "splash",
        enterTransition  = {
            fadeIn(tween(400)) + slideInHorizontally(
                tween(400, easing = EaseOutCubic)
            ) { it / 4 }
        },
        exitTransition   = {
            fadeOut(tween(300)) + slideOutHorizontally(
                tween(300, easing = EaseInCubic)
            ) { -it / 4 }
        },
        popEnterTransition = {
            fadeIn(tween(400)) + slideInHorizontally(
                tween(400, easing = EaseOutCubic)
            ) { -it / 4 }
        },
        popExitTransition  = {
            fadeOut(tween(300)) + slideOutHorizontally(
                tween(300, easing = EaseInCubic)
            ) { it / 4 }
        }
    ) {

        composable(route = "splash") {
            SplashScreen(navController = navController)
        }

        composable(route = "home") {
            HomeScreen(navController = navController)
        }

        composable(route = "localMedia") {
            LocalMediaScreen(
                navController = navController,
                viewModel     = videoPlayerViewModel
            )
        }

        // Luxury Video Gallery: Albums & Grid View
        composable(route = "gallery") {
            GalleryScreen(
                navController = navController,
                viewModel     = videoPlayerViewModel
            )
        }

        composable(
            route = "gallery/{folderName}",
            arguments = listOf(navArgument("folderName") { type = NavType.StringType })
        ) { backStackEntry ->
            val modeOrFolder = backStackEntry.arguments?.getString("folderName")
            GalleryScreen(
                navController = navController,
                viewModel     = videoPlayerViewModel,
                initialMode   = modeOrFolder
            )
        }

        composable(
            route     = "player/{videoId}",
            arguments = listOf(navArgument("videoId") { type = NavType.LongType }),
            enterTransition = {
                fadeIn(tween(600)) + scaleIn(
                    tween(600, easing = EaseOutCubic),
                    initialScale = 0.92f
                )
            },
            exitTransition = {
                fadeOut(tween(400)) + scaleOut(
                    tween(400, easing = EaseInCubic),
                    targetScale = 0.92f
                )
            },
            popExitTransition = {
                fadeOut(tween(400)) + scaleOut(
                    tween(400, easing = EaseInCubic),
                    targetScale = 0.92f
                )
            }
        ) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getLong("videoId") ?: 0L
            PlayerScreen(
                videoId      = videoId,
                viewmodel    = videoPlayerViewModel,
                navController = navController
            )
        }
    }
}
