package com.example.videoplayer.presentation

import android.app.Activity
import android.app.RecoverableSecurityException
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import com.example.videoplayer.data.model.LocalVideo
import com.example.videoplayer.ui.theme.*
import com.example.videoplayer.viewmodel.VideoPlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalMediaScreen(navController: NavHostController, viewModel: VideoPlayerViewModel) {

    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadLocalVideo() }

    var videoToDelete   by remember { mutableStateOf<LocalVideo?>(null) }
    var pendingDelteVideo by remember { mutableStateOf<LocalVideo?>(null) }
    val scope = rememberCoroutineScope()

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingDelteVideo?.let { video ->
                scope.launch { viewModel.removeVideoFromList(video) }
                Toast.makeText(context, "Video deleted", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Delete cancelled", Toast.LENGTH_SHORT).show()
        }
        pendingDelteVideo = null
    }

    /* ── Ambient glow ── */
    val infinite = rememberInfiniteTransition(label = "media")
    val glowAlpha by infinite.animateFloat(
        0.2f, 0.5f,
        infiniteRepeatable(tween(3500, easing = EaseInOut), RepeatMode.Reverse),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LuxuryBlack)
    ) {
        // Ambient top glow
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopEnd)
                .offset(x = 80.dp, y = (-60).dp)
                .blur(100.dp)
                .background(GoldPrimary.copy(alpha = glowAlpha * 0.07f), CircleShape)
        )

        Column(modifier = Modifier.fillMaxSize()) {

            /* ── Top Bar ── */
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(LuxuryDarkSurface, Color.Transparent)
                        )
                    )
                    .padding(top = 48.dp, bottom = 16.dp, start = 8.dp, end = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(GoldSubtle, CircleShape)
                                .border(1.dp, GoldPrimary.copy(0.3f), CircleShape),
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
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "LOCAL VIDEOS",
                            style = MaterialTheme.typography.titleLarge,
                            color = PlatinumLight,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 2.sp
                        )
                        if (!viewModel.isLoading && viewModel.localVideos.isNotEmpty()) {
                            Text(
                                text = "${viewModel.localVideos.size} videos found",
                                style = MaterialTheme.typography.bodySmall,
                                color = GoldPrimary.copy(0.8f)
                            )
                        }
                    }
                }
                // Gold bottom border
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    GoldPrimary.copy(0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            /* ── Body ── */
            when {
                viewModel.isLoading -> LoadingState()
                viewModel.localVideos.isEmpty() -> EmptyState()
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = 16.dp, bottom = 32.dp,
                            start = 20.dp, end = 20.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(viewModel.localVideos) { index, video ->
                            AnimatedVideoRow(
                                video = video,
                                index = index,
                                onClick = { navController.navigate("player/${video.id}") },
                                onDelete = { videoToDelete = video }
                            )
                        }
                    }
                }
            }
        }
    }

    /* ── Delete Dialog ── */
    videoToDelete?.let { video ->
        LuxuryDeleteDialog(
            videoTitle = video.title,
            onDismiss  = { videoToDelete = null },
            onConfirm  = {
                videoToDelete = null
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                        try {
                            val req = MediaStore.createDeleteRequest(
                                context.contentResolver, listOf(video.uri)
                            )
                            pendingDelteVideo = video
                            deleteLauncher.launch(
                                IntentSenderRequest.Builder(req.intentSender).build()
                            )
                        } catch (e: Exception) {
                            Toast.makeText(context, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                        scope.launch {
                            try {
                                val deleted = withContext(Dispatchers.IO) {
                                    context.contentResolver.delete(video.uri, null, null) > 0
                                }
                                if (deleted) {
                                    viewModel.removeVideoFromList(video)
                                    Toast.makeText(context, "Video deleted", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: SecurityException) {
                                val rec = e as? RecoverableSecurityException
                                rec?.let {
                                    pendingDelteVideo = video
                                    deleteLauncher.launch(
                                        IntentSenderRequest.Builder(it.userAction.actionIntent.intentSender).build()
                                    )
                                } ?: Toast.makeText(context, "Cannot delete this video", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    else -> {
                        scope.launch {
                            val ok = viewModel.deleteVideo(video)
                            Toast.makeText(
                                context,
                                if (ok) "Video deleted" else "Delete failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        )
    }
}

/* ── Loading State ── */
@Composable
private fun LoadingState() {
    val infinite = rememberInfiniteTransition(label = "loading")
    val rot by infinite.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "rot"
    )
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Canvas(
                    Modifier.fillMaxSize().rotate(rot)
                ) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color.Transparent, GoldPrimary)
                        ),
                        startAngle = 0f,
                        sweepAngle = 270f,
                        useCenter   = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 4.dp.toPx(),
                            cap   = StrokeCap.Round
                        )
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Scanning media library…",
                style = MaterialTheme.typography.bodyMedium,
                color = PlatinumMid
            )
        }
    }
}

/* ── Empty State ── */
@Composable
private fun EmptyState() {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) { alpha.animateTo(1f, tween(600)) }
    Box(
        Modifier.fillMaxSize().alpha(alpha.value),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(96.dp)
                    .background(GoldSubtle, CircleShape)
                    .border(1.dp, GoldPrimary.copy(0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.VideoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(46.dp),
                    tint = GoldPrimary.copy(0.6f)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "No Videos Found",
                style = MaterialTheme.typography.titleLarge,
                color = PlatinumLight
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Your media library appears to be empty.\nAdd some videos to get started.",
                style = MaterialTheme.typography.bodySmall,
                color = PlatinumDark,
                textAlign = TextAlign.Center
            )
        }
    }
}

/* ── Animated Video Row Wrapper ── */
@Composable
private fun AnimatedVideoRow(
    video: LocalVideo,
    index: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val alpha  = remember { Animatable(0f) }
    val offset = remember { Animatable(30f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index * 60L).coerceAtMost(400L))
        alpha.animateTo(1f, tween(400))
        offset.animateTo(0f, tween(400, easing = EaseOutCubic))
    }
    Box(
        modifier = Modifier
            .alpha(alpha.value)
            .offset(y = offset.value.dp)
    ) {
        VideoRow(video = video, onClick = onClick, onDelete = onDelete)
    }
}

/* ── Luxury Delete Dialog ── */
@Composable
private fun LuxuryDeleteDialog(
    videoTitle: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor  = LuxuryCardSurface,
        shape           = RoundedCornerShape(24.dp),
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(ErrorRed.copy(0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Delete, null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Delete Video",
                        style = MaterialTheme.typography.titleLarge,
                        color = PlatinumLight
                    )
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.fillMaxWidth().height(1.dp).background(
                        Brush.horizontalGradient(
                            listOf(ErrorRed.copy(0.5f), Color.Transparent)
                        )
                    )
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "\"$videoTitle\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GoldPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "This video will be permanently deleted from your device. This action cannot be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PlatinumMid
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors  = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
            ) {
                Text("Delete", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors  = ButtonDefaults.textButtonColors(contentColor = PlatinumMid)
            ) {
                Text("Cancel")
            }
        }
    )
}
