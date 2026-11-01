package com.example.videoplayer.presentation

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import com.example.videoplayer.ui.theme.*
import com.example.videoplayer.utils.formatDuration
import com.example.videoplayer.viewmodel.VideoPlayerViewModel
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
    videoId: Long,
    viewmodel: VideoPlayerViewModel,
    navController: NavHostController
) {
    val context = LocalContext.current
    val activity = context as? Activity
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
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var duration by remember { mutableLongStateOf(0L) }
    var currentPos by remember { mutableLongStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var isSeeking by remember { mutableStateOf(false) }

    var isLocked by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var isLandscape by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            if (!isSeeking) {
                currentPos = exoPlayer.currentPosition.coerceIn(0L, duration)
            }
            delay(350)
        }
    }

    LaunchedEffect(showControls, isPlaying, isLocked) {
        if (showControls && isPlaying && !isLocked) {
            delay(4000)
            showControls = false
        }
    }

    val videoTitle = viewmodel.localVideos.find { it.id == videoId }?.title
        ?.removeSuffix(".mp4")?.removeSuffix(".mkv")
        ?.removeSuffix(".avi")?.removeSuffix(".mov")
        ?: "Video"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isLocked) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        if (!isLocked) {
                            if (offset.x < size.width / 2f) {
                                exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L))
                            } else {
                                exoPlayer.seekTo((exoPlayer.currentPosition + 10_000L).coerceAtMost(duration))
                            }
                        }
                    },
                    onTap = {
                        showControls = !showControls
                    }
                )
            }
    ) {
        // Player Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    this.resizeMode = resizeMode
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Lock Toggle Floating Button
        AnimatedVisibility(
            visible = showControls || isLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
        ) {
            IconButton(
                onClick = {
                    isLocked = !isLocked
                    if (!isLocked) showControls = true
                },
                modifier = Modifier
                    .size(42.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "Lock",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Overlay Controls
        AnimatedVisibility(
            visible = showControls && !isLocked,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = videoTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Speed Selector
                    TextButton(onClick = { showSpeedDialog = true }) {
                        Text(
                            text = "${playbackSpeed}x",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Aspect Ratio Button
                    IconButton(
                        onClick = {
                            resizeMode = when (resizeMode) {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.AspectRatio,
                            contentDescription = "Aspect Ratio",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Rotation Toggle Button
                    IconButton(
                        onClick = {
                            isLandscape = !isLandscape
                            activity?.requestedOrientation = if (isLandscape) {
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.ScreenRotation,
                            contentDescription = "Rotate",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Center Play / Pause Button
                IconButton(
                    onClick = {
                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        showControls = true
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = TextPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Bottom Controls Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    val progress = if (duration > 0L) currentPos.toFloat() / duration.toFloat() else 0f

                    Slider(
                        value = if (isSeeking) progress else (if (duration > 0L) currentPos.toFloat() / duration.toFloat() else 0f),
                        onValueChange = { frac ->
                            isSeeking = true
                            currentPos = (frac * duration).toLong()
                        },
                        onValueChangeFinished = {
                            exoPlayer.seekTo(currentPos)
                            isSeeking = false
                            showControls = true
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryColor,
                            activeTrackColor = PrimaryColor,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            formatDuration(currentPos),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    exoPlayer.seekTo((currentPos - 10_000L).coerceAtLeast(0L))
                                    showControls = true
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Replay10, "-10s", tint = TextPrimary, modifier = Modifier.size(20.dp))
                            }

                            IconButton(
                                onClick = {
                                    exoPlayer.seekTo((currentPos + 10_000L).coerceAtMost(duration))
                                    showControls = true
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Forward10, "+10s", tint = TextPrimary, modifier = Modifier.size(20.dp))
                            }
                        }

                        Text(
                            formatDuration(duration),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }

    // Playback Speed Dialog
    if (showSpeedDialog) {
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            containerColor = AppSurfaceCard,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text("Playback Speed", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    speeds.forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    playbackSpeed = speed
                                    exoPlayer.setPlaybackSpeed(speed)
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (speed == 1.0f) "1.0x (Normal)" else "${speed}x",
                                color = if (playbackSpeed == speed) PrimaryColor else TextPrimary,
                                fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal
                            )
                            if (playbackSpeed == speed) {
                                Icon(Icons.Default.Check, null, tint = PrimaryColor, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}