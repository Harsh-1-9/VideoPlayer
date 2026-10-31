package com.example.videoplayer.viewmodel

import android.net.Uri
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.videoplayer.data.model.LocalVideo
import com.example.videoplayer.data.model.VideoFolder
import com.example.videoplayer.data.repository.VideoRepository

enum class GallerySortOption {
    DATE_DESC,
    DATE_ASC,
    NAME_ASC,
    SIZE_DESC,
    DURATION_DESC
}

enum class QuickFilterMode {
    ALL,
    RECENT,
    HD
}

class VideoPlayerViewModel(private val repo: VideoRepository) : ViewModel() {

    var localVideos by mutableStateOf<List<LocalVideo>>(emptyList())
    var isLoading by mutableStateOf(false)

    // Gallery & Quick Access filters
    var selectedFolderName by mutableStateOf<String?>(null)
    var searchQuery by mutableStateOf("")
    var sortOption by mutableStateOf(GallerySortOption.DATE_DESC)
    var quickFilter by mutableStateOf(QuickFilterMode.ALL)

    // Grouping videos into folders
    val videoFolders by derivedStateOf {
        localVideos
            .groupBy { it.folderName }
            .map { (folderName, videos) ->
                VideoFolder(
                    name = folderName,
                    videoCount = videos.size,
                    totalSize = videos.sumOf { it.size },
                    previewVideo = videos.firstOrNull()
                )
            }
            .sortedByDescending { it.videoCount }
    }

    // Filtered & sorted videos for gallery
    val galleryVideos by derivedStateOf {
        var list = localVideos

        // 1. Quick filter (Recent, HD, All)
        when (quickFilter) {
            QuickFilterMode.RECENT -> {
                // Return videos sorted by newest date
                list = list.sortedByDescending { it.dateAdded }
            }
            QuickFilterMode.HD -> {
                // Filter large/HD videos (>= 20MB or top size)
                val hdList = list.filter { it.size >= 20 * 1024 * 1024L || it.duration >= 60_000L }
                list = if (hdList.isNotEmpty()) hdList else list
            }
            QuickFilterMode.ALL -> {
                // No special pre-filter
            }
        }

        // 2. Filter by folder if selected
        selectedFolderName?.let { folder ->
            list = list.filter { it.folderName.equals(folder, ignoreCase = true) }
        }

        // 3. Filter by search query
        if (searchQuery.isNotBlank()) {
            list = list.filter { it.title.contains(searchQuery.trim(), ignoreCase = true) }
        }

        // 4. Apply sorting
        when (sortOption) {
            GallerySortOption.DATE_DESC -> list.sortedByDescending { it.dateAdded }
            GallerySortOption.DATE_ASC -> list.sortedBy { it.dateAdded }
            GallerySortOption.NAME_ASC -> list.sortedBy { it.title.lowercase() }
            GallerySortOption.SIZE_DESC -> list.sortedByDescending { it.size }
            GallerySortOption.DURATION_DESC -> list.sortedByDescending { it.duration }
        }
    }

    suspend fun loadLocalVideo() {
        isLoading = true
        localVideos = repo.getLocalVideos()
        isLoading = false
    }

    fun getVideoUri(id: Long): Uri = repo.getVideoById(id)

    suspend fun deleteVideo(video: LocalVideo): Boolean {
        val success = repo.deleteVideo(video.uri)
        if (success) {
            localVideos = localVideos.filter { it.id != video.id }
        }
        return success
    }

    fun removeVideoFromList(video: LocalVideo) {
        localVideos = localVideos.filter { it.id != video.id }
    }
}
