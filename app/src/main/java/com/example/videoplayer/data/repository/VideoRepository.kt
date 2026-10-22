package com.example.videoplayer.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.videoplayer.data.model.LocalVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VideoRepository(private val context: Context) {

    suspend fun getLocalVideos(): List<LocalVideo> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<LocalVideo>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.MIME_TYPE

        )
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: "Untitled"
                val duration = cursor.getLong(durationCol)
                val size = cursor.getLong(sizeCol)
                val dateAdded = cursor.getLong(dateCol)
                val mimeType = cursor.getString(mimeCol)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                videos += LocalVideo(
                    id = id,
                    title = name,
                    uri = contentUri,
                    duration = duration,
                    size = size,
                    dateAdded = dateAdded,
                    mimeType = mimeType
                )
            }
        }
        videos
    }

    fun getVideoById(id: Long): Uri {
        return ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
    }

    suspend fun deleteVideo(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val rowsDeleted = context.contentResolver.delete(uri, null, null)
            rowsDeleted > 0
        } catch (e: Exception) {
            false
        }
    }
}