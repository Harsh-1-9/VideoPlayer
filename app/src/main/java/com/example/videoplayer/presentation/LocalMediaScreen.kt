package com.example.videoplayer.presentation

import android.app.Activity
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.videoplayer.data.model.LocalVideo
import com.example.videoplayer.ui.theme.*
import com.example.videoplayer.viewmodel.VideoPlayerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalMediaScreen(navController: NavHostController, viewModel: VideoPlayerViewModel) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.loadLocalVideo() }

    var videoToDelete by remember { mutableStateOf<LocalVideo?>(null) }
    var pendingDeleteVideo by remember { mutableStateOf<LocalVideo?>(null) }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingDeleteVideo?.let { video ->
                scope.launch { viewModel.removeVideoFromList(video) }
                Toast.makeText(context, "Video deleted", Toast.LENGTH_SHORT).show()
            }
        }
        pendingDeleteVideo = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "All Videos",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        if (viewModel.localVideos.isNotEmpty()) {
                            Text(
                                text = "${viewModel.localVideos.size} items",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSurface)
            )
        },
        containerColor = AppBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                viewModel.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryColor)
                    }
                }
                viewModel.localVideos.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.VideoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = TextMuted
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No videos found",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "No media files discovered on your storage",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(viewModel.localVideos) { video ->
                            VideoRow(
                                video = video,
                                onClick = { navController.navigate("player/${video.id}") },
                                onDelete = { videoToDelete = video }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    videoToDelete?.let { video ->
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            containerColor = AppSurfaceCard,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text("Delete Video", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${video.title}\"?",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDel = video
                        videoToDelete = null
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            try {
                                val req = MediaStore.createDeleteRequest(
                                    context.contentResolver, listOf(toDel.uri)
                                )
                                pendingDeleteVideo = toDel
                                deleteLauncher.launch(
                                    IntentSenderRequest.Builder(req.intentSender).build()
                                )
                            } catch (e: Exception) {
                                Toast.makeText(context, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            scope.launch {
                                val ok = viewModel.deleteVideo(toDel)
                                Toast.makeText(context, if (ok) "Video deleted" else "Delete failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Delete", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}