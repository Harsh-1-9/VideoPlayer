package com.example.videoplayer.presentation

import android.Manifest
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.videoplayer.data.model.LocalVideo
import com.example.videoplayer.ui.theme.*
import com.example.videoplayer.utils.formatDuration
import com.example.videoplayer.utils.formatFileSize
import com.example.videoplayer.viewmodel.VideoPlayerViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: VideoPlayerViewModel
) {

    val permissionState = rememberMultiplePermissionsState(
        permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    )

    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            viewModel.loadLocalVideo()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Video Player",
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    if (permissionState.allPermissionsGranted) {
                        IconButton(
                            onClick = { navController.navigate("gallery") }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = TextPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppSurface
                )
            )
        },
        containerColor = AppBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            // Permission Request Card
            if (!permissionState.allPermissionsGranted) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AppSurfaceCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(PrimarySubtle, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.FolderSpecial,
                                    contentDescription = null,
                                    tint = PrimaryLight,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Permission Required",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Grant storage access to scan and play video files stored on your device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { permissionState.launchMultiplePermissionRequest() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Grant Permission", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                // Storage / Media Overview Card
                val totalVideos = viewModel.localVideos.size
                val totalSizeFormatted = formatFileSize(viewModel.localVideos.sumOf { it.size })
                val totalAlbums = viewModel.videoFolders.size

                Card(
                    colors = CardDefaults.cardColors(containerColor = AppSurfaceCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Storage Overview",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = totalSizeFormatted,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryLight
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StatPill(
                                icon = Icons.Default.Movie,
                                value = "$totalVideos",
                                label = "Videos"
                            )
                            StatPill(
                                icon = Icons.Default.Folder,
                                value = "$totalAlbums",
                                label = "Albums"
                            )
                            StatPill(
                                icon = Icons.Default.SdStorage,
                                value = totalSizeFormatted,
                                label = "Total Size"
                            )
                        }
                    }
                }
            }

            // Section 1: Browse Library
            Text(
                text = "Browse Library",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeFeatureCard(
                    title = "Video Gallery",
                    subtitle = "Albums & Grid View",
                    icon = Icons.Default.GridView,
                    accentColor = PrimaryColor,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (permissionState.allPermissionsGranted) {
                            navController.navigate("gallery")
                        } else {
                            permissionState.launchMultiplePermissionRequest()
                        }
                    }
                )

                HomeFeatureCard(
                    title = "All Videos",
                    subtitle = "List View",
                    icon = Icons.Default.VideoLibrary,
                    accentColor = PrimaryColor,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (permissionState.allPermissionsGranted) {
                            navController.navigate("localMedia")
                        } else {
                            permissionState.launchMultiplePermissionRequest()
                        }
                    }
                )
            }

            // Section 2: Quick Filters
            Text(
                text = "Quick Filters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickFilterCard(
                    title = "HD Videos",
                    subtitle = "1080p+",
                    icon = Icons.Default.Hd,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (permissionState.allPermissionsGranted) {
                            navController.navigate("gallery/hd")
                        } else {
                            permissionState.launchMultiplePermissionRequest()
                        }
                    }
                )

                QuickFilterCard(
                    title = "Recent Videos",
                    subtitle = "Newly Added",
                    icon = Icons.Default.History,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (permissionState.allPermissionsGranted) {
                            navController.navigate("gallery/recent")
                        } else {
                            permissionState.launchMultiplePermissionRequest()
                        }
                    }
                )
            }

            // Section 3: recent videos
            if (permissionState.allPermissionsGranted && viewModel.localVideos.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    TextButton(onClick = { navController.navigate("gallery/recent") }) {
                        Text("See All", color = PrimaryLight, fontSize = 13.sp)
                    }
                }

                val recentPreview = viewModel.localVideos.take(6)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(recentPreview) { video ->
                        HomeRecentVideoCard(
                            video = video,
                            onClick = { navController.navigate("player/${video.id}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryLight,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun HomeFeatureCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickFilterCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(PrimarySubtle, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryLight,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun HomeRecentVideoCard(
    video: LocalVideo,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(video.id) {
        val bmp = withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(video.uri, Size(280, 180), null)
                } else {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, video.uri)
                    val frame = retriever.getFrameAtTime(1_000_000)
                    retriever.release()
                    frame
                }
            } catch (_: Exception) {
                null
            }
        }
        thumbnail = bmp
    }

    Card(
        modifier = Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(AppSurface),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(28.dp)
                    )
                }

                if (video.duration > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formatDuration(video.duration),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = video.title.removeSuffix(".mp4").removeSuffix(".mkv"),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatFileSize(video.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }
    }
}