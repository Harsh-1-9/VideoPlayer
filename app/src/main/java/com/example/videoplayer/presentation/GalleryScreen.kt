package com.example.videoplayer.presentation

import android.app.Activity
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.videoplayer.data.model.LocalVideo
import com.example.videoplayer.data.model.VideoFolder
import com.example.videoplayer.ui.theme.*
import com.example.videoplayer.utils.formatDuration
import com.example.videoplayer.utils.formatFileSize
import com.example.videoplayer.viewmodel.GallerySortOption
import com.example.videoplayer.viewmodel.QuickFilterMode
import com.example.videoplayer.viewmodel.VideoPlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class GalleryTab {
    ALL_VIDEOS,
    FOLDERS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    navController: NavHostController,
    viewModel: VideoPlayerViewModel,
    initialMode: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(GalleryTab.ALL_VIDEOS) }
    var isSearchActive by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

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

    LaunchedEffect(initialMode) {
        if (viewModel.localVideos.isEmpty()) {
            viewModel.loadLocalVideo()
        }
        when (initialMode?.lowercase()) {
            "recent" -> {
                viewModel.quickFilter = QuickFilterMode.RECENT
                viewModel.selectedFolderName = null
                viewModel.sortOption = GallerySortOption.DATE_DESC
                selectedTab = GalleryTab.ALL_VIDEOS
            }
            "hd" -> {
                viewModel.quickFilter = QuickFilterMode.HD
                viewModel.selectedFolderName = null
                viewModel.sortOption = GallerySortOption.SIZE_DESC
                selectedTab = GalleryTab.ALL_VIDEOS
            }
            null -> {}
            else -> {
                viewModel.quickFilter = QuickFilterMode.ALL
                viewModel.selectedFolderName = initialMode
                selectedTab = GalleryTab.ALL_VIDEOS
            }
        }
    }

    val pageTitle = when {
        viewModel.quickFilter == QuickFilterMode.RECENT -> "Recent Videos"
        viewModel.quickFilter == QuickFilterMode.HD -> "HD Videos"
        viewModel.selectedFolderName != null -> viewModel.selectedFolderName!!
        else -> "Video Gallery"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = pageTitle,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${viewModel.galleryVideos.size} items",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (viewModel.selectedFolderName != null || viewModel.quickFilter != QuickFilterMode.ALL) {
                            viewModel.selectedFolderName = null
                            viewModel.quickFilter = QuickFilterMode.ALL
                        } else {
                            navController.navigateUp()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { isSearchActive = !isSearchActive }) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextPrimary
                        )
                    }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort",
                                tint = TextPrimary
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier.background(AppSurfaceCard)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Newest First", color = TextPrimary) },
                                onClick = {
                                    viewModel.sortOption = GallerySortOption.DATE_DESC
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Oldest First", color = TextPrimary) },
                                onClick = {
                                    viewModel.sortOption = GallerySortOption.DATE_ASC
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Name (A to Z)", color = TextPrimary) },
                                onClick = {
                                    viewModel.sortOption = GallerySortOption.NAME_ASC
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Largest Size", color = TextPrimary) },
                                onClick = {
                                    viewModel.sortOption = GallerySortOption.SIZE_DESC
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSurface)
            )
        },
        containerColor = AppBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // Search Bar
            AnimatedVisibility(
                visible = isSearchActive,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                OutlinedTextField(
                    value = viewModel.searchQuery,
                    onValueChange = { viewModel.searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search videos...", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryColor,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = AppSurfaceCard,
                        unfocusedContainerColor = AppSurfaceCard,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (viewModel.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery = "" }) {
                                Icon(Icons.Default.Close, null, tint = TextSecondary)
                            }
                        }
                    }
                )
            }

            // Tab Switcher
            if (viewModel.selectedFolderName == null && viewModel.quickFilter == QuickFilterMode.ALL) {
                PrimaryTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = AppSurface,
                    contentColor = PrimaryColor
                ) {
                    Tab(
                        selected = selectedTab == GalleryTab.ALL_VIDEOS,
                        onClick = { selectedTab = GalleryTab.ALL_VIDEOS },
                        text = { Text("Videos (${viewModel.localVideos.size})", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == GalleryTab.FOLDERS,
                        onClick = { selectedTab = GalleryTab.FOLDERS },
                        text = { Text("Albums (${viewModel.videoFolders.size})", fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            // Gallery Grid
            when {
                viewModel.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryColor)
                    }
                }
                selectedTab == GalleryTab.ALL_VIDEOS || viewModel.selectedFolderName != null || viewModel.quickFilter != QuickFilterMode.ALL -> {
                    if (viewModel.galleryVideos.isEmpty()) {
                        EmptyState(message = if (viewModel.searchQuery.isNotBlank()) "No matching videos found" else "No videos in gallery")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(viewModel.galleryVideos) { video ->
                                GalleryVideoCard(
                                    video = video,
                                    onClick = { navController.navigate("player/${video.id}") },
                                    onDelete = { videoToDelete = video }
                                )
                            }
                        }
                    }
                }
                selectedTab == GalleryTab.FOLDERS -> {
                    if (viewModel.videoFolders.isEmpty()) {
                        EmptyState(message = "No albums found")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(viewModel.videoFolders) { folder ->
                                GalleryFolderCard(
                                    folder = folder,
                                    onClick = {
                                        viewModel.selectedFolderName = folder.name
                                        selectedTab = GalleryTab.ALL_VIDEOS
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Dialog
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
                TextButton(onClick = {
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
                }) {
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

@Composable
private fun GalleryVideoCard(
    video: LocalVideo,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(video.id) {
        val bmp = withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(video.uri, Size(360, 240), null)
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
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
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
                        Icons.Default.PlayCircle,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = TextMuted
                    )
                }

                if (video.duration > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .background(Color.Black.copy(0.75f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formatDuration(video.duration),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = video.title.removeSuffix(".mp4").removeSuffix(".mkv").removeSuffix(".avi"),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatFileSize(video.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryFolderCard(
    folder: VideoFolder,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(folder.previewVideo?.id) {
        folder.previewVideo?.let { preview ->
            val bmp = withContext(Dispatchers.IO) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        context.contentResolver.loadThumbnail(preview.uri, Size(360, 240), null)
                    } else {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(context, preview.uri)
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
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
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
                        Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = TextMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(0.75f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${folder.videoCount} videos",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        fontSize = 10.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = formatFileSize(folder.totalSize),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.PermMedia,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = TextMuted
            )
            Spacer(Modifier.height(12.dp))
            Text(
                message,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "No media available in this folder",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}