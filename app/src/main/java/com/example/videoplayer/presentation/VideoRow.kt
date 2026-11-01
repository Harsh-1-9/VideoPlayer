package com.example.videoplayer.presentation

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayCircle
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
import com.example.videoplayer.data.model.LocalVideo
import com.example.videoplayer.ui.theme.*
import com.example.videoplayer.utils.formatDuration
import com.example.videoplayer.utils.formatFileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun VideoRow(
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
                    context.contentResolver.loadThumbnail(video.uri, Size(320, 180), null)
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
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Video Thumbnail
            Box(
                modifier = Modifier
                    .size(width = 100.dp, height = 64.dp)
                    .clip(RoundedCornerShape(8.dp))
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
                            .background(
                                Color.Black.copy(alpha = 0.75f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
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

            Spacer(modifier = Modifier.width(12.dp))

            // Video Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title.removeSuffix(".mp4").removeSuffix(".mkv")
                        .removeSuffix(".avi").removeSuffix(".mov"),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatFileSize(video.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    if (video.mimeType != null) {
                        val format = video.mimeType.substringAfter("video/").uppercase().take(4)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(PrimarySubtle, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = format,
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryLight,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            // Delete Action
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}