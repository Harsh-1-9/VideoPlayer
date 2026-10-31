package com.example.videoplayer.presentation

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import com.example.videoplayer.ui.theme.*
import com.example.videoplayer.utils.formatDuration
import com.example.videoplayer.viewmodel.VideoPlayerViewModel
import kotlinx.coroutines.delay

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

    /* ── ExoPlayer Instance ── */
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
            // Reset orientation on exit
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    /* ── Player states ── */
    var isPlaying by remember { mutableStateOf(true) }
    var duration by remember { mutableStateOf(0L) }
    var currentPos by remember { mutableStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var isSeeking by remember { mutableStateOf(false) }

    // Luxury features
    var isLocked by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var isLandscape by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    // Double tap feedback
    var doubleTapLeftAnim by remember { mutableStateOf(false) }
    var doubleTapRightAnim by remember { mutableStateOf(false) }

    LaunchedEffect(doubleTapLeftAnim) {
        if (doubleTapLeftAnim) {
            delay(650)
            doubleTapLeftAnim = false
        }
    }
    LaunchedEffect(doubleTapRightAnim) {
        if (doubleTapRightAnim) {
            delay(650)
            doubleTapRightAnim = false
        }
    }

    /* ── ExoPlayer Listeners ── */
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

    /* ── Progress Polling ── */
    LaunchedEffect(exoPlayer) {
        while (true) {
            if (!isSeeking) {
                currentPos = exoPlayer.currentPosition.coerceIn(0L, duration)
            }
            delay(400)
        }
    }

    /* ── Auto-hide Controls ── */
    LaunchedEffect(showControls, isPlaying, isLocked) {
        if (showControls && isPlaying && !isLocked) {
            delay(4500)
            showControls = false
        }
    }

    val controlAlpha by animateFloatAsState(
        targetValue = if (showControls) 1f else 0f,
        animationSpec = tween(280),
        label = "controlAlpha"
    )

    val masterAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) { masterAlpha.animateTo(1f, tween(500)) }

    val videoTitle = viewmodel.localVideos.find { it.id == videoId }?.title
        ?.removeSuffix(".mp4")?.removeSuffix(".mkv")
        ?.removeSuffix(".avi")?.removeSuffix(".mov")
        ?: "Now Playing"

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(LuxuryBlack)
            .alpha(masterAlpha.value)
            .pointerInput(isLocked) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        if (!isLocked) {
                            if (offset.x < size.width / 2f) {
                                // Double tap left: Rewind 10s
                                exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L))
                                doubleTapLeftAnim = true
                            } else {
                                // Double tap right: Forward 10s
                                exoPlayer.seekTo((exoPlayer.currentPosition + 10_000L).coerceAtMost(duration))
                                doubleTapRightAnim = true
                            }
                        }
                    },
                    onTap = {
                        showControls = !showControls
                    }
                )
            }
    ) {
        val screenWidth = maxWidth

        /* ── ExoPlayer View ── */
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

        /* ── Double Tap Ripple Overlays ── */
        if (doubleTapLeftAnim) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(screenWidth / 2)
                    .background(
                        Brush.horizontalGradient(
                            listOf(GoldPrimary.copy(alpha = 0.25f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Replay10,
                        contentDescription = "-10s",
                        tint = GoldPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "-10 sec",
                        style = MaterialTheme.typography.labelMedium,
                        color = GoldLight,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (doubleTapRightAnim) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(screenWidth / 2)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, GoldPrimary.copy(alpha = 0.25f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Forward10,
                        contentDescription = "+10s",
                        tint = GoldPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "+10 sec",
                        style = MaterialTheme.typography.labelMedium,
                        color = GoldLight,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        /* ── Dark Scrim Overlay ── */
        if (controlAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(controlAlpha * 0.60f)
                    .background(Color.Black)
            )
        }

        /* ── Screen Lock Floating Button ── */
        if (controlAlpha > 0f || isLocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .alpha(if (isLocked) 1f else controlAlpha)
            ) {
                IconButton(
                    onClick = {
                        isLocked = !isLocked
                        if (!isLocked) showControls = true
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(LuxuryCardSurface.copy(alpha = 0.85f), CircleShape)
                        .border(1.dp, if (isLocked) GoldPrimary else GlassBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Lock Controls",
                        tint = if (isLocked) GoldLight else PlatinumMid,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        /* ── Normal Controls (Hidden when screen is locked) ── */
        if (!isLocked && controlAlpha > 0f) {

            /* ── Top Bar ── */
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .alpha(controlAlpha)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.88f), Color.Transparent)
                        )
                    )
                    .padding(top = 44.dp, start = 12.dp, end = 16.dp, bottom = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(GoldSubtle, CircleShape)
                                .border(1.dp, GoldPrimary.copy(0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = GoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            text = videoTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = PlatinumLight,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "NOW PLAYING",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldPrimary,
                            letterSpacing = 2.sp
                        )
                    }

                    // Playback Speed Selector Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(GoldSubtle)
                            .border(0.5.dp, GoldPrimary.copy(0.5f), RoundedCornerShape(8.dp))
                            .clickable { showSpeedDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${playbackSpeed}x",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldLight,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // Resize Mode Button (Fit vs Zoom vs Fill)
                    IconButton(
                        onClick = {
                            resizeMode = when (resizeMode) {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.AspectRatio,
                            contentDescription = "Aspect Ratio",
                            tint = PlatinumMid,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Screen Rotation Toggle (Portrait / Landscape)
                    IconButton(
                        onClick = {
                            isLandscape = !isLandscape
                            activity?.requestedOrientation = if (isLandscape) {
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.ScreenRotation,
                            contentDescription = "Rotate",
                            tint = if (isLandscape) GoldPrimary else PlatinumMid,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            /* ── Center Play / Pause Button ── */
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .alpha(controlAlpha)
            ) {
                val buttonScale by animateFloatAsState(
                    if (isPlaying) 0.92f else 1f,
                    spring(Spring.DampingRatioMediumBouncy),
                    label = "btnScale"
                )
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .scale(buttonScale)
                        .background(
                            Brush.radialGradient(
                                listOf(GoldPrimary.copy(0.25f), Color.Transparent)
                            ),
                            CircleShape
                        )
                        .border(2.dp, GoldPrimary.copy(0.8f), CircleShape)
                        .clickable(
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                        ) {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            showControls = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = GoldPrimary,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            /* ── Bottom Controls & Progress Bar ── */
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .alpha(controlAlpha)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(0.92f))
                        )
                    )
                    .padding(start = 20.dp, end = 20.dp, bottom = 32.dp, top = 20.dp)
            ) {
                // Seek Bar
                val progress = if (duration > 0L) currentPos.toFloat() / duration.toFloat() else 0f

                LuxurySeekBar(
                    progress = progress,
                    onSeek = { frac ->
                        val pos = (frac * duration).toLong()
                        exoPlayer.seekTo(pos)
                        currentPos = pos
                        isSeeking = false
                        showControls = true
                    },
                    onSeekStart = {
                        isSeeking = true
                        showControls = true
                    }
                )

                Spacer(Modifier.height(8.dp))

                // Time labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatDuration(currentPos),
                        style = MaterialTheme.typography.labelMedium,
                        color = PlatinumMid
                    )
                    Text(
                        formatDuration(duration),
                        style = MaterialTheme.typography.labelMedium,
                        color = PlatinumDark
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Control Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerControlButton(Icons.Default.Replay10, "−10s") {
                        exoPlayer.seekTo((currentPos - 10_000L).coerceAtLeast(0L))
                        showControls = true
                    }
                    PlayerControlButton(Icons.Default.SkipPrevious, "Start") {
                        exoPlayer.seekTo(0L)
                        showControls = true
                    }
                    PlayerControlButton(Icons.Default.SkipNext, "End") {
                        exoPlayer.seekTo(duration)
                        showControls = true
                    }
                    PlayerControlButton(Icons.Default.Forward10, "+10s") {
                        exoPlayer.seekTo((currentPos + 10_000L).coerceAtMost(duration))
                        showControls = true
                    }
                }
            }
        }
    }

    /* ── Playback Speed Dialog ── */
    if (showSpeedDialog) {
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            containerColor = LuxuryCardSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Playback Speed",
                    style = MaterialTheme.typography.titleMedium,
                    color = PlatinumLight,
                    fontWeight = FontWeight.Bold
                )
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
                                .padding(vertical = 12.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (speed == 1.0f) "1.0x (Normal)" else "${speed}x",
                                color = if (playbackSpeed == speed) GoldPrimary else PlatinumMid,
                                fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal
                            )
                            if (playbackSpeed == speed) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

/* ── Luxury Seek Bar ── */
@Composable
private fun LuxurySeekBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    onSeekStart: () -> Unit
) {
    var dragProgress by remember { mutableFloatStateOf(progress) }
    val displayProgress = dragProgress.coerceIn(0f, 1f)

    LaunchedEffect(progress) {
        if (!dragProgress.isNaN()) dragProgress = progress
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        onSeekStart()
                        val frac = (offset.x / size.width).coerceIn(0f, 1f)
                        dragProgress = frac
                        onSeek(frac)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val trackWidth = maxWidth

        // Background track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(CircleShape)
                .background(PlatinumSubtle)
        )
        // Filled portion
        Box(
            modifier = Modifier
                .fillMaxWidth(displayProgress)
                .height(3.dp)
                .align(Alignment.CenterStart)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(listOf(GoldDark, GoldPrimary, GoldLight))
                )
        )
        // Gold glowing thumb
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (trackWidth * displayProgress - 7.dp).coerceAtLeast(0.dp))
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(GoldPrimary, CircleShape)
                    .border(2.dp, GoldLight.copy(0.6f), CircleShape)
            )
        }
    }
}

/* ── Player Control Button ── */
@Composable
private fun PlayerControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(44.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = PlatinumMid,
            modifier = Modifier.size(24.dp)
        )
    }
}
