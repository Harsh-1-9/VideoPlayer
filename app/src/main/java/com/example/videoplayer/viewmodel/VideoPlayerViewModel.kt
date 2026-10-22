package com.example.videoplayer.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.videoplayer.data.model.LocalVideo
import com.example.videoplayer.data.repository.VideoRepository

class VideoPlayerViewModel( private  val repo: VideoRepository) : ViewModel() {

    var localVideos by mutableStateOf<List<LocalVideo>>( emptyList())

    var isLoading by mutableStateOf(false)

    suspend fun loadLocalVideo(){

        isLoading = true
        localVideos = repo.getLocalVideos()
        isLoading = false

    }

    fun getVideoUri(id:Long):Uri = repo.getVideoById(id)


    suspend fun  deleteVideo(video:LocalVideo):Boolean{

        val success = repo.deleteVideo(video.uri)

        if (success){

            localVideos = localVideos.filter { it.id != video.id }
        }

        return  success
    }
    fun removeVideoFromList(video: LocalVideo){
        localVideos = localVideos.filter {  it.id != video.id }
    }
}