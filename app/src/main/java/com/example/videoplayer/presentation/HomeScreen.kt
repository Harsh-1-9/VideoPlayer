package com.example.videoplayer.presentation

import android.Manifest
import android.os.Build
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import com.example.videoplayer.ui.theme.*
import com.google.accompanist.permissions.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(navController: NavHostController) {

    val permissionState = rememberMultiplePermissionsState(
        permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    )

    /* ── Entrance animations ── */
    val headerAlpha  = remember { Animatable(0f) }
    val headerOffset = remember { Animatable(40f) }
    val cardAlpha    = remember { Animatable(0f) }
    val cardOffset   = remember { Animatable(60f) }

    LaunchedEffect(Unit) {
        headerAlpha.animateTo(1f, tween(700, easing = EaseOut))
        headerOffset.animateTo(0f, tween(700, easing = EaseOutCubic))
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200)
        cardAlpha.animateTo(1f, tween(700, easing = EaseOut))
        cardOffset.animateTo(0f, tween(700, easing = EaseOutCubic))
    }

    /* ── Ambient glow pulse ── */
    val infinite = rememberInfiniteTransition(label = "home")
    val glowAlpha by infinite.animateFloat(
        0.3f, 0.7f,
        infiniteRepeatable(tween(3000, easing = EaseInOut), RepeatMode.Reverse),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF080700),
                        LuxuryBlack,
                        Color(0xFF080700)
                    )
                )
            )
    ) {

        /* ── Background glow blob ── */
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(x = (-80).dp, y = (-60).dp)
                .blur(120.dp)
                .background(
                    GoldPrimary.copy(alpha = glowAlpha * 0.08f),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .blur(100.dp)
                .background(
                    GoldDark.copy(alpha = glowAlpha * 0.06f),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 60.dp, start = 24.dp, end = 24.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            /* ── Header ── */
            Column(
                modifier = Modifier
                    .alpha(headerAlpha.value)
                    .offset(y = headerOffset.value.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App icon mark
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(GoldSubtle, CircleShape)
                        .border(1.dp, GoldPrimary.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        modifier = Modifier.size(38.dp),
                        tint = GoldPrimary
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "LUXE PLAYER",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(6.dp))

                // Gold divider
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, GoldPrimary, Color.Transparent)
                            )
                        )
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Browse & play your videos with style",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PlatinumMid,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(48.dp))

            /* ── Section Label ── */
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MEDIA LIBRARY",
                    style = MaterialTheme.typography.labelMedium,
                    color = GoldPrimary,
                    letterSpacing = 2.sp,
                    modifier = Modifier
                        .alpha(cardAlpha.value)
                        .offset(y = cardOffset.value.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                        .height(1.dp)
                        .alpha(cardAlpha.value)
                        .background(
                            Brush.horizontalGradient(
                                listOf(GoldPrimary.copy(0.4f), Color.Transparent)
                            )
                        )
                )
            }

            Spacer(Modifier.height(16.dp))

            /* ── Video Gallery Card (NEW) ── */
            LuxuryMediaCard(
                icon = Icons.Default.Collections,
                title = "Video Gallery",
                subtitle = if (permissionState.allPermissionsGranted)
                    "Browse videos by albums, folders & grid view"
                else
                    "Tap to allow access & view gallery",
                badge = "GALLERY",
                badgeGold = true,
                modifier = Modifier
                    .alpha(cardAlpha.value)
                    .offset(y = cardOffset.value.dp),
                onClick = {
                    if (permissionState.allPermissionsGranted) {
                        navController.navigate("gallery")
                    } else {
                        permissionState.launchMultiplePermissionRequest()
                    }
                }
            )

            Spacer(Modifier.height(16.dp))

            /* ── Local Media Card ── */
            LuxuryMediaCard(
                icon = Icons.Default.VideoLibrary,
                title = "All Videos List",
                subtitle = if (permissionState.allPermissionsGranted)
                    "Browse all videos in a clean list view"
                else
                    "Tap to grant access to your media",
                badge = if (permissionState.allPermissionsGranted) "READY" else "PERMISSION",
                badgeGold = permissionState.allPermissionsGranted,
                modifier = Modifier
                    .alpha(cardAlpha.value)
                    .offset(y = cardOffset.value.dp),
                onClick = {
                    if (permissionState.allPermissionsGranted) {
                        navController.navigate("localMedia")
                    } else {
                        permissionState.launchMultiplePermissionRequest()
                    }
                }
            )

            Spacer(Modifier.height(16.dp))

            /* ── Quick Access card ── */
            LuxuryStatsCard(
                modifier = Modifier
                    .alpha(cardAlpha.value)
                    .offset(y = cardOffset.value.dp),
                onAllVideosClick = {
                    if (permissionState.allPermissionsGranted) {
                        navController.navigate("localMedia")
                    } else {
                        permissionState.launchMultiplePermissionRequest()
                    }
                },
                onHdContentClick = {
                    if (permissionState.allPermissionsGranted) {
                        navController.navigate("gallery/hd")
                    } else {
                        permissionState.launchMultiplePermissionRequest()
                    }
                },
                onRecentClick = {
                    if (permissionState.allPermissionsGranted) {
                        navController.navigate("gallery/recent")
                    } else {
                        permissionState.launchMultiplePermissionRequest()
                    }
                }
            )
        }
    }
}

