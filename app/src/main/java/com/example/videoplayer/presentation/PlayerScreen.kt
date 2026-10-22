package com.example.videoplayer.presentation

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import com.example.videoplayer.viewmodel.VideoPlayerViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
    videoId: Long,
    viewmodel: VideoPlayerViewModel,
    navController: NavHostController
) {

    val context = LocalContext.current
    val videoUri = viewmodel.getVideoUri(videoId)


    val exoPlayer = remember(videoUri) {

        ExoPlayer.Builder(context).build().apply {

            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = true
        }

    }

    DisposableEffect(Unit) {

        onDispose {
            exoPlayer.release()
        }

    }


    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Now Playing") }, navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }, content = {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null
                    )
                })
            })
        }
    ) {

        AndroidView(

            factory = { ctx ->

                PlayerView(ctx).apply {

                    player = exoPlayer
                    layoutParams= ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT)
                }


            },
            modifier = Modifier.fillMaxSize().padding(it)
        )
    }

}