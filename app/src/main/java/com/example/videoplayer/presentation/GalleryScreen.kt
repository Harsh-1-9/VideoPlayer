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
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
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
            null -> {
                // Keep existing or default
            }
            else -> {
                viewModel.quickFilter = QuickFilterMode.ALL
                viewModel.selectedFolderName = initialMode
                selectedTab = GalleryTab.ALL_VIDEOS
            }
        }
    }

    val pageTitle = when {
        viewModel.quickFilter == QuickFilterMode.RECENT -> "RECENT VIDEOS"
        viewModel.quickFilter == QuickFilterMode.HD -> "HD CONTENT"
        viewModel.selectedFolderName != null -> viewModel.selectedFolderName!!.uppercase()
        else -> "VIDEO GALLERY"
    }

    val pageSubtitle = when {
        viewModel.quickFilter == QuickFilterMode.RECENT -> "${viewModel.galleryVideos.size} recently added"
        viewModel.quickFilter == QuickFilterMode.HD -> "${viewModel.galleryVideos.size} high definition videos"
        viewModel.selectedFolderName != null -> "${viewModel.galleryVideos.size} items in folder"
        else -> "${viewModel.galleryVideos.size} videos total"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LuxuryBlack)
    ) {
        // Ambient background glow
        Box(
            modifier = Modifier
                .size(340.dp)
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-50).dp)
                .blur(110.dp)
                .background(GoldPrimary.copy(alpha = 0.08f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-80).dp, y = 50.dp)
                .blur(90.dp)
                .background(GoldDark.copy(alpha = 0.05f), CircleShape)
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
                    .padding(top = 48.dp, bottom = 12.dp, start = 8.dp, end = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (viewModel.selectedFolderName != null || viewModel.quickFilter != QuickFilterMode.ALL) {
                            viewModel.selectedFolderName = null
                            viewModel.quickFilter = QuickFilterMode.ALL
                        } else {
                            navController.navigateUp()
                        }
                    }) {
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

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pageTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = PlatinumLight,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 2.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = pageSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = GoldPrimary.copy(0.85f)
                        )
                    }

                    // Search Toggle
                    IconButton(onClick = { isSearchActive = !isSearchActive }) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isSearchActive) GoldLight else PlatinumMid
                        )
                    }

                    // Sort Menu Button
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort",
                                tint = PlatinumMid
                            )
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier
                                .background(LuxuryCardSurface)
                                .border(1.dp, GoldSubtle, RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Newest First", color = PlatinumLight) },
                                onClick = {
                                    viewModel.sortOption = GallerySortOption.DATE_DESC
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.AccessTime, null, tint = GoldPrimary)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Oldest First", color = PlatinumLight) },
                                onClick = {
                                    viewModel.sortOption = GallerySortOption.DATE_ASC
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Name (A to Z)", color = PlatinumLight) },
                                onClick = {
                                    viewModel.sortOption = GallerySortOption.NAME_ASC
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Largest Size", color = PlatinumLight) },
                                onClick = {
                                    viewModel.sortOption = GallerySortOption.SIZE_DESC
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Longest Duration", color = PlatinumLight) },
                                onClick = {
                                    viewModel.sortOption = GallerySortOption.DURATION_DESC
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }

                // Gold Accent line
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, GoldPrimary.copy(0.35f), Color.Transparent)
                            )
                        )
                )
            }

            /* ── Search Bar Animated ── */
            AnimatedVisibility(
                visible = isSearchActive,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = viewModel.searchQuery,
                        onValueChange = { viewModel.searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search videos…", color = PlatinumDark) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = LuxuryCardSurface,
                            unfocusedContainerColor = LuxuryCardSurface,
                            focusedTextColor = PlatinumLight,
                            unfocusedTextColor = PlatinumLight,
                            cursorColor = GoldPrimary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        trailingIcon = {
                            if (viewModel.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchQuery = "" }) {
                                    Icon(Icons.Default.Close, null, tint = PlatinumMid)
                                }
                            }
                        }
                    )
                }
            }

            /* ── Filter Chip Banner (Folder / Recent / HD) ── */
            if (viewModel.selectedFolderName != null || viewModel.quickFilter != QuickFilterMode.ALL) {
                val bannerLabel = when {
                    viewModel.quickFilter == QuickFilterMode.RECENT -> "Filter: Recent Videos"
                    viewModel.quickFilter == QuickFilterMode.HD -> "Filter: HD Content"
                    else -> "Folder: ${viewModel.selectedFolderName}"
                }
                val bannerIcon = when {
                    viewModel.quickFilter == QuickFilterMode.RECENT -> Icons.Default.AccessTime
                    viewModel.quickFilter == QuickFilterMode.HD -> Icons.Default.Hd
                    else -> Icons.Default.Folder
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(GoldSubtle, RoundedCornerShape(20.dp))
                            .border(1.dp, GoldPrimary.copy(0.4f), RoundedCornerShape(20.dp))
                            .padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                bannerIcon,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = bannerLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = GoldLight
                            )
                            Spacer(Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    viewModel.selectedFolderName = null
                                    viewModel.quickFilter = QuickFilterMode.ALL
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Close, null, tint = GoldPrimary, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            /* ── Tab Switcher: "All Videos" vs "Folders / Albums" ── */
            if (viewModel.selectedFolderName == null && viewModel.quickFilter == QuickFilterMode.ALL) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .background(LuxuryCardSurface, RoundedCornerShape(16.dp))
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    GalleryTabButton(
                        title = "All Videos (${viewModel.localVideos.size})",
                        icon = Icons.Default.GridView,
                        isSelected = selectedTab == GalleryTab.ALL_VIDEOS,
                        onClick = { selectedTab = GalleryTab.ALL_VIDEOS },
                        modifier = Modifier.weight(1f)
                    )
                    GalleryTabButton(
                        title = "Albums (${viewModel.videoFolders.size})",
                        icon = Icons.Default.FolderShared,
                        isSelected = selectedTab == GalleryTab.FOLDERS,
                        onClick = { selectedTab = GalleryTab.FOLDERS },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            /* ── Gallery Content ── */
            when {
                viewModel.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GoldPrimary)
                    }
                }
                selectedTab == GalleryTab.ALL_VIDEOS || viewModel.selectedFolderName != null || viewModel.quickFilter != QuickFilterMode.ALL -> {
                    if (viewModel.galleryVideos.isEmpty()) {
                        EmptyGalleryState(message = if (viewModel.searchQuery.isNotBlank()) "No matching videos" else "No videos in gallery")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            itemsIndexed(viewModel.galleryVideos) { index, video ->
                                GalleryVideoCard(
                                    video = video,
                                    index = index,
                                    onClick = { navController.navigate("player/${video.id}") },
                                    onDelete = { videoToDelete = video }
                                )
                            }
                        }
                    }
                }
                selectedTab == GalleryTab.FOLDERS -> {
                    if (viewModel.videoFolders.isEmpty()) {
                        EmptyGalleryState(message = "No albums found")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            itemsIndexed(viewModel.videoFolders) { index, folder ->
                                GalleryFolderCard(
                                    folder = folder,
                                    index = index,
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

    /* ── Delete Confirmation Dialog ── */
    videoToDelete?.let { video ->
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            containerColor = LuxuryCardSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Delete Video", style = MaterialTheme.typography.titleLarge, color = PlatinumLight)
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${video.title}\"? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PlatinumMid
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
                    Text("Cancel", color = PlatinumMid)
                }
            }
        )
    }
}

