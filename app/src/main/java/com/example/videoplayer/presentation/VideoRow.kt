package com.example.videoplayer.presentation

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Size
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.example.videoplayer.data.model.LocalVideo
import com.example.videoplayer.ui.theme.*
import com.example.videoplayer.utils.formatDuration
import com.example.videoplayer.utils.formatFileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun VideoRow(video: LocalVideo, onClick: () -> Unit, onDelete: () -> Unit) {

    val context = LocalContext.current
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
    var thumbLoaded by remember { mutableStateOf(false) }

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
            } catch (_: Exception) { null }
        }
        thumbnail  = bmp
        thumbLoaded = true
    }

    /* ── Press interaction ── */
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.97f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "rowScale"
    )
    val glowAlpha by animateFloatAsState(
        if (isPressed) 0.25f else 0f,
        tween(200),
        label = "rowGlow"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF161100),
                        Color(0xFF0E0C00),
                        Color(0xFF161100)
                    ),
                    start = Offset(0f, 0f),
                    end   = Offset(1200f, 400f)
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        GoldPrimary.copy(0.45f),
                        GoldPrimary.copy(0.05f),
                        GoldPrimary.copy(0.2f)
                    )
                ),
                RoundedCornerShape(18.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        // Press glow overlay
        if (glowAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(GoldPrimary.copy(alpha = glowAlpha))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {

            /* ── Thumbnail ── */
            Box(
                modifier = Modifier
                    .size(width = 110.dp, height = 70.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(LuxuryElevated),
                contentAlignment = Alignment.Center
            ) {
                if (thumbLoaded && thumbnail != null) {
                    androidx.compose.foundation.Image(
                        bitmap = thumbnail!!.asImageBitmap(),
                        contentDescription = null,
                        modifier      = Modifier.fillMaxSize(),
                        contentScale  = ContentScale.Crop
                    )
                    // Dark gradient overlay at bottom for duration badge
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(0.65f))
                                )
                            )
                    )
                } else if (!thumbLoaded) {
                    // Shimmer placeholder
                    val shimmer = rememberInfiniteTransition(label = "shimmer")
                    val shimX by shimmer.animateFloat(
                        -300f, 900f,
                        infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
                        label = "shimX"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        LuxuryElevated,
                                        PlatinumSubtle.copy(0.4f),
                                        LuxuryElevated
                                    ),
                                    start = Offset(shimX, 0f),
                                    end   = Offset(shimX + 300f, 200f)
                                )
                            )
                    )
                } else {
                    // No thumbnail
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint     = GoldPrimary.copy(0.5f)
                    )
                }

                // Duration badge
                if (video.duration > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(Color.Black.copy(0.75f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text  = formatDuration(video.duration),
                            style = MaterialTheme.typography.labelSmall,
                            color = PlatinumLight,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            /* ── Info ── */
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = video.title.removeSuffix(".mp4").removeSuffix(".mkv")
                        .removeSuffix(".avi").removeSuffix(".mov"),
                    style = MaterialTheme.typography.titleSmall,
                    color = PlatinumLight,
                    fontWeight   = FontWeight.SemiBold,
                    maxLines     = 2,
                    overflow     = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    MetaBadge(formatFileSize(video.size))
                    if (video.mimeType != null) {
                        val format = video.mimeType
                            .substringAfter("video/")
                            .uppercase()
                            .take(4)
                        MetaBadge(format)
                    }
                }
            }

            /* ── Right Controls ── */
            Column(
                verticalArrangement   = Arrangement.SpaceBetween,
                horizontalAlignment   = Alignment.CenterHorizontally
            ) {
                // Play button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(GoldSubtle, CircleShape)
                        .border(1.dp, GoldPrimary.copy(0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        modifier = Modifier.size(20.dp),
                        tint     = GoldPrimary
                    )
                }
                Spacer(Modifier.height(8.dp))
                // Delete
                IconButton(
                    onClick  = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp),
                        tint     = ErrorRed.copy(0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaBadge(text: String) {
    Box(
        modifier = Modifier
            .background(PlatinumSubtle, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text  = text,
            style = MaterialTheme.typography.labelSmall,
            color = PlatinumDark,
            letterSpacing = 0.5.sp
        )
    }
}
