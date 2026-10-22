package com.example.videoplayer.presentation


import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.videoplayer.data.model.LocalVideo
import com.example.videoplayer.utils.formatDuration
import com.example.videoplayer.utils.formatFileSize

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun VideoRow(video: LocalVideo, onClick: () -> Unit, onDelete: () -> Unit) {

    val context = LocalContext.current
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(video.id) {


        val bmp = withContext(Dispatchers.IO) {

            try {


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                    context.contentResolver.loadThumbnail(

                        video.uri,
                        Size(320, 180),
                        null

                    )
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
            .clickable(onClick = onClick)
    ) {


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(96.dp, 64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.onPrimaryContainer),
                contentAlignment = Alignment.Center
            ) {


                if (thumbnail != null) {

                    Image(
                        bitmap = thumbnail!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )

                } else {

                    Icon(imageVector = Icons.Default.PlayCircle, contentDescription = null)

                }
            }
            Column(modifier = Modifier.weight(1f)) {


                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "${formatDuration(video.duration)} . ${formatFileSize(video.size)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)


            IconButton(onClick = onDelete) {

                Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint =  MaterialTheme.colorScheme.error)
            }
        }


    }

}