@Composable
private fun LuxuryMediaCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    badge: String,
    badgeGold: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isHovered by interactionSource.collectIsPressedAsState()

    val elevation by animateDpAsState(
        if (isHovered) 0.dp else 4.dp,
        label = "elevation"
    )
    val scale by animateFloatAsState(
        if (isHovered) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1A1600),
                        Color(0xFF120F00),
                        Color(0xFF1A1600)
                    ),
                    start = Offset(0f, 0f),
                    end   = Offset(1000f, 600f)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        GoldPrimary.copy(0.6f),
                        GoldPrimary.copy(0.1f),
                        GoldPrimary.copy(0.3f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Icon box
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(GoldSubtle, RoundedCornerShape(16.dp))
                    .border(1.dp, GoldPrimary.copy(0.3f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint = GoldPrimary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = PlatinumLight,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = PlatinumDark
                )
                Spacer(Modifier.height(10.dp))
                // Badge
                Box(
                    modifier = Modifier
                        .background(
                            if (badgeGold) GoldSubtle else PlatinumSubtle,
                            RoundedCornerShape(4.dp)
                        )
                        .border(
                            0.5.dp,
                            if (badgeGold) GoldPrimary.copy(0.5f) else PlatinumDark.copy(0.5f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (badgeGold) GoldLight else PlatinumMid,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = GoldPrimary.copy(0.7f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun LuxuryStatsCard(
    modifier: Modifier = Modifier,
    onAllVideosClick: () -> Unit = {},
    onHdContentClick: () -> Unit = {},
    onRecentClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        LuxuryCardSurface,
                        LuxuryElevated
                    )
                )
            )
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "QUICK ACCESS",
                style = MaterialTheme.typography.labelMedium,
                color = GoldPrimary,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickStat(
                    icon = Icons.Default.Movie,
                    label = "All Videos",
                    onClick = onAllVideosClick
                )
                QuickStat(
                    icon = Icons.Default.Hd,
                    label = "HD Content",
                    onClick = onHdContentClick
                )
                QuickStat(
                    icon = Icons.Default.AccessTime,
                    label = "Recent",
                    onClick = onRecentClick
                )
            }
        }
    }
}

@Composable
private fun QuickStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.88f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "quickStatScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(
                    if (isPressed) GoldSubtle else PlatinumSubtle,
                    CircleShape
                )
                .border(
                    width = 1.dp,
                    color = if (isPressed) GoldPrimary else GlassBorder,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = if (isPressed) GoldLight else GoldPrimary
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = if (isPressed) GoldLight else PlatinumLight
        )
    }
}