/* ── Tab Switch Button ── */
@Composable
private fun GalleryTabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgBrush = if (isSelected) {
        Brush.horizontalGradient(listOf(GoldDark, GoldPrimary))
    } else {
        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgBrush)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isSelected) LuxuryBlack else PlatinumMid
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) LuxuryBlack else PlatinumMid
            )
        }
    }
}

/* ── 2-Column Luxury Video Card ── */
@Composable
private fun GalleryVideoCard(
    video: LocalVideo,
    index: Int,
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

    val alpha = remember { Animatable(0f) }
    val offset = remember { Animatable(30f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index * 40L).coerceAtMost(300L))
        alpha.animateTo(1f, tween(350))
        offset.animateTo(0f, tween(350, easing = EaseOutCubic))
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.95f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "gridScale"
    )

    Box(
        modifier = Modifier
            .alpha(alpha.value)
            .offset(y = offset.value.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF161100), Color(0xFF0E0C00))
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(GoldPrimary.copy(0.4f), GoldPrimary.copy(0.08f), GoldPrimary.copy(0.2f))
                ),
                RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // Thumbnail container (aspect 16:10)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .background(LuxuryElevated),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Gradient scrim
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(0.7f))
                                )
                            )
                    )
                } else {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = GoldPrimary.copy(0.4f)
                    )
                }

                // Play icon badge in center
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.Black.copy(0.45f), CircleShape)
                        .border(1.dp, GoldPrimary.copy(0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Duration badge bottom-right
                if (video.duration > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .background(Color.Black.copy(0.85f), RoundedCornerShape(4.dp))
                            .border(0.5.dp, GoldSubtle, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formatDuration(video.duration),
                            style = MaterialTheme.typography.labelSmall,
                            color = PlatinumLight,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Info section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                Text(
                    text = video.title.removeSuffix(".mp4").removeSuffix(".mkv").removeSuffix(".avi"),
                    style = MaterialTheme.typography.titleSmall,
                    color = PlatinumLight,
                    fontWeight = FontWeight.SemiBold,
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
                        color = PlatinumDark,
                        fontSize = 11.sp
                    )

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = ErrorRed.copy(0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/* ── Luxury Folder / Album Card ── */
@Composable
private fun GalleryFolderCard(
    folder: VideoFolder,
    index: Int,
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

    val alpha = remember { Animatable(0f) }
    val offset = remember { Animatable(30f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index * 40L).coerceAtMost(300L))
        alpha.animateTo(1f, tween(350))
        offset.animateTo(0f, tween(350, easing = EaseOutCubic))
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.95f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "folderScale"
    )

    Box(
        modifier = Modifier
            .alpha(alpha.value)
            .offset(y = offset.value.dp)
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF181503), Color(0xFF100E02))
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(GoldPrimary.copy(0.5f), Color.Transparent, GoldPrimary.copy(0.3f))
                ),
                RoundedCornerShape(18.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // Album preview thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .background(LuxuryElevated),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(0.75f))
                                )
                            )
                    )
                } else {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = GoldPrimary.copy(0.5f)
                    )
                }

                // Folder icon floating badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(32.dp)
                        .background(LuxuryBlack.copy(0.75f), CircleShape)
                        .border(1.dp, GoldPrimary.copy(0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Video count badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(GoldSubtle, RoundedCornerShape(6.dp))
                        .border(0.5.dp, GoldPrimary.copy(0.6f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${folder.videoCount} videos",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            // Folder details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = PlatinumLight,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = formatFileSize(folder.totalSize),
                    style = MaterialTheme.typography.labelSmall,
                    color = PlatinumDark
                )
            }
        }
    }
}

/* ── Empty Gallery State ── */
@Composable
private fun EmptyGalleryState(message: String) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(80.dp)
                    .background(GoldSubtle, CircleShape)
                    .border(1.dp, GoldPrimary.copy(0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PermMedia,
                    contentDescription = null,
                    modifier = Modifier.size(38.dp),
                    tint = GoldPrimary.copy(0.6f)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                message,
                style = MaterialTheme.typography.titleMedium,
                color = PlatinumLight
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Add videos to your device to view them in the gallery",
                style = MaterialTheme.typography.bodySmall,
                color = PlatinumDark,
                textAlign = TextAlign.Center
            )
        }
    }
}
